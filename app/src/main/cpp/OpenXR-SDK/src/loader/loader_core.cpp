
#if defined(_MSC_VER) && !defined(_CRT_SECURE_NO_WARNINGS)
#define _CRT_SECURE_NO_WARNINGS
#endif  // defined(_MSC_VER) && !defined(_CRT_SECURE_NO_WARNINGS)

#include "api_layer_interface.hpp"
#include "exception_handling.hpp"
#include "hex_and_handles.h"
#include "loader_init_data.hpp"
#include "loader_instance.hpp"
#include "loader_logger_recorders.hpp"
#include "loader_logger.hpp"
#include "loader_platform.hpp"
#include "runtime_interface.hpp"
#include "xr_generated_dispatch_table_core.h"
#include "xr_generated_loader.hpp"

#include <openxr/openxr.h>

#include <cstring>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

static std::mutex &GetGlobalLoaderMutex() {
    static std::mutex loader_mutex;
    return loader_mutex;
}

static XRAPI_ATTR XrResult XRAPI_CALL LoaderTrampolineCreateDebugUtilsMessengerEXT(
    XrInstance instance, const XrDebugUtilsMessengerCreateInfoEXT *createInfo, XrDebugUtilsMessengerEXT *messenger);
static XRAPI_ATTR XrResult XRAPI_CALL LoaderTrampolineDestroyDebugUtilsMessengerEXT(XrDebugUtilsMessengerEXT messenger);

static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermGetInstanceProcAddr(XrInstance, const char *, PFN_xrVoidFunction *);
static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermCreateInstance(const XrInstanceCreateInfo *, XrInstance *);
static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermCreateApiLayerInstance(const XrInstanceCreateInfo *,
                                                                         const struct XrApiLayerCreateInfo *, XrInstance *);
static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermSetDebugUtilsObjectNameEXT(XrInstance, const XrDebugUtilsObjectNameInfoEXT *);
static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermCreateDebugUtilsMessengerEXT(XrInstance,
                                                                               const XrDebugUtilsMessengerCreateInfoEXT *,
                                                                               XrDebugUtilsMessengerEXT *);
static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermDestroyDebugUtilsMessengerEXT(XrDebugUtilsMessengerEXT);
static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermSubmitDebugUtilsMessageEXT(
    XrInstance instance, XrDebugUtilsMessageSeverityFlagsEXT messageSeverity, XrDebugUtilsMessageTypeFlagsEXT messageTypes,
    const XrDebugUtilsMessengerCallbackDataEXT *callbackData);
static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrGetInstanceProcAddr(XrInstance instance, const char *name,
                                                                  PFN_xrVoidFunction *function);

template <size_t max_length>
inline bool IsMissingNullTerminator(const char (&str)[max_length]) {
    for (size_t index = 0; index < max_length; ++index) {
        if (str[index] == '\0') {
            return false;
        }
    }
    return true;
}

#ifdef XR_KHR_LOADER_INIT_SUPPORT  // platforms that support XR_KHR_loader_init.
XRAPI_ATTR XrResult XRAPI_CALL LoaderXrInitializeLoaderKHR(const XrLoaderInitInfoBaseHeaderKHR *loaderInitInfo) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrInitializeLoaderKHR", "Entering loader trampoline");
    return InitializeLoaderInitData(loaderInitInfo);
}
XRLOADER_ABI_CATCH_FALLBACK
#endif

static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrEnumerateApiLayerProperties(uint32_t propertyCapacityInput,
                                                                          uint32_t *propertyCountOutput,
                                                                          XrApiLayerProperties *properties) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrEnumerateApiLayerProperties", "Entering loader trampoline");

    std::unique_lock<std::mutex> loader_lock(GetGlobalLoaderMutex());

    XrResult result = ApiLayerInterface::GetApiLayerProperties("xrEnumerateApiLayerProperties", propertyCapacityInput,
                                                               propertyCountOutput, properties);
    if (XR_FAILED(result)) {
        LoaderLogger::LogErrorMessage("xrEnumerateApiLayerProperties", "Failed ApiLayerInterface::GetApiLayerProperties");
    }

    return result;
}
XRLOADER_ABI_CATCH_FALLBACK

