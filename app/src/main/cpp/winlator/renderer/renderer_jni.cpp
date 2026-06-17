#include <jni.h>
#include <android/native_window_jni.h>
#include "egl.hpp"

EGLRenderer renderer;

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_widget_XServerView_createSurface(JNIEnv *env, jobject thiz, jobject surface) {
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    renderer.createSurface(window);
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_widget_XServerView_init(JNIEnv *env, jobject thiz) {
    renderer.init();
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_widget_XServerView_stop(JNIEnv *env, jobject thiz) {
    renderer.stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_widget_XServerView_destroySurface(JNIEnv *env, jobject thiz) {
    renderer.destroySurface();
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_widget_XServerView_updateViewport(JNIEnv *env, jobject thiz, jint x, jint y, jint width, jint height) {
    renderer.updateViewport(x, y, width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_widget_XServerView_updateScissor(JNIEnv *env, jobject thiz, jint active, jint x, jint y, jint width, jint height) {
    renderer.updateScissor(active, x, y, width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_widget_XServerView_beginRendering(JNIEnv *env, jobject thiz, jint width, jint height) {
    renderer.beginRendering(width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_widget_XServerView_finishRendering(JNIEnv *env, jobject thiz) {
    renderer.finishRendering();
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_widget_XServerView_renderDrawable(JNIEnv *env, jobject thiz, jint textureId, jfloatArray xForm, jboolean isFromWindow) {
    jsize len = env->GetArrayLength(xForm);
    float *xform = reinterpret_cast<float *>(env->GetFloatArrayElements(xForm, nullptr));
    renderer.renderDrawable(textureId, len, xform, isFromWindow);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_winlator_cmod_renderer_Texture_nativeAllocateTexture(JNIEnv *env, jobject thiz, jshort width, jshort height, jobject data) {
    void *buf = nullptr;
    if (data != nullptr)
        buf = env->GetDirectBufferAddress(data);
        
    return renderer.allocateTexture(width, height, buf);
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_renderer_Texture_nativeUpdateTextureDrawable(JNIEnv *env, jobject thiz, jint textureId, jshort width, jshort height, jobject data) {
    void *buf = nullptr;
    if (data != nullptr)
        buf = env->GetDirectBufferAddress(data);
        
    renderer.updateTextureDrawable(textureId, width, height, buf);
}

extern "C" JNIEXPORT void JNICALL
Java_com_winlator_cmod_renderer_Texture_nativeDestroyTexture(JNIEnv *env, jobject thiz, jint textureId) {
    renderer.destroyTexture(textureId);
}