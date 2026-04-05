package com.smartparking.models;

import com.google.gson.annotations.SerializedName;

public class Notification {

    private int id;
    private String title;
    private String message;

    // "info", "warning", "success", "error"
    private String type;

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }
    public void setRead(boolean read) { this.isRead = read; }
}
