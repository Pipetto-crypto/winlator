#pragma once

#include <string>
#include <algorithm>
#include <thread>
#include <functional>
#include <queue>
#include <cmath>
#include <dlfcn.h>
#include <unordered_set>
#include <condition_variable>
#include <android/choreographer.h>
#include <android/performance_hint.h>

#include "renderer_jni.hpp"
#include "view_transformation.hpp"
#include "window.hpp"
#include "effect_composer.hpp"
#include "cursor.hpp"

class DisplayX {
    private:
        enum class State {
            NONE,
            PAUSE,
            RESUME,
            CREATE_SURFACE,
            DESTROY_SURFACE,
            CHANGE_SURFACE
        };
        
        struct DisplayXLock {
           std::condition_variable cv;
           std::mutex mutex;
           
           std::unique_lock<std::mutex> lock() {
               return std::unique_lock<std::mutex>(mutex);
           }
           
           template<typename Predicate>
           void wait(std::unique_lock<std::mutex>& lock, Predicate pred) {
               cv.wait(lock, pred);
           }
           
           void notify() {
               cv.notify_all();
           }
        };
        
        struct PresentRequest {
            Drawable *drawable;
            int sync_fence;
            uint64_t presentId;
            uint8_t swapchainId;
            int clientFd;
            Window *window;
        };
        
        class PresentQueue {
            private:
                std::queue<std::unique_ptr<PresentRequest>> mQueue;
                std::unordered_set<Window *> mSet;

            public:
                void push(std::unique_ptr<PresentRequest> request) {
                    if (!request || !request->window)
                        return;
                        
                    if (mSet.count(request->window)) {
                        if (request->sync_fence >= 0)
                            close(request->sync_fence);
                            
                        return;
                    }        
                    
                    mSet.insert(request->window);     
                    mQueue.push(std::move(request));
                }

                std::unique_ptr<PresentRequest> pop() {
                    if (mQueue.empty())
                        return nullptr;
                    
                    auto val = std::move(mQueue.front());
                    mQueue.pop();
                    
                    mSet.erase(val->window);
                    return val;
                }

                bool empty() const {
                    return mQueue.empty();
                }
        };
        
        struct DisplayXSwapchain {
            uint8_t id;
            Window *window;
            std::vector<std::unique_ptr<Drawable>> images;
        };
        
        struct OnCompleteContext {
            std::vector<std::unique_ptr<PresentRequest>> requests;
        };
        
        JNIEnv *env;
        int surfaceWidth;
        int surfaceHeight;
        AChoreographer *choreographer;
        ViewTransformation viewTransformation;
        ANativeWindow *native_window;
        
        APerformanceHintManager *performanceHintManager = nullptr;
        APerformanceHintSession *performanceHintSession = nullptr;
        
        DisplayXLock eventLock;
        DisplayXLock presentLock;
        
        ASurfaceTransaction *windowTransaction;
        ASurfaceTransaction *cursorTransaction;
        PresentQueue presentRequests;
        std::queue<std::function<void()>> eventQueue;
        
        std::thread eventThread;
        std::thread networkThread;
        std::thread presentThread;
        
        State state = State::NONE;
        std::atomic_bool paused{false};
        std::atomic_bool stopped{false};
        std::atomic_bool hasSurface{false};
        std::atomic_bool cursorUpdate{false};
        std::atomic_bool surfaceChanged{false};
        std::atomic_bool perfMode{true};
        std::atomic_bool presentRR{true};
        std::atomic_bool backPressure{false};
        std::atomic_bool precisePresentation{false};
        
        bool requestUpdate = false;
        
        bool fullscreen = false;
        int eventsPending = 0;
        int64_t previousReportedWorkTime = 0;
        AVsyncId vsyncId = -1;
        
        void eventThreadLoop();
        void networkThreadLoop();
        void presentThreadLoop();
        static void onFrameCallback64(int64_t frameTimeNanos, void *data);
        static void onVsyncCallback(const AChoreographerFrameCallbackData* callbackData, void* data);
        static void onCommitCallback(void *context, ASurfaceTransactionStats *stats);
        static void onCompleteCallback(void *context, ASurfaceTransactionStats *stats);
        int64_t getCurrentTimeNanos();
        bool isPerformanceHintAPIAvailable();
        
        void createRootWindowControl();
        void createRootCursorControl();
        void resizeRootWindow();
        void destroyRootWindowControl();
        void destroyRootCursorControl();
        void restoreControlState();
        
    public:
        WindowManager *windowManager;
        CursorManager *cursorManager;
        JNIXServer *xServer;
        JNICache *cache;
        EffectComposer *effectComposer;
        
        bool cursorVisible = false;
        
        void start();
        void createSurface(ANativeWindow *window);
        void changeSurface(int width, int height);
        void destroySurface();
        void stop();
        void pause();
        void resume();
        
        void queueEvent(std::function<void()> func);
        void requestWindowUpdate(Window *window);
        void requestCursorUpdate();
        void updateCursorPosition();
        
        void createWindowControl(Window *window);
        void destroyWindowControl(Window *window);
        void mapWindow(Window *window);
        void unmapWindow(Window *window);
        void changeGeometry(Window *window, bool resized);
        void changeZOrder(Window *window);
        void reparentWindow(Window *window, Window *parent);
        void updateCursor(Cursor *cursor);
        void showCursor();
        void toggleFullscreen();
        void setPerformanceMode(bool perfMode);
        void setPresentRR(bool presentRR);
        void setBackPressure(bool backPressure);
        void setPrecisePresentation(bool precisePresentation);
};