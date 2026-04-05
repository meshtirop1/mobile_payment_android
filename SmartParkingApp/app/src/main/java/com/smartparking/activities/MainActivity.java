package com.smartparking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
    private CardView cardVehicles, cardWallet, cardParking, cardHistory;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Smart Parking");
        }

        tvWelcome = findViewById(R.id.tvWelcome);
        tvBalance = findViewById(R.id.tvBalance);
        cardVehicles = findViewById(R.id.cardVehicles);
        cardWallet = findViewById(R.id.cardWallet);
        cardParking = findViewById(R.id.cardParking);
        cardHistory = findViewById(R.id.cardHistory);
        progressBar = findViewById(R.id.progressBar);

        tvWelcome.setText("Welcome, " + ApiClient.getSavedUserName(this) + "!");

        cardVehicles.setOnClickListener(v ->
                startActivity(new Intent(this, AddVehicleActivity.class)));

        cardWallet.setOnClickListener(v ->
                startActivity(new Intent(this, TopUpActivity.class)));

        cardParking.setOnClickListener(v ->
                startActivity(new Intent(this, ParkingSimulationActivity.class)));

        cardHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBalance();
    }

    private void loadBalance() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.getWallet().enqueue(new Callback<ApiResponses.WalletResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.WalletResponse> call, Response<ApiResponses.WalletResponse> response) {
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
