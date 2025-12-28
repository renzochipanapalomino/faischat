package com.example.faischat.data;

import android.text.TextUtils;

import com.example.faischat.model.RegistrationResult;
import com.example.faischat.model.ShareKeyResult;
import com.example.faischat.model.UserRegistration;
import com.example.faischat.model.UserRole;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class inMemoryAuthRepository implements AuthRepository {

    private static final Map<String, RegisteredUser> USERS = new ConcurrentHashMap<>();
    private static final Map<String, ShareKey> SHARE_KEYS = new ConcurrentHashMap<>();

    @Override
    public RegistrationResult registerUser(UserRegistration registration) {
        String userId = resolveUserId(registration.getEmail(), registration.getPhone());

        if (TextUtils.isEmpty(userId)) {
            return new RegistrationResult(
                    false,
                    "Necesitas al menos correo o teléfono.",
                    registration.getDesiredRole(),
                    false
            );
        }

        if (USERS.containsKey(userId)) {
            RegisteredUser existing = USERS.get(userId);
            return new RegistrationResult(
                    false,
                    "El usuario ya existe.",
                    existing.role,
                    existing.isLinked()
            );
        }

        RegisteredUser user = RegisteredUser.from(
                registration,
                userId,
                registration.getDesiredRole()
        );

        USERS.put(userId, user);

        return new RegistrationResult(
                true,
                "Registro exitoso.",
                registration.getDesiredRole(),
                false
        );
    }

    @Override
    public ShareKeyResult generateShareKey(
            String ownerId,
            String label,
            long durationMinutes
    ) {
        if (TextUtils.isEmpty(ownerId)) {
            return new ShareKeyResult(false, "Selecciona una cuenta.", null, 0);
        }

        if (TextUtils.isEmpty(label)) {
            return new ShareKeyResult(false, "Asigna un nombre a la llave.", null, 0);
        }

        if (durationMinutes <= 0) {
            return new ShareKeyResult(false, "Duración inválida.", null, 0);
        }

        String keyValue = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        long expiresAtMillis = System.currentTimeMillis()
                + durationMinutes * 60_000;

        ShareKey key = new ShareKey(
                keyValue,
                label.trim(),
                expiresAtMillis,
                ownerId
        );

        SHARE_KEYS.put(ownerId, key);

        return new ShareKeyResult(
                true,
                "Llave creada correctamente.",
                keyValue,
                (int) expiresAtMillis
        );
    }

    private String resolveUserId(String email, String phone) {
        if (!TextUtils.isEmpty(email)) return normalize(email);
        if (!TextUtils.isEmpty(phone)) return normalize(phone);
        return null;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase();
    }

    private static class RegisteredUser {
        final String id;
        final String name;
        final String email;
        final String phone;
        final UserRole role;
        String partnerId;

        RegisteredUser(
                String id,
                String name,
                String email,
                String phone,
                UserRole role
        ) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.role = role;
        }

        static RegisteredUser from(
                UserRegistration r,
                String id,
                UserRole role
        ) {
            return new RegisteredUser(
                    id,
                    r.getName(),
                    r.getEmail(),
                    r.getPhone(),
                    role
            );
        }

        boolean isLinked() {
            return !TextUtils.isEmpty(partnerId);
        }
    }

    private static class ShareKey {
        final String keyValue;
        final String label;
        final long expiresAtMillis;
        final String ownerId;

        ShareKey(
                String keyValue,
                String label,
                long expiresAtMillis,
                String ownerId
        ) {
            this.keyValue = keyValue;
            this.label = label;
            this.expiresAtMillis = expiresAtMillis;
            this.ownerId = ownerId;
        }
    }
}
