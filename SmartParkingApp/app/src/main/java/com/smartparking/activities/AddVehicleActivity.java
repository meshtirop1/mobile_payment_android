package com.smartparking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.smartparking.R;
import com.smartparking.adapters.VehicleAdapter;
import com.smartparking.api.ApiClient;
import com.smartparking.api.ApiResponses;
import com.smartparking.api.ApiService;
import com.smartparking.models.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddVehicleActivity extends AppCompatActivity implements VehicleAdapter.OnVehicleClickListener {

    private EditText etPlateNumber;
    private Button btnAddVehicle;
    private RecyclerView rvVehicles;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private VehicleAdapter adapter;
    private List<Vehicle> vehicleList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vehicle);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Vehicles");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etPlateNumber = findViewById(R.id.etPlateNumber);
        btnAddVehicle = findViewById(R.id.btnAddVehicle);
        rvVehicles = findViewById(R.id.rvVehicles);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);

        adapter = new VehicleAdapter(vehicleList, this);
        rvVehicles.setLayoutManager(new LinearLayoutManager(this));
        rvVehicles.setAdapter(adapter);

        btnAddVehicle.setOnClickListener(v -> addVehicle());

        loadVehicles();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadVehicles() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.getVehicles().enqueue(new Callback<ApiResponses.VehicleListResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.VehicleListResponse> call, Response<ApiResponses.VehicleListResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    vehicleList.clear();
                    vehicleList.addAll(response.body().vehicles);
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(vehicleList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.VehicleListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddVehicleActivity.this, "Failed to load vehicles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addVehicle() {
        String plate = etPlateNumber.getText().toString().trim();
        if (plate.isEmpty()) {
            Toast.makeText(this, "Please enter a plate number", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnAddVehicle.setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("plate_number", plate);

        ApiService api = ApiClient.getApiService();
        api.addVehicle(body).enqueue(new Callback<ApiResponses.VehicleAddResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.VehicleAddResponse> call, Response<ApiResponses.VehicleAddResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnAddVehicle.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(AddVehicleActivity.this, "Vehicle added!", Toast.LENGTH_SHORT).show();
                    etPlateNumber.setText("");
                    loadVehicles();
                } else {
                    String errorMsg = "Failed to add vehicle";
                    try {
                        if (response.errorBody() != null) {
                            ApiResponses.VehicleAddResponse err = new Gson().fromJson(
                                    response.errorBody().string(), ApiResponses.VehicleAddResponse.class);
                            if (err.error != null) errorMsg = err.error;
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(AddVehicleActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.VehicleAddResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnAddVehicle.setEnabled(true);
                Toast.makeText(AddVehicleActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(Vehicle vehicle) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Vehicle")
                .setMessage("Remove " + vehicle.getPlateNumber() + "?")
                .setPositiveButton("Remove", (dialog, which) -> deleteVehicle(vehicle))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteVehicle(Vehicle vehicle) {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.deleteVehicle(vehicle.getId()).enqueue(new Callback<ApiResponses.MessageResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.MessageResponse> call, Response<ApiResponses.MessageResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(AddVehicleActivity.this, "Vehicle removed", Toast.LENGTH_SHORT).show();
                    loadVehicles();
                } else {
                    Toast.makeText(AddVehicleActivity.this, "Failed to remove vehicle", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.MessageResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddVehicleActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
