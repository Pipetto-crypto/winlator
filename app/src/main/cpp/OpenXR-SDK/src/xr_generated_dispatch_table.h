



#pragma once

#include "xr_dependencies.h"
#include <openxr/openxr.h>
#include <openxr/openxr_platform.h>


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

#if defined(XR_USE_PLATFORM_ANDROID)
    PFN_xrSetAndroidApplicationThreadKHR SetAndroidApplicationThreadKHR;
#endif // defined(XR_USE_PLATFORM_ANDROID)

#if defined(XR_USE_PLATFORM_ANDROID)
    PFN_xrCreateSwapchainAndroidSurfaceKHR CreateSwapchainAndroidSurfaceKHR;
#endif // defined(XR_USE_PLATFORM_ANDROID)

#if defined(XR_USE_GRAPHICS_API_OPENGL)
    PFN_xrGetOpenGLGraphicsRequirementsKHR GetOpenGLGraphicsRequirementsKHR;
#endif // defined(XR_USE_GRAPHICS_API_OPENGL)

#if defined(XR_USE_GRAPHICS_API_OPENGL_ES)
    PFN_xrGetOpenGLESGraphicsRequirementsKHR GetOpenGLESGraphicsRequirementsKHR;
#endif // defined(XR_USE_GRAPHICS_API_OPENGL_ES)

#if defined(XR_USE_GRAPHICS_API_VULKAN)
    PFN_xrGetVulkanInstanceExtensionsKHR GetVulkanInstanceExtensionsKHR;
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    PFN_xrGetVulkanDeviceExtensionsKHR GetVulkanDeviceExtensionsKHR;
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    PFN_xrGetVulkanGraphicsDeviceKHR GetVulkanGraphicsDeviceKHR;
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    PFN_xrGetVulkanGraphicsRequirementsKHR GetVulkanGraphicsRequirementsKHR;
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)

#if defined(XR_USE_GRAPHICS_API_D3D11)
    PFN_xrGetD3D11GraphicsRequirementsKHR GetD3D11GraphicsRequirementsKHR;
#endif // defined(XR_USE_GRAPHICS_API_D3D11)

#if defined(XR_USE_GRAPHICS_API_D3D12)
    PFN_xrGetD3D12GraphicsRequirementsKHR GetD3D12GraphicsRequirementsKHR;
#endif // defined(XR_USE_GRAPHICS_API_D3D12)

    PFN_xrGetVisibilityMaskKHR GetVisibilityMaskKHR;

#if defined(XR_USE_PLATFORM_WIN32)
    PFN_xrConvertWin32PerformanceCounterToTimeKHR ConvertWin32PerformanceCounterToTimeKHR;
#endif // defined(XR_USE_PLATFORM_WIN32)
#if defined(XR_USE_PLATFORM_WIN32)
    PFN_xrConvertTimeToWin32PerformanceCounterKHR ConvertTimeToWin32PerformanceCounterKHR;
#endif // defined(XR_USE_PLATFORM_WIN32)

#if defined(XR_USE_TIMESPEC)
    PFN_xrConvertTimespecTimeToTimeKHR ConvertTimespecTimeToTimeKHR;
#endif // defined(XR_USE_TIMESPEC)
#if defined(XR_USE_TIMESPEC)
    PFN_xrConvertTimeToTimespecTimeKHR ConvertTimeToTimespecTimeKHR;
#endif // defined(XR_USE_TIMESPEC)

    PFN_xrInitializeLoaderKHR InitializeLoaderKHR;

#if defined(XR_USE_GRAPHICS_API_VULKAN)
    PFN_xrCreateVulkanInstanceKHR CreateVulkanInstanceKHR;
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    PFN_xrCreateVulkanDeviceKHR CreateVulkanDeviceKHR;
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    PFN_xrGetVulkanGraphicsDevice2KHR GetVulkanGraphicsDevice2KHR;
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)
#if defined(XR_USE_GRAPHICS_API_VULKAN)
    PFN_xrGetVulkanGraphicsRequirements2KHR GetVulkanGraphicsRequirements2KHR;
