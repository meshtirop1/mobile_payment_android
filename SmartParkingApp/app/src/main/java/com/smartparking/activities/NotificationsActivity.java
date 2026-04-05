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
import com.smartparking.adapters.NotificationAdapter;
import com.smartparking.api.ApiClient;
import com.smartparking.api.ApiResponses;
import com.smartparking.api.ApiService;
import com.smartparking.models.Notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ListView listNotifications;
    private TextView tvEmpty;
    private Button btnMarkAllRead;

    private NotificationAdapter adapter;
    private List<Notification> notifications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notifications");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progressBar);
        listNotifications = findViewById(R.id.listNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);

        adapter = new NotificationAdapter(this, notifications);
        listNotifications.setAdapter(adapter);

        btnMarkAllRead.setOnClickListener(v -> markAllRead());

        loadNotifications();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.getNotifications().enqueue(new Callback<ApiResponses.NotificationListResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.NotificationListResponse> call,
                                   Response<ApiResponses.NotificationListResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().notifications != null) {
                    notifications.clear();
                    notifications.addAll(response.body().notifications);
                    adapter.notifyDataSetChanged();
                }
                updateEmptyState();
            }

            @Override
            public void onFailure(Call<ApiResponses.NotificationListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NotificationsActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void markAllRead() {
        ApiService api = ApiClient.getApiService();
        api.markAllNotificationsRead().enqueue(new Callback<ApiResponses.MessageResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.MessageResponse> call,
                                   Response<ApiResponses.MessageResponse> response) {
                if (response.isSuccessful()) {
                    for (Notification n : notifications) {
                        n.setRead(true);
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(NotificationsActivity.this,
                            "All notifications marked as read", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.MessageResponse> call, Throwable t) {
                Toast.makeText(NotificationsActivity.this,
                        "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState() {
        if (notifications.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listNotifications.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listNotifications.setVisibility(View.VISIBLE);
        }
    }
}
