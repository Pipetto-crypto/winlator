



#pragma once

#include <openxr/openxr.h>


#ifdef __cplusplus
extern "C" { 
#endif
struct XrGeneratedDispatchTable {

    PFN_xrGetInstanceProcAddr GetInstanceProcAddr;
    PFN_xrEnumerateApiLayerProperties EnumerateApiLayerProperties;
    PFN_xrEnumerateInstanceExtensionProperties EnumerateInstanceExtensionProperties;
    PFN_xrCreateInstance CreateInstance;
    PFN_xrDestroyInstance DestroyInstance;
    PFN_xrGetInstanceProperties GetInstanceProperties;
    PFN_xrPollEvent PollEvent;
    PFN_xrResultToString ResultToString;
    PFN_xrStructureTypeToString StructureTypeToString;
    PFN_xrGetSystem GetSystem;
    PFN_xrGetSystemProperties GetSystemProperties;
    PFN_xrEnumerateEnvironmentBlendModes EnumerateEnvironmentBlendModes;
    PFN_xrCreateSession CreateSession;
    PFN_xrDestroySession DestroySession;
    PFN_xrEnumerateReferenceSpaces EnumerateReferenceSpaces;
    PFN_xrCreateReferenceSpace CreateReferenceSpace;
    PFN_xrGetReferenceSpaceBoundsRect GetReferenceSpaceBoundsRect;
    PFN_xrCreateActionSpace CreateActionSpace;
    PFN_xrLocateSpace LocateSpace;
    PFN_xrDestroySpace DestroySpace;
    PFN_xrEnumerateViewConfigurations EnumerateViewConfigurations;
    PFN_xrGetViewConfigurationProperties GetViewConfigurationProperties;
    PFN_xrEnumerateViewConfigurationViews EnumerateViewConfigurationViews;
    PFN_xrEnumerateSwapchainFormats EnumerateSwapchainFormats;
    PFN_xrCreateSwapchain CreateSwapchain;
    PFN_xrDestroySwapchain DestroySwapchain;
    PFN_xrEnumerateSwapchainImages EnumerateSwapchainImages;
    PFN_xrAcquireSwapchainImage AcquireSwapchainImage;
    PFN_xrWaitSwapchainImage WaitSwapchainImage;
    PFN_xrReleaseSwapchainImage ReleaseSwapchainImage;
    PFN_xrBeginSession BeginSession;
    PFN_xrEndSession EndSession;
    PFN_xrRequestExitSession RequestExitSession;
    PFN_xrWaitFrame WaitFrame;
    PFN_xrBeginFrame BeginFrame;
    PFN_xrEndFrame EndFrame;
    PFN_xrLocateViews LocateViews;
    PFN_xrStringToPath StringToPath;
    PFN_xrPathToString PathToString;
    PFN_xrCreateActionSet CreateActionSet;
    PFN_xrDestroyActionSet DestroyActionSet;
    PFN_xrCreateAction CreateAction;
    PFN_xrDestroyAction DestroyAction;
    PFN_xrSuggestInteractionProfileBindings SuggestInteractionProfileBindings;
    PFN_xrAttachSessionActionSets AttachSessionActionSets;
    PFN_xrGetCurrentInteractionProfile GetCurrentInteractionProfile;
    PFN_xrGetActionStateBoolean GetActionStateBoolean;
    PFN_xrGetActionStateFloat GetActionStateFloat;
    PFN_xrGetActionStateVector2f GetActionStateVector2f;
    PFN_xrGetActionStatePose GetActionStatePose;
    PFN_xrSyncActions SyncActions;
    PFN_xrEnumerateBoundSourcesForAction EnumerateBoundSourcesForAction;
    PFN_xrGetInputSourceLocalizedName GetInputSourceLocalizedName;
    PFN_xrApplyHapticFeedback ApplyHapticFeedback;
    PFN_xrStopHapticFeedback StopHapticFeedback;

    PFN_xrLocateSpaces LocateSpaces;

    PFN_xrSetDebugUtilsObjectNameEXT SetDebugUtilsObjectNameEXT;
    PFN_xrCreateDebugUtilsMessengerEXT CreateDebugUtilsMessengerEXT;
    PFN_xrDestroyDebugUtilsMessengerEXT DestroyDebugUtilsMessengerEXT;
    PFN_xrSubmitDebugUtilsMessageEXT SubmitDebugUtilsMessageEXT;
    PFN_xrSessionBeginDebugUtilsLabelRegionEXT SessionBeginDebugUtilsLabelRegionEXT;
    PFN_xrSessionEndDebugUtilsLabelRegionEXT SessionEndDebugUtilsLabelRegionEXT;
    PFN_xrSessionInsertDebugUtilsLabelEXT SessionInsertDebugUtilsLabelEXT;
};


void GeneratedXrPopulateDispatchTable(struct XrGeneratedDispatchTable *table,
                                      XrInstance instance,
                                      PFN_xrGetInstanceProcAddr get_inst_proc_addr);

#ifdef __cplusplus
} // extern "C"
#endif

