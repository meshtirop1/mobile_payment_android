package com.smartparking.models;

import com.google.gson.annotations.SerializedName;

public class ParkingSpot {

    private int id;

    @SerializedName("spot_number")
    private int spotNumber;

    // "free", "occupied", "reserved"
    private String status;

    @SerializedName("floor")
    private String floor;

    public int getId() { return id; }
    public int getSpotNumber() { return spotNumber; }
    public String getStatus() { return status; }
    public String getFloor() { return floor; }

    public boolean isFree() { return "free".equals(status); }
    public boolean isOccupied() { return "occupied".equals(status); }
    public boolean isReserved() { return "reserved".equals(status); }
}
