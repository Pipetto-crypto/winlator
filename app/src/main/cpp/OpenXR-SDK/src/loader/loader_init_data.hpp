
#pragma once

#include <xr_dependencies.h>
#include <openxr/openxr_platform.h>

#ifdef XR_USE_PLATFORM_ANDROID
#include <json/value.h>
#include <android/asset_manager_jni.h>
#include "android_utilities.h"
#endif  // XR_USE_PLATFORM_ANDROID

#ifdef XR_KHR_LOADER_INIT_SUPPORT

/*!
 * Stores a copy of the data passed to the xrInitializeLoaderKHR function in a singleton.
 */
class LoaderInitData {
   public:
    /*!
     * Singleton accessor.
     */
    static LoaderInitData& instance() {
        static LoaderInitData obj;
        return obj;
    }

#if defined(XR_USE_PLATFORM_ANDROID)
    /*!
     * Type alias for the platform-specific structure type.
     */
    using StructType = XrLoaderInitInfoAndroidKHR;
    /*!
     * Native library path.
     */
    std::string _native_library_path;
    /*!
     * Android asset manager.
     */
    AAssetManager* _android_asset_manager;
#else
#error "Platform specific XR_KHR_loader_init structure is not defined for this platform."
#endif

    /*!
     * Get our copy of the data, casted to pass to the runtime's matching method.
     */
    const XrLoaderInitInfoBaseHeaderKHR* getParam() const { return reinterpret_cast<const XrLoaderInitInfoBaseHeaderKHR*>(&_data); }

    /*!
     * Get the data via its real structure type.
     */
    const StructType& getData() const { return _data; }

    /*!
     * Has this been correctly initialized?
     */
    bool initialized() const noexcept { return _initialized; }

    /*!
     * Initialize loader data - called by InitializeLoaderInitData() and thus ultimately by the loader's xrInitializeLoaderKHR
     * implementation. Each platform that needs this extension will provide an implementation of this.
     */
    XrResult initialize(const XrLoaderInitInfoBaseHeaderKHR* info);

   private:
    LoaderInitData() = default;
    StructType _data = {};
    bool _initialized = false;
};

XrResult InitializeLoaderInitData(const XrLoaderInitInfoBaseHeaderKHR* loaderInitInfo);

#ifdef XR_USE_PLATFORM_ANDROID
XrResult GetPlatformRuntimeVirtualManifest(Json::Value& out_manifest);
std::string GetAndroidNativeLibraryDir();
void* Android_Get_Asset_Manager();
#endif  // XR_USE_PLATFORM_ANDROID

#endif  // XR_KHR_LOADER_INIT_SUPPORT
