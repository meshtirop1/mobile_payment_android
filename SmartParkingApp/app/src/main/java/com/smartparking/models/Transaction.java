package com.smartparking.models;

import com.google.gson.annotations.SerializedName;

public class Transaction {
    private int id;

    @SerializedName("plate_number")
    private String plateNumber;

    private double amount;
    private String type;
    private String status;
    private String message;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public String getPlateNumber() { return plateNumber; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
}
