
#pragma once

#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>
#include <set>
#include <map>
#include <shared_mutex>

#include <openxr/openxr.h>

#include "hex_and_handles.h"
#include "object_info.h"

#define XR_LOADER_LOG_MESSAGE_SEVERITY_VERBOSE_BIT 0x00000001
#define XR_LOADER_LOG_MESSAGE_SEVERITY_INFO_BIT 0x00000010
#define XR_LOADER_LOG_MESSAGE_SEVERITY_WARNING_BIT 0x00000100
#define XR_LOADER_LOG_MESSAGE_SEVERITY_ERROR_BIT 0x00001000
#define XR_LOADER_LOG_MESSAGE_SEVERITY_DEFAULT_BITS 0x00000000
typedef XrFlags64 XrLoaderLogMessageSeverityFlagBits;
typedef XrFlags64 XrLoaderLogMessageSeverityFlags;

#define XR_LOADER_LOG_MESSAGE_TYPE_GENERAL_BIT 0x00000001
#define XR_LOADER_LOG_MESSAGE_TYPE_SPECIFICATION_BIT 0x00000002
#define XR_LOADER_LOG_MESSAGE_TYPE_PERFORMANCE_BIT 0x00000004
#define XR_LOADER_LOG_MESSAGE_TYPE_DEFAULT_BITS 0xffffffff
typedef XrFlags64 XrLoaderLogMessageTypeFlagBits;
typedef XrFlags64 XrLoaderLogMessageTypeFlags;

struct XrLoaderLogMessengerCallbackData {
    const char* message_id;
    const char* command_name;
    const char* message;
    uint8_t object_count;
    XrSdkLogObjectInfo* objects;
    uint8_t session_labels_count;
    XrDebugUtilsLabelEXT* session_labels;
};

enum XrLoaderLogType {
    XR_LOADER_LOG_UNKNOWN = 0,
    XR_LOADER_LOG_STDERR,
    XR_LOADER_LOG_STDOUT,
    XR_LOADER_LOG_DEBUG_UTILS,
    XR_LOADER_LOG_DEBUGGER,
    XR_LOADER_LOG_LOGCAT,
};

class LoaderLogRecorder {
   public:
    LoaderLogRecorder(XrLoaderLogType type, void* user_data, XrLoaderLogMessageSeverityFlags message_severities,
                      XrLoaderLogMessageTypeFlags message_types) {
        _active = false;
        _user_data = user_data;
        _type = type;
        _unique_id = 0;
        _message_severities = message_severities;
        _message_types = message_types;
    }
    virtual ~LoaderLogRecorder() = default;

    XrLoaderLogType Type() { return _type; }

    uint64_t UniqueId() { return _unique_id; }

    XrLoaderLogMessageSeverityFlags MessageSeverities() { return _message_severities; }

    XrLoaderLogMessageTypeFlags MessageTypes() { return _message_types; }

    virtual void Start() { _active = true; }

    bool IsPaused() { return _active; }

    virtual void Pause() { _active = false; }

    virtual void Resume() { _active = true; }

    virtual void Stop() { _active = false; }

    virtual bool LogMessage(XrLoaderLogMessageSeverityFlagBits message_severity, XrLoaderLogMessageTypeFlags message_type,
                            const XrLoaderLogMessengerCallbackData* callback_data) = 0;

    virtual bool LogDebugUtilsMessage(XrDebugUtilsMessageSeverityFlagsEXT message_severity,
                                      XrDebugUtilsMessageTypeFlagsEXT message_type,
                                      const XrDebugUtilsMessengerCallbackDataEXT* callback_data);

   protected:
    bool _active;
    XrLoaderLogType _type;
    uint64_t _unique_id;
    void* _user_data;
    XrLoaderLogMessageSeverityFlags _message_severities;
    XrLoaderLogMessageTypeFlags _message_types;
};

class LoaderLogger {
   public:
    static LoaderLogger& GetInstance() {
        static LoaderLogger instance;
        return instance;
    }

    void AddLogRecorder(std::unique_ptr<LoaderLogRecorder>&& recorder);
    void RemoveLogRecorder(uint64_t unique_id);

    void AddLogRecorderForXrInstance(XrInstance instance, std::unique_ptr<LoaderLogRecorder>&& recorder);
    void RemoveLogRecordersForXrInstance(XrInstance instance);

    void AddObjectName(uint64_t object_handle, XrObjectType object_type, const std::string& object_name);
    void BeginLabelRegion(XrSession session, const XrDebugUtilsLabelEXT* label_info);
    void EndLabelRegion(XrSession session);
    void InsertLabel(XrSession session, const XrDebugUtilsLabelEXT* label_info);
    void DeleteSessionLabels(XrSession session);

