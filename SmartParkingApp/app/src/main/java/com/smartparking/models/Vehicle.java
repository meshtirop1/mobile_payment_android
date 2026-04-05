package com.smartparking.models;

import com.google.gson.annotations.SerializedName;

public class Vehicle {
    private int id;

    @SerializedName("plate_number")
    private String plateNumber;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public String getPlateNumber() { return plateNumber; }
    public String getCreatedAt() { return createdAt; }
}
