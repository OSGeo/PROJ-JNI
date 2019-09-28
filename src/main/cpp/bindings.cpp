/*
 * Copyright © 2019 Agency for Data Supply and Efficiency
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
#include <assert.h>
#include <string>
#include <proj.h>
#include "proj/io.hpp"
#include "org_kortforsyningen_proj_Context.h"
#include "org_kortforsyningen_proj_ObjectReference.h"
#include "org_kortforsyningen_proj_AuthorityFactory.h"

using osgeo::proj::io::DatabaseContext;
using osgeo::proj::io::DatabaseContextNNPtr;
using osgeo::proj::io::AuthorityFactory;
using osgeo::proj::io::AuthorityFactoryPtr;




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                          HELPER FUNCTIONS (not invoked from Java)                          │
// └────────────────────────────────────────────────────────────────────────────────────────────┘

#define PJ_FIELD_NAME "ptr"
#define PJ_FIELD_TYPE "J"

/*
 * NOTE ON CHARACTER ENCODING: this implementation assumes that the PROJ library expects strings
 * encoded in UTF-8, regardless the platform encoding. Consequently we use the JNI "StringUTF"
 * functions directly. It is not completely appropriate because JNI functions use a modified UTF-8,
 * but it should be okay if the strings do not use the null character (0) or the supplementary
 * characters (the ones encoded on 4 bytes in a UTF-8 string).
 */

/**
 * Sends the given text to java.lang.System.Logger for the PROJ package.
 * We use this function for debugging purposes only. Use of Java logger
 * instead of C++ std::cout is for avoiding conflicts caused by different
 * languages writing to the same standard output stream.
 *
 * If this function can not print, than it silently ignores the given text.
 * Such failure could happen if the Java method has moved but this C++
 * function has not been updated accordingly.
 *
 * @param  env   The JNI environment.
 * @param  text  The text to log. Shall not be null.
 */
void log(JNIEnv *env, const std::string &text) {
    jclass c = env->FindClass("org/kortforsyningen/proj/ObjectReference");
    if (c) {
        jmethodID method = env->GetStaticMethodID(c, "log", "(Ljava/lang/String;)V");
        if (method) {
            jstring str = env->NewStringUTF(text.c_str());
            if (str) {
                env->CallStaticVoidMethod(c, method, str);
            }
        }
    }
    // Java exception already thrown if any above JNI functions failed.
}


/**
 * Casts the given address as a pointer to PJ_CONTEXT.
 * This function is defined for type safety.
 *
 * @param  ctxPtr  The address of the PJ_CONTEXT for the current thread.
 * @return The given ctxPtr as a pointer to PJ_CONTEXT, or NULL if ctxPtr is zero.
 */
inline PJ_CONTEXT* as_context(jlong ctxPtr) {
    return reinterpret_cast<PJ_CONTEXT*>(ctxPtr);
}


/**
 * Wraps the given shared pointer in a memory block that can be referenced from a Java object.
 * We can not store shared pointer directly in Java object because we need a 64 bits pointer,
 * while shared objects are bigger (128 bits in our tests). This function copies the shared pointer
 * in a newly allocated block and returns the memory address of that block. We may revisit this
 * strategy in a future version if we find a way to store directly shared pointer in a Java object
 * without this indirection level.
 *
 * After return from this function, object.use_count() while have been increased by one, unless the
 * application run out of memory in which case the use count is unchanged and this function returns 0.
 *
 * @param  object  The object to wrap in a memory block that can be referenced from a Java object.
 * @return Address to store in the Java object, or 0 if out of memory.
 */
template <class T> jlong wrap_shared_ptr(std::shared_ptr<T> &object) {
    std::shared_ptr<T> *wrapper = reinterpret_cast<std::shared_ptr<T>*>(calloc(1, sizeof(std::shared_ptr<T>)));
    if (wrapper) {
        *wrapper = object;          // This assignation also increases object.use_count() by one.
    }
    static_assert(sizeof(wrapper) <= sizeof(jlong), "Can not store pointer in a jlong.");
    return reinterpret_cast<jlong>(wrapper);
}


/**
 * Returns the shared pointer stored in the memory block at the given address.
 * This function does not release the memory block. It can be invoked from C++
 * code like below:
 *
 *   AuthorityFactoryPtr factory = unwrap_shared_ptr<AuthorityFactory>(ptr);
 *
 * @param  ptr  Address returned by wrap_shared_ptr(…). Shall not be zero.
 * @return Shared pointer which was wrapped in the specified memory block.
 * @throw  std::invalid_argument if the given value is zero.
 */
template <class T> std::shared_ptr<T> unwrap_shared_ptr(jlong ptr) {
    if (ptr) {
        std::shared_ptr<T> *wrapper = reinterpret_cast<std::shared_ptr<T>*>(ptr);
        return *wrapper;
    }
    throw std::invalid_argument("Null C/C++ pointer");
}


