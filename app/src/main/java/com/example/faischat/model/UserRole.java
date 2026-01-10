package com.example.faischat.model;

public enum UserRole {
    PAREJA_SW,
    MUJER,
    SINGLER;

    public String displayName() {
        switch (this) {
            case PAREJA_SW:
                return "Pareja";
            case MUJER:
                return "Mujer";
            case SINGLER:
            default:
                return "Singler";
        }
    }
}
