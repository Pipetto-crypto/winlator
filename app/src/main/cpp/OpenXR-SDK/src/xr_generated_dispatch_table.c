



#include "xr_generated_dispatch_table.h"



#ifdef __cplusplus
extern "C" { 
#endif
void GeneratedXrPopulateDispatchTable(struct XrGeneratedDispatchTable *table,
                                      XrInstance instance,
                                      PFN_xrGetInstanceProcAddr get_inst_proc_addr) {

    table->GetInstanceProcAddr = get_inst_proc_addr;
    (get_inst_proc_addr(instance, "xrCreateInstance", (PFN_xrVoidFunction*)&table->CreateInstance));
    (get_inst_proc_addr(instance, "xrDestroyInstance", (PFN_xrVoidFunction*)&table->DestroyInstance));
    (get_inst_proc_addr(instance, "xrGetInstanceProperties", (PFN_xrVoidFunction*)&table->GetInstanceProperties));
    (get_inst_proc_addr(instance, "xrPollEvent", (PFN_xrVoidFunction*)&table->PollEvent));
    (get_inst_proc_addr(instance, "xrResultToString", (PFN_xrVoidFunction*)&table->ResultToString));
    (get_inst_proc_addr(instance, "xrStructureTypeToString", (PFN_xrVoidFunction*)&table->StructureTypeToString));
    (get_inst_proc_addr(instance, "xrGetSystem", (PFN_xrVoidFunction*)&table->GetSystem));
    (get_inst_proc_addr(instance, "xrGetSystemProperties", (PFN_xrVoidFunction*)&table->GetSystemProperties));
    (get_inst_proc_addr(instance, "xrEnumerateEnvironmentBlendModes", (PFN_xrVoidFunction*)&table->EnumerateEnvironmentBlendModes));
    (get_inst_proc_addr(instance, "xrCreateSession", (PFN_xrVoidFunction*)&table->CreateSession));
    (get_inst_proc_addr(instance, "xrDestroySession", (PFN_xrVoidFunction*)&table->DestroySession));
    (get_inst_proc_addr(instance, "xrEnumerateReferenceSpaces", (PFN_xrVoidFunction*)&table->EnumerateReferenceSpaces));
    (get_inst_proc_addr(instance, "xrCreateReferenceSpace", (PFN_xrVoidFunction*)&table->CreateReferenceSpace));
    (get_inst_proc_addr(instance, "xrGetReferenceSpaceBoundsRect", (PFN_xrVoidFunction*)&table->GetReferenceSpaceBoundsRect));
    (get_inst_proc_addr(instance, "xrCreateActionSpace", (PFN_xrVoidFunction*)&table->CreateActionSpace));
    (get_inst_proc_addr(instance, "xrLocateSpace", (PFN_xrVoidFunction*)&table->LocateSpace));
    (get_inst_proc_addr(instance, "xrDestroySpace", (PFN_xrVoidFunction*)&table->DestroySpace));
    (get_inst_proc_addr(instance, "xrEnumerateViewConfigurations", (PFN_xrVoidFunction*)&table->EnumerateViewConfigurations));
    (get_inst_proc_addr(instance, "xrGetViewConfigurationProperties", (PFN_xrVoidFunction*)&table->GetViewConfigurationProperties));
    (get_inst_proc_addr(instance, "xrEnumerateViewConfigurationViews", (PFN_xrVoidFunction*)&table->EnumerateViewConfigurationViews));
    (get_inst_proc_addr(instance, "xrEnumerateSwapchainFormats", (PFN_xrVoidFunction*)&table->EnumerateSwapchainFormats));
    (get_inst_proc_addr(instance, "xrCreateSwapchain", (PFN_xrVoidFunction*)&table->CreateSwapchain));
    (get_inst_proc_addr(instance, "xrDestroySwapchain", (PFN_xrVoidFunction*)&table->DestroySwapchain));
    (get_inst_proc_addr(instance, "xrEnumerateSwapchainImages", (PFN_xrVoidFunction*)&table->EnumerateSwapchainImages));
    (get_inst_proc_addr(instance, "xrAcquireSwapchainImage", (PFN_xrVoidFunction*)&table->AcquireSwapchainImage));
    (get_inst_proc_addr(instance, "xrWaitSwapchainImage", (PFN_xrVoidFunction*)&table->WaitSwapchainImage));
    (get_inst_proc_addr(instance, "xrReleaseSwapchainImage", (PFN_xrVoidFunction*)&table->ReleaseSwapchainImage));
    (get_inst_proc_addr(instance, "xrBeginSession", (PFN_xrVoidFunction*)&table->BeginSession));
    (get_inst_proc_addr(instance, "xrEndSession", (PFN_xrVoidFunction*)&table->EndSession));
    (get_inst_proc_addr(instance, "xrRequestExitSession", (PFN_xrVoidFunction*)&table->RequestExitSession));
    (get_inst_proc_addr(instance, "xrWaitFrame", (PFN_xrVoidFunction*)&table->WaitFrame));
    (get_inst_proc_addr(instance, "xrBeginFrame", (PFN_xrVoidFunction*)&table->BeginFrame));
    (get_inst_proc_addr(instance, "xrEndFrame", (PFN_xrVoidFunction*)&table->EndFrame));
    (get_inst_proc_addr(instance, "xrLocateViews", (PFN_xrVoidFunction*)&table->LocateViews));
    (get_inst_proc_addr(instance, "xrStringToPath", (PFN_xrVoidFunction*)&table->StringToPath));
    (get_inst_proc_addr(instance, "xrPathToString", (PFN_xrVoidFunction*)&table->PathToString));
    (get_inst_proc_addr(instance, "xrCreateActionSet", (PFN_xrVoidFunction*)&table->CreateActionSet));
    (get_inst_proc_addr(instance, "xrDestroyActionSet", (PFN_xrVoidFunction*)&table->DestroyActionSet));
    (get_inst_proc_addr(instance, "xrCreateAction", (PFN_xrVoidFunction*)&table->CreateAction));
    (get_inst_proc_addr(instance, "xrDestroyAction", (PFN_xrVoidFunction*)&table->DestroyAction));
    (get_inst_proc_addr(instance, "xrSuggestInteractionProfileBindings", (PFN_xrVoidFunction*)&table->SuggestInteractionProfileBindings));
    (get_inst_proc_addr(instance, "xrAttachSessionActionSets", (PFN_xrVoidFunction*)&table->AttachSessionActionSets));
    (get_inst_proc_addr(instance, "xrGetCurrentInteractionProfile", (PFN_xrVoidFunction*)&table->GetCurrentInteractionProfile));
    (get_inst_proc_addr(instance, "xrGetActionStateBoolean", (PFN_xrVoidFunction*)&table->GetActionStateBoolean));
    (get_inst_proc_addr(instance, "xrGetActionStateFloat", (PFN_xrVoidFunction*)&table->GetActionStateFloat));
    (get_inst_proc_addr(instance, "xrGetActionStateVector2f", (PFN_xrVoidFunction*)&table->GetActionStateVector2f));
    (get_inst_proc_addr(instance, "xrGetActionStatePose", (PFN_xrVoidFunction*)&table->GetActionStatePose));
    (get_inst_proc_addr(instance, "xrSyncActions", (PFN_xrVoidFunction*)&table->SyncActions));
    (get_inst_proc_addr(instance, "xrEnumerateBoundSourcesForAction", (PFN_xrVoidFunction*)&table->EnumerateBoundSourcesForAction));
    (get_inst_proc_addr(instance, "xrGetInputSourceLocalizedName", (PFN_xrVoidFunction*)&table->GetInputSourceLocalizedName));
    (get_inst_proc_addr(instance, "xrApplyHapticFeedback", (PFN_xrVoidFunction*)&table->ApplyHapticFeedback));
    (get_inst_proc_addr(instance, "xrStopHapticFeedback", (PFN_xrVoidFunction*)&table->StopHapticFeedback));

    (get_inst_proc_addr(instance, "xrLocateSpaces", (PFN_xrVoidFunction*)&table->LocateSpaces));

#if defined(XR_USE_PLATFORM_ANDROID)
    (get_inst_proc_addr(instance, "xrSetAndroidApplicationThreadKHR", (PFN_xrVoidFunction*)&table->SetAndroidApplicationThreadKHR));
#endif // defined(XR_USE_PLATFORM_ANDROID)

#if defined(XR_USE_PLATFORM_ANDROID)
    (get_inst_proc_addr(instance, "xrCreateSwapchainAndroidSurfaceKHR", (PFN_xrVoidFunction*)&table->CreateSwapchainAndroidSurfaceKHR));
#endif // defined(XR_USE_PLATFORM_ANDROID)

#if defined(XR_USE_GRAPHICS_API_OPENGL)
    (get_inst_proc_addr(instance, "xrGetOpenGLGraphicsRequirementsKHR", (PFN_xrVoidFunction*)&table->GetOpenGLGraphicsRequirementsKHR));
#endif // defined(XR_USE_GRAPHICS_API_OPENGL)

#if defined(XR_USE_GRAPHICS_API_OPENGL_ES)
    (get_inst_proc_addr(instance, "xrGetOpenGLESGraphicsRequirementsKHR", (PFN_xrVoidFunction*)&table->GetOpenGLESGraphicsRequirementsKHR));
#endif // defined(XR_USE_GRAPHICS_API_OPENGL_ES)

#if defined(XR_USE_GRAPHICS_API_VULKAN)
    (get_inst_proc_addr(instance, "xrGetVulkanInstanceExtensionsKHR", (PFN_xrVoidFunction*)&table->GetVulkanInstanceExtensionsKHR));
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    (get_inst_proc_addr(instance, "xrGetVulkanDeviceExtensionsKHR", (PFN_xrVoidFunction*)&table->GetVulkanDeviceExtensionsKHR));
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    (get_inst_proc_addr(instance, "xrGetVulkanGraphicsDeviceKHR", (PFN_xrVoidFunction*)&table->GetVulkanGraphicsDeviceKHR));
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    (get_inst_proc_addr(instance, "xrGetVulkanGraphicsRequirementsKHR", (PFN_xrVoidFunction*)&table->GetVulkanGraphicsRequirementsKHR));
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)

#if defined(XR_USE_GRAPHICS_API_D3D11)
    (get_inst_proc_addr(instance, "xrGetD3D11GraphicsRequirementsKHR", (PFN_xrVoidFunction*)&table->GetD3D11GraphicsRequirementsKHR));
#endif // defined(XR_USE_GRAPHICS_API_D3D11)

#if defined(XR_USE_GRAPHICS_API_D3D12)
    (get_inst_proc_addr(instance, "xrGetD3D12GraphicsRequirementsKHR", (PFN_xrVoidFunction*)&table->GetD3D12GraphicsRequirementsKHR));
#endif // defined(XR_USE_GRAPHICS_API_D3D12)

    (get_inst_proc_addr(instance, "xrGetVisibilityMaskKHR", (PFN_xrVoidFunction*)&table->GetVisibilityMaskKHR));

#if defined(XR_USE_PLATFORM_WIN32)
    (get_inst_proc_addr(instance, "xrConvertWin32PerformanceCounterToTimeKHR", (PFN_xrVoidFunction*)&table->ConvertWin32PerformanceCounterToTimeKHR));
#endif // defined(XR_USE_PLATFORM_WIN32)
#if defined(XR_USE_PLATFORM_WIN32)
    (get_inst_proc_addr(instance, "xrConvertTimeToWin32PerformanceCounterKHR", (PFN_xrVoidFunction*)&table->ConvertTimeToWin32PerformanceCounterKHR));
#endif // defined(XR_USE_PLATFORM_WIN32)

#if defined(XR_USE_TIMESPEC)
    (get_inst_proc_addr(instance, "xrConvertTimespecTimeToTimeKHR", (PFN_xrVoidFunction*)&table->ConvertTimespecTimeToTimeKHR));
#endif // defined(XR_USE_TIMESPEC)
#if defined(XR_USE_TIMESPEC)
    (get_inst_proc_addr(instance, "xrConvertTimeToTimespecTimeKHR", (PFN_xrVoidFunction*)&table->ConvertTimeToTimespecTimeKHR));
#endif // defined(XR_USE_TIMESPEC)

#if defined(XR_USE_GRAPHICS_API_VULKAN)
    (get_inst_proc_addr(instance, "xrCreateVulkanInstanceKHR", (PFN_xrVoidFunction*)&table->CreateVulkanInstanceKHR));
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    (get_inst_proc_addr(instance, "xrCreateVulkanDeviceKHR", (PFN_xrVoidFunction*)&table->CreateVulkanDeviceKHR));
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    (get_inst_proc_addr(instance, "xrGetVulkanGraphicsDevice2KHR", (PFN_xrVoidFunction*)&table->GetVulkanGraphicsDevice2KHR));
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    (get_inst_proc_addr(instance, "xrGetVulkanGraphicsRequirements2KHR", (PFN_xrVoidFunction*)&table->GetVulkanGraphicsRequirements2KHR));
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)

    (get_inst_proc_addr(instance, "xrLocateSpacesKHR", (PFN_xrVoidFunction*)&table->LocateSpacesKHR));

    (get_inst_proc_addr(instance, "xrPerfSettingsSetPerformanceLevelEXT", (PFN_xrVoidFunction*)&table->PerfSettingsSetPerformanceLevelEXT));

    (get_inst_proc_addr(instance, "xrThermalGetTemperatureTrendEXT", (PFN_xrVoidFunction*)&table->ThermalGetTemperatureTrendEXT));

    (get_inst_proc_addr(instance, "xrSetDebugUtilsObjectNameEXT", (PFN_xrVoidFunction*)&table->SetDebugUtilsObjectNameEXT));
    (get_inst_proc_addr(instance, "xrCreateDebugUtilsMessengerEXT", (PFN_xrVoidFunction*)&table->CreateDebugUtilsMessengerEXT));
    (get_inst_proc_addr(instance, "xrDestroyDebugUtilsMessengerEXT", (PFN_xrVoidFunction*)&table->DestroyDebugUtilsMessengerEXT));
    (get_inst_proc_addr(instance, "xrSubmitDebugUtilsMessageEXT", (PFN_xrVoidFunction*)&table->SubmitDebugUtilsMessageEXT));
    (get_inst_proc_addr(instance, "xrSessionBeginDebugUtilsLabelRegionEXT", (PFN_xrVoidFunction*)&table->SessionBeginDebugUtilsLabelRegionEXT));
    (get_inst_proc_addr(instance, "xrSessionEndDebugUtilsLabelRegionEXT", (PFN_xrVoidFunction*)&table->SessionEndDebugUtilsLabelRegionEXT));
    (get_inst_proc_addr(instance, "xrSessionInsertDebugUtilsLabelEXT", (PFN_xrVoidFunction*)&table->SessionInsertDebugUtilsLabelEXT));

    (get_inst_proc_addr(instance, "xrCreateSpatialAnchorMSFT", (PFN_xrVoidFunction*)&table->CreateSpatialAnchorMSFT));
    (get_inst_proc_addr(instance, "xrCreateSpatialAnchorSpaceMSFT", (PFN_xrVoidFunction*)&table->CreateSpatialAnchorSpaceMSFT));
    (get_inst_proc_addr(instance, "xrDestroySpatialAnchorMSFT", (PFN_xrVoidFunction*)&table->DestroySpatialAnchorMSFT));

    (get_inst_proc_addr(instance, "xrSetInputDeviceActiveEXT", (PFN_xrVoidFunction*)&table->SetInputDeviceActiveEXT));
    (get_inst_proc_addr(instance, "xrSetInputDeviceStateBoolEXT", (PFN_xrVoidFunction*)&table->SetInputDeviceStateBoolEXT));
    (get_inst_proc_addr(instance, "xrSetInputDeviceStateFloatEXT", (PFN_xrVoidFunction*)&table->SetInputDeviceStateFloatEXT));
    (get_inst_proc_addr(instance, "xrSetInputDeviceStateVector2fEXT", (PFN_xrVoidFunction*)&table->SetInputDeviceStateVector2fEXT));
    (get_inst_proc_addr(instance, "xrSetInputDeviceLocationEXT", (PFN_xrVoidFunction*)&table->SetInputDeviceLocationEXT));

    (get_inst_proc_addr(instance, "xrCreateSpatialGraphNodeSpaceMSFT", (PFN_xrVoidFunction*)&table->CreateSpatialGraphNodeSpaceMSFT));
    (get_inst_proc_addr(instance, "xrTryCreateSpatialGraphStaticNodeBindingMSFT", (PFN_xrVoidFunction*)&table->TryCreateSpatialGraphStaticNodeBindingMSFT));
    (get_inst_proc_addr(instance, "xrDestroySpatialGraphNodeBindingMSFT", (PFN_xrVoidFunction*)&table->DestroySpatialGraphNodeBindingMSFT));
    (get_inst_proc_addr(instance, "xrGetSpatialGraphNodeBindingPropertiesMSFT", (PFN_xrVoidFunction*)&table->GetSpatialGraphNodeBindingPropertiesMSFT));

    (get_inst_proc_addr(instance, "xrCreateHandTrackerEXT", (PFN_xrVoidFunction*)&table->CreateHandTrackerEXT));
    (get_inst_proc_addr(instance, "xrDestroyHandTrackerEXT", (PFN_xrVoidFunction*)&table->DestroyHandTrackerEXT));
    (get_inst_proc_addr(instance, "xrLocateHandJointsEXT", (PFN_xrVoidFunction*)&table->LocateHandJointsEXT));

    (get_inst_proc_addr(instance, "xrCreateHandMeshSpaceMSFT", (PFN_xrVoidFunction*)&table->CreateHandMeshSpaceMSFT));
    (get_inst_proc_addr(instance, "xrUpdateHandMeshMSFT", (PFN_xrVoidFunction*)&table->UpdateHandMeshMSFT));

    (get_inst_proc_addr(instance, "xrGetControllerModelKeyMSFT", (PFN_xrVoidFunction*)&table->GetControllerModelKeyMSFT));
    (get_inst_proc_addr(instance, "xrLoadControllerModelMSFT", (PFN_xrVoidFunction*)&table->LoadControllerModelMSFT));
    (get_inst_proc_addr(instance, "xrGetControllerModelPropertiesMSFT", (PFN_xrVoidFunction*)&table->GetControllerModelPropertiesMSFT));
    (get_inst_proc_addr(instance, "xrGetControllerModelStateMSFT", (PFN_xrVoidFunction*)&table->GetControllerModelStateMSFT));

