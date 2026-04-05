package com.smartparking.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.smartparking.R;
import com.smartparking.models.Notification;

import java.util.List;

public class NotificationAdapter extends ArrayAdapter<Notification> {

    public NotificationAdapter(Context context, List<Notification> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_notification, parent, false);
        }

        Notification n = getItem(position);
        if (n == null) return convertView;

        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvMessage = convertView.findViewById(R.id.tvMessage);
        TextView tvTime = convertView.findViewById(R.id.tvTime);
        View unreadIndicator = convertView.findViewById(R.id.unreadIndicator);

        tvTitle.setText(n.getTitle() != null ? n.getTitle() : "Notification");
        tvMessage.setText(n.getMessage() != null ? n.getMessage() : "");

        String time = n.getCreatedAt() != null ? n.getCreatedAt() : "";
        if (time.length() > 16) time = time.substring(0, 16).replace("T", " ");
        tvTime.setText(time);

        if (n.isRead()) {
            unreadIndicator.setVisibility(View.INVISIBLE);
            convertView.setBackgroundColor(Color.WHITE);
        } else {
            unreadIndicator.setVisibility(View.VISIBLE);
            convertView.setBackgroundColor(Color.parseColor("#F3F6FF"));
        }

        return convertView;
    }
}
