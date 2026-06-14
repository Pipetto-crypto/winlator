#include "egl.hpp"

void EGLRenderer::createSurface(ANativeWindow *window) {
    EGLBoolean result;
    
    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display == EGL_NO_DISPLAY) {
        printf("Failed to get default display");
        return;
    }    
        
    EGLint major, minor;
    result = eglInitialize(display, &major, &minor);
    if (result != EGL_TRUE) {
        printf("Failed to initialize egl");
        return;
    }
    
    printf("Initialized egl major %d minor %d", major, minor);
    
    EGLConfig config;
    int num_configs;
    
    const EGLint attrib_list[] = {
        EGL_RED_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 0,
        EGL_STENCIL_SIZE, 0,
        EGL_NONE
    };
    
    result = eglChooseConfig(display, attrib_list, &config, 1, &num_configs);
    if (result != EGL_TRUE || num_configs < 0) {
        printf("Failed to find suitable egl config");
        return;
    }
    
    const EGLint ctx_attrib_list[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };
    
    eglBindAPI(EGL_OPENGL_ES_API);
    
    surface = eglCreateWindowSurface(display, config, window, nullptr);
    if (surface == EGL_NO_SURFACE) {
        printf("Failed to create window surface");
        return;
    }
    
    context = eglCreateContext(display, config, EGL_NO_CONTEXT, ctx_attrib_list);
    if (context == EGL_NO_CONTEXT) {
        printf("Failed to create egl context");
        return;
    }
    
    result = eglMakeCurrent(display, surface, surface, context);
    if (result != EGL_TRUE) {
        printf("Failed to make context current");
        return;
    }
        
    glFrontFace(GL_CCW);
    glDisable(GL_CULL_FACE);

    glDisable(GL_DEPTH_TEST);
    glDepthMask(false);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    
    drawableShader = new DrawableShader();
}

void EGLRenderer::beginRendering(int width, int height) {
    glClear(GL_COLOR_BUFFER_BIT);
    drawableShader->use();
    glUniform2f(drawableShader->getUniformLoc("viewSize"), width, height);
}

void EGLRenderer::updateScissor(int active, int x, int y, int width, int height) {
    if (active) {
        glEnable(GL_SCISSOR_TEST);
        glScissor(x, y, width, height);
    }
    else {
        glDisable(GL_SCISSOR_TEST);
    }
}

void EGLRenderer::updateViewport(int x, int y, int width, int height) {
    glViewport(x, y, width, height);
}

void EGLRenderer::renderDrawable(int textureId, int length, float xform[], bool isFromWindow) {
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glUniform1i(drawableShader->getUniformLoc("texture"), 0);
    glUniform1fv(drawableShader->getUniformLoc("xform"), length, xform);
    glUniform1i(drawableShader->getUniformLoc("is_cursor"), isFromWindow ? 0 : 1);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glBindTexture(GL_TEXTURE_2D, 0);
}

int EGLRenderer::allocateTexture(uint16_t width, uint16_t height, void *data) {
    int textureId;
    glGenTextures(1, (GLuint *)&textureId);

    glActiveTexture(GL_TEXTURE0);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
    glBindTexture(GL_TEXTURE_2D, textureId);

    if (data != nullptr) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_BGRA_EXT, width, height, 0, GL_BGRA_EXT, GL_UNSIGNED_BYTE, data);
    }

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

    glBindTexture(GL_TEXTURE_2D, 0);
    
    return textureId;
}

void EGLRenderer::destroyTexture(int textureId) {
    glDeleteTextures(1, (GLuint *)&textureId);
}

void EGLRenderer::updateTextureDrawable(int textureId, uint16_t width, uint16_t height, void *data) {
    glBindTexture(GL_TEXTURE_2D, textureId);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_BGRA_EXT, GL_UNSIGNED_BYTE, data);
    glBindTexture(GL_TEXTURE_2D, 0);
}

void EGLRenderer::finishRendering() {
    eglSwapBuffers(display, surface);
    drawableShader->disable();
}

void EGLRenderer::destroySurface() {
    glFinish();
    delete drawableShader;
    eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroySurface(display, surface);
    eglDestroyContext(display, context);
    eglTerminate(display);
}
