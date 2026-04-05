package com.smartparking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.smartparking.R;
import com.smartparking.api.ApiClient;
import com.smartparking.api.ApiResponses;
import com.smartparking.api.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome, tvBalance;
    private TextView tvNotificationBadge, tvCardNotifBadge;
    private ImageView ivNotificationBell;
    private CardView cardVehicles, cardWallet, cardParking, cardHistory;
    private CardView cardSpots, cardExit, cardReservations, cardNotifications, cardAdmin;
    private ProgressBar progressBar;

    private Handler badgeHandler;
    private Runnable badgeRunnable;
    private static final long BADGE_POLL_INTERVAL = 30_000L; // 30 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Smart Parking");
        }

        tvWelcome = findViewById(R.id.tvWelcome);
        tvBalance = findViewById(R.id.tvBalance);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        tvCardNotifBadge = findViewById(R.id.tvCardNotifBadge);
        ivNotificationBell = findViewById(R.id.ivNotificationBell);
        progressBar = findViewById(R.id.progressBar);

        cardVehicles = findViewById(R.id.cardVehicles);
        cardWallet = findViewById(R.id.cardWallet);
        cardParking = findViewById(R.id.cardParking);
        cardHistory = findViewById(R.id.cardHistory);
        cardSpots = findViewById(R.id.cardSpots);
        cardExit = findViewById(R.id.cardExit);
        cardReservations = findViewById(R.id.cardReservations);
        cardNotifications = findViewById(R.id.cardNotifications);
        cardAdmin = findViewById(R.id.cardAdmin);

        tvWelcome.setText("Welcome, " + ApiClient.getSavedUserName(this) + "!");

        // Show admin card if admin
        if (ApiClient.isAdmin(this)) {
            cardAdmin.setVisibility(View.VISIBLE);
        }

        cardVehicles.setOnClickListener(v ->
                startActivity(new Intent(this, AddVehicleActivity.class)));

        cardWallet.setOnClickListener(v ->
                startActivity(new Intent(this, TopUpActivity.class)));

        cardParking.setOnClickListener(v ->
                startActivity(new Intent(this, ParkingSimulationActivity.class)));

        cardHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        cardSpots.setOnClickListener(v ->
                startActivity(new Intent(this, ParkingSpotsActivity.class)));

        cardExit.setOnClickListener(v ->
                startActivity(new Intent(this, ParkingExitActivity.class)));

        cardReservations.setOnClickListener(v ->
                startActivity(new Intent(this, ReservationsActivity.class)));

        cardNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        ivNotificationBell.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        cardAdmin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminDashboardActivity.class)));

        // Set up badge polling
        badgeHandler = new Handler(Looper.getMainLooper());
        badgeRunnable = new Runnable() {
            @Override
            public void run() {
                loadUnreadCount();
                badgeHandler.postDelayed(this, BADGE_POLL_INTERVAL);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBalance();
        loadUnreadCount();
        badgeHandler.postDelayed(badgeRunnable, BADGE_POLL_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        badgeHandler.removeCallbacks(badgeRunnable);
    }

    private void loadBalance() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.getWallet().enqueue(new Callback<ApiResponses.WalletResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.WalletResponse> call,
                                   Response<ApiResponses.WalletResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    tvBalance.setText(String.format("Balance: $%.2f", response.body().balance));
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.WalletResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvBalance.setText("Balance: --");
            }
        });
    }

    private void loadUnreadCount() {
        ApiService api = ApiClient.getApiService();
        api.getUnreadCount().enqueue(new Callback<ApiResponses.UnreadCountResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.UnreadCountResponse> call,
                                   Response<ApiResponses.UnreadCountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int count = response.body().count;
                    updateBadge(count);
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.UnreadCountResponse> call, Throwable t) {
                // Silently ignore badge errors
            }
        });
    }

    private void updateBadge(int count) {
        if (count > 0) {
            String label = count > 99 ? "99+" : String.valueOf(count);
            tvNotificationBadge.setText(label);
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvCardNotifBadge.setText(label);
            tvCardNotifBadge.setVisibility(View.VISIBLE);
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
            tvCardNotifBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            ApiClient.clearSession(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
