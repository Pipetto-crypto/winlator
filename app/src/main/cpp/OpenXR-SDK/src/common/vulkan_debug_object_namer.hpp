
#pragma once

#ifdef XR_USE_GRAPHICS_API_VULKAN

#include <vulkan/vulkan_core.h>
#include <stdexcept>

class VulkanDebugObjectNamer {
   public:
    VulkanDebugObjectNamer() = default;

    VulkanDebugObjectNamer(VkInstance instance, VkDevice device) : m_vkDevice{device} {
        vkSetDebugUtilsObjectNameEXT =
            (PFN_vkSetDebugUtilsObjectNameEXT)vkGetInstanceProcAddr(instance, "vkSetDebugUtilsObjectNameEXT");
    }
    VulkanDebugObjectNamer(const VulkanDebugObjectNamer&) = default;
    VulkanDebugObjectNamer& operator=(const VulkanDebugObjectNamer&) = default;

    ~VulkanDebugObjectNamer() { Reset(); }

    void Init(VkInstance instance, VkDevice device) {
        Reset();
        *this = VulkanDebugObjectNamer(instance, device);
    }

    VkResult SetName(VkObjectType objectType, uint64_t objectHandle, const char* pObjectName) const {
        if (m_vkDevice == nullptr) {
            return VK_SUCCESS;
        }
        if (vkSetDebugUtilsObjectNameEXT != nullptr) {
            VkDebugUtilsObjectNameInfoEXT nameInfo{VK_STRUCTURE_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT, nullptr, objectType,
                                                   objectHandle, pObjectName};
            return vkSetDebugUtilsObjectNameEXT(m_vkDevice, &nameInfo);
        }
        return VK_SUCCESS;
    }

    void Reset() {
        vkSetDebugUtilsObjectNameEXT = nullptr;
        m_vkDevice = VK_NULL_HANDLE;
    }

   private:
    VkDevice m_vkDevice{VK_NULL_HANDLE};
    PFN_vkSetDebugUtilsObjectNameEXT vkSetDebugUtilsObjectNameEXT{nullptr};
};

#endif
