package com.example.faischat.model;

public enum UserRole {
    PAREJA_SW,
    UNICORNIO,
    SINGLER;

    public String displayName() {
        switch (this) {
            case PAREJA_SW:
                return "Pareja SW";
            case UNICORNIO:
                return "Unicornio";
            case SINGLER:
            default:
                return "Singler";
        }
    }
}
