/*
 * Copyright © 2019-2021 Agency for Data Supply and Efficiency
 * Copyright © 2021-2023 Open Source Geospatial Foundation
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

// <editor-fold desc="Includes and usings">
#include <assert.h>
#include <cstring>
#include <string>
#include <cmath>
#include <atomic>
#include <proj.h>
#include <proj/crs.hpp>
#include "org_osgeo_proj_Type.h"
#include "org_osgeo_proj_Property.h"
#include "org_osgeo_proj_NativeResource.h"
#include "org_osgeo_proj_Context.h"
#include "org_osgeo_proj_SharedPointer.h"
#include "org_osgeo_proj_CompoundCS.h"
#include "org_osgeo_proj_ObjectFactory.h"
#include "org_osgeo_proj_AuthorityFactory.h"
#include "org_osgeo_proj_ReferencingFormat.h"
#include "org_osgeo_proj_Convention.h"
#include "org_osgeo_proj_Transform.h"
#include "org_osgeo_proj_UnitOfMeasure.h"

// TODO: remove after PROJ 6.3 release.
#include <proj_experimental.h>

/*
 * The strcase*-functions are not Standard C, but a POSIX extension.
 * The Microsoft Visual C++ 2015 compiler does not support them, but
 * provides the alternatives below.
 */
#ifdef _MSC_VER
#include <string.h>
#define strcasecmp _stricmp
#endif

using osgeo::proj::common::Angle;
using osgeo::proj::common::DateTime;
using osgeo::proj::common::IdentifiedObject;
using osgeo::proj::common::IdentifiedObjectNNPtr;
using osgeo::proj::common::IdentifiedObjectPtr;
using osgeo::proj::common::Length;
using osgeo::proj::common::Measure;
using osgeo::proj::common::ObjectDomainNNPtr;
using osgeo::proj::common::ObjectUsage;
using osgeo::proj::common::ObjectUsageNNPtr;
using osgeo::proj::common::Scale;
using osgeo::proj::common::UnitOfMeasure;
using osgeo::proj::crs::BoundCRS;
using osgeo::proj::crs::BoundCRSPtr;
using osgeo::proj::crs::CompoundCRS;
using osgeo::proj::crs::CompoundCRSPtr;
using osgeo::proj::crs::CRS;
using osgeo::proj::crs::CRSNNPtr;
using osgeo::proj::crs::CRSPtr;
using osgeo::proj::crs::DerivedCRS;
using osgeo::proj::crs::EngineeringCRS;
using osgeo::proj::crs::GeodeticCRS;
using osgeo::proj::crs::GeodeticCRSNNPtr;
using osgeo::proj::crs::GeographicCRS;
using osgeo::proj::crs::ProjectedCRS;
using osgeo::proj::crs::SingleCRS;
using osgeo::proj::crs::SingleCRSPtr;
using osgeo::proj::crs::TemporalCRS;
using osgeo::proj::crs::VerticalCRS;
using osgeo::proj::cs::AxisDirection;
using osgeo::proj::cs::CartesianCS;
using osgeo::proj::cs::CartesianCSNNPtr;
using osgeo::proj::cs::CartesianCSPtr;
using osgeo::proj::cs::CoordinateSystem;
using osgeo::proj::cs::CoordinateSystemAxis;
using osgeo::proj::cs::CoordinateSystemAxisNNPtr;
using osgeo::proj::cs::CoordinateSystemAxisPtr;
using osgeo::proj::cs::CoordinateSystemNNPtr;
using osgeo::proj::cs::CoordinateSystemPtr;
using osgeo::proj::cs::EllipsoidalCS;
using osgeo::proj::cs::EllipsoidalCSNNPtr;
using osgeo::proj::cs::SphericalCS;
using osgeo::proj::cs::SphericalCSNNPtr;
using osgeo::proj::cs::SphericalCSPtr;
using osgeo::proj::cs::TemporalCS;
using osgeo::proj::cs::TemporalCSNNPtr;
using osgeo::proj::cs::TemporalMeasureCS;
using osgeo::proj::cs::VerticalCS;
using osgeo::proj::cs::VerticalCSNNPtr;
using osgeo::proj::datum::Datum;
using osgeo::proj::datum::Ellipsoid;
using osgeo::proj::datum::EllipsoidNNPtr;
using osgeo::proj::datum::EngineeringDatum;
using osgeo::proj::datum::EngineeringDatumNNPtr;
using osgeo::proj::datum::GeodeticReferenceFrame;
using osgeo::proj::datum::GeodeticReferenceFrameNNPtr;
using osgeo::proj::datum::PrimeMeridian;
using osgeo::proj::datum::PrimeMeridianNNPtr;
using osgeo::proj::datum::TemporalDatum;
using osgeo::proj::datum::TemporalDatumNNPtr;
using osgeo::proj::datum::VerticalReferenceFrame;
using osgeo::proj::datum::VerticalReferenceFrameNNPtr;
using osgeo::proj::io::AuthorityFactory;
using osgeo::proj::io::AuthorityFactoryPtr;
using osgeo::proj::io::DatabaseContext;
using osgeo::proj::io::DatabaseContextNNPtr;
using osgeo::proj::io::DatabaseContextPtr;
using osgeo::proj::io::IJSONExportable;
using osgeo::proj::io::IPROJStringExportable;
using osgeo::proj::io::IWKTExportable;
using osgeo::proj::io::JSONFormatter;
using osgeo::proj::io::JSONFormatterNNPtr;
using osgeo::proj::io::NoSuchAuthorityCodeException;
using osgeo::proj::io::PROJStringFormatter;
using osgeo::proj::io::PROJStringFormatterNNPtr;
using osgeo::proj::io::PROJStringParser;
using osgeo::proj::io::WKTFormatter;
using osgeo::proj::io::WKTFormatterNNPtr;
using osgeo::proj::io::WKTParser;
using osgeo::proj::metadata::Citation;
using osgeo::proj::metadata::Extent;
using osgeo::proj::metadata::ExtentPtr;
using osgeo::proj::metadata::GeographicBoundingBox;
using osgeo::proj::metadata::GeographicBoundingBoxPtr;
using osgeo::proj::metadata::GeographicExtentNNPtr;
using osgeo::proj::metadata::Identifier;
using osgeo::proj::metadata::IdentifierNNPtr;
using osgeo::proj::metadata::PositionalAccuracyNNPtr;
using osgeo::proj::operation::Conversion;
using osgeo::proj::operation::ConversionNNPtr;
using osgeo::proj::operation::CoordinateOperation;
using osgeo::proj::operation::CoordinateOperationContext;
using osgeo::proj::operation::CoordinateOperationContextNNPtr;
using osgeo::proj::operation::CoordinateOperationFactory;
using osgeo::proj::operation::CoordinateOperationFactoryNNPtr;
using osgeo::proj::operation::CoordinateOperationNNPtr;
using osgeo::proj::operation::GeneralOperationParameterNNPtr;
using osgeo::proj::operation::GeneralParameterValueNNPtr;
using osgeo::proj::operation::OperationMethod;
using osgeo::proj::operation::OperationParameterValue;
using osgeo::proj::operation::OperationParameterValueNNPtr;
using osgeo::proj::operation::OperationParameterValuePtr;
using osgeo::proj::operation::ParameterValue;
using osgeo::proj::operation::ParameterValueNNPtr;
using osgeo::proj::operation::SingleOperation;
using osgeo::proj::operation::Transformation;
using osgeo::proj::util::BaseObject;
using osgeo::proj::util::BaseObjectPtr;
using osgeo::proj::util::GenericNameNNPtr;
using osgeo::proj::util::GenericNamePtr;
using osgeo::proj::util::IComparable;
using osgeo::proj::util::NameSpacePtr;
using osgeo::proj::util::optional;
using osgeo::proj::util::PropertyMap;
// </editor-fold>


/*
 * DEFINITIONS OF TERMS:
 *
 *     In this file, "function" is a C/C++ function (either PROJ or JNI)
 *     and "method" is a Java method, including the ones implemented in
 *     this file.
 */




// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                           INITIALIZATION  (CLASS NativeResource)                           │
// └────────────────────────────────────────────────────────────────────────────────────────────┘
// <editor-fold desc="Initialization">

/**
 * Identifier of the Java field which will contain the pointer to PROJ structure in Java class.
 * We get this field at initialization time and reuse it every time we need the pointer value.
 * According JNI specification, jfieldID and jmethodID are valid until the class is unloaded.
 * We can not cache `jclass` references unless we protect them with `env->NewGlobalRef(…)`.
 *
 * In principle we should keep a reference to NativeResource class for preventing unloading.
 * We don't on the assumption that if the class was unloaded, next use of PROJ would require
 * reloading the class and initialize it again, in which case the values below would be updated.
 * This is the same approach than the one recommended in Android developer guide.
 *
 * We use this identifier in calls to `env->GetLongField(object, java_field_for_pointer)` where
 * `object` can be a subclass of NativeResource. The JNI specification does not said explicitly
 * if `env->GetField` is compatible with class inheritance or if we need to get a new `jfieldID`
 * for each specific class, but tests suggest that inheritance works.
 */
jfieldID  java_field_for_pointer;
jfieldID  java_field_debug_level;
jmethodID java_method_findWrapper;
jmethodID java_method_getDefinedUnit;
jmethodID java_method_wrapGeodeticObject;
jmethodID java_method_getLogger;
jmethodID java_method_log;


/**
 * Invoked at initialization time for setting the values of global variables.
 * This method must be invoked from the class which contains the "ptr" field.
 * If this operation fails, a NoSuchFieldError will be thrown in Java code.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this method has been invoked.
 *                 Must be the class containing the pointer fields.
 */