static XRAPI_ATTR XrResult XRAPI_CALL
LoaderXrEnumerateInstanceExtensionProperties(const char *layerName, uint32_t propertyCapacityInput, uint32_t *propertyCountOutput,
                                             XrExtensionProperties *properties) XRLOADER_ABI_TRY {
    bool just_layer_properties = false;
    LoaderLogger::LogVerboseMessage("xrEnumerateInstanceExtensionProperties", "Entering loader trampoline");

    if (nullptr == propertyCountOutput) {
        return XR_ERROR_VALIDATION_FAILURE;
    }

    if (nullptr != layerName && 0 != strlen(layerName)) {
        just_layer_properties = true;
    }

    std::vector<XrExtensionProperties> extension_properties = {};
    XrResult result;

    {
        std::unique_lock<std::mutex> loader_lock(GetGlobalLoaderMutex());

        result = ApiLayerInterface::GetInstanceExtensionProperties("xrEnumerateInstanceExtensionProperties", layerName,
                                                                   extension_properties);
        if (XR_SUCCEEDED(result) && !just_layer_properties) {
            result = RuntimeInterface::LoadRuntime("xrEnumerateInstanceExtensionProperties");
            if (XR_SUCCEEDED(result)) {
                RuntimeInterface::GetRuntime().GetInstanceExtensionProperties(extension_properties);
            } else {
                LoaderLogger::LogErrorMessage("xrEnumerateInstanceExtensionProperties",
                                              "Failed to find default runtime with RuntimeInterface::LoadRuntime()");
            }
        }
    }

    if (XR_FAILED(result)) {
        LoaderLogger::LogErrorMessage("xrEnumerateInstanceExtensionProperties", "Failed querying extension properties");
        return result;
    }

    if (!just_layer_properties) {
        for (const XrExtensionProperties &loader_prop : LoaderInstance::LoaderSpecificExtensions()) {
            bool found_prop = false;
            for (XrExtensionProperties &existing_prop : extension_properties) {
                if (0 == strcmp(existing_prop.extensionName, loader_prop.extensionName)) {
                    found_prop = true;
                    if (existing_prop.extensionVersion < loader_prop.extensionVersion) {
                        existing_prop.extensionVersion = loader_prop.extensionVersion;
                    }
                    break;
                }
            }
            if (!found_prop) {
                extension_properties.push_back(loader_prop);
            }
        }
    }

    auto num_extension_properties = static_cast<uint32_t>(extension_properties.size());
    if (propertyCapacityInput == 0) {
        *propertyCountOutput = num_extension_properties;
    } else if (nullptr != properties) {
        if (propertyCapacityInput < num_extension_properties) {
            *propertyCountOutput = num_extension_properties;
            LoaderLogger::LogValidationErrorMessage("VUID-xrEnumerateInstanceExtensionProperties-propertyCountOutput-parameter",
                                                    "xrEnumerateInstanceExtensionProperties", "insufficient space in array");
            return XR_ERROR_SIZE_INSUFFICIENT;
        }

        uint32_t num_to_copy = num_extension_properties;
        if (propertyCapacityInput < num_to_copy) {
            num_to_copy = propertyCapacityInput;
        }
        bool properties_valid = true;
        for (uint32_t prop = 0; prop < propertyCapacityInput && prop < extension_properties.size(); ++prop) {
            if (XR_TYPE_EXTENSION_PROPERTIES != properties[prop].type) {
                properties_valid = false;
                LoaderLogger::LogValidationErrorMessage("VUID-XrExtensionProperties-type-type",
                                                        "xrEnumerateInstanceExtensionProperties", "unknown type in properties");
            }
            if (properties_valid) {
                properties[prop] = extension_properties[prop];
            }
        }
        if (!properties_valid) {
            LoaderLogger::LogValidationErrorMessage("VUID-xrEnumerateInstanceExtensionProperties-properties-parameter",
                                                    "xrEnumerateInstanceExtensionProperties", "invalid properties");
            return XR_ERROR_VALIDATION_FAILURE;
        }
        if (nullptr != propertyCountOutput) {
            *propertyCountOutput = num_to_copy;
        }
    } else {
        return XR_ERROR_VALIDATION_FAILURE;
    }
    LoaderLogger::LogVerboseMessage("xrEnumerateInstanceExtensionProperties", "Completed loader trampoline");
    return XR_SUCCESS;
}
XRLOADER_ABI_CATCH_FALLBACK