    bool LogMessage(XrLoaderLogMessageSeverityFlagBits message_severity, XrLoaderLogMessageTypeFlags message_type,
                    const std::string& message_id, const std::string& command_name, const std::string& message,
                    const std::vector<XrSdkLogObjectInfo>& objects = {});
    static bool LogErrorMessage(const std::string& command_name, const std::string& message,
                                const std::vector<XrSdkLogObjectInfo>& objects = {}) {
        return GetInstance().LogMessage(XR_LOADER_LOG_MESSAGE_SEVERITY_ERROR_BIT, XR_LOADER_LOG_MESSAGE_TYPE_GENERAL_BIT,
                                        "OpenXR-Loader", command_name, message, objects);
    }
    static bool LogWarningMessage(const std::string& command_name, const std::string& message,
                                  const std::vector<XrSdkLogObjectInfo>& objects = {}) {
        return GetInstance().LogMessage(XR_LOADER_LOG_MESSAGE_SEVERITY_WARNING_BIT, XR_LOADER_LOG_MESSAGE_TYPE_GENERAL_BIT,
                                        "OpenXR-Loader", command_name, message, objects);
    }
    static bool LogInfoMessage(const std::string& command_name, const std::string& message,
                               const std::vector<XrSdkLogObjectInfo>& objects = {}) {
        return GetInstance().LogMessage(XR_LOADER_LOG_MESSAGE_SEVERITY_INFO_BIT, XR_LOADER_LOG_MESSAGE_TYPE_GENERAL_BIT,
                                        "OpenXR-Loader", command_name, message, objects);
    }
    static bool LogVerboseMessage(const std::string& command_name, const std::string& message,
                                  const std::vector<XrSdkLogObjectInfo>& objects = {}) {
        return GetInstance().LogMessage(XR_LOADER_LOG_MESSAGE_SEVERITY_VERBOSE_BIT, XR_LOADER_LOG_MESSAGE_TYPE_GENERAL_BIT,
                                        "OpenXR-Loader", command_name, message, objects);
    }
    static bool LogValidationErrorMessage(const std::string& vuid, const std::string& command_name, const std::string& message,
                                          const std::vector<XrSdkLogObjectInfo>& objects = {}) {
        return GetInstance().LogMessage(XR_LOADER_LOG_MESSAGE_SEVERITY_ERROR_BIT, XR_LOADER_LOG_MESSAGE_TYPE_SPECIFICATION_BIT,
                                        vuid, command_name, message, objects);
    }
    static bool LogValidationWarningMessage(const std::string& vuid, const std::string& command_name, const std::string& message,
                                            const std::vector<XrSdkLogObjectInfo>& objects = {}) {
        return GetInstance().LogMessage(XR_LOADER_LOG_MESSAGE_SEVERITY_WARNING_BIT, XR_LOADER_LOG_MESSAGE_TYPE_SPECIFICATION_BIT,
                                        vuid, command_name, message, objects);
    }

    bool LogDebugUtilsMessage(XrDebugUtilsMessageSeverityFlagsEXT message_severity, XrDebugUtilsMessageTypeFlagsEXT message_type,
                              const XrDebugUtilsMessengerCallbackDataEXT* callback_data);

    LoaderLogger(const LoaderLogger&) = delete;
    LoaderLogger& operator=(const LoaderLogger&) = delete;

   private:
    LoaderLogger();

    std::shared_timed_mutex _mutex;

    std::vector<std::unique_ptr<LoaderLogRecorder>> _recorders;

    std::unordered_map<XrInstance, std::unordered_set<uint64_t>> _recordersByInstance;

    DebugUtilsData data_;
};

XrLoaderLogMessageSeverityFlags DebugUtilsSeveritiesToLoaderLogMessageSeverities(
    XrDebugUtilsMessageSeverityFlagsEXT utils_severities);
XrDebugUtilsMessageSeverityFlagsEXT LoaderLogMessageSeveritiesToDebugUtilsMessageSeverities(
    XrLoaderLogMessageSeverityFlags log_severities);
XrLoaderLogMessageTypeFlagBits DebugUtilsMessageTypesToLoaderLogMessageTypes(XrDebugUtilsMessageTypeFlagsEXT utils_types);
XrDebugUtilsMessageTypeFlagsEXT LoaderLogMessageTypesToDebugUtilsMessageTypes(XrLoaderLogMessageTypeFlagBits log_types);