#endif // defined(XR_USE_GRAPHICS_API_VULKAN)

    PFN_xrLocateSpacesKHR LocateSpacesKHR;

    PFN_xrPerfSettingsSetPerformanceLevelEXT PerfSettingsSetPerformanceLevelEXT;

    PFN_xrThermalGetTemperatureTrendEXT ThermalGetTemperatureTrendEXT;

    PFN_xrSetDebugUtilsObjectNameEXT SetDebugUtilsObjectNameEXT;
    PFN_xrCreateDebugUtilsMessengerEXT CreateDebugUtilsMessengerEXT;
    PFN_xrDestroyDebugUtilsMessengerEXT DestroyDebugUtilsMessengerEXT;
    PFN_xrSubmitDebugUtilsMessageEXT SubmitDebugUtilsMessageEXT;
    PFN_xrSessionBeginDebugUtilsLabelRegionEXT SessionBeginDebugUtilsLabelRegionEXT;
    PFN_xrSessionEndDebugUtilsLabelRegionEXT SessionEndDebugUtilsLabelRegionEXT;
    PFN_xrSessionInsertDebugUtilsLabelEXT SessionInsertDebugUtilsLabelEXT;

    PFN_xrCreateSpatialAnchorMSFT CreateSpatialAnchorMSFT;
    PFN_xrCreateSpatialAnchorSpaceMSFT CreateSpatialAnchorSpaceMSFT;
    PFN_xrDestroySpatialAnchorMSFT DestroySpatialAnchorMSFT;

    PFN_xrSetInputDeviceActiveEXT SetInputDeviceActiveEXT;
    PFN_xrSetInputDeviceStateBoolEXT SetInputDeviceStateBoolEXT;
    PFN_xrSetInputDeviceStateFloatEXT SetInputDeviceStateFloatEXT;
    PFN_xrSetInputDeviceStateVector2fEXT SetInputDeviceStateVector2fEXT;
    PFN_xrSetInputDeviceLocationEXT SetInputDeviceLocationEXT;

    PFN_xrCreateSpatialGraphNodeSpaceMSFT CreateSpatialGraphNodeSpaceMSFT;
    PFN_xrTryCreateSpatialGraphStaticNodeBindingMSFT TryCreateSpatialGraphStaticNodeBindingMSFT;
    PFN_xrDestroySpatialGraphNodeBindingMSFT DestroySpatialGraphNodeBindingMSFT;
    PFN_xrGetSpatialGraphNodeBindingPropertiesMSFT GetSpatialGraphNodeBindingPropertiesMSFT;

    PFN_xrCreateHandTrackerEXT CreateHandTrackerEXT;
    PFN_xrDestroyHandTrackerEXT DestroyHandTrackerEXT;
    PFN_xrLocateHandJointsEXT LocateHandJointsEXT;

    PFN_xrCreateHandMeshSpaceMSFT CreateHandMeshSpaceMSFT;
    PFN_xrUpdateHandMeshMSFT UpdateHandMeshMSFT;

    PFN_xrGetControllerModelKeyMSFT GetControllerModelKeyMSFT;
    PFN_xrLoadControllerModelMSFT LoadControllerModelMSFT;
    PFN_xrGetControllerModelPropertiesMSFT GetControllerModelPropertiesMSFT;
    PFN_xrGetControllerModelStateMSFT GetControllerModelStateMSFT;

#if defined(XR_USE_PLATFORM_WIN32)
    PFN_xrCreateSpatialAnchorFromPerceptionAnchorMSFT CreateSpatialAnchorFromPerceptionAnchorMSFT;
#endif // defined(XR_USE_PLATFORM_WIN32)
#if defined(XR_USE_PLATFORM_WIN32)
    PFN_xrTryGetPerceptionAnchorFromSpatialAnchorMSFT TryGetPerceptionAnchorFromSpatialAnchorMSFT;
