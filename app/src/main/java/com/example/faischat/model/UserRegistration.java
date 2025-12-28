package com.example.faischat.model;

public class UserRegistration {
    private final String name;
    private final String email;
    private final String phone;
    private final String partnerName;
    private final String partnerPhone;
    private final UserRole desiredRole;

    public UserRegistration(
            String name,
            String email,
            String phone,
            String partnerName,
            String partnerPhone,
            UserRole desiredRole
    ) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.partnerName = partnerName;
        this.partnerPhone = partnerPhone;
        this.desiredRole = desiredRole;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public String getPartnerPhone() {
        return partnerPhone;
    }

    public UserRole getDesiredRole() {
        return desiredRole;
    }
}