/**
 * Frees the memory block wrapping the shared pointer.
 * The use count of that shared pointer is decreased by one.
 * This method does nothing if the memory block has already been released
 * (it would be a bug if it happens, but we nevertheless try to be safe).
 *
 * @param  ptr  Address returned by wrap_shared_ptr(…).
 */
template <class T> void release_shared_ptr(jlong ptr) {
    if (ptr) {
        std::shared_ptr<T> *wrapper = reinterpret_cast<std::shared_ptr<T>*>(ptr);
        *wrapper = nullptr;     // This assignation decreases object.use_count().
        free(wrapper);
    }
}


/**
 * Gets the value of the `ptr` field of given object and sets that value to zero.
 * This method is invoked for implementation of `release()` or `destroy()` methods.
 * In theory we are not allowed to change the value of a final field. But no Java
 * code should use this field and the Java object should be garbage collected soon
 * anyway. We set this field to zero because the consequence of accidentally uses
 * of outdated value from C++ code is potentially worst.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the PROJ structure (not allowed to be NULL).
 * @return The address of the PROJ structure, or NULL if the operation fails
 *         (for example because the `ptr` field has not been found).
 */
jlong get_and_clear_ptr(JNIEnv *env, jobject object) {
    jfieldID id = env->GetFieldID(env->GetObjectClass(object), PJ_FIELD_NAME, PJ_FIELD_TYPE);
    if (id) {
        jlong ptr = env->GetLongField(object, id);
        env->SetLongField(object, id, (jlong) 0);
        return ptr;
    }
    // java.lang.NoSuchFieldError already thrown by GetFieldID(…).
    return 0;
}




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                       CLASS Context                                        │
// └────────────────────────────────────────────────────────────────────────────────────────────┘


/**
 * Allocates a PJ_CONTEXT for using PROJ in a multi-threads environment.
 * Each thread should have its own PJ_CONTEXT instance.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this function has been invoked.
 * @return The address of the new PJ_CONTEXT structure, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_kortforsyningen_proj_Context_create(JNIEnv *env, jclass caller) {
    static_assert(sizeof(PJ_CONTEXT*) <= sizeof(jlong), "Can not store PJ_CONTEXT* in a jlong.");
    PJ_CONTEXT *ctx = proj_context_create();
    return reinterpret_cast<jlong>(ctx);
}


/**
 * Releases a PJ_CONTEXT. This function sets the `ptr` field in the Java object to zero as a safety
 * in case there is two attempts to destroy the same object.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the context to release.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_Context_destroyPJ(JNIEnv *env, jobject object) {
    jlong ctxPtr = get_and_clear_ptr(env, object);
    proj_context_destroy(as_context(ctxPtr));           // Does nothing if ctxPtr is null.
}




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                   CLASS ObjectReference                                    │
// └────────────────────────────────────────────────────────────────────────────────────────────┘


/**
 * Returns the PROJ release number.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this function has been invoked.
 * @return The PROJ release number, or NULL.
 */
JNIEXPORT jstring JNICALL Java_org_kortforsyningen_proj_ObjectReference_version(JNIEnv *env, jclass caller) {
    const char *desc = pj_release;
    return (desc) ? env->NewStringUTF(desc) : NULL;
}




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                   CLASS AuthorityFactory                                   │
// └────────────────────────────────────────────────────────────────────────────────────────────┘


/**
 * Allocates a osgeo::proj::io::AuthorityFactory.
 * The factory should be used by only one thread at a time.
 *
 * @param  env        The JNI environment.
 * @param  caller     The class from which this function has been invoked.
 * @param  ctxPtr     The address of the PJ_CONTEXT for the current thread.
 * @param  authority  Name of the authority for which to create the factory.
 * @param  sibling    if another factory has been created for the same context, that factory.
 *                    Otherwise zero. This is used for sharing the same database context.
 * @return The address of the new authority factory, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_newInstance
    (JNIEnv *env, jclass caller, jlong ctxPtr, jstring authority, jlong sibling)
{
    const char *authority_utf = env->GetStringUTFChars(authority, NULL);
    if (authority_utf) {
        try {
            DatabaseContextNNPtr db = (sibling)
                    ? unwrap_shared_ptr<AuthorityFactory>(sibling)->databaseContext()
                    : DatabaseContext::create(std::string(), std::vector<std::string>(), as_context(ctxPtr));
            AuthorityFactoryPtr factory = AuthorityFactory::create(db, authority_utf).as_nullable();
            return wrap_shared_ptr(factory);
        } catch (const std::exception &e) {
            jclass c = env->FindClass("org/opengis/util/FactoryException");
            if (c) env->ThrowNew(c, e.what());
        }
        env->ReleaseStringUTFChars(authority, authority_utf);       // Must be after the catch block in case an exception happens.
    }
    return 0;
}


/**
 * Releases the osgeo::proj::io::AuthorityFactory wrapped by the given Java object.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the authority factory to release.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_release(JNIEnv *env, jobject object) {
    jlong ptr = get_and_clear_ptr(env, object);
    release_shared_ptr<AuthorityFactory>(ptr);
}
