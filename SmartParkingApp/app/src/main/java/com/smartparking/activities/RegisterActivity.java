package com.smartparking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.smartparking.R;
import com.smartparking.api.ApiClient;
import com.smartparking.api.ApiResponses;
import com.smartparking.api.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Register");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(v -> register());
        tvLogin.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);

        ApiService api = ApiClient.getApiService();
        api.register(body).enqueue(new Callback<ApiResponses.AuthResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.AuthResponse> call, Response<ApiResponses.AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponses.AuthResponse authResponse = response.body();
                    ApiClient.saveSession(RegisterActivity.this,
                            authResponse.token,
                            authResponse.user.getName(),
                            authResponse.user.getEmail());

                    Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    String errorMsg = "Registration failed";
                    try {
                        if (response.errorBody() != null) {
                            ApiResponses.AuthResponse err = new Gson().fromJson(
                                    response.errorBody().string(), ApiResponses.AuthResponse.class);
                            if (err.error != null) errorMsg = err.error;
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