static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrCreateInstance(const XrInstanceCreateInfo *info,
                                                             XrInstance *instance) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrCreateInstance", "Entering loader trampoline");
    if (nullptr == info) {
        LoaderLogger::LogValidationErrorMessage("VUID-xrCreateInstance-info-parameter", "xrCreateInstance", "must be non-NULL");
        return XR_ERROR_VALIDATION_FAILURE;
    }
    uint16_t app_major = XR_VERSION_MAJOR(info->applicationInfo.apiVersion);  // NOLINT
    uint16_t app_minor = XR_VERSION_MINOR(info->applicationInfo.apiVersion);  // NOLINT
    uint16_t loader_major = XR_VERSION_MAJOR(XR_CURRENT_API_VERSION);         // NOLINT
    uint16_t loader_minor = XR_VERSION_MINOR(XR_CURRENT_API_VERSION);         // NOLINT
    if (app_major > loader_major || (app_major == loader_major && app_minor > loader_minor)) {
        std::ostringstream oss;
        oss << "xrCreateInstance called with invalid API version " << app_major << "." << app_minor
            << ".  Max supported version is " << loader_major << "." << loader_minor;
        LoaderLogger::LogErrorMessage("xrCreateInstance", oss.str());
        return XR_ERROR_API_VERSION_UNSUPPORTED;
    }

    if (nullptr == instance) {
        LoaderLogger::LogValidationErrorMessage("VUID-xrCreateInstance-instance-parameter", "xrCreateInstance", "must be non-NULL");
        return XR_ERROR_VALIDATION_FAILURE;
    }

    std::unique_lock<std::mutex> instance_lock(GetGlobalLoaderMutex());

    if (ActiveLoaderInstance::IsAvailable()) {  // If there is an XrInstance already alive.
        LoaderLogger::LogErrorMessage("xrCreateInstance", "Loader does not support simultaneous XrInstances");
        return XR_ERROR_LIMIT_REACHED;
    }

    std::vector<std::unique_ptr<ApiLayerInterface>> api_layer_interfaces;
    XrResult result;

    {
        result = RuntimeInterface::LoadRuntime("xrCreateInstance");
        if (XR_FAILED(result)) {
            LoaderLogger::LogErrorMessage("xrCreateInstance", "Failed loading runtime information");
        } else {
            result = ApiLayerInterface::LoadApiLayers("xrCreateInstance", info->enabledApiLayerCount, info->enabledApiLayerNames,
                                                      api_layer_interfaces);
            if (XR_FAILED(result)) {
                LoaderLogger::LogErrorMessage("xrCreateInstance", "Failed loading layer information");
            }
        }
    }

    LoaderInstance *loader_instance = nullptr;
    if (XR_SUCCEEDED(result)) {
        std::unique_ptr<LoaderInstance> owned_loader_instance;
        result = LoaderInstance::CreateInstance(LoaderXrTermGetInstanceProcAddr, LoaderXrTermCreateInstance,
                                                LoaderXrTermCreateApiLayerInstance, std::move(api_layer_interfaces), info,
                                                &owned_loader_instance);
        if (XR_SUCCEEDED(result)) {
            loader_instance = owned_loader_instance.get();
            result = ActiveLoaderInstance::Set(std::move(owned_loader_instance), "xrCreateInstance");
        }
    }

    if (XR_SUCCEEDED(result)) {
        const auto *next_header = reinterpret_cast<const XrBaseInStructure *>(info->next);
        const XrDebugUtilsMessengerCreateInfoEXT *dbg_utils_create_info = nullptr;
        while (next_header != nullptr) {
            if (next_header->type == XR_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT) {
                LoaderLogger::LogInfoMessage("xrCreateInstance", "Found XrDebugUtilsMessengerCreateInfoEXT in \'next\' chain.");
                dbg_utils_create_info = reinterpret_cast<const XrDebugUtilsMessengerCreateInfoEXT *>(next_header);
                XrDebugUtilsMessengerEXT messenger;
                result = LoaderTrampolineCreateDebugUtilsMessengerEXT(loader_instance->GetInstanceHandle(), dbg_utils_create_info,
                                                                      &messenger);
                if (XR_FAILED(result)) {
                    return XR_ERROR_VALIDATION_FAILURE;
                }
                loader_instance->SetDefaultDebugUtilsMessenger(messenger);
                break;
            }
            next_header = reinterpret_cast<const XrBaseInStructure *>(next_header->next);
        }
    }

    if (XR_FAILED(result)) {
        ActiveLoaderInstance::Remove();
        RuntimeInterface::UnloadRuntime("xrCreateInstance");
        LoaderLogger::LogErrorMessage("xrCreateInstance", "xrCreateInstance failed");
    } else {
        *instance = loader_instance->GetInstanceHandle();
        LoaderLogger::LogVerboseMessage("xrCreateInstance", "Completed loader trampoline");
    }

    return result;
}
XRLOADER_ABI_CATCH_FALLBACK

static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrDestroyInstance(XrInstance instance) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrDestroyInstance", "Entering loader trampoline");
    if (XR_NULL_HANDLE == instance) {
        LoaderLogger::LogErrorMessage("xrDestroyInstance", "Instance handle is XR_NULL_HANDLE.");
        return XR_ERROR_HANDLE_INVALID;
    }

    std::unique_lock<std::mutex> loader_lock(GetGlobalLoaderMutex());

    LoaderInstance *loader_instance;
    XrResult result = ActiveLoaderInstance::Get(&loader_instance, "xrDestroyInstance");
    if (XR_FAILED(result)) {
        return result;
    }

    const std::unique_ptr<XrGeneratedDispatchTable> &dispatch_table = loader_instance->DispatchTable();

    XrDebugUtilsMessengerEXT messenger = loader_instance->DefaultDebugUtilsMessenger();
    if (messenger != XR_NULL_HANDLE) {
        LoaderTrampolineDestroyDebugUtilsMessengerEXT(messenger);
    }

    if (XR_FAILED(dispatch_table->DestroyInstance(instance))) {
        LoaderLogger::LogErrorMessage("xrDestroyInstance", "Unknown error occurred calling down chain");
    }

    ActiveLoaderInstance::Remove();

    LoaderLogger::LogVerboseMessage("xrDestroyInstance", "Completed loader trampoline");

    RuntimeInterface::UnloadRuntime("xrDestroyInstance");

    return XR_SUCCESS;
}
XRLOADER_ABI_CATCH_FALLBACK


