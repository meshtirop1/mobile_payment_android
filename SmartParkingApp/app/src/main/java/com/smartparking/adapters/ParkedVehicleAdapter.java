package com.smartparking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.smartparking.R;
import com.smartparking.api.ApiResponses;

import java.util.List;

public class ParkedVehicleAdapter extends ArrayAdapter<ApiResponses.AdminParkedVehicle> {

    public ParkedVehicleAdapter(Context context, List<ApiResponses.AdminParkedVehicle> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_parked_vehicle, parent, false);
        }

        ApiResponses.AdminParkedVehicle v = getItem(position);
        if (v == null) return convertView;

        TextView tvSpotNum = convertView.findViewById(R.id.tvSpotNum);
        TextView tvPlate = convertView.findViewById(R.id.tvPlate);
        TextView tvUser = convertView.findViewById(R.id.tvUser);
        TextView tvDuration = convertView.findViewById(R.id.tvDuration);
        TextView tvFee = convertView.findViewById(R.id.tvFee);

        tvSpotNum.setText(String.valueOf(v.spotNumber));
        tvPlate.setText(v.plateNumber != null ? v.plateNumber : "--");
        tvUser.setText(v.userName != null ? v.userName : "--");

        long minutes = (long) v.durationMinutes;
        long hours = minutes / 60;
        long mins = minutes % 60;
        String duration = hours > 0
                ? String.format("%dh %02dm", hours, mins)
                : String.format("%dm", mins);
        tvDuration.setText(duration);

        tvFee.setText(String.format("$%.2f", v.estimatedFee));

        return convertView;
    }
}
