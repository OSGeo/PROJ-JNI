/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_osgeo_proj_SharedPointer */

#ifndef _Included_org_osgeo_proj_SharedPointer
#define _Included_org_osgeo_proj_SharedPointer
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    getObjectProperty
 * Signature: (S)Lorg/osgeo/proj/IdentifiableObject;
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_SharedPointer_getObjectProperty
  (JNIEnv *, jobject, jshort);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    getStringProperty
 * Signature: (S)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_osgeo_proj_SharedPointer_getStringProperty
  (JNIEnv *, jobject, jshort);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    getNumericProperty
 * Signature: (S)D
 */
JNIEXPORT jdouble JNICALL Java_org_osgeo_proj_SharedPointer_getNumericProperty
  (JNIEnv *, jobject, jshort);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    getArrayProperty
 * Signature: (S)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_org_osgeo_proj_SharedPointer_getArrayProperty
  (JNIEnv *, jobject, jshort);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    getIntegerProperty
 * Signature: (S)I
 */
JNIEXPORT jint JNICALL Java_org_osgeo_proj_SharedPointer_getIntegerProperty
  (JNIEnv *, jobject, jshort);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    getBooleanProperty
 * Signature: (S)Z
 */
JNIEXPORT jboolean JNICALL Java_org_osgeo_proj_SharedPointer_getBooleanProperty
  (JNIEnv *, jobject, jshort);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    getVectorSize
 * Signature: (S)I
 */
JNIEXPORT jint JNICALL Java_org_osgeo_proj_SharedPointer_getVectorSize
  (JNIEnv *, jobject, jshort);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    getVectorElement
 * Signature: (SI)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_SharedPointer_getVectorElement
  (JNIEnv *, jobject, jshort, jint);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    searchVectorElement
 * Signature: (SLjava/lang/String;)Lorg/osgeo/proj/IdentifiableObject;
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_SharedPointer_searchVectorElement
  (JNIEnv *, jobject, jshort, jstring);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    inverse
 * Signature: ()Lorg/osgeo/proj/IdentifiableObject;
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_SharedPointer_inverse
  (JNIEnv *, jobject);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    normalizeForVisualization
 * Signature: ()Lorg/osgeo/proj/IdentifiableObject;
 */
JNIEXPORT jobject JNICALL Java_org_osgeo_proj_SharedPointer_normalizeForVisualization
  (JNIEnv *, jobject);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    format
 * Signature: (Lorg/osgeo/proj/Context;IIZZ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_osgeo_proj_SharedPointer_format
  (JNIEnv *, jobject, jobject, jint, jint, jboolean, jboolean);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    isEquivalentTo
 * Signature: (Lorg/osgeo/proj/SharedPointer;I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_osgeo_proj_SharedPointer_isEquivalentTo
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    rawPointer
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_osgeo_proj_SharedPointer_rawPointer
  (JNIEnv *, jobject);

/*
 * Class:     org_osgeo_proj_SharedPointer
 * Method:    release
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_osgeo_proj_SharedPointer_release
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
