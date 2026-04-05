package com.smartparking.activities;

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

public class TopUpActivity extends AppCompatActivity {

    private TextView tvCurrentBalance;
    private EditText etAmount;
    private Button btnTopUp5, btnTopUp10, btnTopUp20, btnTopUp50, btnTopUpCustom;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Wallet Top-Up");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        etAmount = findViewById(R.id.etAmount);
        btnTopUp5 = findViewById(R.id.btnTopUp5);
        btnTopUp10 = findViewById(R.id.btnTopUp10);
        btnTopUp20 = findViewById(R.id.btnTopUp20);
        btnTopUp50 = findViewById(R.id.btnTopUp50);
        btnTopUpCustom = findViewById(R.id.btnTopUpCustom);
        progressBar = findViewById(R.id.progressBar);

        btnTopUp5.setOnClickListener(v -> topUp(5));
        btnTopUp10.setOnClickListener(v -> topUp(10));
        btnTopUp20.setOnClickListener(v -> topUp(20));
        btnTopUp50.setOnClickListener(v -> topUp(50));
        btnTopUpCustom.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(this, "Amount must be positive", Toast.LENGTH_SHORT).show();
                    return;
                }
                topUp(amount);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });

        loadBalance();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadBalance() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.getWallet().enqueue(new Callback<ApiResponses.WalletResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.WalletResponse> call, Response<ApiResponses.WalletResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    tvCurrentBalance.setText(String.format("Current Balance: $%.2f", response.body().balance));
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.WalletResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void topUp(double amount) {
        progressBar.setVisibility(View.VISIBLE);
        setButtonsEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);

        ApiService api = ApiClient.getApiService();
        api.topUp(body).enqueue(new Callback<ApiResponses.WalletResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.WalletResponse> call, Response<ApiResponses.WalletResponse> response) {
                progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    tvCurrentBalance.setText(String.format("Current Balance: $%.2f", response.body().balance));
                    etAmount.setText("");
                    Toast.makeText(TopUpActivity.this,
                            String.format("$%.2f added to wallet!", amount), Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = "Top-up failed";
                    try {
                        if (response.errorBody() != null) {
                            ApiResponses.WalletResponse err = new Gson().fromJson(
                                    response.errorBody().string(), ApiResponses.WalletResponse.class);
                            if (err.error != null) errorMsg = err.error;
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(TopUpActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.WalletResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);
                Toast.makeText(TopUpActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        btnTopUp5.setEnabled(enabled);
        btnTopUp10.setEnabled(enabled);
        btnTopUp20.setEnabled(enabled);
        btnTopUp50.setEnabled(enabled);
        btnTopUpCustom.setEnabled(enabled);
    }
}
