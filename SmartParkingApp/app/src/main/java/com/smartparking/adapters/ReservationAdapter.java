package com.smartparking.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartparking.R;
import com.smartparking.models.Reservation;

import java.util.List;

public class ReservationAdapter extends ArrayAdapter<Reservation> {

    public interface OnDeleteListener {
        void onDelete(Reservation reservation, int position);
    }

    private final OnDeleteListener deleteListener;

    public ReservationAdapter(Context context, List<Reservation> items,
                               OnDeleteListener deleteListener) {
        super(context, 0, items);
        this.deleteListener = deleteListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_reservation, parent, false);
        }

        Reservation r = getItem(position);
        if (r == null) return convertView;

        TextView tvSpot = convertView.findViewById(R.id.tvSpot);
        TextView tvPlate = convertView.findViewById(R.id.tvPlate);
        TextView tvTime = convertView.findViewById(R.id.tvTime);
        TextView tvStatus = convertView.findViewById(R.id.tvStatus);
        ImageView btnDelete = convertView.findViewById(R.id.btnDelete);

        tvSpot.setText("Spot #" + r.getSpotNumber());
        tvPlate.setText(r.getPlateNumber() != null ? r.getPlateNumber() : "");

        String from = r.getReservedFrom() != null ? r.getReservedFrom() : "";
        String until = r.getReservedUntil() != null ? r.getReservedUntil() : "";
        if (from.length() > 16) from = from.substring(0, 16).replace("T", " ");
        if (until.length() > 16) until = until.substring(0, 16).replace("T", " ");
        tvTime.setText(from + " → " + until);

        String status = r.getStatus() != null ? r.getStatus() : "active";
        tvStatus.setText(status);
        switch (status) {
            case "active":
                tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                break;
            case "expired":
                tvStatus.setTextColor(Color.parseColor("#757575"));
                break;
            case "cancelled":
                tvStatus.setTextColor(Color.parseColor("#C62828"));
                break;
            default:
                tvStatus.setTextColor(Color.parseColor("#1565C0"));
                break;
        }

        btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(r, position);
            }
        });

        return convertView;
    }
}
