
#include "loader_init_data.hpp"

#ifdef XR_KHR_LOADER_INIT_SUPPORT

XrResult LoaderInitData::initialize(const XrLoaderInitInfoBaseHeaderKHR* info) {
#if defined(XR_USE_PLATFORM_ANDROID)
    if (info->type != XR_TYPE_LOADER_INIT_INFO_ANDROID_KHR) {
        return XR_ERROR_VALIDATION_FAILURE;
    }
    auto cast_info = reinterpret_cast<XrLoaderInitInfoAndroidKHR const*>(info);

    if (cast_info->applicationVM == nullptr) {
        return XR_ERROR_VALIDATION_FAILURE;
    }
    if (cast_info->applicationContext == nullptr) {
        return XR_ERROR_VALIDATION_FAILURE;
    }

    _data = *cast_info;
    _data.next = nullptr;
    jni::init(static_cast<jni::JavaVM*>(_data.applicationVM));
    const jni::Object context = jni::Object{static_cast<jni::jobject>(_data.applicationContext)};

    const auto assetManager = context.call<jni::Object>("getAssets()Landroid/content/res/AssetManager;");
    _android_asset_manager = AAssetManager_fromJava(jni::env(), assetManager.getHandle());

    const auto applicationContext = context.call<jni::Object>("getApplicationContext()Landroid/content/Context;");
    const auto applicationInfo = context.call<jni::Object>("getApplicationInfo()Landroid/content/pm/ApplicationInfo;");
    _native_library_path = applicationInfo.get<std::string>("nativeLibraryDir");
#else
#error "Platform specific XR_KHR_loader_init structure is not defined for this platform."
#endif  // XR_USE_PLATFORM_ANDROID

    _initialized = true;
    return XR_SUCCESS;
}

XrResult InitializeLoaderInitData(const XrLoaderInitInfoBaseHeaderKHR* loaderInitInfo) {
    return LoaderInitData::instance().initialize(loaderInitInfo);
}

#ifdef XR_USE_PLATFORM_ANDROID
std::string GetAndroidNativeLibraryDir() { return LoaderInitData::instance()._native_library_path; }

void* Android_Get_Asset_Manager() { return LoaderInitData::instance()._android_asset_manager; }
#endif  // XR_USE_PLATFORM_ANDROID

#endif  // XR_KHR_LOADER_INIT_SUPPORT
