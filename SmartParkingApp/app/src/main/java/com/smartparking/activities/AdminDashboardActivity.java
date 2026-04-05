package com.smartparking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smartparking.R;
import com.smartparking.adapters.ParkedVehicleAdapter;
import com.smartparking.api.ApiClient;
import com.smartparking.api.ApiResponses;
import com.smartparking.api.ApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvTodayRevenue, tvWeekRevenue, tvMonthRevenue;
    private TextView tvFreeSpots, tvOccupiedSpots, tvReservedSpots;
    private Button btnOpenGate, btnCloseGate;
    private ListView listParkedVehicles;
    private TextView tvNoVehicles;

    private ParkedVehicleAdapter vehicleAdapter;
    private List<ApiResponses.AdminParkedVehicle> parkedVehicles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progressBar);
        tvTodayRevenue = findViewById(R.id.tvTodayRevenue);
        tvWeekRevenue = findViewById(R.id.tvWeekRevenue);
        tvMonthRevenue = findViewById(R.id.tvMonthRevenue);
        tvFreeSpots = findViewById(R.id.tvFreeSpots);
        tvOccupiedSpots = findViewById(R.id.tvOccupiedSpots);
        tvReservedSpots = findViewById(R.id.tvReservedSpots);
        btnOpenGate = findViewById(R.id.btnOpenGate);
        btnCloseGate = findViewById(R.id.btnCloseGate);
        listParkedVehicles = findViewById(R.id.listParkedVehicles);
        tvNoVehicles = findViewById(R.id.tvNoVehicles);

        vehicleAdapter = new ParkedVehicleAdapter(this, parkedVehicles);
        listParkedVehicles.setAdapter(vehicleAdapter);

        btnOpenGate.setOnClickListener(v -> gateOverride("open"));
        btnCloseGate.setOnClickListener(v -> gateOverride("close"));

        loadDashboard();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboard();
    }

    private void loadDashboard() {
        loadRevenue();
        loadSpotsOverview();
        loadParkedVehicles();
    }

    private void loadRevenue() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.getAdminRevenue().enqueue(new Callback<ApiResponses.AdminRevenueResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.AdminRevenueResponse> call,
                                   Response<ApiResponses.AdminRevenueResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponses.AdminRevenueResponse r = response.body();
                    tvTodayRevenue.setText(String.format("$%.2f", r.todayRevenue));
                    tvWeekRevenue.setText(String.format("$%.2f", r.weekRevenue));
                    tvMonthRevenue.setText(String.format("$%.2f", r.monthRevenue));
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.AdminRevenueResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadSpotsOverview() {
        ApiService api = ApiClient.getApiService();
        api.getAdminSpotsOverview().enqueue(new Callback<ApiResponses.AdminSpotsResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.AdminSpotsResponse> call,
                                   Response<ApiResponses.AdminSpotsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponses.AdminSpotsResponse s = response.body();
                    tvFreeSpots.setText(String.valueOf(s.freeSpots));
                    tvOccupiedSpots.setText(String.valueOf(s.occupiedSpots));
                    tvReservedSpots.setText(String.valueOf(s.reservedSpots));
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.AdminSpotsResponse> call, Throwable t) {
                // Silently fail
            }
        });
    }

    private void loadParkedVehicles() {
        ApiService api = ApiClient.getApiService();
        api.getAdminParkedVehicles().enqueue(new Callback<ApiResponses.AdminParkedResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.AdminParkedResponse> call,
                                   Response<ApiResponses.AdminParkedResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().parkedVehicles != null) {
                    parkedVehicles.clear();
                    parkedVehicles.addAll(response.body().parkedVehicles);
                    vehicleAdapter.notifyDataSetChanged();
                    updateVehicleEmptyState();
                } else {
                    updateVehicleEmptyState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.AdminParkedResponse> call, Throwable t) {
                updateVehicleEmptyState();
            }
        });
    }

    private void gateOverride(String action) {
        Map<String, Object> body = new HashMap<>();
        body.put("action", action);

        ApiService api = ApiClient.getApiService();
        api.gateOverride(body).enqueue(new Callback<ApiResponses.MessageResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.MessageResponse> call,
                                   Response<ApiResponses.MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String msg = response.body().message;
                    Toast.makeText(AdminDashboardActivity.this,
                            msg != null ? msg : "Gate " + action + " command sent",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AdminDashboardActivity.this,
                            "Gate override failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.MessageResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateVehicleEmptyState() {
        if (parkedVehicles.isEmpty()) {
            tvNoVehicles.setVisibility(View.VISIBLE);
            listParkedVehicles.setVisibility(View.GONE);
        } else {
            tvNoVehicles.setVisibility(View.GONE);
            listParkedVehicles.setVisibility(View.VISIBLE);
        }
    }
}
