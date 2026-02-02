package com.winlator.cmod.core;

public abstract class DefaultVersion {
    public static final String BOX64 = "0.4.1";
    public static final String WOWBOX64 = "0.4.1";
    public static final String FEXCORE = "2601";
    public static final String WRAPPER = "System";
    public static final String WRAPPER_ADRENO = "turnip_Jan-22-2026_8g2_patched";
    public static final String DXVK = GPUInformation.getRenderer(null, null).contains("Mali") ? "1.10.3" : "2.7.1";
    public static final String VKD3D = "proton-3.0b";
}