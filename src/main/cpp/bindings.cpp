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
#include "proj/crs.hpp"
#include "org_kortforsyningen_proj_NativeResource.h"
#include "org_kortforsyningen_proj_Context.h"
#include "org_kortforsyningen_proj_AuthorityFactory.h"
#include "org_kortforsyningen_proj_WKTFormat$Convention.h"

using osgeo::proj::io::DatabaseContext;
using osgeo::proj::io::DatabaseContextNNPtr;
using osgeo::proj::io::AuthorityFactory;
using osgeo::proj::io::AuthorityFactoryPtr;
using osgeo::proj::io::WKTFormatter;
using osgeo::proj::io::WKTFormatterNNPtr;
using osgeo::proj::util::BaseObject;
using osgeo::proj::util::BaseObjectPtr;
using osgeo::proj::io::IWKTExportable;




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                       INITIALIZATION                                       │
// └────────────────────────────────────────────────────────────────────────────────────────────┘

/**
 * Identifier of the Java field which will contain the pointer to PROJ structure in Java class.
 * We get this field at initialization time and reuse it every time we need the pointer value.
 * According JNI specification, jfieldID and jmethodID are valid until the class is unloaded.
 *
 * In principle we should keep a reference to NativeResource class for preventing unloading.
 * We don't on the assumption that if the class was unloaded, next use of PROJ would require
 * reloading the class and initialize it again, in which case the values below would be updated.
 *
 * We use this identifier in calls to `env->GetLongField(object, java_field_for_pointer)` where
 * `object` can be a subclass of NativeResource. The JNI specification does not said explicitly
 * if `env->GetField` is compatible with class inheritance or if we need to get a new `jfieldID`
 * for each specific class, but tests suggest that inheritance works.
 */
jfieldID java_field_for_pointer;


/**
 * Invoked at initialization time for setting the values of global variables.
 * This function must be invoked from the class which contains the "ptr" field.
 * If this operation fails, a NoSuchFieldError will be thrown in Java code.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this function has been invoked.
 *                 Must be the class containing the pointer fields.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_NativeResource_initialize(JNIEnv *env, jclass caller) {
    java_field_for_pointer = env->GetFieldID(caller, "ptr", "J");
}




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                          HELPER FUNCTIONS (not invoked from Java)                          │
// └────────────────────────────────────────────────────────────────────────────────────────────┘

#define JPJ_FACTORY_EXCEPTION          "org/opengis/util/FactoryException"
#define JPJ_NO_SUCH_AUTHORITY_CODE     "org/opengis/referencing/NoSuchAuthorityCodeException"
#define JPJ_FORMATTING_EXCEPTION       "org/kortforsyningen/proj/FormattingException"
#define JPJ_ILLEGAL_ARGUMENT_EXCEPTION "java/lang/IllegalArgumentException"

/*
 * NOTE ON CHARACTER ENCODING: this implementation assumes that the PROJ library expects strings
 * encoded in UTF-8, regardless the platform encoding. Consequently we use the JNI "StringUTF"
 * functions directly. It is not completely appropriate because JNI functions use a modified UTF-8,
 * but it should be okay if the strings do not use the null character (0) or the supplementary
 * characters (the ones encoded on 4 bytes in a UTF-8 string).
 */


/**
 * Converts the given C++ string into a Java string if non-empty, or returns null if the string is empty.
 * This method assumes UTF-8 encoding with no null character and no supplementary Unicode characters.
 *
 * @param  env   The JNI environment.
 * @param  text  The text to convert to Java string.
 * @return the Java string, or null if the given text is empty.
 */
inline jstring non_empty_string(JNIEnv *env, const std::string &text) {
    return text.empty() ? nullptr : env->NewStringUTF(text.c_str());
}


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
 * @param  text  The text to log.
 */
