package com.example.faischat.data;

import android.text.TextUtils;

import com.example.faischat.model.RegistrationResult;
import com.example.faischat.model.ShareKeyResult;
import com.example.faischat.model.UserRegistration;
import com.example.faischat.model.UserRole;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuthRepository implements AuthRepository {

    private static final Map<String, RegisteredUser> USERS = new ConcurrentHashMap<>();
    private static final Map<String, ShareKey> SHARE_KEYS = new ConcurrentHashMap<>();

    @Override
    public RegistrationResult registerUser(UserRegistration registration) {
        String userId = resolveUserId(registration.getEmail(), registration.getPhone());
        if (TextUtils.isEmpty(userId)) {
            return new RegistrationResult(false, "Necesitas al menos correo o teléfono para crear la cuenta.", registration.getDesiredRole(), false);
        }

        if (USERS.containsKey(userId)) {
            return new RegistrationResult(false, "Ya existe un usuario con esos datos. Inicia sesión o usa otros datos.", USERS.get(userId).role, USERS.get(userId).isLinked());
        }

        if (registration.getDesiredRole() == UserRole.PAREJA_SW) {
            if (TextUtils.isEmpty(registration.getPartnerName()) || TextUtils.isEmpty(registration.getPartnerPhone())) {
                return new RegistrationResult(false, "Para registrarte como pareja debes completar nombre y teléfono de tu pareja.", UserRole.PAREJA_SW, false);
            }

            String partnerId = normalize(registration.getPartnerPhone());
            RegisteredUser partner = USERS.get(partnerId);
            if (partner == null) {
                return new RegistrationResult(false, "Tu pareja aún no existe en el sistema. Pídele que se registre para vincularse.", UserRole.PAREJA_SW, false);
            }

            if (partner.role != UserRole.PAREJA_SW) {
                return new RegistrationResult(false, "La persona encontrada no está registrada como pareja. Valida los datos.", UserRole.PAREJA_SW, false);
            }

            if (!partner.name.equalsIgnoreCase(registration.getPartnerName())) {
                return new RegistrationResult(false, "El nombre o teléfono no coincide con la cuenta de tu pareja.", UserRole.PAREJA_SW, false);
            }

            RegisteredUser newUser = RegisteredUser.from(registration, userId, UserRole.PAREJA_SW);
            newUser.partnerId = partnerId;
            USERS.put(userId, newUser);

            if (TextUtils.isEmpty(partner.partnerId)) {
                partner.partnerId = userId;
                USERS.put(partnerId, partner);
            }

            return new RegistrationResult(true, "Pareja vinculada correctamente. Ambos comparten el mismo espacio seguro.", UserRole.PAREJA_SW, true);
        }

        RegisteredUser newUser = RegisteredUser.from(registration, userId, registration.getDesiredRole());
        USERS.put(userId, newUser);
        return new RegistrationResult(true, "Registro guardado como " + registration.getDesiredRole().displayName() + ". Puedes generar llaves temporales.", registration.getDesiredRole(), false);
    }

    @Override
    public ShareKeyResult generateShareKey(String ownerId, String label, Duration duration) {
        if (TextUtils.isEmpty(ownerId)) {
            return new ShareKeyResult(false, "Primero registra o selecciona la cuenta (correo o teléfono).", null, null);
        }

        RegisteredUser owner = USERS.get(ownerId);
        if (owner == null) {
            return new ShareKeyResult(false, "No encontramos la cuenta. Regístrate antes de generar llaves.", null, null);
        }

        if (TextUtils.isEmpty(label)) {
            return new ShareKeyResult(false, "Asigna un nombre a la llave para saber quién tiene acceso.", null, null);
        }

        if (duration == null || duration.isZero() || duration.isNegative()) {
            return new ShareKeyResult(false, "Define una duración mayor a cero minutos.", null, null);
        }

        String keyValue = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant expiresAt = Instant.now().plus(duration);
        ShareKey shareKey = new ShareKey(keyValue, label.trim(), expiresAt, ownerId);
        SHARE_KEYS.put(ownerId, shareKey);

        return new ShareKeyResult(true, "Llave creada y lista para compartir. Recuerda que puedes revocarla creando una nueva.", keyValue, expiresAt);
    }

    private String resolveUserId(String email, String phone) {
        if (!TextUtils.isEmpty(email)) {
            return normalize(email);
        }
        if (!TextUtils.isEmpty(phone)) {
            return normalize(phone);
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private static class RegisteredUser {
        final String id;
        final String name;
        final String email;
        final String phone;
        final UserRole role;
        String partnerId;

        RegisteredUser(String id, String name, String email, String phone, UserRole role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.role = role;
        }

        static RegisteredUser from(UserRegistration registration, String id, UserRole role) {
            return new RegisteredUser(
                    id,
                    registration.getName(),
                    registration.getEmail(),
                    registration.getPhone(),
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
        final Instant expiresAt;
        final String ownerId;

        ShareKey(String keyValue, String label, Instant expiresAt, String ownerId) {
            this.keyValue = keyValue;
            this.label = label;
            this.expiresAt = expiresAt;
            this.ownerId = ownerId;
        }
    }
}
