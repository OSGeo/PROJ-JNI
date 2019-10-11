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
#include "org_kortforsyningen_proj_Transform.h"

// TODO: remove after PROJ 6.3 release.
#include <../src/proj_experimental.h>

using osgeo::proj::io::DatabaseContext;
using osgeo::proj::io::DatabaseContextPtr;
using osgeo::proj::io::DatabaseContextNNPtr;
using osgeo::proj::io::AuthorityFactory;
using osgeo::proj::io::AuthorityFactoryPtr;
using osgeo::proj::io::WKTFormatter;
using osgeo::proj::io::WKTFormatterNNPtr;
using osgeo::proj::io::IWKTExportable;
using osgeo::proj::io::PROJStringFormatter;
using osgeo::proj::io::PROJStringFormatterNNPtr;
using osgeo::proj::util::BaseObject;
using osgeo::proj::util::BaseObjectPtr;
using osgeo::proj::crs::CRS;
using osgeo::proj::crs::CRSNNPtr;
using osgeo::proj::operation::CoordinateOperation;
using osgeo::proj::operation::CoordinateOperationPtr;
using osgeo::proj::operation::CoordinateOperationNNPtr;
using osgeo::proj::operation::CoordinateOperationFactory;
using osgeo::proj::operation::CoordinateOperationFactoryNNPtr;
using osgeo::proj::operation::CoordinateOperationContext;
using osgeo::proj::operation::CoordinateOperationContextNNPtr;




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
jfieldID  java_field_for_pointer;
jfieldID  java_field_debug_level;
jmethodID java_method_getLogger;
jmethodID java_method_log;


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
    if (java_field_for_pointer) {
        /*
         * Following fields and methods are used for logging purpose only.
         * If any operation fail, `java_method_getLogger` will be left to
         * null. We use that as a sentinel value for determining that the
         * logging system is not available.
         */
        jclass c = env->FindClass("java/lang/System$Logger");
        if (c) {
            java_method_log = env->GetMethodID(c, "log", "(Ljava/lang/System$Logger$Level;Ljava/lang/String;)V");
            if (java_method_log) {
                c = env->FindClass("java/lang/System$Logger$Level");
                if (c) {
                    java_field_debug_level = env->GetStaticFieldID(c, "DEBUG", "Ljava/lang/System$Logger$Level;");
                    if (java_field_debug_level) {
                        java_method_getLogger = env->GetStaticMethodID(caller, "logger", "()Ljava/lang/System$Logger;");
                    }
                }
            }
        }
    }
}


/**
 * Returns the identifier of the Context.database field. We current don't cache this field because
 * it is not used often. This function provides a single place if we want to revisit this choice
 * in the future.
 *
 * @param  env     The JNI environment.
 * @param  context An instance of the Context class.
 * @return The database field ID, or 0 if not found.
 *         In the later case, an exception will be thrown in Java code.
 */
inline jfieldID get_database_field(JNIEnv *env, jobject context) {
    return env->GetFieldID(env->GetObjectClass(context), "database", "J");
}




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                          HELPER FUNCTIONS (not invoked from Java)                          │
// └────────────────────────────────────────────────────────────────────────────────────────────┘

#define JPJ_FACTORY_EXCEPTION          "org/opengis/util/FactoryException"
#define JPJ_NO_SUCH_AUTHORITY_CODE     "org/opengis/referencing/NoSuchAuthorityCodeException"
#define JPJ_TRANSFORM_EXCEPTION        "org/opengis/referencing/operation/TransformException"
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
 * Design note: a simpler strategy would be to invoke a Java method which
 * would do most of the work done with JNI here. The inconvenient is that
 * the logger would report that helper Java method as the source of the
 * log message. By avoiding that alternative strategy, we get slightly
 * more informative log records from the logging system.
 *
 * @param  env   The JNI environment.
 * @param  text  The text to log.
 * @throw  std::exception if a problem occurred while logging the message.
 */
