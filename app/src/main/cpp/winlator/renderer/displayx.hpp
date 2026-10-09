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

#define VK_PRESENT_MODE_MAILBOX_KHR 1
#define VK_PRESENT_MODE_FIFO_KHR 2

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
                std::unordered_map<Window *, std::deque<std::unique_ptr<PresentRequest>>> mUpdatableWindows;
                std::vector<Window *> mUpdatedWindows;
                //Window *lastUpdatedWindow = nullptr;

            public:
                void push(std::unique_ptr<PresentRequest> request, bool singleElement) {
                    if (!request || !request->window)
                        return;
                        
                    auto &queue = mUpdatableWindows[request->window];    
                    mUpdatedWindows.push_back(request->window);
                    //lastUpdatedWindow = request->window;
                    
                    if (queue.empty()) {
                        queue.push_back(std::move(request));
                        return;
                    }
                    
                    if (singleElement) {
                        auto &item = queue.front();
                        if (item->sync_fence >= 0) 
                            close(item->sync_fence);
                            
                        queue.pop_front();
                        queue.push_back(std::move(request));
                        return;
                    }
                    
                    queue.push_back(std::move(request));
                }
                
                void removeWindow(Window *window) {
                    if (!window)
                        return;
                        
                    auto it = mUpdatableWindows.find(window);
                    if (it == mUpdatableWindows.end())
                        return;
                        
                    for (auto& request : it->second) {
                        if (request->sync_fence >= 0)
                            close(request->sync_fence);
                    }
                    
                    it->second.clear();
                    
                    auto itv = std::find(mUpdatedWindows.begin(), mUpdatedWindows.end(), window);
                    if (itv != mUpdatedWindows.end()) 
                        mUpdatedWindows.erase(itv);
                        
                    /*
                    if (lastUpdatedWindow == window)
                        lastUpdatedWindow = nullptr;
                    */    
                        
                    mUpdatableWindows.erase(it);    
                }

                std::deque<std::unique_ptr<PresentRequest>> getLast() {
                    if (mUpdatedWindows.empty())
                        return {};
                        
                    Window *lastUpdatedWindow = mUpdatedWindows.back();
                    mUpdatedWindows.pop_back();
                        
                    auto it = mUpdatableWindows.find(lastUpdatedWindow);
                    if (it == mUpdatableWindows.end() || it->second.empty())
                        return {};
                    
                    return std::exchange(it->second, {});
                }
                
                std::deque<std::unique_ptr<PresentRequest>> buildWindowTree() {
                    std::deque<std::unique_ptr<PresentRequest>> out;
                    
                    for (auto *window : mUpdatedWindows) {
                        auto it = mUpdatableWindows.find(window);
                        if (it == mUpdatableWindows.end() || it->second.empty())
                            continue;
                            
                        auto &queue = it->second;
                        if (out.empty()) {
                            out.swap(queue);
                        }
                        else {
                            out.insert(out.end(), std::make_move_iterator(queue.begin()), std::make_move_iterator(queue.end()));
                            queue.clear();
                        }
                    }
                    
                    mUpdatedWindows.clear();
                    return out;
                }

                bool empty() const {
                    return mUpdatedWindows.empty();
                }
        };
        
        struct DisplayXSwapchain {
            uint8_t id;
            Window *window;
            uint32_t imageCount;
            uint32_t presentMode;
            uint32_t format;
            uint32_t width;
            uint32_t height;
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