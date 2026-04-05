package com.smartparking.api;

import com.google.gson.annotations.SerializedName;
import com.smartparking.models.Transaction;
import com.smartparking.models.User;
import com.smartparking.models.Vehicle;

import java.util.List;

public class ApiResponses {

    public static class AuthResponse {
        public String message;
        public String token;
        public User user;
        public String error;
    }

    public static class VehicleListResponse {
        public List<Vehicle> vehicles;
        public String error;
    }

    public static class VehicleAddResponse {
        public String message;
        public Vehicle vehicle;
        public String error;
    }

    public static class WalletResponse {
        public double balance;
        public String message;
        public String error;
    }

    public static class SimulateEntryResponse {
        public String message;
        public String error;

        @SerializedName("plate_number")
        public String plateNumber;

        @SerializedName("fee_charged")
        public Object feeCharged; // Can be double or String (for crypto)

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

        public double required;
        public String gate;
    }

    public static class TransactionHistoryResponse {
        public List<Transaction> transactions;
        public String error;
    }

    public static class MessageResponse {
        public String message;
        public String error;
    }

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