JNIEXPORT void JNICALL Java_org_osgeo_proj_NativeResource_initialize(JNIEnv *env, jclass caller) {
    java_field_for_pointer = env->GetFieldID(caller, "ptr", "J");
    if (java_field_for_pointer) {
        /*
         * If we can not get the "ptr" field, all other methods are useless.
         * A Java exception is thrown by JNI in such case, which will cause
         * a failure to initialize PROJ-JNI.
         */
        java_method_wrapGeodeticObject = env->GetMethodID(caller, "wrapGeodeticObject", "(SJ)Lorg/osgeo/proj/IdentifiableObject;");
        if (java_method_wrapGeodeticObject) {
            java_method_findWrapper = env->GetMethodID(caller, "findWrapper", "(J)Lorg/osgeo/proj/IdentifiableObject;");
            if (java_method_findWrapper) {
                java_method_getDefinedUnit = env->GetStaticMethodID(caller, "getPredefinedUnit", "(ID)Ljavax/measure/Unit;");
            }
        }
    }
    if (!env->ExceptionCheck()) {
        /*
         * Following fields and methods are used for logging purpose only.
         * If any operation fail, `java_method_getLogger` will be left to
         * null. We use that as a sentinel value for determining that the
         * logging system is not available.
         */
        jclass logger = env->FindClass("java/util/logging/Logger");
        if (logger) {
            jclass level = env->FindClass("java/util/logging/Level");
            if (level) {
                java_method_log = env->GetMethodID(logger, "log", "(Ljava/util/logging/Level;Ljava/lang/String;)V");
                if (java_method_log) {
                    java_field_debug_level = env->GetStaticFieldID(level, "FINE", "Ljava/util/logging/Level;");
                    if (java_field_debug_level) {
                        java_method_getLogger = env->GetStaticMethodID(caller, "logger", "()Ljava/util/logging/Logger;");
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


/**
 * Returns the PROJ release number.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this method has been invoked.
 * @return The PROJ release number, or null.
 */
JNIEXPORT jstring JNICALL Java_org_osgeo_proj_NativeResource_version(JNIEnv *env, jclass caller) {
    const char *desc = pj_release;
    return (desc) ? env->NewStringUTF(desc) : nullptr;
}




// </editor-fold>
// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                          HELPER FUNCTIONS (not invoked from Java)                          │
// └────────────────────────────────────────────────────────────────────────────────────────────┘
// <editor-fold desc="Helper functions">

#define JPJ_FACTORY_EXCEPTION          "org/opengis/util/FactoryException"
#define JPJ_NO_SUCH_AUTHORITY_CODE     "org/opengis/referencing/NoSuchAuthorityCodeException"
#define JPJ_TRANSFORM_EXCEPTION        "org/opengis/referencing/operation/TransformException"
#define JPJ_NON_INVERTIBLE_EXCEPTION   "org/opengis/referencing/operation/NoninvertibleTransformException"
#define JPJ_INVALID_PARAMETER_TYPE     "org/opengis/parameter/InvalidParameterTypeException"
#define JPJ_UNFORMATTABLE_EXCEPTION    "org/osgeo/proj/UnformattableObjectException"
#define JPJ_UNPARSABLE_EXCEPTION       "org/osgeo/proj/UnparsableObjectException"
#define JPJ_OUT_OF_BOUNDS_EXCEPTION    "java/lang/IndexOutOfBoundsException"
#define JPJ_ILLEGAL_ARGUMENT_EXCEPTION "java/lang/IllegalArgumentException"
#define JPJ_RUNTIME_EXCEPTION          "java/lang/RuntimeException"

/*
 * NOTE ON CHARACTER ENCODING: this implementation assumes that the PROJ library expects strings
 * encoded in UTF-8, regardless the platform encoding. Consequently we use the JNI "StringUTF"
 * functions directly. It is not completely appropriate because JNI functions use a modified UTF-8,
 * but it should be okay if the strings do not use the null character (0) or the supplementary
 * characters (the ones encoded on 4 bytes in a UTF-8 string).
 */


/**
 * A constant for empty string.
 */
const std::string empty_string = std::string();

/**
 * Converts the given C++ string into a Java string if non-empty, or returns null if the string is empty.
 * This function assumes UTF-8 encoding with no null character and no supplementary Unicode characters.
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
    jclass c = env->FindClass("org/osgeo/proj/NativeResource");
    if (c) {
        jobject logger = env->CallStaticObjectMethod(c, java_method_getLogger);
        if (!env->ExceptionCheck()) {                               // ExceptionCheck() must be always invoked.
            c = env->FindClass("java/util/logging/Level");
            if (c) {
                jobject level = env->GetStaticObjectField(c, java_field_debug_level);
                jstring str = env->NewStringUTF(text.c_str());
                if (str && logger) {
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
template <class T> inline jlong wrap_shared_ptr(std::shared_ptr<T> &object) {
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
 * This function does nothing if the memory block has already been released
 * (it would be a bug if it happens, but we nevertheless try to be safe).
 *
 * @param  ptr  Address returned by wrap_shared_ptr(…).
 */
template <class T> inline void release_shared_ptr(jlong ptr) {
    if (ptr) {
        std::shared_ptr<T> *wrapper = reinterpret_cast<std::shared_ptr<T>*>(ptr);
        *wrapper = nullptr;     // This assignation decreases object.use_count().
        free(wrapper);
    }
}


/**
 * Gets the value of the `ptr` field of given object and sets that value to zero.
 * This function is invoked for implementation of `release()` or `destroy()` functions.
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
inline jlong get_and_clear_ptr(JNIEnv *env, jobject object) {
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
 * This function never returns null. Instead, a C++ exception is thrown if the pointer is missing.
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
 * Returns the non-null shared pointer for the specified osgeo::proj::util::BaseObject subtype.
 * This function is equivalent to above `get_and_unwrap_ptr` function for the case where the object
 * is a BaseObject subtype (a CRS, CoordinateOperation, etc.), but with additional safety checks.
 * We assume that the cost of those additional checks is low compared to the cost of other tasks
 * (JNI, PROJ operation, etc.).
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the osgeo::proj::util::BaseObject subtype.
 * @return Shared pointer to the PROJ object associated to the given Java object.
 * @throw  std::exception if this function can not get a non-null pointer.
 */
template <class T> inline osgeo::proj::util::nn<std::shared_ptr<T>> get_shared_object(JNIEnv *env, jobject object) {
    BaseObjectPtr ptr = get_and_unwrap_ptr<BaseObject>(env, object);
    return NN_CHECK_THROW(std::dynamic_pointer_cast<T>(ptr));
}


/**
 * Specialization of `get_shared_object` for `IdentifiedObject` type. We provide a special case
 * if the object is an `OperationParameterValue`: that class does not extend `IdentifiedObject`
 * directly, but provides information indirectly through a parameter descriptor.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the osgeo::proj::common::IdentifiedObject subtype.
 * @return Shared pointer to the PROJ object associated to the given Java object.
 * @throw  std::exception if this function can not get a non-null pointer.
 */
IdentifiedObjectNNPtr get_identified_object(JNIEnv *env, jobject object) {
    BaseObjectPtr ptr = get_and_unwrap_ptr<BaseObject>(env, object);
    IdentifiedObjectPtr id = std::dynamic_pointer_cast<IdentifiedObject>(ptr);
    if (id) {
        return NN_CHECK_ASSERT(id);
    }
    return NN_CHECK_THROW(std::dynamic_pointer_cast<OperationParameterValue>(ptr))->parameter();
}


/**
 * Throws a Java exception when a parameter value is requested on a parameter of wrong type.
 *
 * @param  env      The JNI environment.
 * @param  param    The parameter of wrong type.
 * @param  message  The message to put in Java exception.
 */
void invalid_parameter_type(JNIEnv *env, OperationParameterValueNNPtr param, const char* message) {
    jclass c = env->FindClass(JPJ_INVALID_PARAMETER_TYPE);
    if (c) {
        jmethodID method = env->GetMethodID(c, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
        if (method) {
            jstring msg = env->NewStringUTF(message);
            if (msg) {
                jobject jt = env->NewObject(c, method, msg, non_empty_string(env, param->parameter()->nameStr()));
                if (jt) env->Throw(static_cast<jthrowable>(jt));
            }
        }
    }
}


/**
 * Rethrows the given C++ exception as a Java exception with the same message. If a Java exception
 * is already pending (this may happen if the exception was thrown by the JNI framework), then this
 * function does nothing. This function returns normally; the exception will be thrown only when
 * execution returns to Java code.
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
 * This function tries to find a more specialized type for the given object, then calls the Java method
 * `wrapGeodeticObject(…)` with that type in argument. If the type is unknown, then this function returns
 * null and an exception is thrown in Java code.
 *
 * @param  env     The JNI environment.
 * @param  caller  The Java object which is creating another object.
 * @param  object  Shared pointer to wrap. Shall not be empty.
 * @param  type    Base type of the object to wrap. This function will use a more specialized type if possible.
 * @return Wrapper for a PROJ object, or null if an error occurred.
 */
jobject specific_subclass(JNIEnv *env, jobject caller, BaseObjectPtr &object, jshort type) {
    BaseObject *rp = object.get();
    jobject result = env->CallObjectMethod(caller, java_method_findWrapper, reinterpret_cast<jlong>(rp));
    if (env->ExceptionCheck()) {
        return nullptr;
    }
    if (!result) {
again:  switch (type) {
            case org_osgeo_proj_Type_ANY: {
                     if (dynamic_cast<CRS                  *>(rp)) type = org_osgeo_proj_Type_COORDINATE_REFERENCE_SYSTEM;
                else if (dynamic_cast<Datum                *>(rp)) type = org_osgeo_proj_Type_DATUM;
                else if (dynamic_cast<Ellipsoid            *>(rp)) type = org_osgeo_proj_Type_ELLIPSOID;
                else if (dynamic_cast<PrimeMeridian        *>(rp)) type = org_osgeo_proj_Type_PRIME_MERIDIAN;
                else if (dynamic_cast<CoordinateSystem     *>(rp)) type = org_osgeo_proj_Type_COORDINATE_SYSTEM;
                else if (dynamic_cast<CoordinateSystemAxis *>(rp)) type = org_osgeo_proj_Type_AXIS;
                else if (dynamic_cast<CoordinateOperation  *>(rp)) type = org_osgeo_proj_Type_COORDINATE_OPERATION;
                else if (dynamic_cast<OperationMethod      *>(rp)) type = org_osgeo_proj_Type_OPERATION_METHOD;
                else if (dynamic_cast<UnitOfMeasure        *>(rp)) type = org_osgeo_proj_Type_UNIT_OF_MEASURE;
                else if (dynamic_cast<Identifier           *>(rp)) type = org_osgeo_proj_Type_IDENTIFIER;
                else break;
                goto again;
            }
            case org_osgeo_proj_Type_COORDINATE_OPERATION: {
                     if (dynamic_cast<Conversion     *>(rp)) type = org_osgeo_proj_Type_CONVERSION;
                else if (dynamic_cast<Transformation *>(rp)) type = org_osgeo_proj_Type_TRANSFORMATION;
                break;
            }
            case org_osgeo_proj_Type_COORDINATE_REFERENCE_SYSTEM: {
                     if (dynamic_cast<CompoundCRS    *>(rp)) type = org_osgeo_proj_Type_COMPOUND_CRS;
                else if (dynamic_cast<ProjectedCRS   *>(rp)) type = org_osgeo_proj_Type_PROJECTED_CRS;
                else if (dynamic_cast<GeographicCRS  *>(rp)) type = org_osgeo_proj_Type_GEOGRAPHIC_CRS;
                else if (dynamic_cast<VerticalCRS    *>(rp)) type = org_osgeo_proj_Type_VERTICAL_CRS;
                else if (dynamic_cast<TemporalCRS    *>(rp)) type = org_osgeo_proj_Type_TEMPORAL_CRS;
                else if (dynamic_cast<EngineeringCRS *>(rp)) type = org_osgeo_proj_Type_ENGINEERING_CRS;
                else {
                    GeodeticCRS *gc = dynamic_cast<GeodeticCRS*>(rp);
                    if (gc) {
                        type = org_osgeo_proj_Type_GEODETIC_CRS;
                        if (gc->isGeocentric()) {
                            type = org_osgeo_proj_Type_GEOCENTRIC_CRS;
                        }
                    }
                }
                break;
            }
            case org_osgeo_proj_Type_COORDINATE_SYSTEM: {
                     if (dynamic_cast<CartesianCS   *>(rp)) type = org_osgeo_proj_Type_CARTESIAN_CS;
                else if (dynamic_cast<SphericalCS   *>(rp)) type = org_osgeo_proj_Type_SPHERICAL_CS;
                else if (dynamic_cast<EllipsoidalCS *>(rp)) type = org_osgeo_proj_Type_ELLIPSOIDAL_CS;
                else if (dynamic_cast<VerticalCS    *>(rp)) type = org_osgeo_proj_Type_VERTICAL_CS;
                else if (dynamic_cast<TemporalCS    *>(rp)) type = org_osgeo_proj_Type_TEMPORAL_CS;
                break;
            }
            case org_osgeo_proj_Type_DATUM: {
                     if (dynamic_cast<GeodeticReferenceFrame *>(rp)) type = org_osgeo_proj_Type_GEODETIC_REFERENCE_FRAME;
                else if (dynamic_cast<VerticalReferenceFrame *>(rp)) type = org_osgeo_proj_Type_VERTICAL_REFERENCE_FRAME;
                else if (dynamic_cast<TemporalDatum          *>(rp)) type = org_osgeo_proj_Type_TEMPORAL_DATUM;
                else if (dynamic_cast<EngineeringDatum       *>(rp)) type = org_osgeo_proj_Type_ENGINEERING_DATUM;
                break;
            }
        }
        /*
         * At this point `type` is either unchanged, or modified to a more specialized code reflecting the
         * actual PROJ object type. Now delegate to wrapGeodeticObject(…) Java method for creating the Java
         * object of that type. If a Java exception is thrown, we release the PROJ resource and return null.
         * The exception will be propagated in Java code.
         */
        jlong ptr = wrap_shared_ptr<BaseObject>(object);
        if (ptr) {
            result = env->CallObjectMethod(caller, java_method_wrapGeodeticObject, type, ptr);
            if (env->ExceptionCheck() | !result) {              // ExceptionCheck() must be always invoked.
                release_shared_ptr<BaseObject>(ptr);
                result = nullptr;
            }
        }
    }
    return result;
}




// </editor-fold>
// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                    CLASS UnitOfMeasure                                     │
// └────────────────────────────────────────────────────────────────────────────────────────────┘
// <editor-fold desc="Units of measures">


/**
 * Returns a predefined PROJ unit of measurement from given code. This function does
 * not create new unit. See `unit_from_identifier(…)` for a function that may create
 * new units.
 *
 * @param  code  one of the constants enumerated in the Java `UnitOfMeasure` class.
 * @return the PROJ unit of measure, or null if none.
 */
inline const UnitOfMeasure* get_predefined_unit(int code) {
    switch (code) {
        case org_osgeo_proj_UnitOfMeasure_SCALE_UNITY:       return &UnitOfMeasure::SCALE_UNITY;
        case org_osgeo_proj_UnitOfMeasure_PARTS_PER_MILLION: return &UnitOfMeasure::PARTS_PER_MILLION;
        case org_osgeo_proj_UnitOfMeasure_METRE:             return &UnitOfMeasure::METRE;
        case org_osgeo_proj_UnitOfMeasure_RADIAN:            return &UnitOfMeasure::RADIAN;
        case org_osgeo_proj_UnitOfMeasure_MICRORADIAN:       return &UnitOfMeasure::MICRORADIAN;
        case org_osgeo_proj_UnitOfMeasure_DEGREE:            return &UnitOfMeasure::DEGREE;
        case org_osgeo_proj_UnitOfMeasure_ARC_SECOND:        return &UnitOfMeasure::ARC_SECOND;
        case org_osgeo_proj_UnitOfMeasure_GRAD:              return &UnitOfMeasure::GRAD;
        case org_osgeo_proj_UnitOfMeasure_SECOND:            return &UnitOfMeasure::SECOND;
        case org_osgeo_proj_UnitOfMeasure_YEAR:              return &UnitOfMeasure::YEAR;
    }
    return nullptr;
}


/**
 * Creates a Java UnitOfMeasure instance from the information provided in a C++ UnitOfMeasure.
 * This function is used both for instantiating the predefined units enumerated in Units class,
 * or for instantiating a new unit not in the predefined units list.
 *
 * Implementation is not very efficient (method ID searched in each method call), but it should
 * not be invoked often. After initialization, it should be invoked only for uncommon units and
 * only if there is no JSR-385 implementation on the classpath.
 *
 * @param  env       The JNI environment.
 * @param  uomClass  The Java UnitOfMeasure class to instantiate.
 * @param  unit      The PROJ UnitOfMeasure instance to copy in Java, or null.
 * @return The Java unit of measurement, or null.
 */
inline jobject create_unit_fallback(JNIEnv *env, jclass uomClass, const UnitOfMeasure* unit) {
    if (unit) {
        jmethodID c = env->GetMethodID(uomClass, "<init>", "(ILjava/lang/String;D)V");
        if (c) {
            jstring name = nullptr;
            std::string sn = unit->name();
            if (sn.empty() || (name = env->NewStringUTF(sn.c_str()))) {
                return env->NewObject(uomClass, c, (jint) static_cast<int>(unit->type()),
                                      name, (jdouble) unit->conversionToSI());
            }
        }
    }
    return nullptr;
}


/**
 * Returns a Java UnitOfMeasure instance for the given C++ UnitOfMeasure instance.
 * This function returns one of the predefined instance if possible, or create a
 * new instance otherwise.
 *
 * @param  env      The JNI environment.
 * @param  object   The NativeResource for which a unit is fetched.
 * @param  measure  The PROJ UnitOfMeasure instance to mirror in Java.
 * @return instance of Java UnitOfMeasure class.
 */
jobject to_java_unit(JNIEnv *env, jobject object, const UnitOfMeasure* unit) {
    jobject result = env->CallStaticObjectMethod(env->GetObjectClass(object),
                                                 java_method_getDefinedUnit,
                                                 (jint) static_cast<int>(unit->type()),
                                                 (jdouble) unit->conversionToSI());
    if (!env->ExceptionCheck() && !result) {    // Call to ExceptionCheck() must be unconditional.
        /*
         * This block is not very efficient, but should not be invoked often.
         * See the `create_unit_fallback(…)` documentation for rational.
         */
        jclass uomClass = env->FindClass("org/osgeo/proj/UnitOfMeasure");
        if (uomClass) {
            result = create_unit_fallback(env, uomClass, unit);
        }
    }
    return result;
}


/**
 * Creates the Java UnitOfMeasure class for one of the PROJ predefined values.
 * This method is invoked only at initialization time, and only if no JSR-385
 * implementation is provided on the classpath.
 *
 * @param  env     The JNI environment.
 * @param  caller  The Java UnitOfMeasure class to instantiate.
 * @param  code    Code of the the PROJ UnitOfMeasure instance to copy in Java.
 * @return instance of Java UnitOfMeasure class, or null if the given code is unrecognized.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_UnitOfMeasure_create
    (JNIEnv *env, jclass caller, jshort code)
{
    const UnitOfMeasure* unit = get_predefined_unit(code);
    return create_unit_fallback(env, caller, unit);
}




// </editor-fold>
// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                              CLASS Context (except createPJ)                               │
// └────────────────────────────────────────────────────────────────────────────────────────────┘
// <editor-fold desc="Context">


/**
 * Allocates a PJ_CONTEXT for using PROJ in a multi-threads environment.
 * Each thread should have its own PJ_CONTEXT instance.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this method has been invoked.
 * @return The address of the new PJ_CONTEXT structure, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_osgeo_proj_Context_create(JNIEnv *env, jclass caller, jstring searchPaths) {
    static_assert(sizeof(PJ_CONTEXT*) <= sizeof(jlong), "Can not store PJ_CONTEXT* in a jlong.");
    PJ_CONTEXT *ctx = proj_context_create();
	
	if (searchPaths != NULL) {
		const char *path = env->GetStringUTFChars(searchPaths, nullptr);
		if (path) {
			proj_context_set_search_paths(ctx, 1, &path);
			env->ReleaseStringUTFChars(searchPaths, path);
		}
	}
		
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
 * @param  context  The PJ_CONTEXT wrapper for the current thread, or null if none.
 * @return Pointer to shared database, or null if the given context was null.
 * @throw  std::exception if the database creation failed.
 */
DatabaseContextPtr get_database_context(JNIEnv *env, jobject context) {
    if (!context) {
        return nullptr;
    }
    jfieldID fid = get_database_field(env, context);
    if (!fid) {
        throw std::exception();     // Should never happen.
    }
    jlong dbPtr = env->GetLongField(context, fid);
    DatabaseContextPtr db;
    if (dbPtr) {
        db = unwrap_shared_ptr<DatabaseContext>(dbPtr);
    } else {
        log(env, "Creating PROJ database context.");
        db = DatabaseContext::create(empty_string, std::vector<std::string>(), get_context(env, context)).as_nullable();
        dbPtr = wrap_shared_ptr<DatabaseContext>(db);
        env->SetLongField(context, fid, dbPtr);
        // dbPtr may be 0 if out of memory, but the only consequence is that DatabaseContext is not cached.
    }
    return db;
}


/**
 * Releases a PJ_CONTEXT and its associated database context. This method sets the `ptr` and `database`
 * fields in the Java object to zero as a safety in case there is two attempts to destroy the same object.
 *
 * @param  env      The JNI environment.
 * @param  context  The Java object wrapping the context to release.
 */
JNIEXPORT void JNICALL Java_org_osgeo_proj_Context_destroyPJ(JNIEnv *env, jobject context) {
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
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_Context_createFromUserInput(JNIEnv *env, jobject context, jstring text) {
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
    if (result) try {
        return specific_subclass(env, context, result, org_osgeo_proj_Type_ANY);
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
    }
    return nullptr;
}




// </editor-fold>
// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                      CLASS SharedPointer (except format and inverse)                       │
// └────────────────────────────────────────────────────────────────────────────────────────────┘
// <editor-fold desc="Shared pointer">


/**
 * Converts the given osgeo::proj::util::GenericName into a Java string.
 *
 * @param  env   The JNI environment.
 * @param  name  The name to convert to Java string.
 * @return the Java string.
 */
inline jstring name_to_string(JNIEnv *env, GenericNameNNPtr name) {
    const std::string &text = name->toString();
    return env->NewStringUTF(text.c_str());
}


/**
 * Returns a property value as an object.
 *
 * @param  env       The JNI environment.
 * @param  object    The Java object wrapping the PROJ object for which to get a property value.
 * @param  property  One of COORDINATE_SYSTEM, etc. values.
 * @return Value of the specified property, or null if undefined.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_SharedPointer_getObjectProperty
    (JNIEnv *env, jobject object, jshort property)
{
    try {
        BaseObjectPtr value;
        jshort type;
        switch (property) {
            case org_osgeo_proj_Property_NAME: {
                value = get_identified_object(env, object)->name().as_nullable();
                type  = org_osgeo_proj_Type_IDENTIFIER;
                break;
            }
            case org_osgeo_proj_Property_PRIME_MERIDIAN: {
                value = get_shared_object<GeodeticReferenceFrame>(env, object)->primeMeridian().as_nullable();
                type  = org_osgeo_proj_Type_PRIME_MERIDIAN;
                break;
            }
            case org_osgeo_proj_Property_ELLIPSOID: {
                value = get_shared_object<GeodeticReferenceFrame>(env, object)->ellipsoid().as_nullable();
                type  = org_osgeo_proj_Type_ELLIPSOID;
                break;
            }
            case org_osgeo_proj_Property_BASE_CRS: {
                value = get_shared_object<DerivedCRS>(env, object)->baseCRS().as_nullable();
                type  = org_osgeo_proj_Type_COORDINATE_REFERENCE_SYSTEM;
                break;
            }
            case org_osgeo_proj_Property_CONVERT_FROM_BASE: {
                value = get_shared_object<DerivedCRS>(env, object)->derivingConversion().as_nullable();
                type  = org_osgeo_proj_Type_CONVERSION;
                break;
            }
            case org_osgeo_proj_Property_DATUM: {
                value = get_shared_object<SingleCRS>(env, object)->datum();
                type  = org_osgeo_proj_Type_DATUM;
                break;
            }
            case org_osgeo_proj_Property_COORDINATE_SYSTEM: {
                value = get_shared_object<SingleCRS>(env, object)->coordinateSystem().as_nullable();
                type  = org_osgeo_proj_Type_COORDINATE_SYSTEM;
                break;
            }
            case org_osgeo_proj_Property_OPERATION_METHOD: {
                value = get_shared_object<SingleOperation>(env, object)->method().as_nullable();
                type  = org_osgeo_proj_Type_OPERATION_METHOD;
                break;
            }
            case org_osgeo_proj_Property_AXIS_UNIT: {
                const UnitOfMeasure& unit = get_shared_object<CoordinateSystemAxis>(env, object)->unit();
                return to_java_unit(env, object, &unit);
            }
            case org_osgeo_proj_Property_ELLIPSOID_UNIT: {
                const Measure& measure = get_shared_object<Ellipsoid>(env, object)->semiMajorAxis();
                return to_java_unit(env, object, &measure.unit());
            }
            case org_osgeo_proj_Property_MERIDIAN_UNIT: {
                const Measure& measure = get_shared_object<PrimeMeridian>(env, object)->longitude();
                return to_java_unit(env, object, &measure.unit());
            }
            case org_osgeo_proj_Property_PARAMETER_UNIT: {
                ParameterValueNNPtr param = get_shared_object<OperationParameterValue>(env, object)->parameterValue();
                if (param->type() == ParameterValue::Type::MEASURE) {
                    const Measure& measure = param->value();
                    return to_java_unit(env, object, &measure.unit());
                }
                return nullptr;
            }
            default: {
                return nullptr;
            }
        }
        if (value) {
            return specific_subclass(env, object, value, type);
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_RUNTIME_EXCEPTION, e);
    }
    return nullptr;
}


/**
 * Returns a property value as an element in a std::vector for the given name.
 *
 * @param  env       The JNI environment.
 * @param  object    The Java object wrapping the PROJ object for which to get a property value.
 * @param  property  One of OPERATION_PARAMETER, etc. values.
 * @param  name      Name of the element to return, case insensitive.
 * @return Value of the specified property, or null if undefined.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_SharedPointer_searchVectorElement
    (JNIEnv *env, jobject object, jshort property, jstring name)
{
    const char *name_utf = env->GetStringUTFChars(name, nullptr);
    if (name_utf) {
        jshort type;
        try {
            BaseObjectPtr value;
            switch (property) {
                case org_osgeo_proj_Property_METHOD_PARAMETER: {
                    for (GeneralOperationParameterNNPtr param : get_shared_object<OperationMethod>(env, object)->parameters()) {
                        if (!strcasecmp(name_utf, param->nameStr().c_str())) {
                            type = org_osgeo_proj_Type_PARAMETER;
                            value = param.as_nullable();
                            break;
                        }
                    }
                    break;
                }
                case org_osgeo_proj_Property_OPERATION_PARAMETER: {
                    for (GeneralParameterValueNNPtr param : get_shared_object<SingleOperation>(env, object)->parameterValues()) {
                        OperationParameterValuePtr single = std::dynamic_pointer_cast<OperationParameterValue>(param.as_nullable());
                        if (single && !strcasecmp(name_utf, single->parameter()->nameStr().c_str())) {
                            type = org_osgeo_proj_Type_PARAMETER_VALUE;
                            value = single;
                            break;
                        }
                    }
                    break;
                }
                default: {
                    return nullptr;
                }
            }
            if (value) {
                return specific_subclass(env, object, value, type);
            }
        } catch (const std::exception &e) {
            rethrow_as_java_exception(env, JPJ_RUNTIME_EXCEPTION, e);
        }
        env->ReleaseStringUTFChars(name, name_utf);     // Must be after the catch block in case an exception happens.
    }
    return nullptr;
}


/**
 * Returns the alias at given index. This is a shortcut for a relatively frequent operation.
 *
 * @param  env       The JNI environment.
 * @param  object    The Java object wrapping the PROJ object for which to get an alias.
 * @param  index     Index of the alias to return.
 * @return Value of the specified alias.
 */
inline GenericNameNNPtr get_alias(JNIEnv *env, jobject object, jint index) {
    return  get_identified_object(env, object)->aliases().at(index);
}


/**
 * Returns a property value as an element in a std::vector.
 *
 * @param  env       The JNI environment.
 * @param  object    The Java object wrapping the PROJ object for which to get a property value.
 * @param  property  One of AXIS, etc. values.
 * @param  index     Index of the element to return.
 * @return Value of the specified property, or null if undefined.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_SharedPointer_getVectorElement
    (JNIEnv *env, jobject object, jshort property, jint index)
{
    try {
        BaseObjectPtr value;
        jshort type;
        switch (property) {
            case org_osgeo_proj_Property_IDENTIFIER: {
                value = get_identified_object(env, object)->identifiers().at(index).as_nullable();
                type  = org_osgeo_proj_Type_IDENTIFIER;
                break;
            }
            case org_osgeo_proj_Property_ALIAS: {
                return name_to_string(env, get_alias(env, object, index));
            }
            case org_osgeo_proj_Property_ALIAS_NS: {
                NameSpacePtr ns = get_alias(env, object, index)->scope();
                if (ns) {
                    GenericNamePtr name = ns->name();
                    if (name) {
                        return name_to_string(env, NN_CHECK_ASSERT(name));
                    }
                }
                return nullptr;
            }
            case org_osgeo_proj_Property_ALIAS_NS_IS_GLOBAL: {
                NameSpacePtr ns = get_alias(env, object, index)->scope();
                return env->NewStringUTF((!ns || ns->isGlobal()) ? "true" : "false");
            }
            case org_osgeo_proj_Property_FULLY_QUALIFIED: {
                return name_to_string(env, get_alias(env, object, index)->toFullyQualifiedName());
            }
            case org_osgeo_proj_Property_AXIS: {
                value = get_shared_object<CoordinateSystem>(env, object)->axisList().at(index).as_nullable();
                type  = org_osgeo_proj_Type_AXIS;
                break;
            }
            case org_osgeo_proj_Property_METHOD_PARAMETER: {
                value = get_shared_object<OperationMethod>(env, object)->parameters().at(index).as_nullable();
                type  = org_osgeo_proj_Type_PARAMETER;
                break;
            }
            case org_osgeo_proj_Property_OPERATION_PARAMETER: {
                value = get_shared_object<SingleOperation>(env, object)->parameterValues().at(index).as_nullable();
                type  = org_osgeo_proj_Type_PARAMETER_VALUE;
                break;
            }
            case org_osgeo_proj_Property_CRS_COMPONENT: {
                value = get_shared_object<CompoundCRS>(env, object)->componentReferenceSystems().at(index).as_nullable();
                type  = org_osgeo_proj_Type_COORDINATE_REFERENCE_SYSTEM;
                break;
            }
            case org_osgeo_proj_Property_SOURCE_TARGET_CRS: {
                CoordinateOperationNNPtr cop = get_shared_object<CoordinateOperation>(env, object);
                value = (index ? cop->targetCRS() : cop->sourceCRS());
                type  = org_osgeo_proj_Type_COORDINATE_REFERENCE_SYSTEM;
                break;
            }
            default: {
                return nullptr;
            }
        }
        if (value) {
            return specific_subclass(env, object, value, type);
        }
    } catch (const std::out_of_range &e) {
        rethrow_as_java_exception(env, JPJ_OUT_OF_BOUNDS_EXCEPTION, e);
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_RUNTIME_EXCEPTION, e);
    }
    return nullptr;
}


/**
 * Returns the size of the identified property.
 * This method should contain the same cases than `getVectorElement` except `SOURCE_TARGET_CRS`.
 *
 * @param  env       The JNI environment.
 * @param  object    The Java object wrapping the PROJ object for which to get the vector length.
 * @param  property  One of IDENTIFIER, etc. values.
 * @return Vector length in wrapped object, or 0 if unknown.
 */
JNIEXPORT jint JNICALL Java_org_osgeo_proj_SharedPointer_getVectorSize
    (JNIEnv *env, jobject object, jshort property)
{
    try {
        switch (property) {
            case org_osgeo_proj_Property_IDENTIFIER: {
                return get_identified_object(env, object)->identifiers().size();
            }
            case org_osgeo_proj_Property_ALIAS: {
                return get_identified_object(env, object)->aliases().size();
            }
            case org_osgeo_proj_Property_AXIS: {
                return get_and_unwrap_ptr<CoordinateSystem>(env, object)->axisList().size();
            }
            case org_osgeo_proj_Property_METHOD_PARAMETER: {
                return get_shared_object<OperationMethod>(env, object)->parameters().size();
            }
            case org_osgeo_proj_Property_OPERATION_PARAMETER: {
                return get_shared_object<SingleOperation>(env, object)->parameterValues().size();
            }
            case org_osgeo_proj_Property_CRS_COMPONENT: {
                return get_shared_object<CompoundCRS>(env, object)->componentReferenceSystems().size();
            }
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_RUNTIME_EXCEPTION, e);
    }
    return 0;
}


/**
 * Returns the given optional string as a plain string, or as an empty string if absent.
 *
 * @param  text  the optional string to return as a plain string.
 * @return the plain string, or an empty string if absent.
 */
inline const std::string& string_or_empty(const optional<std::string>& text) {
    return text.has_value() ? *text : empty_string;
}


/**
 * Returns the title of the given citation, or an empty string if absent.
 *
 * @param  citation  the citation from which to get the title.
 * @return title of the given citation, or an empty string if absent.
 */
inline const std::string& citation_title(const optional<Citation>& citation) {
    return citation.has_value() ? string_or_empty(citation->title()) : empty_string;
}


/**
 * Returns a property value as a string.
 *
 * @param  env       The JNI environment.
 * @param  object    The Java object wrapping the PROJ object for which to get a property value.
 * @param  property  One of ABBREVIATION, etc. values.
 * @return Value of the specified property, or null if undefined.
 */
JNIEXPORT jstring JNICALL Java_org_osgeo_proj_SharedPointer_getStringProperty
    (JNIEnv *env, jobject object, jshort property)
{
    try {
        const char* value;
        switch (property) {
            case org_osgeo_proj_Property_NAME_STRING: {
                value = get_identified_object(env, object)->nameStr().c_str();
                break;
            }
            case org_osgeo_proj_Property_IDENTIFIER_STRING: {
                int code = get_identified_object(env, object)->getEPSGCode();
                if (code == 0) return nullptr;
                return env->NewStringUTF(("EPSG:" + std::to_string(code)).c_str());
            }
            case org_osgeo_proj_Property_CITATION_TITLE: {
                value = citation_title(get_shared_object<Identifier>(env, object)->authority()).c_str();
                break;
            }
            case org_osgeo_proj_Property_CODESPACE: {
                value = string_or_empty(get_shared_object<Identifier>(env, object)->codeSpace()).c_str();
                break;
            }
            case org_osgeo_proj_Property_CODE: {
                value = get_shared_object<Identifier>(env, object)->code().c_str();
                break;
            }
            case org_osgeo_proj_Property_VERSION: {
                value = string_or_empty(get_shared_object<Identifier>(env, object)->version()).c_str();
                break;
            }
            case org_osgeo_proj_Property_ABBREVIATION: {
                value = get_shared_object<CoordinateSystemAxis>(env, object)->abbreviation().c_str();
                break;
            }
            case org_osgeo_proj_Property_DIRECTION: {
                value = get_shared_object<CoordinateSystemAxis>(env, object)->direction().toString().c_str();
                break;
            }
            case org_osgeo_proj_Property_ANCHOR_DEFINITION: {
                value = string_or_empty(get_shared_object<Datum>(env, object)->anchorDefinition()).c_str();
                break;
            }
            case org_osgeo_proj_Property_OPERATION_VERSION: {
                value = string_or_empty(get_shared_object<CoordinateOperation>(env, object)->operationVersion()).c_str();
                break;
            }
            case org_osgeo_proj_Property_FORMULA: {
                value = string_or_empty(get_shared_object<OperationMethod>(env, object)->formula()).c_str();
                break;
            }
            case org_osgeo_proj_Property_FORMULA_TITLE: {
                value = citation_title(get_shared_object<OperationMethod>(env, object)->formulaCitation()).c_str();
                break;
            }
            case org_osgeo_proj_Property_REMARKS: {
                value = get_identified_object(env, object)->remarks().c_str();
                break;
            }
            case org_osgeo_proj_Property_PUBLICATION_DATE: {
                const optional<DateTime> &date = get_shared_object<Datum>(env, object)->publicationDate();
                if (date.has_value() && date->isISO_8601()) {
                    // NewStringUTF must be in the scope of this block.
                    return env->NewStringUTF(date->toString().c_str());
                }
                return nullptr;
            }
            case org_osgeo_proj_Property_TEMPORAL_ORIGIN: {
                const DateTime &date = get_shared_object<TemporalDatum>(env, object)->temporalOrigin();
                if (date.isISO_8601()) {
                    // NewStringUTF must be in the scope of this block.
                    return env->NewStringUTF(date.toString().c_str());
                }
                return nullptr;
            }
            case org_osgeo_proj_Property_SCOPE: {
                ObjectUsageNNPtr usage = get_shared_object<ObjectUsage>(env, object);
                for (const ObjectDomainNNPtr domain : usage->domains()) {
                    jstring scope = non_empty_string(env, string_or_empty(domain->scope()));
                    if (scope) return scope;            // Returns the first non-empty value.
                }
                return nullptr;
            }
            case org_osgeo_proj_Property_POSITIONAL_ACCURACY: {
                CoordinateOperationNNPtr op = get_shared_object<CoordinateOperation>(env, object);
                for (const PositionalAccuracyNNPtr accuracy : op->coordinateOperationAccuracies()) {
                    jstring result = non_empty_string(env, accuracy->value());
                    if (result) return result;              // Returns the first non-empty value.
                }
                return nullptr;
            }
            case org_osgeo_proj_Property_PARAMETER_STRING: {
                OperationParameterValueNNPtr opv = get_shared_object<OperationParameterValue>(env, object);
                ParameterValueNNPtr param = opv->parameterValue();
                switch (param->type()) {
                    case ParameterValue::Type::STRING:   value = param->stringValue().c_str(); break;
                    case ParameterValue::Type::FILENAME: value = param->valueFile().c_str();   break;
                    default: {
                        invalid_parameter_type(env, opv, "This parameter is not a string.");
                        return nullptr;
                    }
                }
                break;
            }
            case org_osgeo_proj_Property_PARAMETER_FILE: {
                OperationParameterValueNNPtr opv = get_shared_object<OperationParameterValue>(env, object);
                ParameterValueNNPtr param = opv->parameterValue();
                if (param->type() == ParameterValue::Type::FILENAME) {
                    value = param->valueFile().c_str();
                    break;
                } else {
                    invalid_parameter_type(env, opv, "This parameter is not a filename.");
                    return nullptr;
                }
            }
            default: {
                return nullptr;
            }
        }
        if (strlen(value)) {
            return env->NewStringUTF(value);
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_RUNTIME_EXCEPTION, e);
    }
    return nullptr;
}


/**
 * Returns a property value as a floating point number.
 *
 * @param  env       The JNI environment.
 * @param  object    The Java object wrapping the PROJ object for which to get a property value.
 * @param  property  One of MINIMUM, MAXIMUM, etc. values.
 * @return Value of the specified property, or NaN if undefined.
 */
JNIEXPORT jdouble JNICALL Java_org_osgeo_proj_SharedPointer_getNumericProperty
    (JNIEnv *env, jobject object, jshort property)
{
    try {
        optional<double> value;
        switch (property) {
            case org_osgeo_proj_Property_MINIMUM: {
                value = get_shared_object<CoordinateSystemAxis>(env, object)->minimumValue();
                break;
            }
            case org_osgeo_proj_Property_MAXIMUM: {
                value = get_shared_object<CoordinateSystemAxis>(env, object)->maximumValue();
                break;
            }
            case org_osgeo_proj_Property_GREENWICH: {
                value = get_shared_object<PrimeMeridian>(env, object)->longitude().value();
                break;
            }
            case org_osgeo_proj_Property_SEMI_MAJOR: {
                value = get_shared_object<Ellipsoid>(env, object)->semiMajorAxis().value();
                break;
            }
            case org_osgeo_proj_Property_SEMI_MINOR: {
                value = get_shared_object<Ellipsoid>(env, object)->computeSemiMinorAxis().value();
                break;
            }
            case org_osgeo_proj_Property_INVERSE_FLAT: {
                value = get_shared_object<Ellipsoid>(env, object)->computedInverseFlattening();
                break;
            }
            case org_osgeo_proj_Property_PARAMETER_VALUE: {
                OperationParameterValueNNPtr opv = get_shared_object<OperationParameterValue>(env, object);
                ParameterValueNNPtr param = opv->parameterValue();
                switch (param->type()) {
                    case ParameterValue::Type::MEASURE: return param->value().value();
                    case ParameterValue::Type::INTEGER: return param->integerValue();
                }
                invalid_parameter_type(env, opv, "This parameter is not a measure.");
                return NAN;
            }
            default: {
                return NAN;
            }
        }
        if (value.has_value()) {
            return *value;
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_RUNTIME_EXCEPTION, e);
    }
    return NAN;
}


/**
 * Returns a property value as an array of floating-point values.
 *
 * @param  env       The JNI environment.
 * @param  object    The Java object wrapping the PROJ object for which to get a property value.
 * @param  property  One of POSITIONAL_ACCURACY, etc. values.
 * @return Value of the specified property, or null if undefined.
 */
JNIEXPORT jdoubleArray JNICALL Java_org_osgeo_proj_SharedPointer_getArrayProperty
    (JNIEnv *env, jobject object, jshort property)
{
    try {
        switch (property) {
            case org_osgeo_proj_Property_DOMAIN_OF_VALIDITY: {
                ObjectUsageNNPtr usage = get_shared_object<ObjectUsage>(env, object);
                for (const ObjectDomainNNPtr domain : usage->domains()) {
                    ExtentPtr extent = domain->domainOfValidity();
                    if (extent) {
                        jdoubleArray array = env->NewDoubleArray(4);
                        if (!array) break;                              // OutOfMemoryError will be thrown in Java code.
                        jdouble* elements = env->GetDoubleArrayElements(array, nullptr);
                        if (!elements) break;                           // OutOfMemoryError will be thrown in Java code.
                        bool hasBBox = false;
                        for (GeographicExtentNNPtr ge : extent->geographicElements()) {
                            GeographicBoundingBoxPtr bbox = std::dynamic_pointer_cast<GeographicBoundingBox>(ge.as_nullable());
                            if (bbox) {
                                elements[0] = bbox->westBoundLongitude();
                                elements[1] = bbox->eastBoundLongitude();
                                elements[2] = bbox->southBoundLatitude();
                                elements[3] = bbox->northBoundLatitude();
                                hasBBox = true;
                                break;
                            }
                        }
                        env->ReleaseDoubleArrayElements(array, elements, 0);
                        if (hasBBox) return array;
                    }
                }
                break;
            }
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_RUNTIME_EXCEPTION, e);
    }
    return nullptr;
}


/**
 * Returns a property value as an integer value.
 *
 * @param  env       The JNI environment.
 * @param  object    The Java object wrapping the PROJ object for which to get a property value.
 * @param  property  One of PARAMETER_TYPE, etc. values.
 * @return Value of the specified property, or false if undefined.
 */
JNIEXPORT jint JNICALL Java_org_osgeo_proj_SharedPointer_getIntegerProperty
    (JNIEnv *env, jobject object, jshort property)
{
    try {
        switch (property) {
            case org_osgeo_proj_Property_PARAMETER_TYPE: {
                return static_cast<int>(get_shared_object<OperationParameterValue>(env, object)->parameterValue()->type());
            }
            case org_osgeo_proj_Property_PARAMETER_INT: {
                OperationParameterValueNNPtr opv = get_shared_object<OperationParameterValue>(env, object);
                ParameterValueNNPtr param = opv->parameterValue();
                switch (param->type()) {
                    case ParameterValue::Type::INTEGER: return param->integerValue();
                    case ParameterValue::Type::BOOLEAN: return param->booleanValue() ? 1 : 0;
                }
                invalid_parameter_type(env, opv, "This parameter is not an integer.");
                break;
            }
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_RUNTIME_EXCEPTION, e);
    }
    return 0;
}


/**
 * Returns a property value as a boolean value.
 *
 * @param  env       The JNI environment.
 * @param  object    The Java object wrapping the PROJ object for which to get a property value.
 * @param  property  One of IS_SPHERE, etc. values.
 * @return Value of the specified property, or false if undefined.
 */
JNIEXPORT jboolean JNICALL Java_org_osgeo_proj_SharedPointer_getBooleanProperty
    (JNIEnv *env, jobject object, jshort property)
{
    try {
        switch (property) {
            case org_osgeo_proj_Property_HAS_NAME: {
                IdentifiedObjectNNPtr id = get_identified_object(env, object);
                return !id->name()->code().empty() || !id->nameStr().empty();
            }
            case org_osgeo_proj_Property_IS_SPHERE: {
                return get_shared_object<Ellipsoid>(env, object)->isSphere();
            }
            case org_osgeo_proj_Property_IVF_DEFINITIVE: {
                return get_shared_object<Ellipsoid>(env, object)->inverseFlattening().has_value();
            }
            case org_osgeo_proj_Property_PARAMETER_BOOL: {
                OperationParameterValueNNPtr opv = get_shared_object<OperationParameterValue>(env, object);
                ParameterValueNNPtr param = opv->parameterValue();
                if (param->type() == ParameterValue::Type::BOOLEAN) {
                    return param->booleanValue();
                }
                invalid_parameter_type(env, opv, "This parameter is not a boolean.");
                break;
            }
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_RUNTIME_EXCEPTION, e);
    }
    return JNI_FALSE;
}


/**
 * Compares this object with the given object for equality.
 *
 * @param  env        The JNI environment.
 * @param  object     The Java object wrapping the PROJ object.
 * @param  other      The other object to compare with this object.
 * @param  criterion  A IComparable.Criterion ordinal value.
 * @return Whether the two objects are equal.
 */
JNIEXPORT jboolean JNICALL Java_org_osgeo_proj_SharedPointer_isEquivalentTo
    (JNIEnv *env, jobject object, jobject other, jint criterion)
{
    try {
        BaseObjectPtr ptr1, ptr2;
        if ((ptr1 = get_and_unwrap_ptr<BaseObject>(env, object)) &&
            (ptr2 = get_and_unwrap_ptr<BaseObject>(env, other)))
        {
            if (ptr1 == ptr2) {
                return JNI_TRUE;
            }
            std::shared_ptr<IComparable> obj1, obj2;
            if ((obj1 = std::dynamic_pointer_cast<IComparable>(ptr1)) &&
                (obj2 = std::dynamic_pointer_cast<IComparable>(ptr2)))
            {
                return obj1->isEquivalentTo(obj2.get(), static_cast<IComparable::Criterion>(criterion));
            }
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_ILLEGAL_ARGUMENT_EXCEPTION, e);
    }
    return JNI_FALSE;
}


/**
 * Returns the memory address of the PROJ object wrapped by the NativeResource.
 * This is used for computing hash codes and object comparisons only.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the PROJ object.
 * @return Memory address of the wrapper PROJ object.
 */
JNIEXPORT jlong JNICALL Java_org_osgeo_proj_SharedPointer_rawPointer(JNIEnv *env, jobject object) {
    jlong ptr = env->GetLongField(object, java_field_for_pointer);
    if (ptr) {
        BaseObjectPtr sp = unwrap_shared_ptr<BaseObject>(ptr);
        if (sp) return reinterpret_cast<jlong>(sp.get());
    }
    return 0;
}


/**
 * Decrements the references count of the shared pointer. This method is invoked automatically
 * when an instance of IdentifiableObject class is garbage collected.
 *
 * @param  env     The JNI environment.
 * @param  object  The Java object wrapping the shared object to release.
 */
JNIEXPORT void JNICALL Java_org_osgeo_proj_SharedPointer_release(JNIEnv *env, jobject object) {
    jlong ptr = get_and_clear_ptr(env, object);
    release_shared_ptr<BaseObject>(ptr);
}




// </editor-fold>
// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                       CLASS ReferencingFormat + SharedPointer.format                       │
// └────────────────────────────────────────────────────────────────────────────────────────────┘
// <editor-fold desc="Parsing and formatting">


/**
 * Returns a Well-Known Text (WKT), JSON or PROJ string for this object.
 * This is allowed only if this object implements osgeo::proj::io::IWKTExportable,
 * osgeo::proj::io::IJSONExportable or osgeo::proj::io::IPROJStringExportable.
 *
 * @param  env         The JNI environment.
 * @param  object      The Java object wrapping the PROJ object to format.
 * @param  context     The PJ_CONTEXT wrapper, or null if none.
 * @param  convention  One of ReferencingFormat constants.
 * @param  indentation Number of spaces for each indentation level, or -1 for the default value.
 * @param  multiline   Whether the WKT will use multi-line layout.
 * @param  strict      Whether to enforce strictly standard format.
 * @return The Well-Known Text (WKT) for this object, or null if the object is not IWKTExportable.
 */
JNIEXPORT jstring JNICALL Java_org_osgeo_proj_SharedPointer_format
    (JNIEnv *env, jobject object, jobject context, jint convention, jint indentation, jboolean multiline, jboolean strict)
{
    enum format {WKT, PROJ, JSON};
    union version {
        WKTFormatter::Convention wkt;
        PROJStringFormatter::Convention proj;
    };
    format  f;
    version c;
    switch (convention) {
        // TODO: rename "2018" as "2019" in next PROJ release.
        case Format_WKT2_2019:            f = WKT;  c.wkt  = WKTFormatter::Convention::WKT2_2018;            break;
        case Format_WKT2_2015:            f = WKT;  c.wkt  = WKTFormatter::Convention::WKT2_2015;            break;
        case Format_WKT2_2019_SIMPLIFIED: f = WKT;  c.wkt  = WKTFormatter::Convention::WKT2_2018_SIMPLIFIED; break;
        case Format_WKT2_2015_SIMPLIFIED: f = WKT;  c.wkt  = WKTFormatter::Convention::WKT2_2015_SIMPLIFIED; break;
        case Format_WKT1_ESRI:            f = WKT;  c.wkt  = WKTFormatter::Convention::WKT1_ESRI;            break;
        case Format_WKT1_GDAL:            f = WKT;  c.wkt  = WKTFormatter::Convention::WKT1_GDAL;            break;
        case Format_PROJ_5:               f = PROJ; c.proj = PROJStringFormatter::Convention::PROJ_5;        break;
        case Format_PROJ_4:               f = PROJ; c.proj = PROJStringFormatter::Convention::PROJ_4;        break;
        case Format_JSON:                 f = JSON;                                                          break;
        default: {
            jclass c = env->FindClass(JPJ_ILLEGAL_ARGUMENT_EXCEPTION);
            if (c) env->ThrowNew(c, std::to_string(convention).c_str());
            return nullptr;
        }
    }
    try {
        BaseObjectPtr candidate = get_and_unwrap_ptr<BaseObject>(env, object);
        switch (f) {
            case WKT: {
                std::shared_ptr<IWKTExportable> exportable = std::dynamic_pointer_cast<IWKTExportable>(candidate);
                if (!exportable) break;
                DatabaseContextPtr dbContext = get_database_context(env, context);
                WKTFormatterNNPtr  formatter = WKTFormatter::create(c.wkt, dbContext);
                formatter->setMultiLine(multiline);
                formatter->setStrict(strict);
                if (indentation >= 0) {
                    formatter->setIndentationWidth(indentation);
                }
                return non_empty_string(env, exportable->exportToWKT(formatter.get()));
            }
            case JSON: {
                std::shared_ptr<IJSONExportable> exportable = std::dynamic_pointer_cast<IJSONExportable>(candidate);
                if (!exportable) break;
                DatabaseContextPtr dbContext = get_database_context(env, context);
                JSONFormatterNNPtr formatter = JSONFormatter::create(dbContext);
                formatter->setMultiLine(multiline);
                if (indentation >= 0) {
                    formatter->setIndentationWidth(indentation);
                }
                return non_empty_string(env, exportable->exportToJSON(formatter.get()));
            }
            case PROJ: {
                std::shared_ptr<IPROJStringExportable> exportable = std::dynamic_pointer_cast<IPROJStringExportable>(candidate);
                if (!exportable) break;
                DatabaseContextPtr       dbContext = get_database_context(env, context);
                PROJStringFormatterNNPtr formatter = PROJStringFormatter::create(c.proj, dbContext);
                return non_empty_string(env, exportable->exportToPROJString(formatter.get()));
            }
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_UNFORMATTABLE_EXCEPTION, e);
    }
    return nullptr;
}


/**
 * Sends warnings to the `ReferencingFormat` instance used for parsing a text.
 * This method should be invoked only after successful parsing.
 *
 * @param  env       The JNI environment.
 * @param  format    The `ReferencingFormat` instance used for formatting.
 * @param  warnings  The warnings, or an empty list if none.
 */
void send_warnings(JNIEnv *env, jobject format, const std::vector<std::string>& warnings) {
    int n = warnings.size();
    if (n) {
        jmethodID addWarning = env->GetMethodID(env->GetObjectClass(format), "addWarning", "(Ljava/lang/String;)V");
        if (addWarning) {
            for (int i=0; i<n; i++) {
                jstring message = env->NewStringUTF(warnings[i].c_str());
                if (!message) break;
                env->CallVoidMethod(format, addWarning, message);
                if (env->ExceptionCheck()) break;                       // Exception will be thrown in Java code.
            }
        }
    }
}


/**
 * Parses a Well-Known Text (WKT), JSON or PROJ string.
 * Warnings, if any, will be sent to the `format` instance.
 *
 * @param  env         The JNI environment.
 * @param  format      The `ReferencingFormat` instance used for formatting.
 * @param  text        The WKT, JSON or PROJ string to parse.
 * @param  context     The PJ_CONTEXT wrapper.
 * @param  convention  One of ReferencingFormat constants.
 * @param  strict      Whether to enforce strictly standard format.
 * @return The object parsed from given text, or null on failure.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_ReferencingFormat_parse
    (JNIEnv *env, jobject format, jstring text, jobject context, jint convention, jboolean strict)
{
    const char* text_utf = nullptr;
    try {
        BaseObjectPtr object = nullptr;
        switch (convention) {
            case Format_WKT2_2019_SIMPLIFIED:
            case Format_WKT2_2015_SIMPLIFIED:
            case Format_WKT2_2019:
            case Format_WKT2_2015:
            case Format_WKT1_ESRI:
            case Format_WKT1_GDAL: {
                WKTParser parser;
                parser.setStrict(strict);
                parser.attachDatabaseContext(get_database_context(env, context));
                text_utf = env->GetStringUTFChars(text, nullptr);
                if (text_utf) {
                    object = parser.createFromWKT(text_utf).as_nullable();
                    env->ReleaseStringUTFChars(text, text_utf);
                    text_utf = nullptr;
                    const std::list<std::string>& warnings = parser.warningList();
                    send_warnings(env, format, std::vector<std::string>(warnings.begin(), warnings.end()));
                }
                break;
            }
            case Format_PROJ_5:
            case Format_PROJ_4: {
                PROJStringParser parser;
                parser.attachDatabaseContext(get_database_context(env, context));
                text_utf = env->GetStringUTFChars(text, nullptr);
                if (text_utf) {
                    object = parser.createFromPROJString(text_utf).as_nullable();
                    env->ReleaseStringUTFChars(text, text_utf);
                    text_utf = nullptr;
                    send_warnings(env, format, parser.warningList());
                }
                break;
            }
        }
        if (object) {
            return specific_subclass(env, context, object, org_osgeo_proj_Type_ANY);
        }
    } catch (const std::exception &e) {
        if (text_utf) {
            env->ReleaseStringUTFChars(text, text_utf);
        }
        rethrow_as_java_exception(env, JPJ_UNPARSABLE_EXCEPTION, e);
    }
    return nullptr;
}




// </editor-fold>
// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                      CLASS CompoundCS                                      │
// └────────────────────────────────────────────────────────────────────────────────────────────┘
// <editor-fold desc="Compound CS">


/**
 * Returns the given object as a single CRS if possible, or null otherwise.
 * If the CRS is a bound CRS, its base CRS is returned.
 *
 * @param  crs  The object to get as a single CRS.
 * @return The object as a single CRS, or null.
 */
inline const SingleCRSPtr as_single_crs(const CRSPtr &ptr) {
    SingleCRSPtr crs = std::dynamic_pointer_cast<SingleCRS>(ptr);
    if (!crs) {
        BoundCRSPtr bound = std::dynamic_pointer_cast<BoundCRS>(ptr);
        if (bound) {
            CRSNNPtr base = bound->baseCRS();
            crs = std::dynamic_pointer_cast<SingleCRS>(base.as_nullable());
        }
    }
    return crs;
}


/**
 * Returns the given CRS as a compound CRS or throws an exception.
 *
 * @param  crs    The CRS to cast to compound CRS. Must be non-null.
 * @param  depth  Counter for protection against infinite recursivity.
 * @return The compound CRS.
 * @throws std::exception if the given CRS is not compound.
 */
inline const CompoundCRSPtr as_compound_crs(const CRSPtr &crs, int &depth) {
    if (++depth >= 10) {                                // Arbitrary limit.
        throw std::invalid_argument("Too many nesting of compound CRS.");
    }
    CompoundCRSPtr compound = std::dynamic_pointer_cast<CompoundCRS>(crs);
    if (compound) {
        return compound;
    }
    throw std::invalid_argument("Not a recognized CRS type.");
}


/**
 * Returns the axes of the given CRS or throws an exception if none.
 *
 * @param  crs  The CRS for which to get the axes. Must be non-null.
 * @return The CRS axes.
 * @throws std::exception if the given CRS has no coordinate system.
 */
inline std::vector<CoordinateSystemAxisNNPtr> get_axes(const SingleCRSPtr &crs) {
    CoordinateSystemPtr cs = crs->coordinateSystem();
    if (cs) {
        return cs->axisList();
    }
    throw std::invalid_argument("Unspecified coordinate system.");
}


/**
 * Returns the number of dimensions in the given CRS.
 *
 * @param  crs    The CRS for which to get the number of dimensions.
 * @param  depth  Counter for protection against infinite recursivity.
 * @return The number of dimensions.
 * @throw  std::invalid_argument if the given CRS is not a recognized type.
 */
int get_dimension(const CRSPtr &crs, int depth) {
    SingleCRSPtr single = as_single_crs(crs);
    if (single) {
        return get_axes(single).size();
    }
    int n = 0;
    CompoundCRSPtr compound = as_compound_crs(crs, depth);
    for (CRSNNPtr component : compound->componentReferenceSystems()) {
        n += get_dimension(component.as_nullable(), depth);
    }
    return n;
}


/**
 * Returns the axis at the given dimension.
 * It is caller's responsibility to ensure that the given dimension is positive.
 * If the dimension is greater than the number of axes, then this function returns null.
 *
 * The given dimension argument may be modified by this function. The number of dimensions
 * of all CRS components that were skipped is subtracted from the dimension value.
 *
 * @param  crs        The CRS for which to get an axis.
 * @param  dimension  Zero-based dimension index of the axis to get. Must be positive. Will be updated.
 * @param  depth      Counter for protection against infinite recursivity.
 * @return Axis at the given dimension index, or null if the specified dimension is greater than the number of axes.
 * @throw  std::invalid_argument if a CRS is not a recognized type.
 */
CoordinateSystemAxisPtr get_axis(CompoundCRSPtr &crs, int &dimension, int depth) {
    for (CRSNNPtr component : crs->componentReferenceSystems()) {
        SingleCRSPtr single = as_single_crs(component);
        if (single) {
            std::vector<CoordinateSystemAxisNNPtr> axes = get_axes(single);
            const int cd = axes.size();
            if (dimension < cd) {
                return axes[dimension].as_nullable();
            }
            dimension -= cd;
        } else {
            CompoundCRSPtr compound = as_compound_crs(crs, depth);
            CoordinateSystemAxisPtr axis = get_axis(compound, dimension, depth);
            if (axis) {
                return axis;
            }
            // `dimension` has been decremented by above call to `get_axis(…)`.
            depth--;      // Cancel the increment done by `as_compound_crs(…)`.
        }
    }
    return nullptr;
}


/**
 * Returns the number of dimension of a CRS, which may be osgeo::proj::crs::CompoundCRS.
 *
 * @param  env     The JNI environment.
 * @param  caller  The CompoundCS Java class (ignored).
 * @param  crs     The Java object wrapping the osgeo::proj::crs::CRS.
 */
JNIEXPORT jint JNICALL Java_org_osgeo_proj_CompoundCS_getDimension(JNIEnv *env, jclass caller, jobject crs) {
    try {
        return get_dimension(get_and_unwrap_ptr<CRS>(env, crs), 0);
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_ILLEGAL_ARGUMENT_EXCEPTION, e);
    }
    return 0;
}


/**
 * Returns the axis for the given compound CRS at the specified dimension.
 *
 * @param  env        The JNI environment.
 * @param  caller     The CompoundCS Java class (ignored).
 * @param  crs        The Java object wrapping the osgeo::proj::crs::CRS.
 * @param  dimension  The zero based index of axis.
 * @return The axis at the specified dimension.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_CompoundCS_getAxis
    (JNIEnv *env, jclass caller, jobject crs, jint dimension)
{
    if (dimension >= 0) {
        try {
            CompoundCRSPtr compound = get_and_unwrap_ptr<CompoundCRS>(env, crs);
            int dimcp = dimension;      // Need a copy because will be modified by `get_axis`.
            BaseObjectPtr axis = get_axis(compound, dimcp, 0);
            if (axis) {
                return specific_subclass(env, crs, axis, org_osgeo_proj_Type_AXIS);
            }
        } catch (const std::exception &e) {
            rethrow_as_java_exception(env, JPJ_ILLEGAL_ARGUMENT_EXCEPTION, e);
            return nullptr;
        }
    }
    jclass c = env->FindClass(JPJ_OUT_OF_BOUNDS_EXCEPTION);
    if (c) env->ThrowNew(c, std::to_string(dimension).c_str());
    return nullptr;
}




// </editor-fold>
// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                    CLASS ObjectFactory                                     │
// └────────────────────────────────────────────────────────────────────────────────────────────┘
// <editor-fold desc="Object factory">


/**
 * Returns a PROJ unit of measurement from given code.
 *
 * @param  code  one of the constants enumerated in the Java UnitOfMeasure class.
 * @return the PROJ unit of measure, or null if none.
 * @throws std::invalid_argument if the given unit is unsupported.
 */
UnitOfMeasure unit_from_identifier(JNIEnv *env, int code) {
    const UnitOfMeasure* unit = get_predefined_unit(code);
    if (unit) {
        return *unit;
    }
    /*
     * Following code is inefficient but should not be invoked often.
     * It happens only if the specified unit is not a predefined unit,
     * but instead has been created with customized factor.
     *
     * `UnitType.getUserDefinedTypeAndScale(int)` returns an array of length 2
     * with unit type in the first element and scale factor in the second element.
     */
    jclass c = env->FindClass("org/osgeo/proj/UnitType");
    if (c) {
        jmethodID method = env->GetStaticMethodID(c, "getUserDefinedTypeAndScale", "(I)[D");
        if (method) {
            jdoubleArray array = (jdoubleArray) env->CallStaticObjectMethod(c, method, (jint) code);
            if (!env->ExceptionCheck() && array) {        // Call to ExceptionCheck() must be unconditional.
                jdouble* values = env->GetDoubleArrayElements(array, nullptr);
                if (values) {
                    UnitOfMeasure::Type type = static_cast<UnitOfMeasure::Type>((int) values[0]);
                    double scale = values[1];
                    env->ReleaseDoubleArrayElements(array, values, JNI_ABORT);
                    return UnitOfMeasure(empty_string, scale, type);
                }
            }
        }
    }
    throw std::invalid_argument("Unsupported unit of measurement.");
}


/**
 * Returns an array element as a C++ string object.
 *
 * @param  env           The JNI environment.
 * @param  stringValues  The Java `String[]` array.
 * @param  index         Index of the element to get in the given array.
 * @return The element at the given index.
 * @throws std::invalid_argument if there is not value at the given index.
 */
inline const std::string string_array_element(JNIEnv *env, jobjectArray stringValues, int index) {
    jstring value = (jstring) env->GetObjectArrayElement(stringValues, index);
    if (value) {
        const char* utf = env->GetStringUTFChars(value, nullptr);
        if (utf) {
            std::string str = std::string(utf);
            env->ReleaseStringUTFChars(value, utf);
            return str;
        }
    }
    throw std::invalid_argument("Missing parameter value.");
}


/**
 * Returns the non-null shared pointer for the element at the specified index in the array.
 *
 * @param  env         The JNI environment.
 * @param  components  The Java `IdentifiableObject[]` array.
 * @param  index       Index of the element to get in the given array.
 * @return Shared pointer to the PROJ object associated to the specified Java object.
 * @throw  std::exception if this function can not get a non-null pointer.
 */
template <class T> inline osgeo::proj::util::nn<std::shared_ptr<T>> get_component(JNIEnv *env, jobjectArray components, int index) {
    jobject object = env->GetObjectArrayElement(components, index);
    return get_shared_object<T>(env, object);
}


/**
 * Creates a geodetic object of the given type.
 *
 * @param  env           The JNI environment.
 * @param  factory       The ObjectFactory instance creating a new object.
 * @param  properties    the result of {@code properties(Map, int)} call.
 * @param  components    the components of the geodetic object to create.
 * @param  stringValues  any arguments that need to be passed as character string.
 * @param  doubleValues  any arguments that need to be passed as floating point value.
 * @param  unit          unit of measurement as given by `Units.getUnitIdentifier(Unit)` method.
 * @param  type          one of the {@link Type} constants.
 * @return the geodetic object.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_ObjectFactory_create
    (JNIEnv *env, jobject factory, jobjectArray properties, jobjectArray components,
        jobjectArray stringValues, jdoubleArray doubleValues, jint unit, jshort type)
{
    /*
     * Convert the `properties` flat array into a PROJ `PropertyMap` object. All PROJ create functions
     * invoked in this method will need this property map, so we build it unconditionally.
     *
     * NOTES:
     *   - The call to `propertyMap.set(…, char*)` will copy the string,
     *      so it is safe to release the JNI string immediately after.
     *   - `java.lang.Boolean.toString()` value can be only "true" or "false",
     *      so it is okay to check only the first letter for 't' value.
     */
    PropertyMap propertyMap = PropertyMap();
    optional<std::string> anchor = optional<std::string>();
    if (properties) {
        PropertyMap identifierMap = PropertyMap();
        for (int i = env->GetArrayLength(properties); --i >= 0;) {
            jstring value = (jstring) env->GetObjectArrayElement(properties, i);
            if (value) {
                const char* utf = env->GetStringUTFChars(value, nullptr);
                if (!utf) return nullptr;                                       // May be an OutOfMemoryError — abort.
                try {
                    switch (i) {
                        case org_osgeo_proj_ObjectFactory_NAME:         propertyMap.set(IdentifiedObject::NAME_KEY,        utf);        break;
                        case org_osgeo_proj_ObjectFactory_ALIAS:        propertyMap.set(IdentifiedObject::ALIAS_KEY,       utf);        break;
                        case org_osgeo_proj_ObjectFactory_REMARKS:      propertyMap.set(IdentifiedObject::REMARKS_KEY,     utf);        break;
                        case org_osgeo_proj_ObjectFactory_DEPRECATED:   propertyMap.set(IdentifiedObject::DEPRECATED_KEY, *utf == 't'); break;
                        case org_osgeo_proj_ObjectFactory_SCOPE:        propertyMap.set(ObjectUsage::     SCOPE_KEY,       utf);        break;
                        case org_osgeo_proj_ObjectFactory_ANCHOR_POINT: anchor = optional<std::string>(std::string(utf));               break;
                        case org_osgeo_proj_ObjectFactory_CODESPACE:  identifierMap.set(Identifier::      CODESPACE_KEY,   utf);        break;
                        case org_osgeo_proj_ObjectFactory_IDENTIFIER: {
                            identifierMap.set(Identifier::CODE_KEY, utf);
                            IdentifierNNPtr id = Identifier::create(empty_string, identifierMap);
                            propertyMap.set(IdentifiedObject::IDENTIFIERS_KEY, id);
                            break;
                        }
                    }
                } catch (const std::exception &e) {
                    rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
                }
                env->ReleaseStringUTFChars(value, utf);
                if (env->ExceptionCheck()) return nullptr;
            }
        }
    }
    /*
     * At this point we got the `PropertyMap` ready. Now dispatch to a PROJ constructor.
     */
    BaseObjectPtr object = nullptr;
    try {
        switch (type) {
            case org_osgeo_proj_Type_PRIME_MERIDIAN: {
                jdouble* values = env->GetDoubleArrayElements(doubleValues, nullptr);
                if (values) {
                    Angle measure = Angle(values[0], unit_from_identifier(env, unit));
                    env->ReleaseDoubleArrayElements(doubleValues, values, JNI_ABORT);
                    object = PrimeMeridian::create(propertyMap, measure).as_nullable();
                }
                break;
            }
            case org_osgeo_proj_Type_ELLIPSOID: {
                UnitOfMeasure axisUnit = unit_from_identifier(env, unit);
                bool isIvfDefinitive = env->GetArrayLength(doubleValues) >= 3;
                jdouble* values = env->GetDoubleArrayElements(doubleValues, nullptr);
                if (values) {
                    Length semiMajorAxis = Length(values[0], axisUnit);
                    double secondDefiningParameter = values[isIvfDefinitive ? 2 : 1];
                    env->ReleaseDoubleArrayElements(doubleValues, values, JNI_ABORT);
                    if (isIvfDefinitive) {
                        // Inverse flattening factor is not exactly a scale factor, but PROJ API is that way.
                        Scale inverseFlattening = Scale(secondDefiningParameter, UnitOfMeasure::SCALE_UNITY);
                        object = Ellipsoid::createFlattenedSphere(propertyMap, semiMajorAxis, inverseFlattening).as_nullable();
                    } else {
                        Length semiMinorAxis = Length(secondDefiningParameter, axisUnit);
                        object = Ellipsoid::createTwoAxis(propertyMap, semiMajorAxis, semiMinorAxis).as_nullable();
                    }
                }
                break;
            }
            case org_osgeo_proj_Type_AXIS: {
                std::string abbreviation = string_array_element(env, stringValues, 0);
                std::string directionStr = string_array_element(env, stringValues, 1);
                const AxisDirection* direction = AxisDirection::valueOf(directionStr);
                if (!direction) {
                    throw std::invalid_argument("Unsupported axis direction: " + directionStr);
                }
                UnitOfMeasure axisUnit = unit_from_identifier(env, unit);
                object = CoordinateSystemAxis::create(propertyMap, abbreviation, *direction, axisUnit).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_VERTICAL_CS: {
                CoordinateSystemAxisNNPtr axis = get_component<CoordinateSystemAxis>(env, components, 0);
                object = VerticalCS::create(propertyMap, axis).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_TEMPORAL_CS: {
                CoordinateSystemAxisNNPtr axis = get_component<CoordinateSystemAxis>(env, components, 0);
                object = TemporalMeasureCS::create(propertyMap, axis).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_CARTESIAN_CS:
            case org_osgeo_proj_Type_SPHERICAL_CS:
            case org_osgeo_proj_Type_ELLIPSOIDAL_CS: {
                CoordinateSystemAxisNNPtr axis0 = get_component<CoordinateSystemAxis>(env, components, 0);
                CoordinateSystemAxisNNPtr axis1 = get_component<CoordinateSystemAxis>(env, components, 1);
                if (env->GetArrayLength(components) >= 3) {
                    CoordinateSystemAxisNNPtr axis2 = get_component<CoordinateSystemAxis>(env, components, 2);
                    switch (type) {
                        case org_osgeo_proj_Type_CARTESIAN_CS:   object = CartesianCS  ::create(propertyMap, axis0, axis1, axis2).as_nullable(); break;
                        case org_osgeo_proj_Type_ELLIPSOIDAL_CS: object = EllipsoidalCS::create(propertyMap, axis0, axis1, axis2).as_nullable(); break;
                        case org_osgeo_proj_Type_SPHERICAL_CS:   object = SphericalCS  ::create(propertyMap, axis0, axis1, axis2).as_nullable(); break;
                    }
                } else {
                    switch (type) {
                        case org_osgeo_proj_Type_CARTESIAN_CS:   object = CartesianCS  ::create(propertyMap, axis0, axis1).as_nullable(); break;
                        case org_osgeo_proj_Type_ELLIPSOIDAL_CS: object = EllipsoidalCS::create(propertyMap, axis0, axis1).as_nullable(); break;
                    }
                }
                break;
            }
            case org_osgeo_proj_Type_GEODETIC_REFERENCE_FRAME: {
                EllipsoidNNPtr     ellipsoid     = get_component<Ellipsoid>    (env, components, 0);
                PrimeMeridianNNPtr primeMeridian = get_component<PrimeMeridian>(env, components, 1);
                object = GeodeticReferenceFrame::create(propertyMap, ellipsoid, anchor, primeMeridian).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_VERTICAL_REFERENCE_FRAME: {
                object = VerticalReferenceFrame::create(propertyMap, anchor).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_TEMPORAL_DATUM: {
                std::string iso8601 = string_array_element(env, stringValues, 0);
                DateTime origin = DateTime::create(iso8601);
                object = TemporalDatum::create(propertyMap, origin, TemporalDatum::CALENDAR_PROLEPTIC_GREGORIAN).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_ENGINEERING_DATUM: {
                object = EngineeringDatum::create(propertyMap, anchor).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_GEOCENTRIC_CRS: {
                GeodeticReferenceFrameNNPtr datum = get_component<GeodeticReferenceFrame>(env, components, 0);
                CoordinateSystemPtr cs = get_component<CoordinateSystem>(env, components, 1).as_nullable();
                CartesianCSPtr cartesian = std::dynamic_pointer_cast<CartesianCS>(cs);
                if (cartesian) {
                    object = GeodeticCRS::create(propertyMap, datum, NN_CHECK_ASSERT(cartesian)).as_nullable();
                } else {
                    SphericalCSPtr spherical = std::dynamic_pointer_cast<SphericalCS>(cs);
                    object = GeodeticCRS::create(propertyMap, datum, NN_CHECK_THROW(spherical)).as_nullable();
                }
                break;
            }
            case org_osgeo_proj_Type_GEOGRAPHIC_CRS: {
                GeodeticReferenceFrameNNPtr datum = get_component<GeodeticReferenceFrame>(env, components, 0);
                EllipsoidalCSNNPtr          cs    = get_component<EllipsoidalCS>         (env, components, 1);
                object = GeographicCRS::create(propertyMap, datum, cs).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_VERTICAL_CRS: {
                VerticalReferenceFrameNNPtr datum = get_component<VerticalReferenceFrame>(env, components, 0);
                VerticalCSNNPtr             cs    = get_component<VerticalCS>            (env, components, 1);
                object = VerticalCRS::create(propertyMap, datum, cs).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_TEMPORAL_CRS: {
                TemporalDatumNNPtr datum = get_component<TemporalDatum>(env, components, 0);
                TemporalCSNNPtr    cs    = get_component<TemporalCS>   (env, components, 1);
                object = TemporalCRS::create(propertyMap, datum, cs).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_ENGINEERING_CRS: {
                EngineeringDatumNNPtr datum = get_component<EngineeringDatum>(env, components, 0);
                CoordinateSystemNNPtr cs    = get_component<CoordinateSystem>(env, components, 1);
                object = EngineeringCRS::create(propertyMap, datum, cs).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_PROJECTED_CRS: {
                GeodeticCRSNNPtr baseCRS  = get_component<GeodeticCRS>(env, components, 0);
                ConversionNNPtr  fromBase = get_component<Conversion> (env, components, 1);
                CartesianCSNNPtr cs       = get_component<CartesianCS>(env, components, 2);
                object = ProjectedCRS::create(propertyMap, baseCRS, fromBase, cs).as_nullable();
                break;
            }
            case org_osgeo_proj_Type_COMPOUND_CRS: {
                int n = env->GetArrayLength(components);
                std::vector<CRSNNPtr> items;
                for (int i=0; i<n; i++) {
                    items.push_back(get_component<CRS>(env, components, i));
                }
                object = CompoundCRS::create(propertyMap, items).as_nullable();
                break;
            }
        }
        if (object) {
            return specific_subclass(env, factory, object, type);
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
    }
    return nullptr;
}




// </editor-fold>
// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                                   CLASS AuthorityFactory                                   │
// └────────────────────────────────────────────────────────────────────────────────────────────┘
// <editor-fold desc="Authority factory">


/**
 * Allocates a osgeo::proj::io::AuthorityFactory.
 * The factory should be used by only one thread at a time.
 *
 * @param  env        The JNI environment.
 * @param  caller     The class from which this method has been invoked.
 * @param  context    The wrapper of the PJ_CONTEXT for the current thread.
 * @param  authority  Name of the authority for which to create the factory.
 * @return The address of the new authority factory, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_osgeo_proj_AuthorityFactory_newInstance
    (JNIEnv *env, jclass caller, jobject context, jstring authority)
{
    jlong result = 0;
    const char *authority_utf = env->GetStringUTFChars(authority, nullptr);
    if (authority_utf) {
        try {
            const std::string authority_str = authority_utf;
            DatabaseContextNNPtr db = NN_CHECK_THROW(get_database_context(env, context));
            AuthorityFactoryPtr factory = AuthorityFactory::create(db, authority_str).as_nullable();
            result = wrap_shared_ptr<AuthorityFactory>(factory);
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
JNIEXPORT void JNICALL Java_org_osgeo_proj_AuthorityFactory_release(JNIEnv *env, jobject factory) {
    jlong ptr = get_and_clear_ptr(env, factory);
    release_shared_ptr<AuthorityFactory>(ptr);
}


/**
 * Rethrows the given C++ exception as a Java exception with the same message, authority name
 * and authority code. This function returns normally; the exception will be thrown in Java only.
 *
 * @param  env  The JNI environment.
 * @param  e    The C++ exception to rethrow in Java.
 */
void rethrow_as_java_exception(JNIEnv *env, const NoSuchAuthorityCodeException &e) {
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
JNIEXPORT jstring JNICALL Java_org_osgeo_proj_AuthorityFactory_getDescriptionText
    (JNIEnv *env, jobject factory, jstring code)
{
    jstring result = nullptr;
    const char *code_utf = env->GetStringUTFChars(code, nullptr);
    if (code_utf) {
        try {
            AuthorityFactoryPtr pf = get_and_unwrap_ptr<AuthorityFactory>(env, factory);
            std::string desc = pf->getDescriptionText(code_utf);
            if (!desc.empty()) result = env->NewStringUTF(desc.c_str());
        } catch (const NoSuchAuthorityCodeException &e) {
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
 * The PROJ function invoked by this method is determined by the type argument.
 *
 * @param  env      The JNI environment.
 * @param  factory  The Java object wrapping the authority factory to use.
 * @param  type     One of {@link #ELLIPSOID}, {@link #PRIME_MERIDIAN}, etc. constants.
 * @param  code     Object code allocated by authority.
 * @return Wrapper for a PROJ object, or null if out of memory.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_AuthorityFactory_createGeodeticObject
    (JNIEnv *env, jobject factory, jshort type, jstring code)
{
    const char *code_utf = env->GetStringUTFChars(code, nullptr);
    if (code_utf) {
        const std::string code_str = std::string(code_utf);                     // This constructor creates a copy.
        env->ReleaseStringUTFChars(code, code_utf);
        BaseObjectPtr rp = nullptr;
        try {
            AuthorityFactoryPtr pf = get_and_unwrap_ptr<AuthorityFactory>(env, factory);
            switch (type) {
                case org_osgeo_proj_Type_ANY:                         rp = pf->createObject                    (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_PRIME_MERIDIAN:              rp = pf->createPrimeMeridian             (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_ELLIPSOID:                   rp = pf->createEllipsoid                 (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_GEODETIC_REFERENCE_FRAME:    rp = pf->createGeodeticDatum             (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_VERTICAL_REFERENCE_FRAME:    rp = pf->createVerticalDatum             (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_TEMPORAL_DATUM:              // No specific function - use generic one.
                case org_osgeo_proj_Type_ENGINEERING_DATUM:           // No specific function - use generic one.
                case org_osgeo_proj_Type_DATUM:                       rp = pf->createDatum                     (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_CARTESIAN_CS:                // No specific function - use generic one.
                case org_osgeo_proj_Type_SPHERICAL_CS:                // No specific function - use generic one.
                case org_osgeo_proj_Type_ELLIPSOIDAL_CS:              // No specific function - use generic one.
                case org_osgeo_proj_Type_VERTICAL_CS:                 // No specific function - use generic one.
                case org_osgeo_proj_Type_TEMPORAL_CS:                 // No specific function - use generic one.
                case org_osgeo_proj_Type_COORDINATE_SYSTEM:           rp = pf->createCoordinateSystem          (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_GEOCENTRIC_CRS:              // Handled as GeodeticCRS by ISO 19111.
                case org_osgeo_proj_Type_GEODETIC_CRS:                rp = pf->createGeodeticCRS               (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_GEOGRAPHIC_CRS:              rp = pf->createGeographicCRS             (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_VERTICAL_CRS:                rp = pf->createVerticalCRS               (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_PROJECTED_CRS:               rp = pf->createProjectedCRS              (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_COMPOUND_CRS:                rp = pf->createCompoundCRS               (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_TEMPORAL_CRS:                // No specific function - use generic one.
                case org_osgeo_proj_Type_ENGINEERING_CRS:             // No specific function - use generic one.
                case org_osgeo_proj_Type_COORDINATE_REFERENCE_SYSTEM: rp = pf->createCoordinateReferenceSystem (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_CONVERSION:                  rp = pf->createConversion                (code_str).as_nullable(); break;
                case org_osgeo_proj_Type_COORDINATE_OPERATION:        rp = pf->createCoordinateOperation(code_str, false).as_nullable(); break;
                case org_osgeo_proj_Type_UNIT_OF_MEASURE: {
                    const UnitOfMeasure* unit = pf->createUnitOfMeasure(code_str).as_nullable().get();
                    return to_java_unit(env, factory, unit);
                }
                default: {
                    jclass c = env->FindClass(JPJ_FACTORY_EXCEPTION);
                    if (c) env->ThrowNew(c, "Unsupported object type.");
                }
            }
        } catch (const NoSuchAuthorityCodeException &e) {
            rethrow_as_java_exception(env, e);
        } catch (const std::exception &e) {
            rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
        }
        if (rp) try {
            return specific_subclass(env, factory, rp, type);
        } catch (const std::exception &e) {
            rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
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
 * All enumeration values in arguments are represented by integer, with -1 for the PROJ default value.
 *
 * @param  env                          The JNI environment.
 * @param  factory                      The Java object wrapping the authority factory to use.
 * @param  sourceCRS                    Input coordinate reference system.
 * @param  targetCRS                    Output coordinate reference system.
 * @param  westBoundLongitude           The minimal x value.
 * @param  eastBoundLongitude           The maximal x value.
 * @param  southBoundLatitude           The minimal y value.
 * @param  northBoundLatitude           The maximal y value.
 * @param  desiredAccuracy              Desired accuracy (in metres), or 0 for the best accuracy available.
 * @param  sourceAndTargetCRSExtentUse  How CRS extents are used when considering if a transformation can be used.
 * @param  spatialCriterion             Criterion when comparing the areas of validity.
 * @param  gridAvailabilityUse          How grid availability is used.
 * @param  allowUseIntermediateCRS      Whether an intermediate pivot CRS can be used for researching coordinate operations.
 * @param  discardSuperseded            Whether transformations that are superseded (but not deprecated) should be discarded.
 * @return The coordinate operations.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_AuthorityFactory_createOperation
    (JNIEnv *env, jobject factory, jobject sourceCRS, jobject targetCRS,
     jdouble westBoundLongitude, jdouble eastBoundLongitude,
     jdouble southBoundLatitude, jdouble northBoundLatitude,
     jdouble desiredAccuracy,
     jint sourceAndTargetCRSExtentUse, jint spatialCriterion, jint gridAvailabilityUse, jint allowUseIntermediateCRS,
     jboolean discardSuperseded)
{
    try {
        CRSNNPtr                        source  = get_shared_object<CRS>(env, sourceCRS);
        CRSNNPtr                        target  = get_shared_object<CRS>(env, targetCRS);
        AuthorityFactoryPtr             pf      = get_and_unwrap_ptr<AuthorityFactory>(env, factory);
        CoordinateOperationContextNNPtr context = CoordinateOperationContext::create(pf, nullptr, desiredAccuracy);
        context->setDiscardSuperseded(discardSuperseded);
        if (sourceAndTargetCRSExtentUse >= 0) {
            context->setSourceAndTargetCRSExtentUse(static_cast<CoordinateOperationContext::SourceTargetCRSExtentUse>(sourceAndTargetCRSExtentUse));
        }
        if (spatialCriterion >= 0) {
            context->setSpatialCriterion(static_cast<CoordinateOperationContext::SpatialCriterion>(spatialCriterion));
        }
        if (gridAvailabilityUse >= 0) {
            context->setGridAvailabilityUse(static_cast<CoordinateOperationContext::GridAvailabilityUse>(gridAvailabilityUse));
        }
        if (allowUseIntermediateCRS >= 0) {
            context->setAllowUseIntermediateCRS(static_cast<CoordinateOperationContext::IntermediateCRSUse>(allowUseIntermediateCRS));
        }
        if (northBoundLatitude > southBoundLatitude || eastBoundLongitude > westBoundLongitude) {
            context->setAreaOfInterest(Extent::createFromBBOX(
                    westBoundLongitude, southBoundLatitude,
                    eastBoundLongitude, northBoundLatitude));
        }
        /*
         * At this time, it does not seem worth to cache the CoordinateOperationFactory instance.
         */
        CoordinateOperationFactoryNNPtr opf = CoordinateOperationFactory::create();
        std::vector<CoordinateOperationNNPtr> operations = opf->createOperations(source, target, context);
        if (!operations.empty()) {
            BaseObjectPtr op = operations[0].as_nullable();
            if (op) {
                return specific_subclass(env, factory, op, org_osgeo_proj_Type_COORDINATE_OPERATION);
            }
        }
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_FACTORY_EXCEPTION, e);
    }
    return nullptr;
}




// </editor-fold>
// ┌────────────────────────────────────────────────────────────────────────────────────────────┐
// │                 CLASS Transform + Context.createPJ + SharedPointer.inverse                 │
// └────────────────────────────────────────────────────────────────────────────────────────────┘
// <editor-fold desc="Transform">


/**
 * Creates the PJ object from a coordinate operation, to be wrapped in a Transform.
 * The PJ creation may be costly, so the result should be cached.
 *
 * @param  env          The JNI environment.
 * @param  context      The thread context in which the operation is applied.
 * @param  operation    The Java object wrapping the coordinate operation to use.
 * @return pointer to the PJ object, or null if the creation failed.
 */
JNIEXPORT jlong JNICALL Java_org_osgeo_proj_Context_createPJ(JNIEnv *env, jobject context, jobject operation) {
    try {
        CoordinateOperationNNPtr cop       = get_shared_object<CoordinateOperation>(env, operation);
        DatabaseContextPtr       dbContext = get_database_context(env, context);
        PROJStringFormatterNNPtr formatter = PROJStringFormatter::create(PROJStringFormatter::Convention::PROJ_5, dbContext);
        std::string              projDef   = cop->exportToPROJString(formatter.get());
        PJ_CONTEXT               *ctx      = get_context(env, context);
        PJ                       *pj       = proj_create(ctx, projDef.c_str());
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
 * This method must be invoked before and after call to transform method.
 *
 * @param  env        The JNI environment.
 * @param  transform  The Java object wrapping the PJ to use.
 * @param  Context    The context to assign, or null for removing context assignment.
 */
JNIEXPORT void JNICALL Java_org_osgeo_proj_Transform_assign(JNIEnv *env, jobject transform, jobject context) {
    PJ *pj = get_PJ(env, transform);
    if (pj) {
        PJ_CONTEXT *ctx = context ? get_context(env, context) : nullptr;
        proj_assign_context(pj, ctx);
    }
}


/**
 * Whether a call to `GetPrimitiveArrayCritical(…)` gave us a copy of all data instead than giving us
 * a direct access to the Java array. Tests suggest that we get a direct access. However if a copy is
 * done, that could have severe performance implications. In current version we just log a warning in
 * order to be informed that there is a potential problem. But a future version could check this flag
 * for deciding to use `GetDoubleArrayRegion(…)` instead of `GetPrimitiveArrayCritical(…)`.
 *
 * See https://github.com/OSGeo/PROJ-JNI/issues/19
 */
std::atomic_flag arrayCriticalDoesCopies;


/**
 * Transforms in-place the coordinates in the given array.
 * The coordinates array shall contain (x,y,z,t,…) tuples,
 * where the z and any additional dimensions are optional.
 * Note that any dimension after the t value are ignored.
 *
 * Note that PJ are context-dependent. If the method is invoked in
 * a context different than the one for which PJ has been created,
 * then the following function shall be invoked first:
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
JNIEXPORT void JNICALL Java_org_osgeo_proj_Transform_transform
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
         * system call that may wait for the JVM and to not invoke any other JNI function.
         */
        jboolean isCopy;
        double *data = reinterpret_cast<jdouble*>(env->GetPrimitiveArrayCritical(coordinates, &isCopy));
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
                proj_errno_reset(pj);
                jclass c = env->FindClass(JPJ_TRANSFORM_EXCEPTION);
                if (c) env->ThrowNew(c, proj_errno_string(err));
            } else if (isCopy) {
                // Log this warning only on the first time.
                if (!arrayCriticalDoesCopies.test_and_set()) {
                    log(env, "Java Native Interface (JNI) had to copy coordinate array on this platform. "
                             "This constraint may reduce performance.");
                }
            }
        }
    }
}


/**
 * Creates the inverse of the wrapped operation.
 *
 * @param  env         The JNI environment.
 * @param  operation   The Java object wrapping the PROJ operation to inverse.
 * @return inverse operation, or null if out of memory.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_SharedPointer_inverse(JNIEnv *env, jobject operation) {
    try {
        CoordinateOperationNNPtr cop = get_shared_object<CoordinateOperation>(env, operation);
        cop = cop->inverse();
        BaseObjectPtr ptr = cop.as_nullable();
        return specific_subclass(env, operation, ptr, org_osgeo_proj_Type_COORDINATE_OPERATION);
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_NON_INVERTIBLE_EXCEPTION, e);
    }
    return nullptr;
}


/**
 * Creates an object with axis order such as the east direction is first and north direction is second,
 * if possible.
 *
 * @param  env         The JNI environment.
 * @param  operation   The Java object wrapping the PROJ operation to inverse.
 * @return an object with an axis order convenient for visualization.
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_SharedPointer_normalizeForVisualization(JNIEnv *env, jobject operation) {
    try {
        CoordinateOperationNNPtr cop = get_shared_object<CoordinateOperation>(env, operation);
        cop = cop->normalizeForVisualization();
        BaseObjectPtr ptr = cop.as_nullable();
        return specific_subclass(env, operation, ptr, org_osgeo_proj_Type_COORDINATE_OPERATION);
    } catch (const std::exception &e) {
        rethrow_as_java_exception(env, JPJ_ILLEGAL_ARGUMENT_EXCEPTION, e);
    }
    return nullptr;
}


/**
 * Destroys the PJ object.
 *
 * @param  env        The JNI environment.
 * @param  transform  The Java object wrapping the PJ to use.
 */
JNIEXPORT void JNICALL Java_org_osgeo_proj_Transform_destroy(JNIEnv *env, jobject transform) {
    jlong pjPtr = get_and_clear_ptr(env, transform);
    proj_destroy(reinterpret_cast<PJ*>(pjPtr));         // Does nothing if pjPtr is null.
}
// </editor-fold>
