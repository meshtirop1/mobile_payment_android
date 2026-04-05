package com.smartparking.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
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
import com.smartparking.models.ParkingSpot;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParkingSpotsActivity extends AppCompatActivity {

    public static final String EXTRA_SPOT_NUMBER = "spot_number";

    private GridLayout gridSpots;
    private ProgressBar progressBar;
    private TextView tvSelectedSpot;
    private Button btnProceed;

    private int selectedSpotNumber = -1;
    private CardView lastSelectedCard = null;

    private static final int COLOR_FREE = Color.parseColor("#4CAF50");
    private static final int COLOR_OCCUPIED = Color.parseColor("#F44336");
    private static final int COLOR_RESERVED = Color.parseColor("#FFC107");
    private static final int COLOR_SELECTED = Color.parseColor("#1565C0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_spots);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Parking Spots");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        gridSpots = findViewById(R.id.gridSpots);
        progressBar = findViewById(R.id.progressBar);
        tvSelectedSpot = findViewById(R.id.tvSelectedSpot);
        btnProceed = findViewById(R.id.btnProceed);

        btnProceed.setOnClickListener(v -> {
            if (selectedSpotNumber > 0) {
                Intent intent = new Intent(this, ParkingSimulationActivity.class);
                intent.putExtra(EXTRA_SPOT_NUMBER, selectedSpotNumber);
                startActivity(intent);
            }
        });

        loadSpots();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSpots();
    }

    private void loadSpots() {
        progressBar.setVisibility(View.VISIBLE);
        gridSpots.removeAllViews();
        selectedSpotNumber = -1;
        lastSelectedCard = null;
        btnProceed.setEnabled(false);
        tvSelectedSpot.setText("Tap a free spot to select it");

        ApiService api = ApiClient.getApiService();
        api.getParkingSpots().enqueue(new Callback<ApiResponses.SpotListResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.SpotListResponse> call,
                                   Response<ApiResponses.SpotListResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().spots != null) {
                    buildGrid(response.body().spots);
                } else {
                    Toast.makeText(ParkingSpotsActivity.this,
                            "Failed to load spots", Toast.LENGTH_SHORT).show();
                    buildFallbackGrid();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.SpotListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ParkingSpotsActivity.this,
                        "Connection error", Toast.LENGTH_SHORT).show();
                buildFallbackGrid();
            }
        });
    }

    private void buildFallbackGrid() {
        // Build 20 spots all marked free as fallback
        gridSpots.removeAllViews();
        for (int i = 1; i <= 20; i++) {
            addSpotCell(i, "free");
        }
    }

    private void buildGrid(List<ParkingSpot> spots) {
        gridSpots.removeAllViews();
        // Always show 20 spots in a 4x5 grid
        for (int i = 1; i <= 20; i++) {
            String status = "free";
            for (ParkingSpot spot : spots) {
                if (spot.getSpotNumber() == i) {
                    status = spot.getStatus();
                    break;
                }
            }
            addSpotCell(i, status);
        }
    }

    private void addSpotCell(int spotNumber, String status) {
        CardView card = new CardView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        int size = dpToPx(72);
        params.width = size;
        params.height = size;
        params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        card.setLayoutParams(params);
        card.setRadius(dpToPx(8));
        card.setCardElevation(dpToPx(2));

        TextView tv = new TextView(this);
        tv.setText(String.valueOf(spotNumber));
        tv.setTextSize(16);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);

        int bgColor;
        switch (status) {
            case "occupied":
                bgColor = COLOR_OCCUPIED;
                break;
            case "reserved":
                bgColor = COLOR_RESERVED;
                break;
            default:
                bgColor = COLOR_FREE;
                break;
        }
        card.setCardBackgroundColor(bgColor);
        card.addView(tv);

        if ("free".equals(status)) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> onSpotSelected(card, spotNumber));
        }

        gridSpots.addView(card);
    }

    private void onSpotSelected(CardView card, int spotNumber) {
        // Deselect previous
        if (lastSelectedCard != null && lastSelectedCard != card) {
            lastSelectedCard.setCardBackgroundColor(COLOR_FREE);
        }
        // Select this one
        card.setCardBackgroundColor(COLOR_SELECTED);
        lastSelectedCard = card;
        selectedSpotNumber = spotNumber;
        tvSelectedSpot.setText("Selected: Spot #" + spotNumber + " — tap Proceed to enter");
        btnProceed.setEnabled(true);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
