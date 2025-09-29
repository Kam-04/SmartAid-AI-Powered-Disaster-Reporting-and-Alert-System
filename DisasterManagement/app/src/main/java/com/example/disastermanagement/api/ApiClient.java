package com.example.disastermanagement.api;

import android.content.Context;
import android.util.Log;

import com.example.disastermanagement.R;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";

    private static Retrofit retrofit = null;
    private static final int TIMEOUT = 60; // seconds

    private static String BASE_URL = "http://192.168.0.102:5000/"; // Update this to your actual server IP

    private static OkHttpClient client;

    public static OkHttpClient getClient() {
        if (client == null) {
            try {
                // Add logging interceptor for debugging
                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                client = new OkHttpClient.Builder()
                        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .addInterceptor(loggingInterceptor)
                        .build();

                Log.d(TAG, "Created new OkHttpClient instance");
            } catch (Exception e) {
                Log.e(TAG, "Error creating OkHttpClient", e);
                throw new IllegalStateException("Failed to initialize HTTP client", e);
            }
        }
        return client;
    }



    // API interfaces for different modules
    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    public static Request buildRequest(String endpoint, String method, RequestBody body) {
        if (BASE_URL == null || BASE_URL.isEmpty()) {
            throw new IllegalStateException("Base URL is not initialized");
        }

        String url = BASE_URL + endpoint;

        Log.d(TAG, "Building request to: " + url + " with method: " + method);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url);

        switch (method.toUpperCase()) {
            case "GET":
                requestBuilder.get();
                break;
            case "POST":
                requestBuilder.post(body);
                break;
            case "PUT":
                requestBuilder.put(body);
                break;
            case "DELETE":
                requestBuilder.delete();
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        return requestBuilder.build();
    }



    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://newsapi.org/")  // Base URL for News API
                    .addConverterFactory(GsonConverterFactory.create())  // Gson converter to handle JSON
                    .build();
        }
        return retrofit;
    }

    /**
     * Force a reset of the Retrofit instance - useful when changing the base URL
     */
    public static void resetApiClient() {
        retrofit = null;
    }
} 