static XrResult ValidateApplicationInfo(const XrApplicationInfo &info) {
    if (IsMissingNullTerminator<XR_MAX_APPLICATION_NAME_SIZE>(info.applicationName)) {
        LoaderLogger::LogValidationErrorMessage("VUID-XrApplicationInfo-applicationName-parameter", "xrCreateInstance",
                                                "application name missing NULL terminator.");
        return XR_ERROR_NAME_INVALID;
    }
    if (IsMissingNullTerminator<XR_MAX_ENGINE_NAME_SIZE>(info.engineName)) {
        LoaderLogger::LogValidationErrorMessage("VUID-XrApplicationInfo-engineName-parameter", "xrCreateInstance",
                                                "engine name missing NULL terminator.");
        return XR_ERROR_NAME_INVALID;
    }
    if (strlen(info.applicationName) == 0) {
        LoaderLogger::LogErrorMessage("xrCreateInstance",
                                      "VUID-XrApplicationInfo-engineName-parameter: application name can not be empty.");
        return XR_ERROR_NAME_INVALID;
    }
    return XR_SUCCESS;
}

static XrResult ValidateInstanceCreateInfo(const XrInstanceCreateInfo *info) {
    if (XR_TYPE_INSTANCE_CREATE_INFO != info->type) {
        LoaderLogger::LogValidationErrorMessage("VUID-XrInstanceCreateInfo-type-type", "xrCreateInstance",
                                                "expected XR_TYPE_INSTANCE_CREATE_INFO.");
        return XR_ERROR_VALIDATION_FAILURE;
    }
    if (0 != info->createFlags) {
        LoaderLogger::LogValidationErrorMessage("VUID-XrInstanceCreateInfo-createFlags-zerobitmask", "xrCreateInstance",
                                                "flags must be 0.");
        return XR_ERROR_VALIDATION_FAILURE;
    }
    XrResult result = ValidateApplicationInfo(info->applicationInfo);
    if (XR_FAILED(result)) {
        LoaderLogger::LogValidationErrorMessage("VUID-XrInstanceCreateInfo-applicationInfo-parameter", "xrCreateInstance",
                                                "info->applicationInfo is not valid.");
        return result;
    }
    if ((info->enabledExtensionCount != 0u) && nullptr == info->enabledExtensionNames) {
        LoaderLogger::LogValidationErrorMessage("VUID-XrInstanceCreateInfo-enabledExtensionNames-parameter", "xrCreateInstance",
                                                "enabledExtensionCount is non-0 but array is NULL");
        return XR_ERROR_VALIDATION_FAILURE;
    }
    return XR_SUCCESS;
}

static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermCreateInstance(const XrInstanceCreateInfo *createInfo,
                                                                 XrInstance *instance) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrCreateInstance", "Entering loader terminator");
    XrResult result = ValidateInstanceCreateInfo(createInfo);
    if (XR_FAILED(result)) {
        LoaderLogger::LogValidationErrorMessage("VUID-xrCreateInstance-info-parameter", "xrCreateInstance",
                                                "something wrong with XrInstanceCreateInfo contents");
        return result;
    }
    result = RuntimeInterface::GetRuntime().CreateInstance(createInfo, instance);
    LoaderLogger::LogVerboseMessage("xrCreateInstance", "Completed loader terminator");
    return result;
}
XRLOADER_ABI_CATCH_FALLBACK

static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermCreateApiLayerInstance(const XrInstanceCreateInfo *info,
                                                                         const struct XrApiLayerCreateInfo * /*apiLayerInfo*/,
                                                                         XrInstance *instance) {
    return LoaderXrTermCreateInstance(info, instance);
}

static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermDestroyInstance(XrInstance instance) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrDestroyInstance", "Entering loader terminator");
    LoaderLogger::GetInstance().RemoveLogRecordersForXrInstance(instance);
    XrResult result = RuntimeInterface::GetRuntime().DestroyInstance(instance);
    LoaderLogger::LogVerboseMessage("xrDestroyInstance", "Completed loader terminator");
    return result;
}
XRLOADER_ABI_CATCH_FALLBACK

static XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermGetInstanceProcAddr(XrInstance instance, const char *name,
                                                                      PFN_xrVoidFunction *function) XRLOADER_ABI_TRY {
    *function = nullptr;


    if (0 == strcmp(name, "xrGetInstanceProcAddr")) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrTermGetInstanceProcAddr);
    } else if (0 == strcmp(name, "xrCreateInstance")) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrTermCreateInstance);
    } else if (0 == strcmp(name, "xrDestroyInstance")) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrTermDestroyInstance);
    } else if (0 == strcmp(name, "xrSetDebugUtilsObjectNameEXT")) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrTermSetDebugUtilsObjectNameEXT);
    } else if (0 == strcmp(name, "xrCreateDebugUtilsMessengerEXT")) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrTermCreateDebugUtilsMessengerEXT);
    } else if (0 == strcmp(name, "xrDestroyDebugUtilsMessengerEXT")) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrTermDestroyDebugUtilsMessengerEXT);
    } else if (0 == strcmp(name, "xrSubmitDebugUtilsMessageEXT")) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrTermSubmitDebugUtilsMessageEXT);
    } else if (0 == strcmp(name, "xrCreateApiLayerInstance")) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrTermCreateApiLayerInstance);
    }

    if (nullptr != *function) {
        return XR_SUCCESS;
    }

    return RuntimeInterface::GetInstanceProcAddr(instance, name, function);
}
XRLOADER_ABI_CATCH_FALLBACK


