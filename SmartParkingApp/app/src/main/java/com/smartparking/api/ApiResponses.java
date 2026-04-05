package com.smartparking.api;

import com.google.gson.annotations.SerializedName;
import com.smartparking.models.Notification;
import com.smartparking.models.ParkingSpot;
import com.smartparking.models.Reservation;
import com.smartparking.models.Transaction;
import com.smartparking.models.User;
import com.smartparking.models.Vehicle;

import java.util.List;

public class ApiResponses {

    // --- Auth ---
    public static class AuthResponse {
        public String message;
        public String token;
        public User user;
        public String error;

        @SerializedName("is_admin")
        public boolean isAdmin;
    }

    // --- Vehicles ---
    public static class VehicleListResponse {
        public List<Vehicle> vehicles;
        public String error;
    }

    public static class VehicleAddResponse {
        public String message;
        public Vehicle vehicle;
        public String error;
    }

    // --- Wallet ---
    public static class WalletResponse {
        public double balance;
        public String message;
        public String error;
    }

    // --- Parking Spots ---
    public static class SpotListResponse {
        public List<ParkingSpot> spots;
        public String error;
    }

    // --- Parking Entry ---
    public static class ParkingEntryResponse {
        public String message;
        public String error;

        @SerializedName("spot_number")
        public int spotNumber;

        @SerializedName("plate_number")
        public String plateNumber;

        @SerializedName("entry_time")
        public String entryTime;

        @SerializedName("session_id")
        public int sessionId;
    }

    // --- Parking Exit ---
    public static class ParkingExitResponse {
        public String message;
        public String error;

        @SerializedName("plate_number")
        public String plateNumber;

        @SerializedName("spot_number")
        public int spotNumber;

        @SerializedName("fee_charged")
        public double feeCharged;

        @SerializedName("duration_minutes")
        public double durationMinutes;

        @SerializedName("remaining_balance")
        public double remainingBalance;

        @SerializedName("exit_time")
        public String exitTime;
    }

    // --- Active Session ---
    public static class ActiveSessionResponse {
        @SerializedName("has_session")
        public boolean hasSession;

        @SerializedName("spot_number")
        public int spotNumber;

        @SerializedName("plate_number")
        public String plateNumber;

        @SerializedName("entry_time")
        public String entryTime;

        @SerializedName("duration_minutes")
        public double durationMinutes;

        @SerializedName("estimated_fee")
        public double estimatedFee;

        @SerializedName("session_id")
        public int sessionId;

        public String error;
    }

    // --- Simulate Entry (legacy simulate endpoint) ---
    public static class SimulateEntryResponse {
        public String message;
        public String error;

        @SerializedName("plate_number")
        public String plateNumber;

        @SerializedName("fee_charged")
        public Object feeCharged;

        @SerializedName("remaining_balance")
        public double remainingBalance;

        @SerializedName("remaining_crypto_balance")
        public String remainingCryptoBalance;

        @SerializedName("current_balance")
        public double currentBalance;

        @SerializedName("tx_hash")
        public String txHash;

        @SerializedName("payment_method")
        public String paymentMethod;

        @SerializedName("spot_number")
        public int spotNumber;

        public double required;
        public String gate;
    }

    // --- Transactions ---
    public static class TransactionHistoryResponse {
        public List<Transaction> transactions;
        public String error;
    }

    // --- Reservations ---
    public static class ReservationListResponse {
        public List<Reservation> reservations;
        public String error;
    }

    public static class ReservationCreateResponse {
        public String message;
        public Reservation reservation;
        public String error;
    }

    // --- Notifications ---
    public static class NotificationListResponse {
        public List<Notification> notifications;
        public String error;
    }

    public static class UnreadCountResponse {
        public int count;
        public String error;
    }

    // --- Admin Dashboard ---
    public static class AdminParkedResponse {
        @SerializedName("parked_vehicles")
        public List<AdminParkedVehicle> parkedVehicles;
        public String error;
    }

    public static class AdminParkedVehicle {
        @SerializedName("spot_number")
        public int spotNumber;

        @SerializedName("plate_number")
        public String plateNumber;

        @SerializedName("user_name")
        public String userName;

        @SerializedName("entry_time")
        public String entryTime;

        @SerializedName("duration_minutes")
        public double durationMinutes;

        @SerializedName("estimated_fee")
        public double estimatedFee;
    }

    public static class AdminRevenueResponse {
        @SerializedName("today_revenue")
        public double todayRevenue;

        @SerializedName("week_revenue")
        public double weekRevenue;

        @SerializedName("month_revenue")
        public double monthRevenue;

        @SerializedName("total_sessions_today")
        public int totalSessionsToday;

        public String error;
    }

    public static class AdminSpotsResponse {
        @SerializedName("total_spots")
        public int totalSpots;

        @SerializedName("occupied_spots")
        public int occupiedSpots;

        @SerializedName("free_spots")
        public int freeSpots;

        @SerializedName("reserved_spots")
        public int reservedSpots;

        public List<ParkingSpot> spots;
        public String error;
    }

    // --- Generic ---
    public static class MessageResponse {
        public String message;
        public String error;
    }

    // --- Crypto (legacy) ---
    public static class CryptoDepositResponse {
        public String message;
        public String error;

        @SerializedName("tx_hash")
        public String txHash;

        @SerializedName("crypto_balance")
        public String cryptoBalance;
    }

    public static class CryptoBalanceResponse {
        public String balance;
        public String address;
        public boolean connected;
        public String error;
    }

    public static class BlockchainStatusResponse {
        public boolean connected;
        public String network;
    }
}
