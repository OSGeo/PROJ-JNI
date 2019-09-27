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
using osgeo::proj::io::AuthorityFactoryNNPtr;




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                           HELPER METHODS (not invoked from Java)                           │
// └────────────────────────────────────────────────────────────────────────────────────────────┘

/*
 * NOTE ON CHARACTER ENCODING: this implementation assumes that the PROJ library expects strings
 * encoded in UTF-8, regardless the platform encoding. Consequently we use the JNI "StringUTF"
 * methods directly. It is not completely appropriate because JNI methods use a modified UTF-8,
 * but it should be okay if the strings do not use the null character (0) or the supplementary
 * characters (the ones encoded on 4 bytes in a UTF-8 string).
 */

/**
 * Sends the given text to java.lang.System.Logger for the PROJ package.
 * We use this method for debugging purposes only. The use of Java logger
 * instead of C++ std::cout is for avoiding conflicts caused by different
 * languages writing to the same standard output stream.
 *
 * If this method can not print, than it silently ignores the given text.
 * Such failure could happen if the Java method has moved but this C++
 * code has not been updated accordingly.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this method has been invoked.
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
}


/**
 * Casts the given address as a pointer to PJ_CONTEXT.
 * This method is defined for type safety.
 *
 * @param  ctxPtr  The address of the PJ_CONTEXT for the current thread.
 * @return The given ctxPtr as a pointer to PJ_CONTEXT.
 */
inline PJ_CONTEXT* as_context(jlong ctxPtr) {
    return reinterpret_cast<PJ_CONTEXT*>(ctxPtr);
}




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                       CLASS Context                                        │
// └────────────────────────────────────────────────────────────────────────────────────────────┘


/**
 * Allocates a PJ_CONTEXT for using PROJ in a multi-threads environment.
 * Each thread should have its own PJ_CONTEXT instance.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this method has been invoked.
 * @return The address of the new PJ_CONTEXT structure, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_kortforsyningen_proj_Context_create(JNIEnv *env, jclass caller) {
    static_assert(sizeof(PJ_CONTEXT*) <= sizeof(jlong), "Can not store PJ_CONTEXT* in a jlong.");
    PJ_CONTEXT *ctx = proj_context_create();
    return reinterpret_cast<jlong>(ctx);
}


/**
 * Releases a PJ_CONTEXT.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this method has been invoked.
 * @param  ctxPtr  The address of the PJ_CONTEXT to release.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_Context_destroy(JNIEnv *env, jclass caller, jlong ctxPtr) {
    proj_context_destroy(as_context(ctxPtr));
}




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                   CLASS ObjectReference                                    │
// └────────────────────────────────────────────────────────────────────────────────────────────┘


/**
 * Returns the PROJ release number.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this method has been invoked.
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
 * @param  caller     The class from which this method has been invoked.
 * @param  ctxPtr     The address of the PJ_CONTEXT for the current thread.
 * @param  authority  Name of the authority for which to create the factory.
 * @return The address of the new authority factory, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_newInstance(JNIEnv *env, jclass caller, jlong ctxPtr, jstring authority) {
    const char *authority_utf = env->GetStringUTFChars(authority, NULL);
    if (authority_utf) {
        try {
            DatabaseContextNNPtr db = DatabaseContext::create(std::string(), std::vector<std::string>(), as_context(ctxPtr));
            AuthorityFactoryNNPtr factory = AuthorityFactory::create(db, authority_utf);
            // TODO: wrap the shared pointer.
        } catch (const std::exception &e) {
            jclass c = env->FindClass("org/opengis/util/FactoryException");
            if (c) env->ThrowNew(c, e.what());
        }
        env->ReleaseStringUTFChars(authority, authority_utf);       // Must be after the catch block in case an exception happens.
    }
    return 0;
}
