package com.example.faischat.model;

public class RegistrationResult {
    private final boolean success;
    private final String message;
    private final UserRole finalRole;
    private final boolean linkedWithPartner;

    public RegistrationResult(boolean success, String message, UserRole finalRole, boolean linkedWithPartner) {
        this.success = success;
        this.message = message;
        this.finalRole = finalRole;
        this.linkedWithPartner = linkedWithPartner;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public UserRole getFinalRole() {
        return finalRole;
    }

    public boolean isLinkedWithPartner() {
        return linkedWithPartner;
    }
}
