package com.example.faischat.data;

import com.example.faischat.model.RegistrationResult;
import com.example.faischat.model.ShareKeyResult;
import com.example.faischat.model.UserRegistration;

import java.time.Duration;

public interface AuthRepository {

    RegistrationResult registerUser(UserRegistration registration);

    ShareKeyResult generateShareKey(String ownerId, String label, Duration duration);
}
