package com.winlator.cmod.xserver.errors;

public class BadSHMSegment extends XRequestError {
    public static final int ERROR_CODE = 128;

    public BadSHMSegment(int id) {
        super(ERROR_CODE, id);
    }

    public BadSHMSegment(String message) {
        super(ERROR_CODE, 0);
        System.err.println("BadSHMSegment: " + message);
    }
}