void log(JNIEnv *env, const std::string &text) {
    if (!java_method_getLogger) {
        return;                         // Logging system not available.
    }
    jclass c = env->FindClass("org/kortforsyningen/proj/NativeResource");
    if (c) {
        jobject logger = env->CallStaticObjectMethod(c, java_method_getLogger);
        if (!env->ExceptionCheck() && logger) {                         // ExceptionCheck() must be always invoked.
            c = env->FindClass("java/lang/System$Logger$Level");
            if (c) {
                jobject level = env->GetStaticObjectField(c, java_field_debug_level);
                jstring str = env->NewStringUTF(text.c_str());
                if (str) {
                    env->CallObjectMethod(logger, java_method_log, level, str);
                    if (!env->ExceptionCheck()) {
                        return;                             // Success.
                    }
                }
            }
        }
    }
    /*
     * Java exception already thrown if any above JNI functions failed.
     * But we also want a C++ exception in order to interrupt the caller.
     * We could consider that failure to log should be silently ignored,
     * but in this case a failure would be caused by a bug in above JNI calls,
     * not a problem with logging system. We want to be informed of such bugs.
     */
    throw std::exception();
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
 * Returns the shared pointer for the given ptr field value in a Java object.
 * The given ptr shall not be null (this is not verified).
 *
 * @param  ptr  Address returned by wrap_shared_ptr(…).
 * @return The shared pointer.
 */
template <class T> inline std::shared_ptr<T> unwrap_shared_ptr(jlong ptr) {
    std::shared_ptr<T> *wrapper = reinterpret_cast<std::shared_ptr<T>*>(ptr);
    return *wrapper;
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
            return unwrap_shared_ptr<T>(ptr);
        }
    }
    throw std::invalid_argument("Null pointer to PROJ object.");
}


/**
 * Rethrows the given C++ exception as a Java exception with the same message. If a Java exception
 * is already pending (this may happen if the exception was thrown by the JNI framework), then this
 * method does nothing. This method returns normally; the exception will be thrown only when execution
 * returns to Java code.
 *
 * @param  env   The JNI environment.
 * @param  type  Java class name of the exception to throw.
 * @param  e     The C++ exception to rethrow in Java.
 */
