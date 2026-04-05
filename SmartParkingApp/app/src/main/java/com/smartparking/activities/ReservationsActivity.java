package com.smartparking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smartparking.R;
import com.smartparking.adapters.ReservationAdapter;
import com.smartparking.api.ApiClient;
import com.smartparking.api.ApiResponses;
import com.smartparking.api.ApiService;
import com.smartparking.models.Reservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReservationsActivity extends AppCompatActivity {

    private EditText etSpotNumber, etPlateNumber, etReservedFrom, etReservedUntil;
    private Button btnCreate;
    private ProgressBar progressBar;
    private ListView listReservations;
    private TextView tvEmpty;

    private ReservationAdapter adapter;
    private List<Reservation> reservations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservations);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Reservations");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etSpotNumber = findViewById(R.id.etSpotNumber);
        etPlateNumber = findViewById(R.id.etPlateNumber);
        etReservedFrom = findViewById(R.id.etReservedFrom);
        etReservedUntil = findViewById(R.id.etReservedUntil);
        btnCreate = findViewById(R.id.btnCreate);
        progressBar = findViewById(R.id.progressBar);
        listReservations = findViewById(R.id.listReservations);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new ReservationAdapter(this, reservations,
                (reservation, position) -> deleteReservation(reservation, position));
        listReservations.setAdapter(adapter);

        btnCreate.setOnClickListener(v -> createReservation());

        loadReservations();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadReservations() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.getReservations().enqueue(new Callback<ApiResponses.ReservationListResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.ReservationListResponse> call,
                                   Response<ApiResponses.ReservationListResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().reservations != null) {
                    reservations.clear();
                    reservations.addAll(response.body().reservations);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                } else {
                    updateEmptyState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.ReservationListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ReservationsActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void createReservation() {
        String spotStr = etSpotNumber.getText().toString().trim();
        String plate = etPlateNumber.getText().toString().trim();
        String from = etReservedFrom.getText().toString().trim();
        String until = etReservedUntil.getText().toString().trim();

        if (spotStr.isEmpty() || plate.isEmpty() || from.isEmpty() || until.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int spotNumber;
        try {
            spotNumber = Integer.parseInt(spotStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid spot number", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnCreate.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("spot_number", spotNumber);
        body.put("plate_number", plate);
        body.put("reserved_from", from);
        body.put("reserved_until", until);

        ApiService api = ApiClient.getApiService();
        api.createReservation(body).enqueue(new Callback<ApiResponses.ReservationCreateResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.ReservationCreateResponse> call,
                                   Response<ApiResponses.ReservationCreateResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnCreate.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ReservationsActivity.this,
                            response.body().message != null ? response.body().message : "Reservation created",
                            Toast.LENGTH_SHORT).show();
                    clearForm();
                    loadReservations();
                } else {
                    try {
                        String error = response.errorBody() != null
                                ? response.errorBody().string() : "Creation failed";
                        Toast.makeText(ReservationsActivity.this, error, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(ReservationsActivity.this,
                                "Creation failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.ReservationCreateResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnCreate.setEnabled(true);
                Toast.makeText(ReservationsActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteReservation(Reservation reservation, int position) {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.deleteReservation(reservation.getId()).enqueue(new Callback<ApiResponses.MessageResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.MessageResponse> call,
                                   Response<ApiResponses.MessageResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    reservations.remove(position);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    Toast.makeText(ReservationsActivity.this,
                            "Reservation cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ReservationsActivity.this,
                            "Failed to cancel reservation", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.MessageResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ReservationsActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearForm() {
        etSpotNumber.setText("");
        etPlateNumber.setText("");
        etReservedFrom.setText("");
        etReservedUntil.setText("");
    }

    private void updateEmptyState() {
        if (reservations.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listReservations.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listReservations.setVisibility(View.VISIBLE);
        }
    }
}
