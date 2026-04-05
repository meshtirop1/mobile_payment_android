package com.smartparking.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // --- Auth ---
    @POST("api/register/")
    Call<ApiResponses.AuthResponse> register(@Body Map<String, String> body);

    @POST("api/login/")
    Call<ApiResponses.AuthResponse> login(@Body Map<String, String> body);

    // --- Vehicles ---
    @GET("api/vehicles/")
    Call<ApiResponses.VehicleListResponse> getVehicles();

    @POST("api/vehicles/")
    Call<ApiResponses.VehicleAddResponse> addVehicle(@Body Map<String, String> body);

    @DELETE("api/vehicles/{id}/")
    Call<ApiResponses.MessageResponse> deleteVehicle(@Path("id") int id);

    // --- Wallet ---
    @GET("api/wallet/")
    Call<ApiResponses.WalletResponse> getWallet();

    @POST("api/wallet/topup/")
    Call<ApiResponses.WalletResponse> topUp(@Body Map<String, Object> body);

    // --- Parking ---
    @GET("api/parking/spots/")
    Call<ApiResponses.SpotListResponse> getParkingSpots();

    @POST("api/parking/entry/")
    Call<ApiResponses.ParkingEntryResponse> parkingEntry(@Body Map<String, Object> body);

    @POST("api/parking/exit/")
    Call<ApiResponses.ParkingExitResponse> parkingExit(@Body Map<String, Object> body);

    @GET("api/parking/active-session/")
    Call<ApiResponses.ActiveSessionResponse> getActiveSession();

    @POST("api/parking/simulate-entry/")
    Call<ApiResponses.SimulateEntryResponse> simulateEntry(@Body Map<String, String> body);

    // --- Transactions ---
    @GET("api/parking/transactions/history/")
    Call<ApiResponses.TransactionHistoryResponse> getTransactionHistory();

    // --- Reservations ---
    @GET("api/reservations/")
    Call<ApiResponses.ReservationListResponse> getReservations();

    @POST("api/reservations/")
    Call<ApiResponses.ReservationCreateResponse> createReservation(@Body Map<String, Object> body);

    @DELETE("api/reservations/{id}/")
    Call<ApiResponses.MessageResponse> deleteReservation(@Path("id") int id);

    // --- Notifications ---
    @GET("api/notifications/")
    Call<ApiResponses.NotificationListResponse> getNotifications();

    @GET("api/notifications/unread-count/")
    Call<ApiResponses.UnreadCountResponse> getUnreadCount();

    @POST("api/notifications/read-all/")
    Call<ApiResponses.MessageResponse> markAllNotificationsRead();

    // --- Admin Dashboard ---
    @GET("api/admin-dashboard/parked-vehicles/")
    Call<ApiResponses.AdminParkedResponse> getAdminParkedVehicles();

    @GET("api/admin-dashboard/revenue/")
    Call<ApiResponses.AdminRevenueResponse> getAdminRevenue();

    @GET("api/admin-dashboard/spots-overview/")
    Call<ApiResponses.AdminSpotsResponse> getAdminSpotsOverview();

    @POST("api/admin-dashboard/gate-override/")
    Call<ApiResponses.MessageResponse> gateOverride(@Body Map<String, Object> body);

    // --- Crypto (legacy) ---
    @POST("api/crypto/deposit")
    Call<ApiResponses.CryptoDepositResponse> cryptoDeposit(@Body Map<String, Object> body);

    @GET("api/crypto/balance")
    Call<ApiResponses.CryptoBalanceResponse> getCryptoBalance();

    @GET("api/blockchain/status")
    Call<ApiResponses.BlockchainStatusResponse> getBlockchainStatus();
}
