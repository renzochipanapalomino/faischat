package com.example.f0aischat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.faischat.data.AuthRepository;
import com.example.faischat.data.inMemoryAuthRepository;
import com.example.faischat.data.SupabaseClient;
import com.example.faischat.model.RegistrationResult;
import com.example.faischat.model.ShareKeyResult;
import com.example.faischat.model.UserRegistration;
import com.example.faischat.model.UserRole;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.time.Duration;

public class MainActivity extends AppCompatActivity {

    private SupabaseClient supabaseClient;
    private AuthRepository authRepository;

    // Vistas de la UI
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;
    private TextInputEditText partnerNameInput;
    private TextInputEditText partnerPhoneInput;

    private MaterialButtonToggleGroup roleGroup;
    private View partnerSection;

    private TextView registrationStatus;
    private TextView supabaseStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Ajuste para la UI Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicialización de Supabase
        supabaseClient = new SupabaseClient(
                BuildConfig.SUPABASE_URL,
                BuildConfig.SUPABASE_ANON_KEY
        );

        // Inicialización del repositorio de autenticación
        // CORRECCIÓN: Se usa el nombre de clase correcto 'InMemoryAuthRepository'
        authRepository = new inMemoryAuthRepository() {
            @Override
            public ShareKeyResult generateShareKey(String ownerId, String label, Duration duration) {
                return null;
            }
        };

        // Vinculación de vistas y configuración de listeners
        bindViews();
        setupRoleSelection();
        setupActions();
        updateSupabaseStatus();
    } // CORRECCIÓN: Se eliminó la llave '}' extra que estaba aquí.

    private void updateSupabaseStatus() {
        // CORRECCIÓN: Lógica para verificar y mostrar el estado de Supabase al inicio.
        if (supabaseClient.isConfigured()) {
            supabaseStatus.setText(getString(R.string.supabase_configured_ok));
            supabaseStatus.setTextColor(ContextCompat.getColor(this, R.color.design_default_color_primary));
        } else {
            supabaseStatus.setText(getString(R.string.supabase_not_configured));
            supabaseStatus.setTextColor(ContextCompat.getColor(this, R.color.design_default_color_error));
        }
    }

    private void bindViews() {
        nameInput = findViewById(R.id.input_name);
        emailInput = findViewById(R.id.input_email);
        phoneInput = findViewById(R.id.input_phone);
        passwordInput = findViewById(R.id.input_password);
        partnerNameInput = findViewById(R.id.input_partner_name);
        partnerPhoneInput = findViewById(R.id.input_partner_phone);

        roleGroup = findViewById(R.id.role_group);
        partnerSection = findViewById(R.id.partner_section);

        registrationStatus = findViewById(R.id.registration_status);
        supabaseStatus = findViewById(R.id.supabase_status);
    }

    private void setupRoleSelection() {
        roleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            UserRole role = resolveRoleFromId(checkedId);
            partnerSection.setVisibility(role == UserRole.PAREJA_SW ? View.VISIBLE : View.GONE);
        });
        roleGroup.check(R.id.role_couple); // Establece una selección por defecto
    }

    private void setupActions() {
        Button registerButton = findViewById(R.id.button_register);
        registerButton.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
        UserRole role = resolveRoleFromId(roleGroup.getCheckedButtonId());

        // Se crea el objeto de registro con los datos del formulario
        UserRegistration registration = new UserRegistration(
                text(nameInput),
                text(emailInput),
                text(phoneInput),
                text(passwordInput), // Se pasa la contraseña aquí
                text(partnerNameInput),
                text(partnerPhoneInput),
                role
        );

        // Se registra el usuario en el repositorio en memoria
        RegistrationResult result = authRepository.registerUser(registration);
        registrationStatus.setText(result.getMessage());

        // Si el registro local es exitoso, se intenta sincronizar con Supabase
        if (result.isSuccess()) {
            syncWithSupabase(registration);
        }
    }

    private void syncWithSupabase(UserRegistration registration) {
        if (!supabaseClient.isConfigured()) {
            supabaseStatus.setText(getString(R.string.supabase_not_configured));
            return;
        }

        if (TextUtils.isEmpty(registration.getEmail()) || TextUtils.isEmpty(registration.getPassword())) {
            supabaseStatus.setText("El correo y la contraseña son requeridos para Supabase.");
            return;
        }

        supabaseStatus.setText(getString(R.string.supabase_creating));

        // CORRECCIÓN: Se llama a la interfaz correcta 'SupabaseCallback'
        supabaseClient.signUp(
                registration.getEmail(),
                registration.getPassword(),
                new SupabaseClient.SupabaseCallback() {
                    @Override
                    public void onSuccess(String msg) {
                        supabaseStatus.setText("Supabase: " + msg);
                    }

                    @Override
                    public void onError(String msg) {
                        supabaseStatus.setText("Supabase Error: " + msg);
                    }
                }
        );
    }

    private UserRole resolveRoleFromId(int id) {
        if (id == R.id.role_couple) return UserRole.PAREJA_SW;
        if (id == R.id.role_unicorn) return UserRole.UNICORNIO;
        return UserRole.SINGLER;
    }

    private String text(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}
