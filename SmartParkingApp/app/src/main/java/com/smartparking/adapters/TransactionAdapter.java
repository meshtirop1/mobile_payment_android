package com.smartparking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smartparking.R;
import com.smartparking.models.Transaction;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final List<Transaction> transactions;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction txn = transactions.get(position);

        holder.tvPlateNumber.setText(txn.getPlateNumber());
        holder.tvAmount.setText(String.format("-$%.2f", txn.getAmount()));
        holder.tvMessage.setText(txn.getMessage());
        holder.tvDate.setText(txn.getCreatedAt());

        if ("success".equals(txn.getStatus())) {
            holder.tvStatus.setText("SUCCESS");
            holder.tvStatus.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.success_green));
        } else {
            holder.tvStatus.setText("FAILED");
            holder.tvStatus.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.error_red));
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlateNumber, tvAmount, tvStatus, tvMessage, tvDate;

        ViewHolder(View itemView) {
            super(itemView);
            tvPlateNumber = itemView.findViewById(R.id.tvPlateNumber);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
