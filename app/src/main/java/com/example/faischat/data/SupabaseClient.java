package com.example.faischat.data;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SupabaseClient {
    private final String supabaseUrl;
    private final String anonKey;
    private final String dbPassword; // Campo añadido para guardar la contraseña
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Executor executor = Executors.newSingleThreadExecutor();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public void signUp(String email, String dbPassword, SupabaseCallback supabaseCallback) {
    }

    public interface SupabaseCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public SupabaseClient(@NonNull String supabaseUrl, @NonNull String anonKey, String supabaseDbPassword) {
        this.supabaseUrl = supabaseUrl;
        this.anonKey = anonKey;
        this.dbPassword = supabaseDbPassword; // Ahora se asigna correctamente
    }

    // Método corregido para devolver la contraseña
    public String getDbPassword() {
        return dbPassword;
    }

    public boolean isConfigured() {
        return !TextUtils.isEmpty(supabaseUrl) && !TextUtils.isEmpty(anonKey)
                && !supabaseUrl.contains("YOUR_");
    }

    // Cambiado el nombre de signUp a signUpOrLogin para que coincida con tu MainActivity
    public void signUpOrLogin(String email, String password, SupabaseCallback callback) {
        if (!isConfigured()) {
            callback.onError("Supabase no configurado.");
            return;
        }

        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("email", email);
                payload.put("password", password);

                Request request = new Request.Builder()
                        .url(supabaseUrl + "/auth/v1/signup")
                        .addHeader("apikey", anonKey)
                        .addHeader("Authorization", "Bearer " + anonKey)
                        .post(RequestBody.create(payload.toString(), JSON))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        runOnMain(() -> callback.onSuccess("Usuario sincronizado con Supabase"));
                    } else {
                        runOnMain(() -> callback.onError("Error " + response.code() + ": " + body));
                    }
                }
            } catch (IOException | JSONException e) {
                runOnMain(() -> callback.onError("Error de red: " + e.getMessage()));
            }
        });
    }

    private void runOnMain(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
