#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_tangoplus_facebeauty_util_NativeLib_getSecretKey(JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, "mySuperSecretKeyTangoPlus0585!");
}