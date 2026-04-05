package com.smartparking.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // For physical device: use your computer's IP address (same Wi-Fi network)
    // For emulator: use "http://10.0.2.2:3000/"
    private static final String BASE_URL = "http://10.240.216.34:3000/";
    private static Retrofit retrofit = null;
    private static String authToken = null;

    private static final String PREF_NAME = "SmartParkingPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request.Builder builder = chain.request().newBuilder();
                            if (authToken != null) {
                                builder.addHeader("Authorization", "Bearer " + authToken);
                            }
                            builder.addHeader("Content-Type", "application/json");
                            return chain.proceed(builder.build());
                        }
                    })
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    public static void setToken(String token) {
        authToken = token;
        // Force rebuild of retrofit to use new token
        retrofit = null;
    }

    public static String getToken() {
        return authToken;
    }

    public static void saveSession(Context context, String token, String name, String email) {
        authToken = token;
        retrofit = null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    public static boolean loadSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        if (token != null) {
            authToken = token;
            retrofit = null;
            return true;
        }
        return false;
    }

    public static String getSavedUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "User");
    }

    public static String getSavedUserEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public static void clearSession(Context context) {
        authToken = null;
        retrofit = null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
