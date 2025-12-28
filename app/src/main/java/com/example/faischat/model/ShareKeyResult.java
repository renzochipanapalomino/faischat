package com.example.faischat.model;

public class ShareKeyResult {
    private final boolean success;
    private final String message;
    private final String keyValue;
    private final long expiresAtMillis;

    public ShareKeyResult(boolean success, String message, String keyValue, long expiresAtMillis) {
        this.success = success;
        this.message = message;
        this.keyValue = keyValue;
        this.expiresAtMillis = expiresAtMillis;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }
}
