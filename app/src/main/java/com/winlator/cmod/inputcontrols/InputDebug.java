package com.winlator.cmod.inputcontrols;

/**
 * Single switch for the verbose logging of the whole input system (fakeinput, gyro, controllers, OSC).
 * Keep it false in committed code: the guarded calls run at sensor/event rate and would flood logcat.
 * Low-frequency logs (lifecycle, settings, failures) are not gated by this flag.
 */
public final class InputDebug {
    public static final boolean VERBOSE = false;

    private InputDebug() {}
}