#if defined(XR_USE_PLATFORM_WIN32)
    (get_inst_proc_addr(instance, "xrCreateSpatialAnchorFromPerceptionAnchorMSFT", (PFN_xrVoidFunction*)&table->CreateSpatialAnchorFromPerceptionAnchorMSFT));
#endif // defined(XR_USE_PLATFORM_WIN32)
#if defined(XR_USE_PLATFORM_WIN32)
    (get_inst_proc_addr(instance, "xrTryGetPerceptionAnchorFromSpatialAnchorMSFT", (PFN_xrVoidFunction*)&table->TryGetPerceptionAnchorFromSpatialAnchorMSFT));
#endif // defined(XR_USE_PLATFORM_WIN32)

    (get_inst_proc_addr(instance, "xrEnumerateReprojectionModesMSFT", (PFN_xrVoidFunction*)&table->EnumerateReprojectionModesMSFT));

    (get_inst_proc_addr(instance, "xrUpdateSwapchainFB", (PFN_xrVoidFunction*)&table->UpdateSwapchainFB));
    (get_inst_proc_addr(instance, "xrGetSwapchainStateFB", (PFN_xrVoidFunction*)&table->GetSwapchainStateFB));

    (get_inst_proc_addr(instance, "xrCreateBodyTrackerFB", (PFN_xrVoidFunction*)&table->CreateBodyTrackerFB));
    (get_inst_proc_addr(instance, "xrDestroyBodyTrackerFB", (PFN_xrVoidFunction*)&table->DestroyBodyTrackerFB));
    (get_inst_proc_addr(instance, "xrLocateBodyJointsFB", (PFN_xrVoidFunction*)&table->LocateBodyJointsFB));
    (get_inst_proc_addr(instance, "xrGetBodySkeletonFB", (PFN_xrVoidFunction*)&table->GetBodySkeletonFB));

    (get_inst_proc_addr(instance, "xrEnumerateSceneComputeFeaturesMSFT", (PFN_xrVoidFunction*)&table->EnumerateSceneComputeFeaturesMSFT));
    (get_inst_proc_addr(instance, "xrCreateSceneObserverMSFT", (PFN_xrVoidFunction*)&table->CreateSceneObserverMSFT));
    (get_inst_proc_addr(instance, "xrDestroySceneObserverMSFT", (PFN_xrVoidFunction*)&table->DestroySceneObserverMSFT));
    (get_inst_proc_addr(instance, "xrCreateSceneMSFT", (PFN_xrVoidFunction*)&table->CreateSceneMSFT));
    (get_inst_proc_addr(instance, "xrDestroySceneMSFT", (PFN_xrVoidFunction*)&table->DestroySceneMSFT));
    (get_inst_proc_addr(instance, "xrComputeNewSceneMSFT", (PFN_xrVoidFunction*)&table->ComputeNewSceneMSFT));
    (get_inst_proc_addr(instance, "xrGetSceneComputeStateMSFT", (PFN_xrVoidFunction*)&table->GetSceneComputeStateMSFT));
    (get_inst_proc_addr(instance, "xrGetSceneComponentsMSFT", (PFN_xrVoidFunction*)&table->GetSceneComponentsMSFT));
    (get_inst_proc_addr(instance, "xrLocateSceneComponentsMSFT", (PFN_xrVoidFunction*)&table->LocateSceneComponentsMSFT));
    (get_inst_proc_addr(instance, "xrGetSceneMeshBuffersMSFT", (PFN_xrVoidFunction*)&table->GetSceneMeshBuffersMSFT));

    (get_inst_proc_addr(instance, "xrDeserializeSceneMSFT", (PFN_xrVoidFunction*)&table->DeserializeSceneMSFT));
    (get_inst_proc_addr(instance, "xrGetSerializedSceneFragmentDataMSFT", (PFN_xrVoidFunction*)&table->GetSerializedSceneFragmentDataMSFT));

    (get_inst_proc_addr(instance, "xrEnumerateDisplayRefreshRatesFB", (PFN_xrVoidFunction*)&table->EnumerateDisplayRefreshRatesFB));
    (get_inst_proc_addr(instance, "xrGetDisplayRefreshRateFB", (PFN_xrVoidFunction*)&table->GetDisplayRefreshRateFB));
    (get_inst_proc_addr(instance, "xrRequestDisplayRefreshRateFB", (PFN_xrVoidFunction*)&table->RequestDisplayRefreshRateFB));

    (get_inst_proc_addr(instance, "xrEnumerateViveTrackerPathsHTCX", (PFN_xrVoidFunction*)&table->EnumerateViveTrackerPathsHTCX));

    (get_inst_proc_addr(instance, "xrCreateFacialTrackerHTC", (PFN_xrVoidFunction*)&table->CreateFacialTrackerHTC));
    (get_inst_proc_addr(instance, "xrDestroyFacialTrackerHTC", (PFN_xrVoidFunction*)&table->DestroyFacialTrackerHTC));
    (get_inst_proc_addr(instance, "xrGetFacialExpressionsHTC", (PFN_xrVoidFunction*)&table->GetFacialExpressionsHTC));

    (get_inst_proc_addr(instance, "xrEnumerateColorSpacesFB", (PFN_xrVoidFunction*)&table->EnumerateColorSpacesFB));
    (get_inst_proc_addr(instance, "xrSetColorSpaceFB", (PFN_xrVoidFunction*)&table->SetColorSpaceFB));

    (get_inst_proc_addr(instance, "xrGetHandMeshFB", (PFN_xrVoidFunction*)&table->GetHandMeshFB));

    (get_inst_proc_addr(instance, "xrCreateSpatialAnchorFB", (PFN_xrVoidFunction*)&table->CreateSpatialAnchorFB));
    (get_inst_proc_addr(instance, "xrGetSpaceUuidFB", (PFN_xrVoidFunction*)&table->GetSpaceUuidFB));
    (get_inst_proc_addr(instance, "xrEnumerateSpaceSupportedComponentsFB", (PFN_xrVoidFunction*)&table->EnumerateSpaceSupportedComponentsFB));
    (get_inst_proc_addr(instance, "xrSetSpaceComponentStatusFB", (PFN_xrVoidFunction*)&table->SetSpaceComponentStatusFB));
    (get_inst_proc_addr(instance, "xrGetSpaceComponentStatusFB", (PFN_xrVoidFunction*)&table->GetSpaceComponentStatusFB));

    (get_inst_proc_addr(instance, "xrCreateFoveationProfileFB", (PFN_xrVoidFunction*)&table->CreateFoveationProfileFB));
    (get_inst_proc_addr(instance, "xrDestroyFoveationProfileFB", (PFN_xrVoidFunction*)&table->DestroyFoveationProfileFB));

    (get_inst_proc_addr(instance, "xrQuerySystemTrackedKeyboardFB", (PFN_xrVoidFunction*)&table->QuerySystemTrackedKeyboardFB));
    (get_inst_proc_addr(instance, "xrCreateKeyboardSpaceFB", (PFN_xrVoidFunction*)&table->CreateKeyboardSpaceFB));

    (get_inst_proc_addr(instance, "xrCreateTriangleMeshFB", (PFN_xrVoidFunction*)&table->CreateTriangleMeshFB));
    (get_inst_proc_addr(instance, "xrDestroyTriangleMeshFB", (PFN_xrVoidFunction*)&table->DestroyTriangleMeshFB));
    (get_inst_proc_addr(instance, "xrTriangleMeshGetVertexBufferFB", (PFN_xrVoidFunction*)&table->TriangleMeshGetVertexBufferFB));
    (get_inst_proc_addr(instance, "xrTriangleMeshGetIndexBufferFB", (PFN_xrVoidFunction*)&table->TriangleMeshGetIndexBufferFB));
    (get_inst_proc_addr(instance, "xrTriangleMeshBeginUpdateFB", (PFN_xrVoidFunction*)&table->TriangleMeshBeginUpdateFB));
    (get_inst_proc_addr(instance, "xrTriangleMeshEndUpdateFB", (PFN_xrVoidFunction*)&table->TriangleMeshEndUpdateFB));
    (get_inst_proc_addr(instance, "xrTriangleMeshBeginVertexBufferUpdateFB", (PFN_xrVoidFunction*)&table->TriangleMeshBeginVertexBufferUpdateFB));
    (get_inst_proc_addr(instance, "xrTriangleMeshEndVertexBufferUpdateFB", (PFN_xrVoidFunction*)&table->TriangleMeshEndVertexBufferUpdateFB));

    (get_inst_proc_addr(instance, "xrCreatePassthroughFB", (PFN_xrVoidFunction*)&table->CreatePassthroughFB));
    (get_inst_proc_addr(instance, "xrDestroyPassthroughFB", (PFN_xrVoidFunction*)&table->DestroyPassthroughFB));
    (get_inst_proc_addr(instance, "xrPassthroughStartFB", (PFN_xrVoidFunction*)&table->PassthroughStartFB));
    (get_inst_proc_addr(instance, "xrPassthroughPauseFB", (PFN_xrVoidFunction*)&table->PassthroughPauseFB));
    (get_inst_proc_addr(instance, "xrCreatePassthroughLayerFB", (PFN_xrVoidFunction*)&table->CreatePassthroughLayerFB));
    (get_inst_proc_addr(instance, "xrDestroyPassthroughLayerFB", (PFN_xrVoidFunction*)&table->DestroyPassthroughLayerFB));
    (get_inst_proc_addr(instance, "xrPassthroughLayerPauseFB", (PFN_xrVoidFunction*)&table->PassthroughLayerPauseFB));
    (get_inst_proc_addr(instance, "xrPassthroughLayerResumeFB", (PFN_xrVoidFunction*)&table->PassthroughLayerResumeFB));
    (get_inst_proc_addr(instance, "xrPassthroughLayerSetStyleFB", (PFN_xrVoidFunction*)&table->PassthroughLayerSetStyleFB));
    (get_inst_proc_addr(instance, "xrCreateGeometryInstanceFB", (PFN_xrVoidFunction*)&table->CreateGeometryInstanceFB));
    (get_inst_proc_addr(instance, "xrDestroyGeometryInstanceFB", (PFN_xrVoidFunction*)&table->DestroyGeometryInstanceFB));
    (get_inst_proc_addr(instance, "xrGeometryInstanceSetTransformFB", (PFN_xrVoidFunction*)&table->GeometryInstanceSetTransformFB));

    (get_inst_proc_addr(instance, "xrEnumerateRenderModelPathsFB", (PFN_xrVoidFunction*)&table->EnumerateRenderModelPathsFB));
    (get_inst_proc_addr(instance, "xrGetRenderModelPropertiesFB", (PFN_xrVoidFunction*)&table->GetRenderModelPropertiesFB));
    (get_inst_proc_addr(instance, "xrLoadRenderModelFB", (PFN_xrVoidFunction*)&table->LoadRenderModelFB));

    (get_inst_proc_addr(instance, "xrSetEnvironmentDepthEstimationVARJO", (PFN_xrVoidFunction*)&table->SetEnvironmentDepthEstimationVARJO));

    (get_inst_proc_addr(instance, "xrSetMarkerTrackingVARJO", (PFN_xrVoidFunction*)&table->SetMarkerTrackingVARJO));
    (get_inst_proc_addr(instance, "xrSetMarkerTrackingTimeoutVARJO", (PFN_xrVoidFunction*)&table->SetMarkerTrackingTimeoutVARJO));
    (get_inst_proc_addr(instance, "xrSetMarkerTrackingPredictionVARJO", (PFN_xrVoidFunction*)&table->SetMarkerTrackingPredictionVARJO));
    (get_inst_proc_addr(instance, "xrGetMarkerSizeVARJO", (PFN_xrVoidFunction*)&table->GetMarkerSizeVARJO));
    (get_inst_proc_addr(instance, "xrCreateMarkerSpaceVARJO", (PFN_xrVoidFunction*)&table->CreateMarkerSpaceVARJO));

    (get_inst_proc_addr(instance, "xrSetViewOffsetVARJO", (PFN_xrVoidFunction*)&table->SetViewOffsetVARJO));