static XRAPI_ATTR XrResult XRAPI_CALL
LoaderTrampolineCreateDebugUtilsMessengerEXT(XrInstance instance, const XrDebugUtilsMessengerCreateInfoEXT *createInfo,
                                             XrDebugUtilsMessengerEXT *messenger) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrCreateDebugUtilsMessengerEXT", "Entering loader trampoline");

    if (instance == XR_NULL_HANDLE) {
        LoaderLogger::LogErrorMessage("xrCreateDebugUtilsMessengerEXT", "Instance handle is XR_NULL_HANDLE.");
        return XR_ERROR_HANDLE_INVALID;
    }

    LoaderInstance *loader_instance;
    XrResult result = ActiveLoaderInstance::Get(&loader_instance, "xrCreateDebugUtilsMessengerEXT");
    if (XR_FAILED(result)) {
        return result;
    }

    result = loader_instance->DispatchTable()->CreateDebugUtilsMessengerEXT(instance, createInfo, messenger);
    LoaderLogger::LogVerboseMessage("xrCreateDebugUtilsMessengerEXT", "Completed loader trampoline");
    return result;
}
XRLOADER_ABI_CATCH_BAD_ALLOC_OOM XRLOADER_ABI_CATCH_FALLBACK

    static XRAPI_ATTR XrResult XRAPI_CALL
    LoaderTrampolineDestroyDebugUtilsMessengerEXT(XrDebugUtilsMessengerEXT messenger) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrDestroyDebugUtilsMessengerEXT", "Entering loader trampoline");

    if (messenger == XR_NULL_HANDLE) {
        LoaderLogger::LogErrorMessage("xrDestroyDebugUtilsMessengerEXT", "Messenger handle is XR_NULL_HANDLE.");
        return XR_ERROR_HANDLE_INVALID;
    }

    LoaderInstance *loader_instance;
    XrResult result = ActiveLoaderInstance::Get(&loader_instance, "xrDestroyDebugUtilsMessengerEXT");
    if (XR_FAILED(result)) {
        return result;
    }

    result = loader_instance->DispatchTable()->DestroyDebugUtilsMessengerEXT(messenger);
    LoaderLogger::LogVerboseMessage("xrDestroyDebugUtilsMessengerEXT", "Completed loader trampoline");
    return result;
}
XRLOADER_ABI_CATCH_FALLBACK

static XRAPI_ATTR XrResult XRAPI_CALL
LoaderTrampolineSessionBeginDebugUtilsLabelRegionEXT(XrSession session, const XrDebugUtilsLabelEXT *labelInfo) XRLOADER_ABI_TRY {
    if (session == XR_NULL_HANDLE) {
        LoaderLogger::LogErrorMessage("xrSessionBeginDebugUtilsLabelRegionEXT", "Session handle is XR_NULL_HANDLE.");
        return XR_ERROR_HANDLE_INVALID;
    }

    if (nullptr == labelInfo) {
        LoaderLogger::LogValidationErrorMessage("VUID-xrSessionBeginDebugUtilsLabelRegionEXT-labelInfo-parameter",
                                                "xrSessionBeginDebugUtilsLabelRegionEXT", "labelInfo must be non-NULL",
                                                {XrSdkLogObjectInfo{session, XR_OBJECT_TYPE_SESSION}});
        return XR_ERROR_VALIDATION_FAILURE;
    }

    LoaderInstance *loader_instance;
    XrResult result = ActiveLoaderInstance::Get(&loader_instance, "xrSessionBeginDebugUtilsLabelRegionEXT");
    if (XR_FAILED(result)) {
        return result;
    }
    LoaderLogger::GetInstance().BeginLabelRegion(session, labelInfo);
    const std::unique_ptr<XrGeneratedDispatchTable> &dispatch_table = loader_instance->DispatchTable();
    if (nullptr != dispatch_table->SessionBeginDebugUtilsLabelRegionEXT) {
        return dispatch_table->SessionBeginDebugUtilsLabelRegionEXT(session, labelInfo);
    }
    return XR_SUCCESS;
}
XRLOADER_ABI_CATCH_FALLBACK

static XRAPI_ATTR XrResult XRAPI_CALL LoaderTrampolineSessionEndDebugUtilsLabelRegionEXT(XrSession session) XRLOADER_ABI_TRY {
    if (session == XR_NULL_HANDLE) {
        LoaderLogger::LogErrorMessage("xrSessionEndDebugUtilsLabelRegionEXT", "Session handle is XR_NULL_HANDLE.");
        return XR_ERROR_HANDLE_INVALID;
    }

    LoaderInstance *loader_instance;
    XrResult result = ActiveLoaderInstance::Get(&loader_instance, "xrSessionEndDebugUtilsLabelRegionEXT");
    if (XR_FAILED(result)) {
        return result;
    }

    LoaderLogger::GetInstance().EndLabelRegion(session);
    const std::unique_ptr<XrGeneratedDispatchTable> &dispatch_table = loader_instance->DispatchTable();
    if (nullptr != dispatch_table->SessionEndDebugUtilsLabelRegionEXT) {
        return dispatch_table->SessionEndDebugUtilsLabelRegionEXT(session);
    }
    return XR_SUCCESS;
}
XRLOADER_ABI_CATCH_FALLBACK

