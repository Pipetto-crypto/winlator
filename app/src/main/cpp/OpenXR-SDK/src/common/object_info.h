/*!
 * @file
 *
 * The core of an XR_EXT_debug_utils implementation, used/shared by the loader and several SDK layers.
 */

#pragma once

#include "hex_and_handles.h"

#include <openxr/openxr.h>

#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

struct XrSdkGenericObject {
    uint64_t handle;

    XrObjectType type;
    template <typename HandleType>
    HandleType& GetTypedHandle() {
        return TreatIntegerAsHandle<HandleType&>(handle);
    }

    template <typename HandleType>
    HandleType const& GetTypedHandle() const {
        return TreatIntegerAsHandle<HandleType&>(handle);
    }

    template <typename T>
    XrSdkGenericObject(T h, XrObjectType t) : handle(MakeHandleGeneric(h)), type(t) {}

    XrSdkGenericObject(uint64_t h, XrObjectType t) : handle(h), type(t) {}
};

struct XrSdkLogObjectInfo {
    uint64_t handle;

    XrObjectType type;

    std::string name;

    template <typename HandleType>
    HandleType& GetTypedHandle() {
        return TreatIntegerAsHandle<HandleType&>(handle);
    }

    template <typename HandleType>
    HandleType const& GetTypedHandle() const {
        return TreatIntegerAsHandle<HandleType&>(handle);
    }

    XrSdkLogObjectInfo() = default;

    template <typename T>
    XrSdkLogObjectInfo(T h, XrObjectType t) : handle(MakeHandleGeneric(h)), type(t) {}

    XrSdkLogObjectInfo(uint64_t h, XrObjectType t) : handle(h), type(t) {}
    XrSdkLogObjectInfo(uint64_t h, XrObjectType t, const char* n) : handle(h), type(t), name(n == nullptr ? "" : n) {}

    std::string ToString() const;
};

static inline bool Equivalent(XrSdkLogObjectInfo const& a, XrSdkLogObjectInfo const& b) {
    return a.handle == b.handle && a.type == b.type;
}

static inline bool Equivalent(XrDebugUtilsObjectNameInfoEXT const& a, XrSdkLogObjectInfo const& b) {
    return a.objectHandle == b.handle && a.objectType == b.type;
}

static inline bool Equivalent(XrSdkLogObjectInfo const& a, XrDebugUtilsObjectNameInfoEXT const& b) { return Equivalent(b, a); }

class ObjectInfoCollection {
   public:
    void AddObjectName(uint64_t object_handle, XrObjectType object_type, const std::string& object_name);

    void RemoveObject(uint64_t object_handle, XrObjectType object_type);

    XrSdkLogObjectInfo const* LookUpStoredObjectInfo(XrSdkLogObjectInfo const& info) const;

    XrSdkLogObjectInfo* LookUpStoredObjectInfo(XrSdkLogObjectInfo const& info);

    XrSdkLogObjectInfo const* LookUpStoredObjectInfo(uint64_t handle, XrObjectType type) const {
        return LookUpStoredObjectInfo({handle, type});
    }

    bool LookUpObjectName(XrDebugUtilsObjectNameInfoEXT& info) const;

    bool LookUpObjectName(XrSdkLogObjectInfo& info) const;

    bool Empty() const { return object_info_.empty(); }

   private:
    std::vector<XrSdkLogObjectInfo> object_info_;
};

struct XrSdkSessionLabel;
using XrSdkSessionLabelPtr = std::unique_ptr<XrSdkSessionLabel>;
using XrSdkSessionLabelList = std::vector<XrSdkSessionLabelPtr>;

struct XrSdkSessionLabel {
    static XrSdkSessionLabelPtr make(const XrDebugUtilsLabelEXT& label_info, bool individual);

    std::string label_name;
    XrDebugUtilsLabelEXT debug_utils_label;
    bool is_individual_label;

   private:
    XrSdkSessionLabel(const XrDebugUtilsLabelEXT& label_info, bool individual);
};

struct NamesAndLabels {
    NamesAndLabels() = default;
    NamesAndLabels(std::vector<XrSdkLogObjectInfo> obj, std::vector<XrDebugUtilsLabelEXT> lab);
    std::vector<XrSdkLogObjectInfo> sdk_objects;

    std::vector<XrDebugUtilsObjectNameInfoEXT> objects;
    std::vector<XrDebugUtilsLabelEXT> labels;

    void PopulateCallbackData(XrDebugUtilsMessengerCallbackDataEXT& data) const;
};

struct AugmentedCallbackData {
    std::vector<XrDebugUtilsLabelEXT> labels;
    std::vector<XrDebugUtilsObjectNameInfoEXT> new_objects;
    XrDebugUtilsMessengerCallbackDataEXT modified_data;
    const XrDebugUtilsMessengerCallbackDataEXT* exported_data;
};

class DebugUtilsData {
   public:
    DebugUtilsData() = default;

    DebugUtilsData(const DebugUtilsData&) = delete;
    DebugUtilsData& operator=(const DebugUtilsData&) = delete;

    bool Empty() const { return object_info_.Empty() && session_labels_.empty(); }

    void AddObjectName(uint64_t object_handle, XrObjectType object_type, const std::string& object_name);

    void BeginLabelRegion(XrSession session, const XrDebugUtilsLabelEXT& label_info);

    void EndLabelRegion(XrSession session);

    void InsertLabel(XrSession session, const XrDebugUtilsLabelEXT& label_info);

    void DeleteSessionLabels(XrSession session);

    void LookUpSessionLabels(XrSession session, std::vector<XrDebugUtilsLabelEXT>& labels) const;

    void DeleteObject(uint64_t object_handle, XrObjectType object_type);

    NamesAndLabels PopulateNamesAndLabels(std::vector<XrSdkLogObjectInfo> objects) const;

    void WrapCallbackData(AugmentedCallbackData* aug_data,
                          const XrDebugUtilsMessengerCallbackDataEXT* provided_callback_data) const;

   private:
    void RemoveIndividualLabel(XrSdkSessionLabelList& label_vec);
    XrSdkSessionLabelList* GetSessionLabelList(XrSession session);
    XrSdkSessionLabelList& GetOrCreateSessionLabelList(XrSession session);

    std::unordered_map<XrSession, std::unique_ptr<XrSdkSessionLabelList>> session_labels_;

    ObjectInfoCollection object_info_;
};
