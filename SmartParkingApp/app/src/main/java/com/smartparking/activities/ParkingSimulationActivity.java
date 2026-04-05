package com.smartparking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

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

public class ParkingSimulationActivity extends AppCompatActivity {

    private EditText etPlateNumber;
    private Button btnSimulateEntry;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbWallet, rbCrypto;
    private CardView cardResult;
    private ImageView ivGateStatus;
    private TextView tvGateStatus, tvResultMessage, tvResultDetails;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_simulation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Parking Simulation");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etPlateNumber = findViewById(R.id.etPlateNumber);
        btnSimulateEntry = findViewById(R.id.btnSimulateEntry);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        rbWallet = findViewById(R.id.rbWallet);
        rbCrypto = findViewById(R.id.rbCrypto);
        cardResult = findViewById(R.id.cardResult);
        ivGateStatus = findViewById(R.id.ivGateStatus);
        tvGateStatus = findViewById(R.id.tvGateStatus);
        tvResultMessage = findViewById(R.id.tvResultMessage);
        tvResultDetails = findViewById(R.id.tvResultDetails);
        progressBar = findViewById(R.id.progressBar);

        cardResult.setVisibility(View.GONE);
        rbWallet.setChecked(true);

        btnSimulateEntry.setOnClickListener(v -> simulateEntry());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void simulateEntry() {
        String plate = etPlateNumber.getText().toString().trim();
        if (plate.isEmpty()) {
            Toast.makeText(this, "Please enter a plate number", Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentMethod = rbCrypto.isChecked() ? "crypto" : "wallet";

        progressBar.setVisibility(View.VISIBLE);
        btnSimulateEntry.setEnabled(false);
        cardResult.setVisibility(View.GONE);

        Map<String, String> body = new HashMap<>();
        body.put("plate_number", plate);
        body.put("payment_method", paymentMethod);

        ApiService api = ApiClient.getApiService();
        api.simulateEntry(body).enqueue(new Callback<ApiResponses.SimulateEntryResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.SimulateEntryResponse> call,
                                   Response<ApiResponses.SimulateEntryResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnSimulateEntry.setEnabled(true);
                cardResult.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null) {
                    showSuccess(response.body());
                } else {
                    try {
                        String errorJson = response.errorBody().string();
                        ApiResponses.SimulateEntryResponse result = new Gson().fromJson(
                                errorJson, ApiResponses.SimulateEntryResponse.class);
                        showFailure(result);
                    } catch (Exception e) {
                        showGenericError("An error occurred");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.SimulateEntryResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSimulateEntry.setEnabled(true);
                cardResult.setVisibility(View.VISIBLE);
                showGenericError("Connection error: " + t.getMessage());
            }
        });
    }

    private void showSuccess(ApiResponses.SimulateEntryResponse result) {
        ivGateStatus.setImageResource(R.drawable.ic_gate_open);
        ivGateStatus.setColorFilter(ContextCompat.getColor(this, R.color.success_green));
        tvGateStatus.setText("GATE OPEN");
        tvGateStatus.setTextColor(ContextCompat.getColor(this, R.color.success_green));
        tvResultMessage.setText(result.message);

        StringBuilder details = new StringBuilder();
        details.append("Plate: ").append(result.plateNumber);
        details.append("\nFee: ").append(result.feeCharged);
        details.append("\nPayment: ").append(
                "crypto".equals(result.paymentMethod) ? "Cryptocurrency" : "Wallet");

        if ("crypto".equals(result.paymentMethod)) {
            if (result.remainingCryptoBalance != null) {
                details.append("\nCrypto Balance: ").append(result.remainingCryptoBalance);
            }
            if (result.txHash != null) {
                details.append("\nTX: ").append(result.txHash.substring(0, Math.min(20, result.txHash.length()))).append("...");
            }
        } else {
            details.append(String.format("\nRemaining: $%.2f", result.remainingBalance));
        }

        tvResultDetails.setText(details.toString());
        cardResult.setCardBackgroundColor(ContextCompat.getColor(this, R.color.success_bg));
    }

    private void showFailure(ApiResponses.SimulateEntryResponse result) {
        ivGateStatus.setImageResource(R.drawable.ic_gate_closed);
        ivGateStatus.setColorFilter(ContextCompat.getColor(this, R.color.error_red));
        tvGateStatus.setText("GATE CLOSED");
        tvGateStatus.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        tvResultMessage.setText(result != null && result.error != null ? result.error : "Entry denied");

        StringBuilder details = new StringBuilder();
        if (result != null) {
            if (result.plateNumber != null) details.append("Plate: ").append(result.plateNumber);
            if (result.required > 0) {
                details.append("\nRequired: $").append(String.format("%.2f", result.required));
                details.append("\nYour Balance: $").append(String.format("%.2f", result.currentBalance));
            }
            if (result.paymentMethod != null) {
                details.append("\nPayment: ").append(
                        "crypto".equals(result.paymentMethod) ? "Cryptocurrency" : "Wallet");
            }
        }
        tvResultDetails.setText(details.toString());
        cardResult.setCardBackgroundColor(ContextCompat.getColor(this, R.color.error_bg));
    }

    private void showGenericError(String message) {
        ivGateStatus.setImageResource(R.drawable.ic_gate_closed);
        ivGateStatus.setColorFilter(ContextCompat.getColor(this, R.color.error_red));
        tvGateStatus.setText("ERROR");
        tvGateStatus.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        tvResultMessage.setText(message);
        tvResultDetails.setText("");
        cardResult.setCardBackgroundColor(ContextCompat.getColor(this, R.color.error_bg));
    }
}