#if defined(XR_USE_PLATFORM_ML)
    (get_inst_proc_addr(instance, "xrCreateSpaceFromCoordinateFrameUIDML", (PFN_xrVoidFunction*)&table->CreateSpaceFromCoordinateFrameUIDML));
#endif // defined(XR_USE_PLATFORM_ML)

    (get_inst_proc_addr(instance, "xrCreateMarkerDetectorML", (PFN_xrVoidFunction*)&table->CreateMarkerDetectorML));
    (get_inst_proc_addr(instance, "xrDestroyMarkerDetectorML", (PFN_xrVoidFunction*)&table->DestroyMarkerDetectorML));
    (get_inst_proc_addr(instance, "xrSnapshotMarkerDetectorML", (PFN_xrVoidFunction*)&table->SnapshotMarkerDetectorML));
    (get_inst_proc_addr(instance, "xrGetMarkerDetectorStateML", (PFN_xrVoidFunction*)&table->GetMarkerDetectorStateML));
    (get_inst_proc_addr(instance, "xrGetMarkersML", (PFN_xrVoidFunction*)&table->GetMarkersML));
    (get_inst_proc_addr(instance, "xrGetMarkerReprojectionErrorML", (PFN_xrVoidFunction*)&table->GetMarkerReprojectionErrorML));
    (get_inst_proc_addr(instance, "xrGetMarkerLengthML", (PFN_xrVoidFunction*)&table->GetMarkerLengthML));
    (get_inst_proc_addr(instance, "xrGetMarkerNumberML", (PFN_xrVoidFunction*)&table->GetMarkerNumberML));
    (get_inst_proc_addr(instance, "xrGetMarkerStringML", (PFN_xrVoidFunction*)&table->GetMarkerStringML));
    (get_inst_proc_addr(instance, "xrCreateMarkerSpaceML", (PFN_xrVoidFunction*)&table->CreateMarkerSpaceML));

    (get_inst_proc_addr(instance, "xrEnableLocalizationEventsML", (PFN_xrVoidFunction*)&table->EnableLocalizationEventsML));
    (get_inst_proc_addr(instance, "xrQueryLocalizationMapsML", (PFN_xrVoidFunction*)&table->QueryLocalizationMapsML));
    (get_inst_proc_addr(instance, "xrRequestMapLocalizationML", (PFN_xrVoidFunction*)&table->RequestMapLocalizationML));
    (get_inst_proc_addr(instance, "xrImportLocalizationMapML", (PFN_xrVoidFunction*)&table->ImportLocalizationMapML));
    (get_inst_proc_addr(instance, "xrCreateExportedLocalizationMapML", (PFN_xrVoidFunction*)&table->CreateExportedLocalizationMapML));
    (get_inst_proc_addr(instance, "xrDestroyExportedLocalizationMapML", (PFN_xrVoidFunction*)&table->DestroyExportedLocalizationMapML));
    (get_inst_proc_addr(instance, "xrGetExportedLocalizationMapDataML", (PFN_xrVoidFunction*)&table->GetExportedLocalizationMapDataML));

    (get_inst_proc_addr(instance, "xrCreateSpatialAnchorStoreConnectionMSFT", (PFN_xrVoidFunction*)&table->CreateSpatialAnchorStoreConnectionMSFT));
    (get_inst_proc_addr(instance, "xrDestroySpatialAnchorStoreConnectionMSFT", (PFN_xrVoidFunction*)&table->DestroySpatialAnchorStoreConnectionMSFT));
    (get_inst_proc_addr(instance, "xrPersistSpatialAnchorMSFT", (PFN_xrVoidFunction*)&table->PersistSpatialAnchorMSFT));
    (get_inst_proc_addr(instance, "xrEnumeratePersistedSpatialAnchorNamesMSFT", (PFN_xrVoidFunction*)&table->EnumeratePersistedSpatialAnchorNamesMSFT));
    (get_inst_proc_addr(instance, "xrCreateSpatialAnchorFromPersistedNameMSFT", (PFN_xrVoidFunction*)&table->CreateSpatialAnchorFromPersistedNameMSFT));
    (get_inst_proc_addr(instance, "xrUnpersistSpatialAnchorMSFT", (PFN_xrVoidFunction*)&table->UnpersistSpatialAnchorMSFT));
    (get_inst_proc_addr(instance, "xrClearSpatialAnchorStoreMSFT", (PFN_xrVoidFunction*)&table->ClearSpatialAnchorStoreMSFT));

    (get_inst_proc_addr(instance, "xrGetSceneMarkerRawDataMSFT", (PFN_xrVoidFunction*)&table->GetSceneMarkerRawDataMSFT));
    (get_inst_proc_addr(instance, "xrGetSceneMarkerDecodedStringMSFT", (PFN_xrVoidFunction*)&table->GetSceneMarkerDecodedStringMSFT));

    (get_inst_proc_addr(instance, "xrQuerySpacesFB", (PFN_xrVoidFunction*)&table->QuerySpacesFB));
    (get_inst_proc_addr(instance, "xrRetrieveSpaceQueryResultsFB", (PFN_xrVoidFunction*)&table->RetrieveSpaceQueryResultsFB));

    (get_inst_proc_addr(instance, "xrSaveSpaceFB", (PFN_xrVoidFunction*)&table->SaveSpaceFB));
    (get_inst_proc_addr(instance, "xrEraseSpaceFB", (PFN_xrVoidFunction*)&table->EraseSpaceFB));

