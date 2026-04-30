
#pragma once

#include "loader_platform.hpp"

#include <openxr/openxr.h>

#include <string>
#include <vector>
#include <unordered_map>
#include <mutex>
#include <memory>

namespace Json {
class Value;
}

class RuntimeManifestFile;
struct XrGeneratedDispatchTable;

class RuntimeInterface {
   public:
    virtual ~RuntimeInterface();

    static XrResult LoadRuntime(const std::string& openxr_command);
    static void UnloadRuntime(const std::string& openxr_command);
    static RuntimeInterface& GetRuntime() { return *(GetInstance().get()); }
    static XrResult GetInstanceProcAddr(XrInstance instance, const char* name, PFN_xrVoidFunction* function);

    static const XrGeneratedDispatchTable* GetDispatchTable(XrInstance instance);
    static const XrGeneratedDispatchTable* GetDebugUtilsMessengerDispatchTable(XrDebugUtilsMessengerEXT messenger);

    void GetInstanceExtensionProperties(std::vector<XrExtensionProperties>& extension_properties);
    bool SupportsExtension(const std::string& extension_name);
    XrResult CreateInstance(const XrInstanceCreateInfo* info, XrInstance* instance);
    XrResult DestroyInstance(XrInstance instance);
    bool TrackDebugMessenger(XrInstance instance, XrDebugUtilsMessengerEXT messenger);
    void ForgetDebugMessenger(XrDebugUtilsMessengerEXT messenger);

    RuntimeInterface() = delete;

    RuntimeInterface(const RuntimeInterface&) = delete;
    RuntimeInterface& operator=(const RuntimeInterface&) = delete;

   private:
    RuntimeInterface(LoaderPlatformLibraryHandle runtime_library, PFN_xrGetInstanceProcAddr get_instance_proc_addr);
    void SetSupportedExtensions(std::vector<std::string>& supported_extensions);
    static XrResult TryLoadingSingleRuntime(const std::string& openxr_command, std::unique_ptr<RuntimeManifestFile>& manifest_file);

    static std::unique_ptr<RuntimeInterface>& GetInstance() {
        static std::unique_ptr<RuntimeInterface> instance;
        return instance;
    }

    LoaderPlatformLibraryHandle _runtime_library;
    PFN_xrGetInstanceProcAddr _get_instance_proc_addr;
    std::unordered_map<XrInstance, std::unique_ptr<XrGeneratedDispatchTable>> _dispatch_table_map;
    std::mutex _dispatch_table_mutex;
    std::unordered_map<XrDebugUtilsMessengerEXT, XrInstance> _messenger_to_instance_map;
    std::mutex _messenger_to_instance_mutex;
    std::vector<std::string> _supported_extensions;
};
