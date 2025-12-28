package com.example.faischat.model;

import java.time.Instant;

public class ShareKeyResult {

    private final boolean success;
    private final String message;
    private final String keyValue;
    private final Instant expiresAt;

    public ShareKeyResult(
            boolean success,
            String message,
            String keyValue,
            Instant expiresAt
    ) {
        this.success = success;
        this.message = message;
        this.keyValue = keyValue;
        this.expiresAt = expiresAt;
    }

    public ShareKeyResult(boolean success, String message, String keyValue, int i, boolean success1, String message1, String keyValue1, Instant expiresAt) {
        this.success = success1;
        this.message = message1;
        this.keyValue = keyValue1;
        this.expiresAt = expiresAt;
    }

    public ShareKeyResult(boolean success, String message, String keyValue, int i) {

        this.success = false;
        this.message = "";
        expiresAt = null;
        this.keyValue = "";
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

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