void rethrow_as_java_exception(JNIEnv *env, const char *type, const std::exception &e) {
    if (!env->ExceptionCheck()) {
        jclass c = env->FindClass(type);
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
            if (!env->ExceptionCheck() && result) {                                 // ExceptionCheck() must be always invoked.
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
 * Gets the database context from a given Context. The database is created when first needed
 * and will be released when destroyPJ(…) will be invoked.
 *
 * @param  env      The JNI environment.
 * @param  context  The Context object for the current thread.
 * @return Pointer to shared database.
 * @throw  std::exception if the database creation failed.
 */
DatabaseContextPtr get_database_context(JNIEnv *env, jobject context) {
    jfieldID fid = get_database_field(env, context);
    if (fid) {
        DatabaseContextPtr db;
        jlong dbPtr = env->GetLongField(context, fid);
        if (dbPtr) {
            db = unwrap_shared_ptr<DatabaseContext>(dbPtr);
        } else {
            log(env, "Creating PROJ database context.");
            db = DatabaseContext::create(std::string(), std::vector<std::string>(), get_context(env, context)).as_nullable();
            dbPtr = wrap_shared_ptr(db);
            env->SetLongField(context, fid, dbPtr);
        }
        return db;
    }
    throw std::exception();     // Should never happen.
}


/**
 * Releases a PJ_CONTEXT and its associated database context. This function sets the `ptr` and `database`
 * fields in the Java object to zero as a safety in case there is two attempts to destroy the same object.
 *
 * @param  env      The JNI environment.
 * @param  context  The Java object wrapping the context to release.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_Context_destroyPJ(JNIEnv *env, jobject context) {
    jfieldID fid = get_database_field(env, context);
    if (fid) {
        release_shared_ptr<DatabaseContext>(env->GetLongField(context, fid));
        env->SetLongField(context, fid, (jlong) 0);
    }
    jlong ctxPtr = get_and_clear_ptr(env, context);
    proj_context_destroy(reinterpret_cast<PJ_CONTEXT*>(ctxPtr));    // Does nothing if ctxPtr is null.
}


/**
 * Instantiate a geodetic object from a user specified text.
 * The returned object will typically by a subtype of CoordinateReferenceSystem.
 *
 * @param  env      The JNI environment.
 * @param  context  An instance of Context for the current thread.
 * @return the user specified object, or null if the operation failed.
 */
JNIEXPORT jobject JNICALL Java_org_kortforsyningen_proj_Context_createFromUserInput(JNIEnv *env, jobject context, jstring text) {
    BaseObjectPtr result = nullptr;
    const char *text_utf = env->GetStringUTFChars(text, nullptr);
    if (text_utf) {
        try {
            DatabaseContextNNPtr db = NN_CHECK_THROW(get_database_context(env, context));
            result = osgeo::proj::io::createFromUserInput(text_utf, db).as_nullable();
        } catch (const std::exception &e) {
            rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
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
        rethrow_as_java_exception(env, JPJ_FORMATTING_EXCEPTION, e);
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
 * @return The address of the new authority factory, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_newInstance
    (JNIEnv *env, jclass caller, jobject context, jstring authority)
{
    jlong result = 0;
    const char *authority_utf = env->GetStringUTFChars(authority, nullptr);
    if (authority_utf) {
        try {
            const std::string authority_str = authority_utf;
            DatabaseContextNNPtr db = NN_CHECK_THROW(get_database_context(env, context));
            AuthorityFactoryPtr factory = AuthorityFactory::create(db, authority_str).as_nullable();
            result = wrap_shared_ptr(factory);
            /*
             * Log a message at debug level about the factory we just created.
             * The -1 in use count is for ignoring the reference in this block.
             */
            log(env, "Created factory for \"" + authority_str + "\" authority."
                  + " Database context use count is " + std::to_string(db.as_nullable().use_count() - 1) + '.');
        } catch (const std::exception &e) {
            rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
        }
        env->ReleaseStringUTFChars(authority, authority_utf);       // Must be after the catch block in case an exception happens.
    }
    return result;
}


/**
 * Releases the osgeo::proj::io::AuthorityFactory wrapped by the given Java object.
 * This method decrements the object.use_count() value of the shared pointer.
 *
 * @param  env      The JNI environment.
 * @param  factory  The Java object wrapping the authority factory to release.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_release(JNIEnv *env, jobject factory) {
    jlong ptr = get_and_clear_ptr(env, factory);
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
 * @param  env      The JNI environment.
 * @param  factory  The Java object wrapping the authority factory to use.
 * @param  code     Object code allocated by authority.
 * @return Description of the identified object, or null if that object has no description.
 */
JNIEXPORT jstring JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_getDescriptionText
    (JNIEnv *env, jobject factory, jstring code)
{
    jstring result = nullptr;
    const char *code_utf = env->GetStringUTFChars(code, nullptr);
    if (code_utf) {
        try {
            AuthorityFactoryPtr pf = get_and_unwrap_ptr<AuthorityFactory>(env, factory);
            std::string desc = pf->getDescriptionText(code_utf);
            if (!desc.empty()) result = env->NewStringUTF(desc.c_str());
        } catch (const osgeo::proj::io::NoSuchAuthorityCodeException &e) {
            rethrow_as_java_exception(env, e);
        } catch (const std::exception &e) {
            rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
        }
        env->ReleaseStringUTFChars(code, code_utf);
    }
    return result;
}


/**
 * Returns the wrapper for an osgeo::proj::common::IdentifiedObject from the specified code.
 * The PROJ method invoked by this function is determined by the type argument.
 *
 * @param  env      The JNI environment.
 * @param  factory  The Java object wrapping the authority factory to use.
 * @param  type     One of {@link #ELLIPSOID}, {@link #PRIME_MERIDIAN}, etc. constants.
 * @param  code     Object code allocated by authority.
 * @return Wrapper for a PROJ object, or null if out of memory.
 */
JNIEXPORT jobject JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_createGeodeticObject
    (JNIEnv *env, jobject factory, jint type, jstring code)
{
    const char *code_utf = env->GetStringUTFChars(code, nullptr);
    if (code_utf) {
        BaseObjectPtr rp = nullptr;
        try {
            AuthorityFactoryPtr pf = get_and_unwrap_ptr<AuthorityFactory>(env, factory);
            switch (type) {
                case org_kortforsyningen_proj_AuthorityFactory_ANY:                         rp = pf->createObject                    (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_PRIME_MERIDIAN:              rp = pf->createPrimeMeridian             (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_ELLIPSOID:                   rp = pf->createEllipsoid                 (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_DATUM:                       rp = pf->createDatum                     (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_GEODETIC_REFERENCE_FRAME:    rp = pf->createGeodeticDatum             (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_VERTICAL_REFERENCE_FRAME:    rp = pf->createVerticalDatum             (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_UNIT_OF_MEASURE:             rp = pf->createUnitOfMeasure             (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_COORDINATE_SYSTEM:           rp = pf->createCoordinateSystem          (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_COORDINATE_REFERENCE_SYSTEM: rp = pf->createCoordinateReferenceSystem (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_GEODETIC_CRS:                rp = pf->createGeodeticCRS               (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_GEOGRAPHIC_CRS:              rp = pf->createGeographicCRS             (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_VERTICAL_CRS:                rp = pf->createVerticalCRS               (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_PROJECTED_CRS:               rp = pf->createProjectedCRS              (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_COMPOUND_CRS:                rp = pf->createCompoundCRS               (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_CONVERSION:                  rp = pf->createConversion                (code_utf).as_nullable(); break;
                case org_kortforsyningen_proj_AuthorityFactory_COORDINATE_OPERATION:        rp = pf->createCoordinateOperation(code_utf, false).as_nullable(); break;
                default: {
                    jclass c = env->FindClass(JPJ_FACTORY_EXCEPTION);
                    if (c) env->ThrowNew(c, "Unsupported object type.");
                }
            }
        } catch (const osgeo::proj::io::NoSuchAuthorityCodeException &e) {
            rethrow_as_java_exception(env, e);
        } catch (const std::exception &e) {
            rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
        }
        env->ReleaseStringUTFChars(code, code_utf);
        if (rp) {
            return specific_subclass(env, rp, type);
        }
    }
    return nullptr;
}


/**
 * Finds a list of coordinate operation between the given source and target CRS.
 * The operations are sorted with the most relevant ones first: by descending area
 * (intersection of the transformation area with the area of interest, or intersection
 * of the transformation with the area of use of the CRS), and by increasing accuracy.
 * Operations with unknown accuracy are sorted last, whatever their area.
 *
 * @param  env                The JNI environment.
 * @param  factory            The Java object wrapping the authority factory to use.
 * @param  sourceCRS          Input coordinate reference system.
 * @param  targetCRS          Output coordinate reference system.
 * @param  desiredAccuracy    Desired accuracy (in metres), or 0 for the best accuracy available.
 * @param  discardSuperseded  Whether transformations that are superseded (but not deprecated) should be discarded.
 * @return The coordinate operations.
 */
JNIEXPORT jobject JNICALL Java_org_kortforsyningen_proj_AuthorityFactory_createOperation
    (JNIEnv *env, jobject factory, jobject sourceCRS, jobject targetCRS,
     jdouble desiredAccuracy, jboolean discardSuperseded)
{
    try {
        CRSNNPtr source = NN_CHECK_THROW(get_and_unwrap_ptr<CRS>(env, sourceCRS));
        CRSNNPtr target = NN_CHECK_THROW(get_and_unwrap_ptr<CRS>(env, targetCRS));
        AuthorityFactoryPtr pf = get_and_unwrap_ptr<AuthorityFactory>(env, factory);
        CoordinateOperationContextNNPtr context = CoordinateOperationContext::create(pf, nullptr, desiredAccuracy);
        context->setDiscardSuperseded(discardSuperseded);
        /*
         * At this time, it does not seem worth to cache the CoordinateOperationFactory instance.
         */
        CoordinateOperationFactoryNNPtr opf = CoordinateOperationFactory::create();
        std::vector<CoordinateOperationNNPtr> operations = opf->createOperations(source, target, context);
        if (!operations.empty()) {
            BaseObjectPtr op = operations[0].as_nullable();
            return specific_subclass(env, op, org_kortforsyningen_proj_AuthorityFactory_COORDINATE_OPERATION);
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
    }
    return nullptr;
}




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                      CLASS Transform                                       │
// └────────────────────────────────────────────────────────────────────────────────────────────┘


/**
 * Creates the PJ object from a coordinate operation, to be wrapped in a Transform.
 * The PJ creation may be costly, so the result should be cached.
 *
 * @param  env          The JNI environment.
 * @param  context      The thread context in which the operation is applied.
 * @param  operation    The Java object wrapping the coordinate operation to use.
 * @return pointer to the PJ object, or null if the creation failed.
 */
JNIEXPORT jlong JNICALL Java_org_kortforsyningen_proj_Context_createPJ(JNIEnv *env, jobject context, jobject operation) {
    try {
        PJ_CONTEXT *ctx = get_context(env, context);
        DatabaseContextPtr dbContext = get_database_context(env, context);
        CoordinateOperationPtr cop = get_and_unwrap_ptr<CoordinateOperation>(env, operation);
        PROJStringFormatterNNPtr formatter = PROJStringFormatter::create(PROJStringFormatter::Convention::PROJ_5, dbContext);
        std::string projString = cop->exportToPROJString(formatter.get());
        PJ *pj = proj_create(ctx, projString.c_str());
        return reinterpret_cast<jlong>(pj);
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_TRANSFORM_EXCEPTION, e);
    }
    return 0;
}


/**
 * Returns the pointer to PJ for the given Transform object in Java.
 *
 * @param  env      The JNI environment.
 * @param  context  The Transform object.
 * @return The pointer to PJ, or null if none.
 */
inline PJ* get_PJ(JNIEnv *env, jobject transform) {
    return reinterpret_cast<PJ*>(env->GetLongField(transform, java_field_for_pointer));
}


/**
 * Assigns a PJ_CONTEXT to the PJ wrapped by the Transform.
 * This function must be invoked before and after call to transform method.
 *
 * @param  env        The JNI environment.
 * @param  transform  The Java object wrapping the PJ to use.
 * @param  Context    The context to assign, or null for removing context assignment.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_Transform_assign(JNIEnv *env, jobject transform, jobject context) {
    PJ *pj = get_PJ(env, transform);
    if (pj) {
        PJ_CONTEXT *ctx = context ? get_context(env, context) : nullptr;
        proj_assign_context(pj, ctx);
    }
}


/**
 * Transforms in-place the coordinates in the given array.
 * The coordinates array shall contain (x,y,z,t,…) tuples,
 * where the z and any additional dimensions are optional.
 * Note that any dimension after the t value are ignored.
 *
 * Note that PJ are context-dependent. If the method is invoked in
 * a context different than the one for which PJ has been created,
 * then the following method shall be invoked first:
 *
 *    void proj_assign_context(PJ* pj, PJ_CONTEXT* ctx);
 *
 * @param  env          The JNI environment.
 * @param  transform    The Java object wrapping the PJ to use.
 * @param  dimension    The dimension of each coordinate value.
 * @param  coordinates  The coordinates to transform, as a sequence of (x,y,z,…) tuples.
 * @param  offset       Offset of the first coordinate in the given array.
 * @param  numPts       Number of points to transform.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_Transform_transform
    (JNIEnv *env, jobject transform, const jint dimension, jdoubleArray coordinates, jint offset, jint numPts)
{
    PJ *pj = get_PJ(env, transform);
    if (pj) {
        const size_t stride = sizeof(jdouble) * dimension;
        /*
         * Using GetPrimitiveArrayCritical/ReleasePrimitiveArrayCritical rather than
         * GetDoubleArrayElements/ReleaseDoubleArrayElements increase the chances that
         * the JVM returns direct reference to its internal array without copying data.
         * However we must promise to run the "critical" code fast, to not make any
         * system call that may wait for the JVM and to not invoke any other JNI method.
         */
        double *data = reinterpret_cast<jdouble*>(env->GetPrimitiveArrayCritical(coordinates, nullptr));
        if (data) {
            double *x = data + offset;
            double *y = (dimension >= 2) ? x+1 : nullptr;
            double *z = (dimension >= 3) ? x+2 : nullptr;
            double *t = (dimension >= 4) ? x+3 : nullptr;
            proj_trans_generic(pj, PJ_FWD,
                    x, stride, numPts,
                    y, stride, numPts,
                    z, stride, numPts,
                    t, stride, numPts);
            env->ReleasePrimitiveArrayCritical(coordinates, data, 0);
            const int err = proj_errno(pj);
            if (err) {
                jclass c = env->FindClass(JPJ_TRANSFORM_EXCEPTION);
                if (c) env->ThrowNew(c, proj_errno_string(err));
            }
        }
    }
}


/**
 * Destroys the PJ object.
 *
 * @param  env        The JNI environment.
 * @param  transform  The Java object wrapping the PJ to use.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_Transform_destroy(JNIEnv *env, jobject transform) {
    jlong pjPtr = get_and_clear_ptr(env, transform);
    proj_destroy(reinterpret_cast<PJ*>(pjPtr));         // Does nothing if pjPtr is null.
}