#endif // defined(XR_USE_PLATFORM_WIN32)

    PFN_xrEnumerateReprojectionModesMSFT EnumerateReprojectionModesMSFT;

    PFN_xrUpdateSwapchainFB UpdateSwapchainFB;
    PFN_xrGetSwapchainStateFB GetSwapchainStateFB;

    PFN_xrCreateBodyTrackerFB CreateBodyTrackerFB;
    PFN_xrDestroyBodyTrackerFB DestroyBodyTrackerFB;
    PFN_xrLocateBodyJointsFB LocateBodyJointsFB;
    PFN_xrGetBodySkeletonFB GetBodySkeletonFB;

    PFN_xrEnumerateSceneComputeFeaturesMSFT EnumerateSceneComputeFeaturesMSFT;
    PFN_xrCreateSceneObserverMSFT CreateSceneObserverMSFT;
    PFN_xrDestroySceneObserverMSFT DestroySceneObserverMSFT;
    PFN_xrCreateSceneMSFT CreateSceneMSFT;
    PFN_xrDestroySceneMSFT DestroySceneMSFT;
    PFN_xrComputeNewSceneMSFT ComputeNewSceneMSFT;
    PFN_xrGetSceneComputeStateMSFT GetSceneComputeStateMSFT;
    PFN_xrGetSceneComponentsMSFT GetSceneComponentsMSFT;
    PFN_xrLocateSceneComponentsMSFT LocateSceneComponentsMSFT;
    PFN_xrGetSceneMeshBuffersMSFT GetSceneMeshBuffersMSFT;

    PFN_xrDeserializeSceneMSFT DeserializeSceneMSFT;
    PFN_xrGetSerializedSceneFragmentDataMSFT GetSerializedSceneFragmentDataMSFT;

    PFN_xrEnumerateDisplayRefreshRatesFB EnumerateDisplayRefreshRatesFB;
    PFN_xrGetDisplayRefreshRateFB GetDisplayRefreshRateFB;
    PFN_xrRequestDisplayRefreshRateFB RequestDisplayRefreshRateFB;

    PFN_xrEnumerateViveTrackerPathsHTCX EnumerateViveTrackerPathsHTCX;

    PFN_xrCreateFacialTrackerHTC CreateFacialTrackerHTC;
    PFN_xrDestroyFacialTrackerHTC DestroyFacialTrackerHTC;
    PFN_xrGetFacialExpressionsHTC GetFacialExpressionsHTC;

    PFN_xrEnumerateColorSpacesFB EnumerateColorSpacesFB;
    PFN_xrSetColorSpaceFB SetColorSpaceFB;

    PFN_xrGetHandMeshFB GetHandMeshFB;

    PFN_xrCreateSpatialAnchorFB CreateSpatialAnchorFB;
    PFN_xrGetSpaceUuidFB GetSpaceUuidFB;
    PFN_xrEnumerateSpaceSupportedComponentsFB EnumerateSpaceSupportedComponentsFB;
    PFN_xrSetSpaceComponentStatusFB SetSpaceComponentStatusFB;
    PFN_xrGetSpaceComponentStatusFB GetSpaceComponentStatusFB;

    PFN_xrCreateFoveationProfileFB CreateFoveationProfileFB;
    PFN_xrDestroyFoveationProfileFB DestroyFoveationProfileFB;

    PFN_xrQuerySystemTrackedKeyboardFB QuerySystemTrackedKeyboardFB;
    PFN_xrCreateKeyboardSpaceFB CreateKeyboardSpaceFB;

    PFN_xrCreateTriangleMeshFB CreateTriangleMeshFB;
    PFN_xrDestroyTriangleMeshFB DestroyTriangleMeshFB;
    PFN_xrTriangleMeshGetVertexBufferFB TriangleMeshGetVertexBufferFB;
    PFN_xrTriangleMeshGetIndexBufferFB TriangleMeshGetIndexBufferFB;
    PFN_xrTriangleMeshBeginUpdateFB TriangleMeshBeginUpdateFB;
    PFN_xrTriangleMeshEndUpdateFB TriangleMeshEndUpdateFB;
    PFN_xrTriangleMeshBeginVertexBufferUpdateFB TriangleMeshBeginVertexBufferUpdateFB;
    PFN_xrTriangleMeshEndVertexBufferUpdateFB TriangleMeshEndVertexBufferUpdateFB;

    PFN_xrCreatePassthroughFB CreatePassthroughFB;
    PFN_xrDestroyPassthroughFB DestroyPassthroughFB;
    PFN_xrPassthroughStartFB PassthroughStartFB;
    PFN_xrPassthroughPauseFB PassthroughPauseFB;
    PFN_xrCreatePassthroughLayerFB CreatePassthroughLayerFB;
    PFN_xrDestroyPassthroughLayerFB DestroyPassthroughLayerFB;
    PFN_xrPassthroughLayerPauseFB PassthroughLayerPauseFB;
    PFN_xrPassthroughLayerResumeFB PassthroughLayerResumeFB;
    PFN_xrPassthroughLayerSetStyleFB PassthroughLayerSetStyleFB;
    PFN_xrCreateGeometryInstanceFB CreateGeometryInstanceFB;
    PFN_xrDestroyGeometryInstanceFB DestroyGeometryInstanceFB;
    PFN_xrGeometryInstanceSetTransformFB GeometryInstanceSetTransformFB;

    PFN_xrEnumerateRenderModelPathsFB EnumerateRenderModelPathsFB;
    PFN_xrGetRenderModelPropertiesFB GetRenderModelPropertiesFB;
    PFN_xrLoadRenderModelFB LoadRenderModelFB;

    PFN_xrSetEnvironmentDepthEstimationVARJO SetEnvironmentDepthEstimationVARJO;

    PFN_xrSetMarkerTrackingVARJO SetMarkerTrackingVARJO;
    PFN_xrSetMarkerTrackingTimeoutVARJO SetMarkerTrackingTimeoutVARJO;
    PFN_xrSetMarkerTrackingPredictionVARJO SetMarkerTrackingPredictionVARJO;
    PFN_xrGetMarkerSizeVARJO GetMarkerSizeVARJO;
    PFN_xrCreateMarkerSpaceVARJO CreateMarkerSpaceVARJO;

    PFN_xrSetViewOffsetVARJO SetViewOffsetVARJO;

