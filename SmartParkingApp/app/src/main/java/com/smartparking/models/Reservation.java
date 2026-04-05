package com.smartparking.models;

import com.google.gson.annotations.SerializedName;

public class Reservation {

    private int id;

    @SerializedName("spot_number")
    private int spotNumber;

    @SerializedName("plate_number")
    private String plateNumber;

    @SerializedName("reserved_from")
    private String reservedFrom;

    @SerializedName("reserved_until")
    private String reservedUntil;

    // "active", "expired", "cancelled"
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public int getSpotNumber() { return spotNumber; }
    public String getPlateNumber() { return plateNumber; }
    public String getReservedFrom() { return reservedFrom; }
    public String getReservedUntil() { return reservedUntil; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
