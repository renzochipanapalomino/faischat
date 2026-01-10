package com.example.faischat.data;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final String supabaseUrl;
    private final String anonKey;
    private final String dbPassword;

    public SupabaseClient(@NonNull String supabaseUrl, @NonNull String anonKey) {
        this.supabaseUrl = supabaseUrl;
        this.anonKey = anonKey;
        this.dbPassword = "";
    }

    public SupabaseClient(
            @NonNull String supabaseUrl,
            @NonNull String anonKey,
            @NonNull String dbPassword
    ) {
        this.supabaseUrl = supabaseUrl;
        this.anonKey = anonKey;
        this.dbPassword = dbPassword;
    }

    public boolean isConfigured() {
        return !TextUtils.isEmpty(supabaseUrl)
                && !supabaseUrl.contains("tu-proyecto")
                && !TextUtils.isEmpty(anonKey)
                && !anonKey.contains("TU_ANON_KEY");
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void signUpOrLogin(String email, String password, SupabaseCallback callback) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            callback.onError("Supabase necesita correo y contraseña.");
            return;
        }

        if (!isConfigured()) {
            callback.onError("Configura SUPABASE_URL y SUPABASE_ANON_KEY en BuildConfig antes de usar el login.");
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

                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful()) {
                    runOnMain(() -> callback.onSuccess("Cuenta creada en Supabase."));
                } else {
                    String body = response.body() != null ? response.body().string() : "";
                    String message = "Supabase respondió " + response.code() + ": " + body;
                    runOnMain(() -> callback.onError(message));
                }
            } catch (IOException | JSONException e) {
                runOnMain(() -> callback.onError("Error de red: " + e.getMessage()));
            }
        });
    }

    private void runOnMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    public interface SupabaseCallback {
        void onSuccess(String message);

        void onError(String message);
    }
}
