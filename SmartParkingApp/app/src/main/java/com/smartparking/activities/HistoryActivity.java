package com.smartparking.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.smartparking.R;
import com.smartparking.adapters.TransactionAdapter;
import com.smartparking.api.ApiClient;
import com.smartparking.api.ApiResponses;
import com.smartparking.api.ApiService;
import com.smartparking.models.Transaction;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Transaction History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvTransactions = findViewById(R.id.rvTransactions);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        adapter = new TransactionAdapter(transactionList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadHistory);

        loadHistory();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.getTransactionHistory().enqueue(new Callback<ApiResponses.TransactionHistoryResponse>() {
            @Override
            public void onResponse(Call<ApiResponses.TransactionHistoryResponse> call,
                                   Response<ApiResponses.TransactionHistoryResponse> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    transactionList.clear();
                    transactionList.addAll(response.body().transactions);
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponses.TransactionHistoryResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(HistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