#if defined(XR_USE_PLATFORM_ML)
    PFN_xrCreateSpaceFromCoordinateFrameUIDML CreateSpaceFromCoordinateFrameUIDML;
#endif // defined(XR_USE_PLATFORM_ML)

    PFN_xrCreateMarkerDetectorML CreateMarkerDetectorML;
    PFN_xrDestroyMarkerDetectorML DestroyMarkerDetectorML;
    PFN_xrSnapshotMarkerDetectorML SnapshotMarkerDetectorML;
    PFN_xrGetMarkerDetectorStateML GetMarkerDetectorStateML;
    PFN_xrGetMarkersML GetMarkersML;
    PFN_xrGetMarkerReprojectionErrorML GetMarkerReprojectionErrorML;
    PFN_xrGetMarkerLengthML GetMarkerLengthML;
    PFN_xrGetMarkerNumberML GetMarkerNumberML;
    PFN_xrGetMarkerStringML GetMarkerStringML;
    PFN_xrCreateMarkerSpaceML CreateMarkerSpaceML;

    PFN_xrEnableLocalizationEventsML EnableLocalizationEventsML;
    PFN_xrQueryLocalizationMapsML QueryLocalizationMapsML;
    PFN_xrRequestMapLocalizationML RequestMapLocalizationML;
    PFN_xrImportLocalizationMapML ImportLocalizationMapML;
    PFN_xrCreateExportedLocalizationMapML CreateExportedLocalizationMapML;
    PFN_xrDestroyExportedLocalizationMapML DestroyExportedLocalizationMapML;
    PFN_xrGetExportedLocalizationMapDataML GetExportedLocalizationMapDataML;

    PFN_xrCreateSpatialAnchorStoreConnectionMSFT CreateSpatialAnchorStoreConnectionMSFT;
    PFN_xrDestroySpatialAnchorStoreConnectionMSFT DestroySpatialAnchorStoreConnectionMSFT;
    PFN_xrPersistSpatialAnchorMSFT PersistSpatialAnchorMSFT;
    PFN_xrEnumeratePersistedSpatialAnchorNamesMSFT EnumeratePersistedSpatialAnchorNamesMSFT;
    PFN_xrCreateSpatialAnchorFromPersistedNameMSFT CreateSpatialAnchorFromPersistedNameMSFT;
    PFN_xrUnpersistSpatialAnchorMSFT UnpersistSpatialAnchorMSFT;
    PFN_xrClearSpatialAnchorStoreMSFT ClearSpatialAnchorStoreMSFT;

    PFN_xrGetSceneMarkerRawDataMSFT GetSceneMarkerRawDataMSFT;
    PFN_xrGetSceneMarkerDecodedStringMSFT GetSceneMarkerDecodedStringMSFT;

    PFN_xrQuerySpacesFB QuerySpacesFB;
    PFN_xrRetrieveSpaceQueryResultsFB RetrieveSpaceQueryResultsFB;

    PFN_xrSaveSpaceFB SaveSpaceFB;
    PFN_xrEraseSpaceFB EraseSpaceFB;

