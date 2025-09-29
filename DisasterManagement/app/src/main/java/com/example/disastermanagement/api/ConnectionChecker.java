package com.example.disastermanagement.api;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.disastermanagement.R;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utility class to check backend connectivity
 */
public class ConnectionChecker {
    private static final String TAG = "ConnectionChecker";

    public interface ConnectionListener {
        void onConnected();
        void onConnectionFailed(String error);
    }

    /**
     * Check if the app can connect to the backend API
     *
     * @param context The context to get the ApiService
     * @param listener Callback to handle connection result
     */
    public static void checkConnection(Context context, ConnectionListener listener) {
        // Force a refresh of the API client in case the URL was changed
        ApiClient.resetApiClient();
        ApiService apiService = ApiClient.getApiService(context);

        // Log the base URL we're connecting to
        String baseUrl = context.getString(R.string.api_base_url);
        Log.d(TAG, "Attempting to connect to backend at: " + baseUrl);

        // Using the simple status endpoint for basic connectivity check
        Call<ResponseBody> call = apiService.getApiStatus();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Backend connection successful");
                    listener.onConnected();
                } else {
                    String errorMsg = "Server returned error: " + response.code();
                    Log.e(TAG, errorMsg);
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e(TAG, "Could not read error body");
                        }
                    }
                    listener.onConnectionFailed(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                String errorMsg = "Connection failed: " + t.getMessage();
                Log.e(TAG, errorMsg, t);

                // Add more detailed diagnostics
                String additionalInfo = "";
                if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
                    additionalInfo = "\n\nPlease check if:\n" +
                            "1. The server URL is correct\n" +
                            "2. Your device has internet access\n" +
                            "3. The backend server is running";
                }

                listener.onConnectionFailed(errorMsg + additionalInfo);
            }
        });
    }

    /**
     * Simple method to check connection and show toast message
     */
    public static void checkConnectionWithToast(Context context) {
        checkConnection(context, new ConnectionListener() {
            @Override
            public void onConnected() {
                Toast.makeText(context, context.getString(R.string.connection_success), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectionFailed(String error) {
                Toast.makeText(context, context.getString(R.string.connection_failed, error), Toast.LENGTH_LONG).show();
            }
        });
    }
}