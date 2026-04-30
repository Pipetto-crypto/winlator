#include <jni.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_core_PatchElf_createElfObject(JNIEnv *env, jobject thiz, jstring path) {
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_destroyElfObject(JNIEnv *env, jobject thiz, jlong object_ptr) {
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_isChanged(JNIEnv *env, jobject thiz, jlong object_ptr) {
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_winlator_cmod_core_PatchElf_getInterpreter(JNIEnv *env, jobject thiz, jlong object_ptr) {
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_setInterpreter(JNIEnv *env, jobject thiz, jlong object_ptr,
                                               jstring interpreter) {
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_winlator_cmod_core_PatchElf_getOsAbi(JNIEnv *env, jobject thiz, jlong object_ptr) {
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_replaceOsAbi(JNIEnv *env, jobject thiz, jlong object_ptr,
                                             jstring os_abi) {
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_winlator_cmod_core_PatchElf_getSoName(JNIEnv *env, jobject thiz, jlong object_ptr) {
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_replaceSoName(JNIEnv *env, jobject thiz, jlong object_ptr,
                                              jstring so_name) {
}
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_winlator_cmod_core_PatchElf_getRPath(JNIEnv *env, jobject thiz, jlong object_ptr) {
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_addRPath(JNIEnv *env, jobject thiz, jlong object_ptr,
                                         jstring rpath) {
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_removeRPath(JNIEnv *env, jobject thiz, jlong object_ptr,
                                            jstring rpath) {
}
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_winlator_cmod_core_PatchElf_getNeeded(JNIEnv *env, jobject thiz, jlong object_ptr) {
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_addNeeded(JNIEnv *env, jobject thiz, jlong object_ptr,
                                          jstring needed) {
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_removeNeeded(JNIEnv *env, jobject thiz, jlong object_ptr,
                                             jstring needed) {
}
