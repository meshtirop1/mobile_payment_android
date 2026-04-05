package com.smartparking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartparking.R;
import com.smartparking.models.Vehicle;

import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.ViewHolder> {

    public interface OnVehicleClickListener {
        void onDeleteClick(Vehicle vehicle);
    }

    private final List<Vehicle> vehicles;
    private final OnVehicleClickListener listener;

    public VehicleAdapter(List<Vehicle> vehicles, OnVehicleClickListener listener) {
        this.vehicles = vehicles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vehicle vehicle = vehicles.get(position);
        holder.tvPlateNumber.setText(vehicle.getPlateNumber());
        holder.tvAddedDate.setText("Added: " + vehicle.getCreatedAt());
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(vehicle));
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlateNumber, tvAddedDate;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvPlateNumber = itemView.findViewById(R.id.tvPlateNumber);
            tvAddedDate = itemView.findViewById(R.id.tvAddedDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
