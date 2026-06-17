#pragma once

#include <android/native_window.h>
#include <android/hardware_buffer.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <string>
#include <algorithm>

#include "shader.hpp"

#define LOG_TAG "EGLRenderer"
#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

class EGLRenderer {
    private:
        EGLDisplay display;
        EGLConfig config;
        EGLSurface surface;
        EGLContext context;
        DrawableShader *drawableShader;
    
    public:
        void init();
        void createSurface(ANativeWindow *window);
        void destroySurface();
        void stop();
        void beginRendering(int width, int height);
        void updateViewport(int x, int y, int width, int height);
        void updateScissor(int active, int x, int y, int width, int height);
        void renderDrawable(int textureId, int length, float xform[], bool isFromWindow);
        void finishRendering();
        void updateTextureDrawable(int textureId, uint16_t width, uint16_t height, void *data);
        void destroyTexture(int textureId);
        int allocateTexture(uint16_t width, uint16_t height, void *data);
};