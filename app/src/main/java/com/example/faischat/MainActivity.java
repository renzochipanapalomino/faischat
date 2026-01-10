package com.example.faischat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.faischat.data.AuthRepository;
import com.example.faischat.data.SupabaseClient;
import com.example.faischat.data.inMemoryAuthRepository;
import com.example.faischat.model.RegistrationResult;
import com.example.faischat.model.ShareKeyResult;
import com.example.faischat.model.UserRegistration;
import com.example.faischat.model.UserRole;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.time.Duration;

public class MainActivity extends AppCompatActivity {

    private final SupabaseClient supabaseClient = new SupabaseClient(
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_ANON_KEY,
            BuildConfig.SUPABASE_DB_PASSWORD
    );

    private final AuthRepository authRepository = new inMemoryAuthRepository() {
        @Override
        public ShareKeyResult generateShareKey(String ownerId, String label, Duration duration) {
            return null;
        }
    };

    private TextInputEditText emailInput, passwordInput, partnerNameInput, partnerPhoneInput;
    private MaterialButtonToggleGroup roleGroup;
    private View partnerSection;
    private TextView registrationStatus, supabaseStatus, forgotPasswordText;
    private Button loginButton, registerButton, googleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        bindViews();
        setupRoleSelection();
        setupActions();
        updateSupabaseStatus();
    }

    private void bindViews() {
        emailInput = findViewById(R.id.input_email);
        passwordInput = findViewById(R.id.input_password);
        partnerNameInput = findViewById(R.id.input_partner_name);
        partnerPhoneInput = findViewById(R.id.input_partner_phone);
        roleGroup = findViewById(R.id.role_group);
        partnerSection = findViewById(R.id.partner_section);
        registrationStatus = findViewById(R.id.registration_status);
        supabaseStatus = findViewById(R.id.supabase_status);
        forgotPasswordText = findViewById(R.id.text_forgot_password);
        loginButton = findViewById(R.id.button_login);
        registerButton = findViewById(R.id.button_register);
        googleButton = findViewById(R.id.button_google);
    }

    private void setupRoleSelection() {
        if (roleGroup != null) {
            roleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                UserRole role = resolveRoleFromId(checkedId);
                updatePartnerVisibility(role == UserRole.PAREJA_SW);
            });
            roleGroup.check(R.id.role_couple);
        }
    }

    private void setupActions() {
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> handleLogin());
        }
        if (registerButton != null) {
            registerButton.setOnClickListener(v -> handleRegistration());
        }
        if (googleButton != null) {
            googleButton.setOnClickListener(v -> handleGoogleSignIn());
        }
        if (forgotPasswordText != null) {
            forgotPasswordText.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, R.string.forgot_password, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void handleLogin() {
        String email = safeText(emailInput);
        String password = safeText(passwordInput);

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError(getString(R.string.already_account)); // Usando un recurso existente para evitar hardcoding
            return;
        }
        UserRole role = (roleGroup != null) ? resolveRoleFromId(roleGroup.getCheckedButtonId()) : UserRole.SINGLER;
        showRegistrationFeedback(new RegistrationResult(true, "Acceso...", role, false));
    }

    private void handleRegistration() {
        UserRole role = (roleGroup != null) ? resolveRoleFromId(roleGroup.getCheckedButtonId()) : UserRole.SINGLER;

        UserRegistration registration = new UserRegistration(
                "Usuario",
                safeText(emailInput),
                "000000000",
                safeText(passwordInput),
                safeText(partnerNameInput),
                safeText(partnerPhoneInput),
                role
        );

        RegistrationResult result = authRepository.registerUser(registration);
        showRegistrationFeedback(result);

        if (result.isSuccess()) {
            syncWithSupabase(registration);
        }
    }

    private void handleGoogleSignIn() {
        Toast.makeText(this, R.string.button_google, Toast.LENGTH_SHORT).show();
    }

    private void showRegistrationFeedback(RegistrationResult result) {
        if (registrationStatus == null) return;
        int colorRes = result.isSuccess() ? android.R.color.holo_green_dark : android.R.color.holo_red_dark;
        registrationStatus.setTextColor(ContextCompat.getColor(this, colorRes));
        registrationStatus.setText(result.getMessage());
    }

    private void showError(String message) {
        if (registrationStatus == null) return;
        registrationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        registrationStatus.setText(message);
    }

    private void updatePartnerVisibility(boolean visible) {
        if (partnerSection != null) {
            partnerSection.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void updateSupabaseStatus() {
        if (supabaseStatus == null) return;
        if (supabaseClient.isConfigured()) {
            supabaseStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            supabaseStatus.setText(R.string.supabase_ready);
        } else {
            supabaseStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            supabaseStatus.setText(R.string.supabase_not_configured);
        }
    }

    private void syncWithSupabase(UserRegistration registration) {
        if (supabaseStatus == null || !supabaseClient.isConfigured()) return;
        supabaseStatus.setText(R.string.supabase_creating);
        supabaseClient.signUp(registration.getEmail(), supabaseClient.getDbPassword(), new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    supabaseStatus.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_green_dark));
                    supabaseStatus.setText(message);
                });
            }
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    supabaseStatus.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                    supabaseStatus.setText(message);
                });
            }
        });
    }

    private UserRole resolveRoleFromId(int id) {
        if (id == R.id.role_couple) return UserRole.PAREJA_SW;
        if (id == R.id.role_woman) return UserRole.MUJER;
        return UserRole.SINGLER;
    }

    private String safeText(TextInputEditText editText) {
        return (editText != null && editText.getText() != null) ? editText.getText().toString().trim() : "";
    }
}