static XRAPI_ATTR XrResult XRAPI_CALL
LoaderTrampolineSessionInsertDebugUtilsLabelEXT(XrSession session, const XrDebugUtilsLabelEXT *labelInfo) XRLOADER_ABI_TRY {
    if (session == XR_NULL_HANDLE) {
        LoaderLogger::LogErrorMessage("xrSessionInsertDebugUtilsLabelEXT", "Session handle is XR_NULL_HANDLE.");
        return XR_ERROR_HANDLE_INVALID;
    }

    LoaderInstance *loader_instance;
    XrResult result = ActiveLoaderInstance::Get(&loader_instance, "xrSessionInsertDebugUtilsLabelEXT");
    if (XR_FAILED(result)) {
        return result;
    }

    if (nullptr == labelInfo) {
        LoaderLogger::LogValidationErrorMessage("VUID-xrSessionInsertDebugUtilsLabelEXT-labelInfo-parameter",
                                                "xrSessionInsertDebugUtilsLabelEXT", "labelInfo must be non-NULL",
                                                {XrSdkLogObjectInfo{session, XR_OBJECT_TYPE_SESSION}});
        return XR_ERROR_VALIDATION_FAILURE;
    }

    LoaderLogger::GetInstance().InsertLabel(session, labelInfo);

    const std::unique_ptr<XrGeneratedDispatchTable> &dispatch_table = loader_instance->DispatchTable();
    if (nullptr != dispatch_table->SessionInsertDebugUtilsLabelEXT) {
        return dispatch_table->SessionInsertDebugUtilsLabelEXT(session, labelInfo);
    }

    return XR_SUCCESS;
}
XRLOADER_ABI_CATCH_FALLBACK

static XRAPI_ATTR XrResult XRAPI_CALL
LoaderTrampolineSetDebugUtilsObjectNameEXT(XrInstance instance, const XrDebugUtilsObjectNameInfoEXT *nameInfo) XRLOADER_ABI_TRY {
    LoaderInstance *loader_instance;
    XrResult result = ActiveLoaderInstance::Get(&loader_instance, "xrSetDebugUtilsObjectNameEXT");
    if (XR_SUCCEEDED(result)) {
        result = loader_instance->DispatchTable()->SetDebugUtilsObjectNameEXT(instance, nameInfo);
    }
    return result;
}
XRLOADER_ABI_CATCH_FALLBACK

static XRAPI_ATTR XrResult XRAPI_CALL LoaderTrampolineSubmitDebugUtilsMessageEXT(
    XrInstance instance, XrDebugUtilsMessageSeverityFlagsEXT messageSeverity, XrDebugUtilsMessageTypeFlagsEXT messageTypes,
    const XrDebugUtilsMessengerCallbackDataEXT *callbackData) XRLOADER_ABI_TRY {
    LoaderInstance *loader_instance;
    XrResult result = ActiveLoaderInstance::Get(&loader_instance, "xrSubmitDebugUtilsMessageEXT");
    if (XR_SUCCEEDED(result)) {
        result =
            loader_instance->DispatchTable()->SubmitDebugUtilsMessageEXT(instance, messageSeverity, messageTypes, callbackData);
    }
    return result;
}
XRLOADER_ABI_CATCH_FALLBACK


XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermCreateDebugUtilsMessengerEXT(XrInstance instance,
                                                                        const XrDebugUtilsMessengerCreateInfoEXT *createInfo,
                                                                        XrDebugUtilsMessengerEXT *messenger) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrCreateDebugUtilsMessengerEXT", "Entering loader terminator");
    if (nullptr == messenger) {
        LoaderLogger::LogValidationErrorMessage("VUID-xrCreateDebugUtilsMessengerEXT-messenger-parameter",
                                                "xrCreateDebugUtilsMessengerEXT", "invalid messenger pointer");
        return XR_ERROR_VALIDATION_FAILURE;
    }
    const XrGeneratedDispatchTable *dispatch_table = RuntimeInterface::GetDispatchTable(instance);
    XrResult result = XR_SUCCESS;
    if (nullptr != dispatch_table->CreateDebugUtilsMessengerEXT) {
        result = dispatch_table->CreateDebugUtilsMessengerEXT(instance, createInfo, messenger);
    } else {
        char *temp_mess_ptr = new char;
        *messenger = reinterpret_cast<XrDebugUtilsMessengerEXT>(temp_mess_ptr);
    }
    if (XR_SUCCEEDED(result)) {
        LoaderLogger::GetInstance().AddLogRecorderForXrInstance(instance, MakeDebugUtilsLoaderLogRecorder(createInfo, *messenger));
        RuntimeInterface::GetRuntime().TrackDebugMessenger(instance, *messenger);
    }
    LoaderLogger::LogVerboseMessage("xrCreateDebugUtilsMessengerEXT", "Completed loader terminator");
    return result;
}
XRLOADER_ABI_CATCH_FALLBACK

XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermDestroyDebugUtilsMessengerEXT(XrDebugUtilsMessengerEXT messenger) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrDestroyDebugUtilsMessengerEXT", "Entering loader terminator");
    const XrGeneratedDispatchTable *dispatch_table = RuntimeInterface::GetDebugUtilsMessengerDispatchTable(messenger);
    XrResult result = XR_SUCCESS;
    LoaderLogger::GetInstance().RemoveLogRecorder(MakeHandleGeneric(messenger));
    RuntimeInterface::GetRuntime().ForgetDebugMessenger(messenger);
    if (nullptr != dispatch_table->DestroyDebugUtilsMessengerEXT) {
        result = dispatch_table->DestroyDebugUtilsMessengerEXT(messenger);
    } else {
        delete (reinterpret_cast<char *>(MakeHandleGeneric(messenger)));
    }
    LoaderLogger::LogVerboseMessage("xrDestroyDebugUtilsMessengerEXT", "Completed loader terminator");
    return result;
}
XRLOADER_ABI_CATCH_FALLBACK

XRAPI_ATTR XrResult XRAPI_CALL LoaderXrTermSubmitDebugUtilsMessageEXT(
    XrInstance instance, XrDebugUtilsMessageSeverityFlagsEXT messageSeverity, XrDebugUtilsMessageTypeFlagsEXT messageTypes,
    const XrDebugUtilsMessengerCallbackDataEXT *callbackData) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrSubmitDebugUtilsMessageEXT", "Entering loader terminator");
    const XrGeneratedDispatchTable *dispatch_table = RuntimeInterface::GetDispatchTable(instance);
    XrResult result = XR_SUCCESS;
    if (nullptr != dispatch_table->SubmitDebugUtilsMessageEXT) {
        result = dispatch_table->SubmitDebugUtilsMessageEXT(instance, messageSeverity, messageTypes, callbackData);
    } else {
        LoaderLogger::GetInstance().LogDebugUtilsMessage(messageSeverity, messageTypes, callbackData);
    }
    LoaderLogger::LogVerboseMessage("xrSubmitDebugUtilsMessageEXT", "Completed loader terminator");
    return result;
}
XRLOADER_ABI_CATCH_FALLBACK

XRAPI_ATTR XrResult XRAPI_CALL
LoaderXrTermSetDebugUtilsObjectNameEXT(XrInstance instance, const XrDebugUtilsObjectNameInfoEXT *nameInfo) XRLOADER_ABI_TRY {
    LoaderLogger::LogVerboseMessage("xrSetDebugUtilsObjectNameEXT", "Entering loader terminator");
    const XrGeneratedDispatchTable *dispatch_table = RuntimeInterface::GetDispatchTable(instance);
    XrResult result = XR_SUCCESS;
    if (nullptr != dispatch_table->SetDebugUtilsObjectNameEXT) {
        result = dispatch_table->SetDebugUtilsObjectNameEXT(instance, nameInfo);
    }
    LoaderLogger::GetInstance().AddObjectName(nameInfo->objectHandle, nameInfo->objectType, nameInfo->objectName);
    LoaderLogger::LogVerboseMessage("xrSetDebugUtilsObjectNameEXT", "Completed loader terminator");
    return result;
}
XRLOADER_ABI_CATCH_FALLBACK

XRAPI_ATTR XrResult XRAPI_CALL LoaderXrGetInstanceProcAddr(XrInstance instance, const char *name,
                                                           PFN_xrVoidFunction *function) XRLOADER_ABI_TRY {
    if (nullptr == function) {
        LoaderLogger::LogValidationErrorMessage("VUID-xrGetInstanceProcAddr-function-parameter", "xrGetInstanceProcAddr",
                                                "Invalid Function pointer");
        return XR_ERROR_VALIDATION_FAILURE;
    }

    if (nullptr == name) {
        LoaderLogger::LogValidationErrorMessage("VUID-xrGetInstanceProcAddr-function-parameter", "xrGetInstanceProcAddr",
                                                "Invalid Name pointer");
        return XR_ERROR_VALIDATION_FAILURE;
    }

    *function = nullptr;

    LoaderInstance *loader_instance = nullptr;
    if (instance == XR_NULL_HANDLE) {
        if (strcmp(name, "xrCreateInstance") != 0 && strcmp(name, "xrEnumerateApiLayerProperties") != 0 &&
            strcmp(name, "xrEnumerateInstanceExtensionProperties") != 0 && strcmp(name, "xrInitializeLoaderKHR") != 0) {
            std::string error_str = "XR_NULL_HANDLE for instance but query for ";
            error_str += name;
            error_str += " requires a valid instance";
            LoaderLogger::LogValidationErrorMessage("VUID-xrGetInstanceProcAddr-instance-parameter", "xrGetInstanceProcAddr",
                                                    error_str);
            return XR_ERROR_HANDLE_INVALID;
        }
    } else {
        XrResult result = ActiveLoaderInstance::Get(&loader_instance, "xrGetInstanceProcAddr");
        if (XR_FAILED(result)) {
            return result;
        }
        if (loader_instance->GetInstanceHandle() != instance) {
            return XR_ERROR_HANDLE_INVALID;
        }
    }

    if (strcmp(name, "xrGetInstanceProcAddr") == 0) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrGetInstanceProcAddr);
        return XR_SUCCESS;
    } else if (strcmp(name, "xrInitializeLoaderKHR") == 0) {
#ifdef XR_KHR_LOADER_INIT_SUPPORT
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrInitializeLoaderKHR);
        return XR_SUCCESS;
