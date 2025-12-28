package com.example.faischat.model;

import java.time.Instant;

public class ShareKeyResult {
    private final boolean success;
    private final String message;
    private final String keyValue;
    private final Instant expiresAt;

    public ShareKeyResult(boolean success, String message, String keyValue, Instant expiresAt) {
        this.success = success;
        this.message = message;
        this.keyValue = keyValue;
        this.expiresAt = expiresAt;
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
