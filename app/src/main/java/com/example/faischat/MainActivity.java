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

import com.example.faischat.data.SupabaseClient;
import com.example.faischat.data.AuthRepository;
import com.example.faischat.data.inMemoryAuthRepository;
import com.example.faischat.model.RegistrationResult;
import com.example.faischat.model.ShareKeyResult;
import com.example.faischat.model.UserRegistration;
import com.example.faischat.model.UserRole;
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

    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText partnerNameInput;
    private TextInputEditText partnerPhoneInput;

    private RadioGroup roleGroup;
    private View partnerSection;

    private TextView registrationStatus;
    private TextView supabaseStatus;

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
        updateSupabaseStatus();
    }

    private void bindViews() {
        nameInput = findViewById(R.id.input_name);
        emailInput = findViewById(R.id.input_email);
        phoneInput = findViewById(R.id.input_phone);
        partnerNameInput = findViewById(R.id.input_partner_name);
        partnerPhoneInput = findViewById(R.id.input_partner_phone);

        roleGroup = findViewById(R.id.role_group);
        partnerSection = findViewById(R.id.partner_section);

        registrationStatus = findViewById(R.id.registration_status);
        supabaseStatus = findViewById(R.id.supabase_status);
    }

    private void setupRoleSelection() {
        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            UserRole role = resolveRoleFromId(checkedId);
            updatePartnerVisibility(role == UserRole.PAREJA_SW);
        });

        ((RadioButton) findViewById(R.id.role_couple)).setChecked(true);
        updatePartnerVisibility(true);
    }

    private void setupActions() {
        Button registerButton = findViewById(R.id.button_register);
        registerButton.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
        UserRole role = resolveRoleFromId(roleGroup.getCheckedRadioButtonId());

        UserRegistration registration = new UserRegistration(
                safeText(nameInput),
                safeText(emailInput),
                safeText(phoneInput),
                safeText(partnerNameInput),
                safeText(partnerPhoneInput),
                role
        );

        RegistrationResult result = authRepository.registerUser(registration);
        showRegistrationFeedback(result);
        syncWithSupabase(registration);
    }

    private void showRegistrationFeedback(RegistrationResult result) {
        int color = result.isSuccess()
                ? com.google.android.material.R.color.design_default_color_primary
                : android.R.color.holo_red_dark;

        registrationStatus.setTextColor(ContextCompat.getColor(this, color));
        registrationStatus.setText(result.getMessage());
    }

    private void updatePartnerVisibility(boolean visible) {
        partnerSection.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateSupabaseStatus() {
        if (supabaseStatus == null) return;

        if (supabaseClient.isConfigured()) {
            supabaseStatus.setTextColor(ContextCompat.getColor(this, R.color.teal_700));
            supabaseStatus.setText(getString(R.string.supabase_ready));
        } else {
            supabaseStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            supabaseStatus.setText(getString(R.string.supabase_not_configured));
        }
    }

    private void syncWithSupabase(UserRegistration registration) {
        if (supabaseStatus == null) return;

        if (!supabaseClient.isConfigured()) {
            supabaseStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            supabaseStatus.setText(getString(R.string.supabase_not_configured));
            return;
        }

        if (TextUtils.isEmpty(registration.getEmail())) {
            supabaseStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            supabaseStatus.setText(getString(R.string.supabase_missing_email));
            return;
        }

        String supabasePassword = supabaseClient.getDbPassword();
        if (TextUtils.isEmpty(supabasePassword)) {
            supabaseStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            supabaseStatus.setText(getString(R.string.supabase_missing_password));
            return;
        }

        supabaseStatus.setTextColor(ContextCompat.getColor(this, R.color.teal_700));
        supabaseStatus.setText(getString(R.string.supabase_creating));

        supabaseClient.signUpOrLogin(
                registration.getEmail(),
                supabasePassword,
                new SupabaseClient.SupabaseCallback() {
                    @Override
                    public void onSuccess(String message) {
                        supabaseStatus.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.teal_700));
                        supabaseStatus.setText(message);
                    }

                    @Override
                    public void onError(String message) {
                        supabaseStatus.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                        supabaseStatus.setText(message);
                    }
                }
        );
    }

    private UserRole resolveRoleFromId(int id) {
        if (id == R.id.role_couple) return UserRole.PAREJA_SW;
        if (id == R.id.role_unicorn) return UserRole.UNICORNIO;
        return UserRole.SINGLER;
    }

    private String safeText(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }
}