#else
        return XR_ERROR_FUNCTION_UNSUPPORTED;
#endif
    } else if (strcmp(name, "xrEnumerateApiLayerProperties") == 0) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrEnumerateApiLayerProperties);
        return XR_SUCCESS;
    } else if (strcmp(name, "xrEnumerateInstanceExtensionProperties") == 0) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrEnumerateInstanceExtensionProperties);
        return XR_SUCCESS;
    } else if (strcmp(name, "xrCreateInstance") == 0) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrCreateInstance);
        return XR_SUCCESS;
    } else if (strcmp(name, "xrDestroyInstance") == 0) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderXrDestroyInstance);
        return XR_SUCCESS;
    }

    if (*function == nullptr) {
        if (strcmp(name, "xrCreateDebugUtilsMessengerEXT") == 0) {
            *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderTrampolineCreateDebugUtilsMessengerEXT);
        } else if (strcmp(name, "xrDestroyDebugUtilsMessengerEXT") == 0) {
            *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderTrampolineDestroyDebugUtilsMessengerEXT);
        } else if (strcmp(name, "xrSessionBeginDebugUtilsLabelRegionEXT") == 0) {
            *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderTrampolineSessionBeginDebugUtilsLabelRegionEXT);
        } else if (strcmp(name, "xrSessionEndDebugUtilsLabelRegionEXT") == 0) {
            *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderTrampolineSessionEndDebugUtilsLabelRegionEXT);
        } else if (strcmp(name, "xrSessionInsertDebugUtilsLabelEXT") == 0) {
            *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderTrampolineSessionInsertDebugUtilsLabelEXT);
        } else if (strcmp(name, "xrSetDebugUtilsObjectNameEXT") == 0) {
            *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderTrampolineSetDebugUtilsObjectNameEXT);
        } else if (strcmp(name, "xrSubmitDebugUtilsMessageEXT") == 0) {
            *function = reinterpret_cast<PFN_xrVoidFunction>(LoaderTrampolineSubmitDebugUtilsMessageEXT);
        }

        if (*function != nullptr && !loader_instance->ExtensionIsEnabled("XR_EXT_debug_utils")) {
            *function = nullptr;
            return XR_ERROR_FUNCTION_UNSUPPORTED;
        }
    }

    if (*function != nullptr) {
        return XR_SUCCESS;
    }

    return loader_instance->GetInstanceProcAddr(name, function);
}
XRLOADER_ABI_CATCH_FALLBACK

LOADER_EXPORT XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateApiLayerProperties(uint32_t propertyCapacityInput,
                                                                           uint32_t *propertyCountOutput,
                                                                           XrApiLayerProperties *properties) {
    return LoaderXrEnumerateApiLayerProperties(propertyCapacityInput, propertyCountOutput, properties);
}

LOADER_EXPORT XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateInstanceExtensionProperties(const char *layerName,
                                                                                    uint32_t propertyCapacityInput,
                                                                                    uint32_t *propertyCountOutput,
                                                                                    XrExtensionProperties *properties) {
    return LoaderXrEnumerateInstanceExtensionProperties(layerName, propertyCapacityInput, propertyCountOutput, properties);
}

LOADER_EXPORT XRAPI_ATTR XrResult XRAPI_CALL xrCreateInstance(const XrInstanceCreateInfo *info, XrInstance *instance) {
    return LoaderXrCreateInstance(info, instance);
}

LOADER_EXPORT XRAPI_ATTR XrResult XRAPI_CALL xrDestroyInstance(XrInstance instance) { return LoaderXrDestroyInstance(instance); }

LOADER_EXPORT XRAPI_ATTR XrResult XRAPI_CALL xrGetInstanceProcAddr(XrInstance instance, const char *name,
                                                                   PFN_xrVoidFunction *function) {
    return LoaderXrGetInstanceProcAddr(instance, name, function);
}

#ifdef XR_KHR_LOADER_INIT_SUPPORT
LOADER_EXPORT XRAPI_ATTR XrResult XRAPI_CALL xrInitializeLoaderKHR(const XrLoaderInitInfoBaseHeaderKHR *loaderInitInfo) {
    return LoaderXrInitializeLoaderKHR(loaderInitInfo);
}
#endif