void log(JNIEnv *env, const std::string &text) {
    jclass c = env->FindClass("org/kortforsyningen/proj/NativeResource");
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
 * anyway. We set this field to zero because the consequence of accidentally using
 * an outdated value from C++ code is potentially worst.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the PROJ structure (not allowed to be null).
 * @return The address of the PROJ structure, or null if the operation fails
 *         (for example because the `ptr` field has not been found).
 */
jlong get_and_clear_ptr(JNIEnv *env, jobject object) {
    jlong ptr = env->GetLongField(object, java_field_for_pointer);
    env->SetLongField(object, java_field_for_pointer, (jlong) 0);
    return ptr;
}


/**
 * Returns the shared pointer stored in the memory block referenced by the `ptr`
 * field in the given Java object. This function does not release the memory block
 * and does not alter the Java field. It can be invoked from C++ code like below:
 *
 *   try {
 *       AuthorityFactoryPtr factory = get_and_unwrap_ptr<AuthorityFactory>(env, object);
 *   } catch (const std::exception &e) {
 *       rethrow_as_some_java_exception(env, e);
 *   }
 *
 * This method never returns null. Instead, a C++ exception is thrown if the pointer is missing.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the PROJ structure.
 * @return Shared pointer to the PROJ object associated to the given Java object.
 * @throw  std::invalid_argument if the `ptr` field in the Java object is zero.
 */
template <class T> std::shared_ptr<T> get_and_unwrap_ptr(JNIEnv *env, jobject object) {
    if (object) {
        jlong ptr = env->GetLongField(object, java_field_for_pointer);
        if (ptr) {
            std::shared_ptr<T> *wrapper = reinterpret_cast<std::shared_ptr<T>*>(ptr);
            return *wrapper;
        }
    }
    throw std::invalid_argument("Null pointer to PROJ object.");
}


/**
 * Rethrows the given C++ exception as an org.opengis.util.FactoryException with the same message.
 * If a Java exception is already pending (this may happen if the exception was thrown by the JNI
 * framework), then this method does nothing. This method returns normally; the exception will be
 * thrown only when execution returns to Java code.
 *
 * @param  env  The JNI environment.
 * @param  e    The C++ exception to rethrow in Java.
 */
void rethrow_as_factory_exception(JNIEnv *env, const std::exception &e) {
    if (!env->ExceptionCheck()) {
        jclass c = env->FindClass(JPJ_FACTORY_EXCEPTION);
        if (c) env->ThrowNew(c, e.what());
        // If c was null, the appropriate Java exception is thrown by JNI.
    }
}


/**
 * Wraps the given PROJ object into the most specific Java object provided by the PROJ-JNI bindings.
 * This method tries to find a more specialized type for the given object, then calls the Java method
 * `wrapGeodeticObject(…)` with that type in argument. If the type is unknown, then this method returns
 * null and an exception is thrown in Java code.
 *
 * @param  env     The JNI environment.
 * @param  object  Shared pointer to wrap.
 * @param  type    Base type of the object to wrap. This method will use a more specialized type if possible.
 * @return Wrapper for a PROJ object, or null if out of memory.
 */
jobject specific_subclass(JNIEnv *env, BaseObjectPtr &object, jint type) {
rd: switch (type) {
        case org_kortforsyningen_proj_AuthorityFactory_ANY: {
            BaseObject *ptr = object.get();
                 if (dynamic_cast<osgeo::proj::crs::CRS                       *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_COORDINATE_REFERENCE_SYSTEM;
            else if (dynamic_cast<osgeo::proj::operation::CoordinateOperation *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_COORDINATE_OPERATION;
            else if (dynamic_cast<osgeo::proj::datum::Datum                   *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_DATUM;
            else if (dynamic_cast<osgeo::proj::datum::Ellipsoid               *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_ELLIPSOID;
            else if (dynamic_cast<osgeo::proj::datum::PrimeMeridian           *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_PRIME_MERIDIAN;
            else if (dynamic_cast<osgeo::proj::cs::CoordinateSystem           *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_COORDINATE_SYSTEM;
            else if (dynamic_cast<osgeo::proj::common::UnitOfMeasure          *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_UNIT_OF_MEASURE;
            else break;
            goto rd;
        }
        case org_kortforsyningen_proj_AuthorityFactory_COORDINATE_OPERATION: {
            BaseObject *ptr = object.get();
            if (dynamic_cast<osgeo::proj::operation::Conversion *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_CONVERSION;
            break;
        }
        case org_kortforsyningen_proj_AuthorityFactory_COORDINATE_REFERENCE_SYSTEM: {
            BaseObject *ptr = object.get();
                 if (dynamic_cast<osgeo::proj::crs::CompoundCRS   *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_COMPOUND_CRS;
            else if (dynamic_cast<osgeo::proj::crs::ProjectedCRS  *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_PROJECTED_CRS;
            else if (dynamic_cast<osgeo::proj::crs::GeographicCRS *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_GEOGRAPHIC_CRS;
            else if (dynamic_cast<osgeo::proj::crs::GeodeticCRS   *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_GEODETIC_CRS;
            else if (dynamic_cast<osgeo::proj::crs::VerticalCRS   *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_VERTICAL_CRS;
            break;
        }
        case org_kortforsyningen_proj_AuthorityFactory_DATUM: {
            BaseObject *ptr = object.get();
                 if (dynamic_cast<osgeo::proj::datum::GeodeticReferenceFrame *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_GEODETIC_REFERENCE_FRAME;
            else if (dynamic_cast<osgeo::proj::datum::VerticalReferenceFrame *>(ptr)) type = org_kortforsyningen_proj_AuthorityFactory_VERTICAL_REFERENCE_FRAME;
            break;
        }
    }
    /*
     * At this point `type` is either unchanged, or modified to a more specialized code reflecting the
     * actual PROJ object type. Now delegate to wrapGeodeticObject(…) Java method for creating the Java
     * object of that type. If a Java exception is thrown, we release the PROJ resource and return null.
     * The exception will be propagated in Java code.
     */
    jclass c = env->FindClass("org/kortforsyningen/proj/AuthorityFactory");
    if (c) {
        jmethodID method = env->GetStaticMethodID(c, "wrapGeodeticObject", "(IJ)Lorg/kortforsyningen/proj/IdentifiableObject;");
        if (method) {
            jlong ptr = wrap_shared_ptr(object);
            jobject result = env->CallStaticObjectMethod(c, method, type, ptr);
            if (result && !env->ExceptionCheck()) {
                return result;
            }
            release_shared_ptr<BaseObject>(ptr);
        }
    }
    return nullptr;
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
 * Returns the pointer to PJ_CONTEXT for the given Context object in Java.
 *
 * @param  env      The JNI environment.
 * @param  context  The Context object for the current thread.
 * @return The pointer to PJ_CONTEXT, or null if none.
 */
inline PJ_CONTEXT* get_context(JNIEnv *env, jobject context) {
    jlong ctxPtr = env->GetLongField(context, java_field_for_pointer);
    return reinterpret_cast<PJ_CONTEXT*>(ctxPtr);
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
    proj_context_destroy(reinterpret_cast<PJ_CONTEXT*>(ctxPtr));    // Does nothing if ctxPtr is null.
}


/**
 * Instantiate a geodetic object from a user specified text.
 * The returned object will typically by a subtype of CoordinateReferenceSystem.
 */
JNIEXPORT jobject JNICALL Java_org_kortforsyningen_proj_Context_createFromUserInput(JNIEnv *env, jobject object, jstring text) {
    BaseObjectPtr result = nullptr;
    const char *text_utf = env->GetStringUTFChars(text, nullptr);
    if (text_utf) {
        PJ_CONTEXT *ctx = get_context(env, object);
        try {
            result = osgeo::proj::io::createFromUserInput(text_utf, ctx).as_nullable();
        } catch (const std::exception &e) {
            jclass c = env->FindClass(JPJ_FACTORY_EXCEPTION);
            if (c) env->ThrowNew(c, e.what());
        }
        env->ReleaseStringUTFChars(text, text_utf);     // Must be after the catch block in case an exception happens.
    }
    if (result) {
        return specific_subclass(env, result, org_kortforsyningen_proj_AuthorityFactory_ANY);
    }
    return nullptr;
}




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                    CLASS NativeResource                                    │
// └────────────────────────────────────────────────────────────────────────────────────────────┘


/**
 * Returns the PROJ release number.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this function has been invoked.
 * @return The PROJ release number, or null.
 */
JNIEXPORT jstring JNICALL Java_org_kortforsyningen_proj_NativeResource_version(JNIEnv *env, jclass caller) {
    const char *desc = pj_release;
    return (desc) ? env->NewStringUTF(desc) : nullptr;
}


/**
 * Returns a Well-Known Text (WKT) for this object.
 * This is allowed only if this object implements osgeo::proj::io::IWKTExportable.
 *
 * @param  env         The JNI environment.
 * @param  object      The Java object wrapping the PROJ object to format.
 * @param  convention  One of WKTFormat constants.
 * @return The Well-Known Text (WKT) for this object, or null if the object is not IWKTExportable.
 */
JNIEXPORT jstring JNICALL Java_org_kortforsyningen_proj_NativeResource_toWKT
    (JNIEnv *env, jobject object, jint convention, jboolean multiline, jboolean strict)
{
    WKTFormatter::Convention c;
    switch (convention) {
        case WKTFormat_WKT2_2019:            c = WKTFormatter::Convention::WKT2_2018;            break;      // TODO: rename "2018" as "2019" in next PROJ release.
        case WKTFormat_WKT2_2015:            c = WKTFormatter::Convention::WKT2_2015;            break;
        case WKTFormat_WKT2_2019_SIMPLIFIED: c = WKTFormatter::Convention::WKT2_2018_SIMPLIFIED; break;
        case WKTFormat_WKT2_2015_SIMPLIFIED: c = WKTFormatter::Convention::WKT2_2015_SIMPLIFIED; break;
        case WKTFormat_WKT1_GDAL:            c = WKTFormatter::Convention::WKT1_GDAL;            break;
        case WKTFormat_WKT1_ESRI:            c = WKTFormatter::Convention::WKT1_ESRI;            break;
        default: {
            jclass c = env->FindClass(JPJ_ILLEGAL_ARGUMENT_EXCEPTION);
            if (c) env->ThrowNew(c, std::to_string(convention).c_str());
            return nullptr;
        }
    }
    try {
        BaseObjectPtr candidate = get_and_unwrap_ptr<BaseObject>(env, object);
        std::shared_ptr<IWKTExportable> exportable = std::dynamic_pointer_cast<IWKTExportable>(candidate);
        if (exportable) {
            WKTFormatterNNPtr formatter = WKTFormatter::create(c);
            formatter->setMultiLine(multiline);
            formatter->setStrict(strict);
            return non_empty_string(env, exportable->exportToWKT(formatter.get()));
        }
    } catch (const std::exception &e) {
        jclass c = env->FindClass(JPJ_FORMATTING_EXCEPTION);
        if (c) env->ThrowNew(c, e.what());
    }
    return nullptr;
}


/**
 * Decrements the references count of the shared pointer. This method is invoked automatically
 * when an instance of IdentifiableObject class is garbage collected.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the shared object to release.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_NativeResource_run(JNIEnv *env, jobject object) {
    jlong ptr = get_and_clear_ptr(env, object);
    release_shared_ptr<BaseObject>(ptr);
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
 * @param  context    The wrapper of the PJ_CONTEXT for the current thread.
 * @param  authority  Name of the authority for which to create the factory.
 * @param  sibling    if another factory has been created for the same context, that factory.
 *                    Otherwise zero. This is used for sharing the same database context.
 * @return The address of the new authority factory, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_newInstance
    (JNIEnv *env, jclass caller, jobject context, jstring authority, jobject sibling)
{
    jlong result = 0;
    const char *authority_utf = env->GetStringUTFChars(authority, nullptr);
    if (authority_utf) {
        try {
            DatabaseContextNNPtr db = (sibling)
                    ? get_and_unwrap_ptr<AuthorityFactory>(env, sibling)->databaseContext()
                    : DatabaseContext::create(std::string(), std::vector<std::string>(), get_context(env, context));
            AuthorityFactoryPtr factory = AuthorityFactory::create(db, authority_utf).as_nullable();
            result = wrap_shared_ptr(factory);
        } catch (const std::exception &e) {
            jclass c = env->FindClass(JPJ_FACTORY_EXCEPTION);
            if (c) env->ThrowNew(c, e.what());
        }
        env->ReleaseStringUTFChars(authority, authority_utf);       // Must be after the catch block in case an exception happens.
    }
    return result;
}


/**
 * Releases the osgeo::proj::io::AuthorityFactory wrapped by the given Java object.
 * This method decrements the object.use_count() value of the shared pointer.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the authority factory to release.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_release(JNIEnv *env, jobject object) {
    jlong ptr = get_and_clear_ptr(env, object);
    release_shared_ptr<AuthorityFactory>(ptr);
}


/**
 * Rethrows the given C++ exception as a Java exception with the same message, authority name
 * and authority code. This method returns normally; the exception will be thrown in Java only.
 *
 * @param  env  The JNI environment.
 * @param  e    The C++ exception to rethrow in Java.
 */
void rethrow_as_java_exception(JNIEnv *env, const osgeo::proj::io::NoSuchAuthorityCodeException &e) {
    jclass c = env->FindClass(JPJ_NO_SUCH_AUTHORITY_CODE);
    if (c) {
        jmethodID method = env->GetMethodID(c, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
        if (method) {
            const char *message = e.what();
            jobject jt = env->NewObject(c, method, message ? env->NewStringUTF(message) : nullptr,
                                                   non_empty_string(env, e.getAuthority()),
                                                   non_empty_string(env, e.getAuthorityCode()));
            if (jt) env->Throw(static_cast<jthrowable>(jt));
        }
    }
    // If any of above tests failed, JNI will have throw the appropriate exception in Java.
}


/**
 * Gets a description of the object corresponding to a code.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the authority factory to use.
 * @param  code    Object code allocated by authority.
 * @return Description of the identified object, or null if that object has no description.
 * @throws FactoryException if the description can not be obtained for the given code.
 */
JNIEXPORT jstring JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_getDescriptionText
    (JNIEnv *env, jobject object, jstring code)
{
    jstring result = nullptr;
    const char *code_utf = env->GetStringUTFChars(code, nullptr);
    if (code_utf) {
        try {
            AuthorityFactoryPtr factory = get_and_unwrap_ptr<AuthorityFactory>(env, object);
            std::string desc = factory->getDescriptionText(code_utf);
            if (!desc.empty()) result = env->NewStringUTF(desc.c_str());
        } catch (const osgeo::proj::io::NoSuchAuthorityCodeException &e) {
            rethrow_as_java_exception(env, e);
        } catch (const std::exception &e) {
            rethrow_as_factory_exception(env, e);
        }
        env->ReleaseStringUTFChars(code, code_utf);
    }
    return result;
}


/**
 * Returns the wrapper for an osgeo::proj::common::IdentifiedObject from the specified code.
 * The PROJ method invoked by this function is determined by the type argument.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the authority factory to use.
 * @param  type    One of {@link #ELLIPSOID}, {@link #PRIME_MERIDIAN}, etc. constants.
 * @param  code    Object code allocated by authority.
 * @return Wrapper for a PROJ object, or null if out of memory.
 * @throws FactoryException if no object can be created for the given code.
 */
JNIEXPORT jobject JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_createGeodeticObject
    (JNIEnv *env, jobject object, jint type, jstring code)
{
    const char *code_utf = env->GetStringUTFChars(code, nullptr);
    if (code_utf) {
        BaseObjectPtr rp = nullptr;
        try {
            AuthorityFactoryPtr factory = get_and_unwrap_ptr<AuthorityFactory>(env, object);
            switch (type) {
                case org_kortforsyningen_proj_AuthorityFactory_ANY:                         rp = factory->createObject                    (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_PRIME_MERIDIAN:              rp = factory->createPrimeMeridian             (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_ELLIPSOID:                   rp = factory->createEllipsoid                 (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_DATUM:                       rp = factory->createDatum                     (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_GEODETIC_REFERENCE_FRAME:    rp = factory->createGeodeticDatum             (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_VERTICAL_REFERENCE_FRAME:    rp = factory->createVerticalDatum             (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_UNIT_OF_MEASURE:             rp = factory->createUnitOfMeasure             (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_COORDINATE_SYSTEM:           rp = factory->createCoordinateSystem          (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_COORDINATE_REFERENCE_SYSTEM: rp = factory->createCoordinateReferenceSystem (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_GEODETIC_CRS:                rp = factory->createGeodeticCRS               (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_GEOGRAPHIC_CRS:              rp = factory->createGeographicCRS             (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_VERTICAL_CRS:                rp = factory->createVerticalCRS               (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_PROJECTED_CRS:               rp = factory->createProjectedCRS              (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_COMPOUND_CRS:                rp = factory->createCompoundCRS               (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_CONVERSION:                  rp = factory->createConversion                (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_COORDINATE_OPERATION:        rp = factory->createCoordinateOperation(code_utf, false).as_nullable(); break;
                default: {
                    jclass c = env->FindClass(JPJ_FACTORY_EXCEPTION);
                    if (c) env->ThrowNew(c, "Unsupported object type.");
                }
            }
        } catch (const osgeo::proj::io::NoSuchAuthorityCodeException &e) {
            rethrow_as_java_exception(env, e);
        } catch (const std::exception &e) {
            rethrow_as_factory_exception(env, e);
        }
        env->ReleaseStringUTFChars(code, code_utf);
        if (rp) {
            return specific_subclass(env, rp, type);
        }
    }
    return nullptr;
}
