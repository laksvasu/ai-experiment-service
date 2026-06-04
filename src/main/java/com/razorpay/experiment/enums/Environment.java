package com.razorpay.experiment.enums;

public enum Environment {
    DEV,
    STAGING,
    PROD;

    public static Environment fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment must not be blank");
        }
        return Environment.valueOf(value.trim().toUpperCase());
    }
}
