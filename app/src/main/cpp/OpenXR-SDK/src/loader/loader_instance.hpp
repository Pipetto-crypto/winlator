
#pragma once

#include "extra_algorithms.h"

#include <openxr/openxr.h>
#include <openxr/openxr_loader_negotiation.h>

#include <array>
#include <cmath>
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>
#include <vector>

class ApiLayerInterface;
struct XrGeneratedDispatchTable;
class LoaderInstance;

namespace ActiveLoaderInstance {
XrResult Set(std::unique_ptr<LoaderInstance> loader_instance, const char* log_function_name);

bool IsAvailable();

XrResult Get(LoaderInstance** loader_instance, const char* log_function_name);

void Remove();
};  // namespace ActiveLoaderInstance

class LoaderInstance {
   public:
    static XrResult CreateInstance(PFN_xrGetInstanceProcAddr get_instance_proc_addr_term, PFN_xrCreateInstance create_instance_term,
                                   PFN_xrCreateApiLayerInstance create_api_layer_instance_term,
                                   std::vector<std::unique_ptr<ApiLayerInterface>> layer_interfaces,
                                   const XrInstanceCreateInfo* createInfo, std::unique_ptr<LoaderInstance>* loader_instance);
    static const std::array<XrExtensionProperties, 1>& LoaderSpecificExtensions();

    virtual ~LoaderInstance();

    XrInstance GetInstanceHandle() { return _runtime_instance; }
    const std::unique_ptr<XrGeneratedDispatchTable>& DispatchTable() { return _dispatch_table; }
    std::vector<std::unique_ptr<ApiLayerInterface>>& LayerInterfaces() { return _api_layer_interfaces; }
    bool ExtensionIsEnabled(const std::string& extension);
    XrDebugUtilsMessengerEXT DefaultDebugUtilsMessenger() { return _messenger; }
    void SetDefaultDebugUtilsMessenger(XrDebugUtilsMessengerEXT messenger) { _messenger = messenger; }
    XrResult GetInstanceProcAddr(const char* name, PFN_xrVoidFunction* function);

   private:
    LoaderInstance(XrInstance instance, const XrInstanceCreateInfo* createInfo, PFN_xrGetInstanceProcAddr topmost_gipa,
                   std::vector<std::unique_ptr<ApiLayerInterface>> api_layer_interfaces);

   private:
    XrInstance _runtime_instance{XR_NULL_HANDLE};
    PFN_xrGetInstanceProcAddr _topmost_gipa{nullptr};
    std::vector<std::string> _enabled_extensions;
    std::vector<std::unique_ptr<ApiLayerInterface>> _api_layer_interfaces;

    std::unique_ptr<XrGeneratedDispatchTable> _dispatch_table;
    XrDebugUtilsMessengerEXT _messenger{XR_NULL_HANDLE};
};
