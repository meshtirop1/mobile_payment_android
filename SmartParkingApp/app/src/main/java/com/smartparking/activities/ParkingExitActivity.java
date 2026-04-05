package com.smartparking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.smartparking.R;
import com.smartparking.api.ApiClient;
import com.smartparking.api.ApiResponses;
import com.smartparking.api.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParkingExitActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private CardView cardNoSession, cardActiveSession, cardResult;
    private TextView tvSpotNumber, tvPlateNumber, tvEntryTime, tvDuration, tvEstimatedFee;
    private TextView tvResultMessage;
    private Button btnExit;

    private int activeSessionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_exit);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Exit Parking");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progressBar);
        cardNoSession = findViewById(R.id.cardNoSession);
        cardActiveSession = findViewById(R.id.cardActiveSession);
        cardResult = findViewById(R.id.cardResult);
        tvSpotNumber = findViewById(R.id.tvSpotNumber);
        tvPlateNumber = findViewById(R.id.tvPlateNumber);
        tvEntryTime = findViewById(R.id.tvEntryTime);
        tvDuration = findViewById(R.id.tvDuration);
        tvEstimatedFee = findViewById(R.id.tvEstimatedFee);
        tvResultMessage = findViewById(R.id.tvResultMessage);
        btnExit = findViewById(R.id.btnExit);

        btnExit.setOnClickListener(v -> performExit());

        loadActiveSession();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActiveSession();
    }

    private void loadActiveSession() {
        progressBar.setVisibility(View.VISIBLE);
        cardNoSession.setVisibility(View.GONE);
        cardActiveSession.setVisibility(View.GONE);
        btnExit.setVisibility(View.GONE);
        cardResult.setVisibility(View.GONE);

        ApiService api = ApiClient.getApiService();
        api.getActiveSession().enqueue(new Callback<ApiResponses.ActiveSessionResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.ActiveSessionResponse> call,
                                   Response<ApiResponses.ActiveSessionResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponses.ActiveSessionResponse session = response.body();
                    if (session.hasSession) {
                        activeSessionId = session.sessionId;
                        showActiveSession(session);
                    } else {
                        cardNoSession.setVisibility(View.VISIBLE);
                    }
                } else {
                    cardNoSession.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.ActiveSessionResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                cardNoSession.setVisibility(View.VISIBLE);
                Toast.makeText(ParkingExitActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showActiveSession(ApiResponses.ActiveSessionResponse session) {
        tvSpotNumber.setText(String.valueOf(session.spotNumber));
        tvPlateNumber.setText(session.plateNumber != null ? session.plateNumber : "--");

        String entryTime = session.entryTime != null ? session.entryTime : "--";
        // Trim ISO timestamp for readability
        if (entryTime.length() > 16) {
            entryTime = entryTime.substring(0, 16).replace("T", " ");
        }
        tvEntryTime.setText(entryTime);

        long minutes = (long) session.durationMinutes;
        long hours = minutes / 60;
        long mins = minutes % 60;
        String duration = hours > 0
                ? String.format("%dh %02dm", hours, mins)
                : String.format("%dm", mins);
        tvDuration.setText(duration);

        tvEstimatedFee.setText(String.format("$%.2f", session.estimatedFee));

        cardActiveSession.setVisibility(View.VISIBLE);
        btnExit.setVisibility(View.VISIBLE);
    }

    private void performExit() {
        progressBar.setVisibility(View.VISIBLE);
        btnExit.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        if (activeSessionId > 0) {
            body.put("session_id", activeSessionId);
        }

        ApiService api = ApiClient.getApiService();
        api.parkingExit(body).enqueue(new Callback<ApiResponses.ParkingExitResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.ParkingExitResponse> call,
                                   Response<ApiResponses.ParkingExitResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnExit.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponses.ParkingExitResponse result = response.body();
                    showExitResult(result);
                } else {
                    Toast.makeText(ParkingExitActivity.this,
                            "Exit failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.ParkingExitResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnExit.setEnabled(true);
                Toast.makeText(ParkingExitActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showExitResult(ApiResponses.ParkingExitResponse result) {
        cardActiveSession.setVisibility(View.GONE);
        btnExit.setVisibility(View.GONE);

        long minutes = (long) result.durationMinutes;
        long hours = minutes / 60;
        long mins = minutes % 60;
        String duration = hours > 0
                ? String.format("%dh %02dm", hours, mins)
                : String.format("%dm", mins);

        StringBuilder sb = new StringBuilder();
        sb.append(result.message != null ? result.message : "Exit successful");
        sb.append("\n\nPlate: ").append(result.plateNumber != null ? result.plateNumber : "--");
        sb.append("\nSpot: #").append(result.spotNumber);
        sb.append("\nDuration: ").append(duration);
        sb.append("\nFee Charged: $").append(String.format("%.2f", result.feeCharged));
        sb.append("\nRemaining Balance: $").append(String.format("%.2f", result.remainingBalance));

        tvResultMessage.setText(sb.toString());
        cardResult.setVisibility(View.VISIBLE);
        cardResult.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.success_bg));
    }
}
