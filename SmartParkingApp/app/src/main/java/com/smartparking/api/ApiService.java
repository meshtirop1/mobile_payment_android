package com.smartparking.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("api/register")
    Call<ApiResponses.AuthResponse> register(@Body Map<String, String> body);

    @POST("api/login")
    Call<ApiResponses.AuthResponse> login(@Body Map<String, String> body);

    @GET("api/vehicles")
    Call<ApiResponses.VehicleListResponse> getVehicles();

    @POST("api/vehicles")
    Call<ApiResponses.VehicleAddResponse> addVehicle(@Body Map<String, String> body);

    @DELETE("api/vehicles/{id}")
    Call<ApiResponses.MessageResponse> deleteVehicle(@Path("id") int id);

    @GET("api/wallet")
    Call<ApiResponses.WalletResponse> getWallet();

    @POST("api/wallet/topup")
    Call<ApiResponses.WalletResponse> topUp(@Body Map<String, Object> body);

    @POST("api/simulate-entry")
    Call<ApiResponses.SimulateEntryResponse> simulateEntry(@Body Map<String, String> body);

    @GET("api/transactions/history")
    Call<ApiResponses.TransactionHistoryResponse> getTransactionHistory();

    @POST("api/crypto/deposit")
    Call<ApiResponses.CryptoDepositResponse> cryptoDeposit(@Body Map<String, Object> body);

    @GET("api/crypto/balance")
    Call<ApiResponses.CryptoBalanceResponse> getCryptoBalance();

    @GET("api/blockchain/status")
    Call<ApiResponses.BlockchainStatusResponse> getBlockchainStatus();
}
