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
import com.example.faischat.data.SupabaseClient;
import com.example.faischat.model.RegistrationResult;
import com.example.faischat.model.UserRegistration;
import com.example.faischat.model.UserRole;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Pantalla principal de login/registro con roles y sincronización a Supabase.
 */
public class MainActivity extends AppCompatActivity {

    private final AuthRepository authRepository = new InMemoryAuthRepository();
    private SupabaseClient supabaseClient;

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
        supabaseClient = new SupabaseClient(
                BuildConfig.SUPABASE_URL,
                BuildConfig.SUPABASE_ANON_KEY,
                BuildConfig.SUPABASE_DB_PASSWORD
        );
        setupRoleSelection();
        setupActions();
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
            UserRole selectedRole = resolveRoleFromId(checkedId);
            updatePartnerVisibility(selectedRole == UserRole.PAREJA_SW);
        });
        // Selección por defecto: pareja
        ((RadioButton) findViewById(R.id.role_couple)).setChecked(true);
        updatePartnerVisibility(true);
    }

    private void setupActions() {
        Button registerButton = findViewById(R.id.button_register);
        Button googleButton = findViewById(R.id.button_google);

        registerButton.setOnClickListener(v -> handleRegistration());
        googleButton.setOnClickListener(v -> handleGoogle());
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

        if (result.isSuccess()) {
            syncWithSupabase(email);
        }
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

    private void handleGoogle() {
        supabaseStatus.setText("Próximamente: vincula tu cuenta Google. Configura el SDK de Google para activar esta opción.");
        supabaseStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
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

    private void syncWithSupabase(String email) {
        if (supabaseStatus == null) {
            return;
        }
        if (TextUtils.isEmpty(email)) {
            supabaseStatus.setText("Para sincronizar con Supabase agrega un correo.");
            supabaseStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            return;
        }
        supabaseStatus.setText("Sincronizando con Supabase...");
        supabaseStatus.setTextColor(ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary));

        supabaseClient.signUpOrLogin(email, BuildConfig.SUPABASE_DB_PASSWORD, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String message) {
                supabaseStatus.setText(message);
                supabaseStatus.setTextColor(ContextCompat.getColor(MainActivity.this, com.google.android.material.R.color.design_default_color_primary));
            }

            @Override
            public void onError(String message) {
                supabaseStatus.setText(message);
                supabaseStatus.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
            }
        });
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
}
