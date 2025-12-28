package com.example.faischat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.faischat.data.AuthRepository;
import com.example.faischat.data.InMemoryAuthRepository;
import com.example.faischat.model.RegistrationResult;
import com.example.faischat.model.ShareKeyResult;
import com.example.faischat.model.UserRegistration;
import com.example.faischat.model.UserRole;
import com.google.android.material.textfield.TextInputEditText;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final AuthRepository authRepository = new InMemoryAuthRepository();

    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText partnerNameInput;
    private TextInputEditText partnerPhoneInput;
    private TextInputEditText keyLabelInput;
    private TextInputEditText keyDurationInput;
    private RadioGroup roleGroup;
    private View partnerSection;
    private TextView registrationStatus;
    private TextView keyStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        setupRoleSelection();
        setupActions();
    }

    private void bindViews() {
        nameInput = findViewById(R.id.input_name);
        emailInput = findViewById(R.id.input_email);
        phoneInput = findViewById(R.id.input_phone);
        partnerNameInput = findViewById(R.id.input_partner_name);
        partnerPhoneInput = findViewById(R.id.input_partner_phone);
        keyLabelInput = findViewById(R.id.input_key_label);
        keyDurationInput = findViewById(R.id.input_key_duration);
        roleGroup = findViewById(R.id.role_group);
        partnerSection = findViewById(R.id.partner_section);
        registrationStatus = findViewById(R.id.registration_status);
        keyStatus = findViewById(R.id.key_status);
    }

    private void setupRoleSelection() {
        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            UserRole selectedRole = resolveRoleFromId(checkedId);
            updatePartnerVisibility(selectedRole == UserRole.PAREJA_SW);
        });
        // Default selection
        ((RadioButton) findViewById(R.id.role_couple)).setChecked(true);
        updatePartnerVisibility(true);
    }

    private void setupActions() {
        Button registerButton = findViewById(R.id.button_register);
        Button generateKeyButton = findViewById(R.id.button_generate_key);

        registerButton.setOnClickListener(v -> handleRegistration());
        generateKeyButton.setOnClickListener(v -> handleKeyGeneration());
    }

    private void handleRegistration() {
        UserRole selectedRole = resolveRoleFromId(roleGroup.getCheckedRadioButtonId());
        String name = safeText(nameInput);
        String email = safeText(emailInput);
        String phone = safeText(phoneInput);
        String partnerName = safeText(partnerNameInput);
        String partnerPhone = safeText(partnerPhoneInput);

        UserRegistration registration = new UserRegistration(
                name,
                email,
                phone,
                partnerName,
                partnerPhone,
                selectedRole
        );

        RegistrationResult result = authRepository.registerUser(registration);
        showRegistrationFeedback(result);
    }

    private void handleKeyGeneration() {
        String email = safeText(emailInput);
        String phone = safeText(phoneInput);
        String keyLabel = safeText(keyLabelInput);
        String durationText = safeText(keyDurationInput);

        Duration duration = parseDuration(durationText);
        String ownerId = resolveUserId(email, phone);
        ShareKeyResult shareKeyResult = authRepository.generateShareKey(ownerId, keyLabel, duration);
        showKeyFeedback(shareKeyResult);
    }

    private void showRegistrationFeedback(RegistrationResult result) {
        int color = result.isSuccess() ? com.google.android.material.R.color.design_default_color_primary : android.R.color.holo_red_dark;
        registrationStatus.setTextColor(ContextCompat.getColor(this, color));
        String feedback = result.getMessage();
        if (result.isSuccess()) {
            feedback += "\nRol asignado: " + result.getFinalRole().displayName();
            if (result.isLinkedWithPartner()) {
                feedback += " (pareja vinculada)";
            }
        }
        registrationStatus.setText(feedback);
    }

    private void showKeyFeedback(ShareKeyResult result) {
        int color = result.isSuccess() ? com.google.android.material.R.color.design_default_color_primary : android.R.color.holo_red_dark;
        keyStatus.setTextColor(ContextCompat.getColor(this, color));
        String feedback = result.getMessage();
        if (result.isSuccess()) {
            feedback += "\nCódigo: " + result.getKeyValue();
            feedback += formatExpiration(result.getExpiresAt());
        }
        keyStatus.setText(feedback);
    }

    private String formatExpiration(Instant expiresAt) {
        if (expiresAt == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.getDefault())
                .withZone(ZoneId.systemDefault());
        return "\nExpira: " + formatter.format(expiresAt);
    }

    private void updatePartnerVisibility(boolean visible) {
        partnerSection.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private UserRole resolveRoleFromId(int checkedId) {
        if (checkedId == R.id.role_couple) {
            return UserRole.PAREJA_SW;
        } else if (checkedId == R.id.role_unicorn) {
            return UserRole.UNICORNIO;
        }
        return UserRole.SINGLER;
    }

    private String safeText(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private String resolveUserId(String email, String phone) {
        if (!TextUtils.isEmpty(email)) {
            return email.trim().toLowerCase();
        }
        if (!TextUtils.isEmpty(phone)) {
            return phone.trim().toLowerCase();
        }
        return null;
    }

    private Duration parseDuration(String durationText) {
        if (TextUtils.isEmpty(durationText)) {
            return null;
        }
        try {
            long minutes = Long.parseLong(durationText);
            if (minutes <= 0) {
                return null;
            }
            return Duration.ofMinutes(minutes);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