#if defined(XR_USE_PLATFORM_WIN32)
    PFN_xrGetAudioOutputDeviceGuidOculus GetAudioOutputDeviceGuidOculus;
#endif // defined(XR_USE_PLATFORM_WIN32)
#if defined(XR_USE_PLATFORM_WIN32)
    PFN_xrGetAudioInputDeviceGuidOculus GetAudioInputDeviceGuidOculus;
#endif // defined(XR_USE_PLATFORM_WIN32)

    PFN_xrShareSpacesFB ShareSpacesFB;

    PFN_xrGetSpaceBoundingBox2DFB GetSpaceBoundingBox2DFB;
    PFN_xrGetSpaceBoundingBox3DFB GetSpaceBoundingBox3DFB;
    PFN_xrGetSpaceSemanticLabelsFB GetSpaceSemanticLabelsFB;
    PFN_xrGetSpaceBoundary2DFB GetSpaceBoundary2DFB;
    PFN_xrGetSpaceRoomLayoutFB GetSpaceRoomLayoutFB;

    PFN_xrSetDigitalLensControlALMALENCE SetDigitalLensControlALMALENCE;

    PFN_xrRequestSceneCaptureFB RequestSceneCaptureFB;

    PFN_xrGetSpaceContainerFB GetSpaceContainerFB;

    PFN_xrGetFoveationEyeTrackedStateMETA GetFoveationEyeTrackedStateMETA;

    PFN_xrCreateFaceTrackerFB CreateFaceTrackerFB;
    PFN_xrDestroyFaceTrackerFB DestroyFaceTrackerFB;
    PFN_xrGetFaceExpressionWeightsFB GetFaceExpressionWeightsFB;

    PFN_xrCreateEyeTrackerFB CreateEyeTrackerFB;
    PFN_xrDestroyEyeTrackerFB DestroyEyeTrackerFB;
    PFN_xrGetEyeGazesFB GetEyeGazesFB;

    PFN_xrPassthroughLayerSetKeyboardHandsIntensityFB PassthroughLayerSetKeyboardHandsIntensityFB;

    PFN_xrGetDeviceSampleRateFB GetDeviceSampleRateFB;

    PFN_xrGetPassthroughPreferencesMETA GetPassthroughPreferencesMETA;

    PFN_xrCreateVirtualKeyboardMETA CreateVirtualKeyboardMETA;
    PFN_xrDestroyVirtualKeyboardMETA DestroyVirtualKeyboardMETA;
    PFN_xrCreateVirtualKeyboardSpaceMETA CreateVirtualKeyboardSpaceMETA;
    PFN_xrSuggestVirtualKeyboardLocationMETA SuggestVirtualKeyboardLocationMETA;
    PFN_xrGetVirtualKeyboardScaleMETA GetVirtualKeyboardScaleMETA;
    PFN_xrSetVirtualKeyboardModelVisibilityMETA SetVirtualKeyboardModelVisibilityMETA;
    PFN_xrGetVirtualKeyboardModelAnimationStatesMETA GetVirtualKeyboardModelAnimationStatesMETA;
    PFN_xrGetVirtualKeyboardDirtyTexturesMETA GetVirtualKeyboardDirtyTexturesMETA;
    PFN_xrGetVirtualKeyboardTextureDataMETA GetVirtualKeyboardTextureDataMETA;
    PFN_xrSendVirtualKeyboardInputMETA SendVirtualKeyboardInputMETA;
    PFN_xrChangeVirtualKeyboardTextContextMETA ChangeVirtualKeyboardTextContextMETA;

    PFN_xrEnumerateExternalCamerasOCULUS EnumerateExternalCamerasOCULUS;

    PFN_xrEnumeratePerformanceMetricsCounterPathsMETA EnumeratePerformanceMetricsCounterPathsMETA;
    PFN_xrSetPerformanceMetricsStateMETA SetPerformanceMetricsStateMETA;
    PFN_xrGetPerformanceMetricsStateMETA GetPerformanceMetricsStateMETA;
    PFN_xrQueryPerformanceMetricsCounterMETA QueryPerformanceMetricsCounterMETA;

    PFN_xrSaveSpaceListFB SaveSpaceListFB;

    PFN_xrCreateSpaceUserFB CreateSpaceUserFB;
    PFN_xrGetSpaceUserIdFB GetSpaceUserIdFB;
    PFN_xrDestroySpaceUserFB DestroySpaceUserFB;

    PFN_xrGetRecommendedLayerResolutionMETA GetRecommendedLayerResolutionMETA;

    PFN_xrCreatePassthroughColorLutMETA CreatePassthroughColorLutMETA;
    PFN_xrDestroyPassthroughColorLutMETA DestroyPassthroughColorLutMETA;
    PFN_xrUpdatePassthroughColorLutMETA UpdatePassthroughColorLutMETA;

    PFN_xrGetSpaceTriangleMeshMETA GetSpaceTriangleMeshMETA;

    PFN_xrCreateFaceTracker2FB CreateFaceTracker2FB;
    PFN_xrDestroyFaceTracker2FB DestroyFaceTracker2FB;
    PFN_xrGetFaceExpressionWeights2FB GetFaceExpressionWeights2FB;

    PFN_xrCreateEnvironmentDepthProviderMETA CreateEnvironmentDepthProviderMETA;
    PFN_xrDestroyEnvironmentDepthProviderMETA DestroyEnvironmentDepthProviderMETA;
    PFN_xrStartEnvironmentDepthProviderMETA StartEnvironmentDepthProviderMETA;
    PFN_xrStopEnvironmentDepthProviderMETA StopEnvironmentDepthProviderMETA;
    PFN_xrCreateEnvironmentDepthSwapchainMETA CreateEnvironmentDepthSwapchainMETA;
    PFN_xrDestroyEnvironmentDepthSwapchainMETA DestroyEnvironmentDepthSwapchainMETA;
    PFN_xrEnumerateEnvironmentDepthSwapchainImagesMETA EnumerateEnvironmentDepthSwapchainImagesMETA;
    PFN_xrGetEnvironmentDepthSwapchainStateMETA GetEnvironmentDepthSwapchainStateMETA;
    PFN_xrAcquireEnvironmentDepthImageMETA AcquireEnvironmentDepthImageMETA;
    PFN_xrSetEnvironmentDepthHandRemovalMETA SetEnvironmentDepthHandRemovalMETA;

    PFN_xrSetTrackingOptimizationSettingsHintQCOM SetTrackingOptimizationSettingsHintQCOM;

    PFN_xrCreatePassthroughHTC CreatePassthroughHTC;
    PFN_xrDestroyPassthroughHTC DestroyPassthroughHTC;

    PFN_xrApplyFoveationHTC ApplyFoveationHTC;

    PFN_xrCreateSpatialAnchorHTC CreateSpatialAnchorHTC;
    PFN_xrGetSpatialAnchorNameHTC GetSpatialAnchorNameHTC;

    PFN_xrApplyForceFeedbackCurlMNDX ApplyForceFeedbackCurlMNDX;

    PFN_xrCreatePlaneDetectorEXT CreatePlaneDetectorEXT;
    PFN_xrDestroyPlaneDetectorEXT DestroyPlaneDetectorEXT;
    PFN_xrBeginPlaneDetectionEXT BeginPlaneDetectionEXT;
    PFN_xrGetPlaneDetectionStateEXT GetPlaneDetectionStateEXT;
    PFN_xrGetPlaneDetectionsEXT GetPlaneDetectionsEXT;
    PFN_xrGetPlanePolygonBufferEXT GetPlanePolygonBufferEXT;

    PFN_xrPollFutureEXT PollFutureEXT;
    PFN_xrCancelFutureEXT CancelFutureEXT;

    PFN_xrEnableUserCalibrationEventsML EnableUserCalibrationEventsML;
};


void GeneratedXrPopulateDispatchTable(struct XrGeneratedDispatchTable *table,
                                      XrInstance instance,
                                      PFN_xrGetInstanceProcAddr get_inst_proc_addr);

#ifdef __cplusplus
} // extern "C"
#endif