#if defined(XR_USE_PLATFORM_WIN32)
    (get_inst_proc_addr(instance, "xrGetAudioOutputDeviceGuidOculus", (PFN_xrVoidFunction*)&table->GetAudioOutputDeviceGuidOculus));
#endif // defined(XR_USE_PLATFORM_WIN32)
#if defined(XR_USE_PLATFORM_WIN32)
    (get_inst_proc_addr(instance, "xrGetAudioInputDeviceGuidOculus", (PFN_xrVoidFunction*)&table->GetAudioInputDeviceGuidOculus));
#endif // defined(XR_USE_PLATFORM_WIN32)

    (get_inst_proc_addr(instance, "xrShareSpacesFB", (PFN_xrVoidFunction*)&table->ShareSpacesFB));

    (get_inst_proc_addr(instance, "xrGetSpaceBoundingBox2DFB", (PFN_xrVoidFunction*)&table->GetSpaceBoundingBox2DFB));
    (get_inst_proc_addr(instance, "xrGetSpaceBoundingBox3DFB", (PFN_xrVoidFunction*)&table->GetSpaceBoundingBox3DFB));
    (get_inst_proc_addr(instance, "xrGetSpaceSemanticLabelsFB", (PFN_xrVoidFunction*)&table->GetSpaceSemanticLabelsFB));
    (get_inst_proc_addr(instance, "xrGetSpaceBoundary2DFB", (PFN_xrVoidFunction*)&table->GetSpaceBoundary2DFB));
    (get_inst_proc_addr(instance, "xrGetSpaceRoomLayoutFB", (PFN_xrVoidFunction*)&table->GetSpaceRoomLayoutFB));

    (get_inst_proc_addr(instance, "xrSetDigitalLensControlALMALENCE", (PFN_xrVoidFunction*)&table->SetDigitalLensControlALMALENCE));

    (get_inst_proc_addr(instance, "xrRequestSceneCaptureFB", (PFN_xrVoidFunction*)&table->RequestSceneCaptureFB));

    (get_inst_proc_addr(instance, "xrGetSpaceContainerFB", (PFN_xrVoidFunction*)&table->GetSpaceContainerFB));

    (get_inst_proc_addr(instance, "xrGetFoveationEyeTrackedStateMETA", (PFN_xrVoidFunction*)&table->GetFoveationEyeTrackedStateMETA));

    (get_inst_proc_addr(instance, "xrCreateFaceTrackerFB", (PFN_xrVoidFunction*)&table->CreateFaceTrackerFB));
    (get_inst_proc_addr(instance, "xrDestroyFaceTrackerFB", (PFN_xrVoidFunction*)&table->DestroyFaceTrackerFB));
    (get_inst_proc_addr(instance, "xrGetFaceExpressionWeightsFB", (PFN_xrVoidFunction*)&table->GetFaceExpressionWeightsFB));

    (get_inst_proc_addr(instance, "xrCreateEyeTrackerFB", (PFN_xrVoidFunction*)&table->CreateEyeTrackerFB));
    (get_inst_proc_addr(instance, "xrDestroyEyeTrackerFB", (PFN_xrVoidFunction*)&table->DestroyEyeTrackerFB));
    (get_inst_proc_addr(instance, "xrGetEyeGazesFB", (PFN_xrVoidFunction*)&table->GetEyeGazesFB));

    (get_inst_proc_addr(instance, "xrPassthroughLayerSetKeyboardHandsIntensityFB", (PFN_xrVoidFunction*)&table->PassthroughLayerSetKeyboardHandsIntensityFB));

    (get_inst_proc_addr(instance, "xrGetDeviceSampleRateFB", (PFN_xrVoidFunction*)&table->GetDeviceSampleRateFB));

    (get_inst_proc_addr(instance, "xrGetPassthroughPreferencesMETA", (PFN_xrVoidFunction*)&table->GetPassthroughPreferencesMETA));

    (get_inst_proc_addr(instance, "xrCreateVirtualKeyboardMETA", (PFN_xrVoidFunction*)&table->CreateVirtualKeyboardMETA));
    (get_inst_proc_addr(instance, "xrDestroyVirtualKeyboardMETA", (PFN_xrVoidFunction*)&table->DestroyVirtualKeyboardMETA));
    (get_inst_proc_addr(instance, "xrCreateVirtualKeyboardSpaceMETA", (PFN_xrVoidFunction*)&table->CreateVirtualKeyboardSpaceMETA));
    (get_inst_proc_addr(instance, "xrSuggestVirtualKeyboardLocationMETA", (PFN_xrVoidFunction*)&table->SuggestVirtualKeyboardLocationMETA));
    (get_inst_proc_addr(instance, "xrGetVirtualKeyboardScaleMETA", (PFN_xrVoidFunction*)&table->GetVirtualKeyboardScaleMETA));
    (get_inst_proc_addr(instance, "xrSetVirtualKeyboardModelVisibilityMETA", (PFN_xrVoidFunction*)&table->SetVirtualKeyboardModelVisibilityMETA));
    (get_inst_proc_addr(instance, "xrGetVirtualKeyboardModelAnimationStatesMETA", (PFN_xrVoidFunction*)&table->GetVirtualKeyboardModelAnimationStatesMETA));
    (get_inst_proc_addr(instance, "xrGetVirtualKeyboardDirtyTexturesMETA", (PFN_xrVoidFunction*)&table->GetVirtualKeyboardDirtyTexturesMETA));
    (get_inst_proc_addr(instance, "xrGetVirtualKeyboardTextureDataMETA", (PFN_xrVoidFunction*)&table->GetVirtualKeyboardTextureDataMETA));
    (get_inst_proc_addr(instance, "xrSendVirtualKeyboardInputMETA", (PFN_xrVoidFunction*)&table->SendVirtualKeyboardInputMETA));
    (get_inst_proc_addr(instance, "xrChangeVirtualKeyboardTextContextMETA", (PFN_xrVoidFunction*)&table->ChangeVirtualKeyboardTextContextMETA));

    (get_inst_proc_addr(instance, "xrEnumerateExternalCamerasOCULUS", (PFN_xrVoidFunction*)&table->EnumerateExternalCamerasOCULUS));

    (get_inst_proc_addr(instance, "xrEnumeratePerformanceMetricsCounterPathsMETA", (PFN_xrVoidFunction*)&table->EnumeratePerformanceMetricsCounterPathsMETA));
    (get_inst_proc_addr(instance, "xrSetPerformanceMetricsStateMETA", (PFN_xrVoidFunction*)&table->SetPerformanceMetricsStateMETA));
    (get_inst_proc_addr(instance, "xrGetPerformanceMetricsStateMETA", (PFN_xrVoidFunction*)&table->GetPerformanceMetricsStateMETA));
    (get_inst_proc_addr(instance, "xrQueryPerformanceMetricsCounterMETA", (PFN_xrVoidFunction*)&table->QueryPerformanceMetricsCounterMETA));

    (get_inst_proc_addr(instance, "xrSaveSpaceListFB", (PFN_xrVoidFunction*)&table->SaveSpaceListFB));

    (get_inst_proc_addr(instance, "xrCreateSpaceUserFB", (PFN_xrVoidFunction*)&table->CreateSpaceUserFB));
    (get_inst_proc_addr(instance, "xrGetSpaceUserIdFB", (PFN_xrVoidFunction*)&table->GetSpaceUserIdFB));
    (get_inst_proc_addr(instance, "xrDestroySpaceUserFB", (PFN_xrVoidFunction*)&table->DestroySpaceUserFB));

    (get_inst_proc_addr(instance, "xrGetRecommendedLayerResolutionMETA", (PFN_xrVoidFunction*)&table->GetRecommendedLayerResolutionMETA));

    (get_inst_proc_addr(instance, "xrCreatePassthroughColorLutMETA", (PFN_xrVoidFunction*)&table->CreatePassthroughColorLutMETA));
    (get_inst_proc_addr(instance, "xrDestroyPassthroughColorLutMETA", (PFN_xrVoidFunction*)&table->DestroyPassthroughColorLutMETA));
    (get_inst_proc_addr(instance, "xrUpdatePassthroughColorLutMETA", (PFN_xrVoidFunction*)&table->UpdatePassthroughColorLutMETA));

    (get_inst_proc_addr(instance, "xrGetSpaceTriangleMeshMETA", (PFN_xrVoidFunction*)&table->GetSpaceTriangleMeshMETA));

    (get_inst_proc_addr(instance, "xrCreateFaceTracker2FB", (PFN_xrVoidFunction*)&table->CreateFaceTracker2FB));
    (get_inst_proc_addr(instance, "xrDestroyFaceTracker2FB", (PFN_xrVoidFunction*)&table->DestroyFaceTracker2FB));
    (get_inst_proc_addr(instance, "xrGetFaceExpressionWeights2FB", (PFN_xrVoidFunction*)&table->GetFaceExpressionWeights2FB));

    (get_inst_proc_addr(instance, "xrCreateEnvironmentDepthProviderMETA", (PFN_xrVoidFunction*)&table->CreateEnvironmentDepthProviderMETA));
    (get_inst_proc_addr(instance, "xrDestroyEnvironmentDepthProviderMETA", (PFN_xrVoidFunction*)&table->DestroyEnvironmentDepthProviderMETA));
    (get_inst_proc_addr(instance, "xrStartEnvironmentDepthProviderMETA", (PFN_xrVoidFunction*)&table->StartEnvironmentDepthProviderMETA));
    (get_inst_proc_addr(instance, "xrStopEnvironmentDepthProviderMETA", (PFN_xrVoidFunction*)&table->StopEnvironmentDepthProviderMETA));
    (get_inst_proc_addr(instance, "xrCreateEnvironmentDepthSwapchainMETA", (PFN_xrVoidFunction*)&table->CreateEnvironmentDepthSwapchainMETA));
    (get_inst_proc_addr(instance, "xrDestroyEnvironmentDepthSwapchainMETA", (PFN_xrVoidFunction*)&table->DestroyEnvironmentDepthSwapchainMETA));
    (get_inst_proc_addr(instance, "xrEnumerateEnvironmentDepthSwapchainImagesMETA", (PFN_xrVoidFunction*)&table->EnumerateEnvironmentDepthSwapchainImagesMETA));
    (get_inst_proc_addr(instance, "xrGetEnvironmentDepthSwapchainStateMETA", (PFN_xrVoidFunction*)&table->GetEnvironmentDepthSwapchainStateMETA));
    (get_inst_proc_addr(instance, "xrAcquireEnvironmentDepthImageMETA", (PFN_xrVoidFunction*)&table->AcquireEnvironmentDepthImageMETA));
    (get_inst_proc_addr(instance, "xrSetEnvironmentDepthHandRemovalMETA", (PFN_xrVoidFunction*)&table->SetEnvironmentDepthHandRemovalMETA));

    (get_inst_proc_addr(instance, "xrSetTrackingOptimizationSettingsHintQCOM", (PFN_xrVoidFunction*)&table->SetTrackingOptimizationSettingsHintQCOM));

    (get_inst_proc_addr(instance, "xrCreatePassthroughHTC", (PFN_xrVoidFunction*)&table->CreatePassthroughHTC));
    (get_inst_proc_addr(instance, "xrDestroyPassthroughHTC", (PFN_xrVoidFunction*)&table->DestroyPassthroughHTC));

    (get_inst_proc_addr(instance, "xrApplyFoveationHTC", (PFN_xrVoidFunction*)&table->ApplyFoveationHTC));

    (get_inst_proc_addr(instance, "xrCreateSpatialAnchorHTC", (PFN_xrVoidFunction*)&table->CreateSpatialAnchorHTC));
    (get_inst_proc_addr(instance, "xrGetSpatialAnchorNameHTC", (PFN_xrVoidFunction*)&table->GetSpatialAnchorNameHTC));

    (get_inst_proc_addr(instance, "xrApplyForceFeedbackCurlMNDX", (PFN_xrVoidFunction*)&table->ApplyForceFeedbackCurlMNDX));

    (get_inst_proc_addr(instance, "xrCreatePlaneDetectorEXT", (PFN_xrVoidFunction*)&table->CreatePlaneDetectorEXT));
    (get_inst_proc_addr(instance, "xrDestroyPlaneDetectorEXT", (PFN_xrVoidFunction*)&table->DestroyPlaneDetectorEXT));
    (get_inst_proc_addr(instance, "xrBeginPlaneDetectionEXT", (PFN_xrVoidFunction*)&table->BeginPlaneDetectionEXT));
    (get_inst_proc_addr(instance, "xrGetPlaneDetectionStateEXT", (PFN_xrVoidFunction*)&table->GetPlaneDetectionStateEXT));
    (get_inst_proc_addr(instance, "xrGetPlaneDetectionsEXT", (PFN_xrVoidFunction*)&table->GetPlaneDetectionsEXT));
    (get_inst_proc_addr(instance, "xrGetPlanePolygonBufferEXT", (PFN_xrVoidFunction*)&table->GetPlanePolygonBufferEXT));

    (get_inst_proc_addr(instance, "xrPollFutureEXT", (PFN_xrVoidFunction*)&table->PollFutureEXT));
    (get_inst_proc_addr(instance, "xrCancelFutureEXT", (PFN_xrVoidFunction*)&table->CancelFutureEXT));

    (get_inst_proc_addr(instance, "xrEnableUserCalibrationEventsML", (PFN_xrVoidFunction*)&table->EnableUserCalibrationEventsML));
}


#ifdef __cplusplus
} // extern "C"
#endif

