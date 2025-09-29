package com.example.disastermanagement.modules;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.disastermanagement.R;
import com.example.disastermanagement.api.ApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.graphics.Color;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.SmsManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import android.content.Intent;

public class SOSLocationActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "SOSLocationActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 60000; // 1 minute

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private TextView locationText;
    private TextView coordinatesText;
    private TextView timeElapsed;
    private Button btnRefreshLocation;
    private Button btnStopSharing;

    private Location currentLocation;
    private String locationId;
    private long startTimeMillis;
    private boolean isSharing = false;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private boolean isFinishing = false;
    private AlertDialog activeDialog = null;

    private static final int DIALOG_STATE_NONE = 0;
    private static final int DIALOG_STATE_SHOWING = 1;
    private int currentDialogState = DIALOG_STATE_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Simple try-catch without complex dialog handling in onCreate
        try {
            Log.d(TAG, "SOSLocationActivity onCreate started");

            setContentView(R.layout.activity_sos_location);

            // Set up toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            // Initialize UI components
            initializeUI();

            // Initialize location services
            initializeLocationServices();

            // Request permissions and check if location is enabled
            checkAndRequestPermissions();

            // Initialize timer
            startTimeMillis = System.currentTimeMillis();
            initializeTimer();

            // Set default location immediately (this doesn't show dialogs)
            setDefaultLocation();

            // Set up the map (delayed to ensure UI is ready)
            setupMapWithDelay();

            // Handle intent extras and start location sharing if needed
            handleIntentExtrasAndStartSharing();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            // Don't show toast here to avoid "activity has action" error
        }
    }

    private void checkAndRequestPermissions() {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request permissions without showing dialog
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void setupMapWithDelay() {
        new Handler().postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            try {
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapFragment);
                if (mapFragment != null) {
                    Log.d(TAG, "Getting map async");
                    mapFragment.getMapAsync(this);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up map", e);
            }
        }, 800);
    }

    private void handleIntentExtrasAndStartSharing() {
        // Log intent extras
        logIntentExtras();

        // Check if we're already sharing (coming from EmergencyResponse)
        if (getIntent().hasExtra("ALREADY_SHARING")) {
            isSharing = getIntent().getBooleanExtra("ALREADY_SHARING", false);

            if (getIntent().hasExtra("LOCATION_ID")) {
                locationId = getIntent().getStringExtra("LOCATION_ID");
            }

            if (getIntent().hasExtra("LATITUDE") && getIntent().hasExtra("LONGITUDE")) {
                double lat = getIntent().getDoubleExtra("LATITUDE", 0);
                double lng = getIntent().getDoubleExtra("LONGITUDE", 0);

                Location location = new Location("intent");
                location.setLatitude(lat);
                location.setLongitude(lng);
                currentLocation = location;

                // Update UI but don't show any dialogs
                updateLocationUI();
            }
        }
        // If not sharing, wait and then update location
        else {
            new Handler().postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;

                try {
                    if (checkLocationPermission()) {
                        updateLocation();
                    }
                    updateLocationUI();
                } catch (Exception e) {
                    Log.e(TAG, "Error in delayed location update", e);
                }
            }, 2000);
        }
    }

    /**
     * Log all intent extras for debugging purposes
     */
    private void logIntentExtras() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.d(TAG, "Intent extras:");
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d(TAG, key + ": " + (value != null ? value.toString() : "null"));
            }
        } else {
            Log.d(TAG, "No intent extras");
        }
    }

    private void initializeUI() {
        try {
            locationText = findViewById(R.id.locationText);
            coordinatesText = findViewById(R.id.coordinatesText);
            timeElapsed = findViewById(R.id.timeElapsed);
            btnRefreshLocation = findViewById(R.id.btnRefreshLocation);
            btnStopSharing = findViewById(R.id.btnStopSharing);

            btnRefreshLocation.setOnClickListener(v -> {
                Toast.makeText(this, "Refreshing location...", Toast.LENGTH_SHORT).show();

                // First check permissions
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                    return;
                }

                // Check if location services are enabled
                if (!isLocationEnabled()) {
                    Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show();
                    showLocationServicesDialog();
                    return;
                }

                // Use our enhanced location tracking
                enableMyLocation();
            });

            btnStopSharing.setOnClickListener(v -> {
                stopLocationSharing();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI", e);
            Toast.makeText(this, "Error initializing UI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(LOCATION_UPDATE_INTERVAL / 2);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
                    updateLocationUI();

                    // If already sharing, update the location on server
                    if (isSharing && locationId != null) {
                        updateLocationOnServer();
                    }
                }
            }
        };
    }

    private void initializeTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTimeMillis;
                int hours = (int) (millis / (1000 * 60 * 60));
                int minutes = (int) (millis / (1000 * 60)) % 60;
                int seconds = (int) (millis / 1000) % 60;

                timeElapsed.setText(String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            Log.d(TAG, "Map is ready");
            mMap = googleMap;

            // Basic map settings - keep these simple
            mMap.getUiSettings().setZoomControlsEnabled(true);

            // IMPORTANT: Create and show a default location marker IMMEDIATELY
            // to ensure something appears on the map
            LatLng defaultLocation = new LatLng(19.0760, 72.8777); // Mumbai
            mMap.addMarker(new MarkerOptions()
                    .position(defaultLocation)
                    .title("Default Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));

            Log.d(TAG, "Map initialized with default marker");

            // Request permissions if needed
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            // Use our enhanced location tracking specifically designed for SOS situations
            enableMyLocation();

        } catch (Exception e) {
            Log.e(TAG, "Error in onMapReady", e);
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // Use our enhanced location tracking
                        enableMyLocation();
                    }
                }
            } else {
                Toast.makeText(this, "Location permission is required for SOS services", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted, requesting location...");
            Toast.makeText(this, "Fetching location...", Toast.LENGTH_SHORT).show();

            // Check if location services are enabled
            boolean isLocationEnabled = isLocationEnabled();
            if (!isLocationEnabled) {
                Log.e(TAG, "Location services are disabled");
                Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show();
                // Show dialog to open location settings
                showLocationServicesDialog();
                return;
            }

            // Request last known location with high priority
            LocationRequest highPriorityRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(0)
                    .setNumUpdates(1);

            Log.d(TAG, "Requesting current location with high priority...");

            try {
                fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                Log.d(TAG, "getCurrentLocation success: " + location.getLatitude() + ", " + location.getLongitude());
                                currentLocation = location;
                                updateLocationUI();

                                // If not already sharing, start sharing
                                if (!isSharing) {
                                    // Before trying to connect to the server, check if network is available
                                    if (isNetworkAvailable()) {
                                        // Test server connectivity before attempting to share
                                        testServerConnectivity(() -> {
                                            // Server is reachable, share location
                                            shareLocationWithServer();
                                        }, () -> {
                                            // Server is not reachable, use SMS fallback
                                            Toast.makeText(SOSLocationActivity.this,
                                                    "Cannot connect to server. Using SMS fallback.",
                                                    Toast.LENGTH_SHORT).show();
                                            sendLocationViaSMS();
                                            // Still mark as sharing for UI purposes
                                            isSharing = true;
                                        });
                                    } else {
                                        // No network, use SMS fallback
                                        Toast.makeText(SOSLocationActivity.this,
                                                "No network connection. Using SMS fallback.",
                                                Toast.LENGTH_SHORT).show();
                                        sendLocationViaSMS();
                                        // Still mark as sharing for UI purposes
                                        isSharing = true;
                                    }
                                }
                            } else {
                                Log.e(TAG, "getCurrentLocation returned null location");
                                // Fall back to getLastLocation
                                getLastKnownLocation();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "getCurrentLocation failed: " + e.getMessage(), e);
                            // Fall back to getLastLocation
                            getLastKnownLocation();
                        });
            } catch (Exception e) {
                Log.e(TAG, "Exception when requesting location: " + e.getMessage(), e);
                // Fall back to getLastLocation
                getLastKnownLocation();
            }
        } else {
            Log.e(TAG, "Location permission not granted");
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getLastKnownLocation() {
        Log.d(TAG, "Falling back to getLastLocation...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            Log.d(TAG, "getLastLocation success: " + location.getLatitude() + ", " + location.getLongitude());
                            currentLocation = location;
                            updateLocationUI();

                            if (!isSharing) {
                                if (isNetworkAvailable()) {
                                    testServerConnectivity(() -> {
                                        shareLocationWithServer();
                                    }, () -> {
                                        Toast.makeText(SOSLocationActivity.this,
                                                "Cannot connect to server. Using SMS fallback.",
                                                Toast.LENGTH_SHORT).show();
                                        sendLocationViaSMS();
                                        isSharing = true;
                                    });
                                } else {
                                    Toast.makeText(SOSLocationActivity.this,
                                            "No network connection. Using SMS fallback.",
                                            Toast.LENGTH_SHORT).show();
                                    sendLocationViaSMS();
                                    isSharing = true;
                                }
                            }
                        } else {
                            Log.e(TAG, "getLastLocation returned null location");
                            Toast.makeText(SOSLocationActivity.this, "Cannot get your location. Please try refreshing.", Toast.LENGTH_SHORT).show();
                            // Request location updates explicitly
                            requestNewLocationData();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "getLastLocation failed: " + e.getMessage(), e);
                        Toast.makeText(SOSLocationActivity.this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Request location updates explicitly
                        requestNewLocationData();
                    });
        }
    }

    private void requestNewLocationData() {
        Log.d(TAG, "Requesting new location data...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest request = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(0)
                    .setFastestInterval(0)
                    .setNumUpdates(1);

            fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        Log.e(TAG, "LocationResult is null in requestNewLocationData");
                        return;
                    }
                    if (locationResult.getLocations().size() > 0) {
                        Location location = locationResult.getLocations().get(0);
                        Log.d(TAG, "New location received: " + location.getLatitude() + ", " + location.getLongitude());
                        currentLocation = location;
                        updateLocationUI();
                    } else {
                        Log.e(TAG, "No locations in LocationResult");
                    }
                    // Remove updates after receiving
                    fusedLocationClient.removeLocationUpdates(this);
                }
            }, Looper.getMainLooper());
        }
    }

    private boolean isLocationEnabled() {
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
    }

    private void showLocationServicesDialog() {
        if (currentDialogState != DIALOG_STATE_NONE || isFinishing() || isDestroyed()) {
            return;
        }

        try {
            currentDialogState = DIALOG_STATE_SHOWING;

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Location Services Disabled")
                    .setMessage("Please enable location services to use this feature")
                    .setPositiveButton("Open Settings", (dialogInterface, i) -> {
                        currentDialogState = DIALOG_STATE_NONE;
                        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", (dialogInterface, i) -> {
                        currentDialogState = DIALOG_STATE_NONE;
                    })
                    .setOnCancelListener(dialog1 -> {
                        currentDialogState = DIALOG_STATE_NONE;
                    })
                    .create();

            dialog.show();
        } catch (Exception e) {
            currentDialogState = DIALOG_STATE_NONE;
            Log.e(TAG, "Error showing location services dialog", e);
        }
    }

    private void updateLocationUI() {
        if (currentLocation != null) {
            try {
                Log.d(TAG, "Updating location UI with: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());

                // Mark the SOS location on the map
                markSOSLocation(currentLocation.getLatitude(), currentLocation.getLongitude());

                // Update text views
                if (coordinatesText != null) {
                    String coords = String.format(Locale.US, "Lat: %.6f, Lng: %.6f",
                            currentLocation.getLatitude(), currentLocation.getLongitude());
                    Log.d(TAG, "Setting coordinates text: " + coords);
                    coordinatesText.setText(coords);
                }

                // Get address from coordinates
                getAddressFromLocation(currentLocation);
            } catch (Exception e) {
                Log.e(TAG, "Error updating location UI", e);
                Toast.makeText(this, "Error updating location display: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Cannot update UI - currentLocation is null");
        }
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                // Get address lines
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(address.getAddressLine(i));
                }

                locationText.setText(sb.toString());
            } else {
                locationText.setText("Address not found");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting address", e);
            locationText.setText("Unable to get address");
        }
    }

    private void shareLocationWithServer() {
        if (currentLocation == null) {
            Toast.makeText(this, "No location available to share", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // If we already have a location ID, update instead of creating
            if (isSharing && locationId != null) {
                updateLocationOnServer();
                return;
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("user_id", "user123"); // In a real app, use the actual user ID
            requestBody.put("latitude", currentLocation.getLatitude());
            requestBody.put("longitude", currentLocation.getLongitude());
            requestBody.put("disaster_type", "sos");
            requestBody.put("message", "Emergency SOS - Need assistance");

            // Get address if available
            if (!locationText.getText().toString().equals("Fetching your location...") &&
                    !locationText.getText().toString().equals("Address not found") &&
                    !locationText.getText().toString().equals("Unable to get address")) {
                requestBody.put("address", locationText.getText().toString());
            }

            // Send the request
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), requestBody.toString());

            // Show loading dialog
            AlertDialog progressDialog = new AlertDialog.Builder(this)
                    .setTitle("Sending SOS")
                    .setMessage("Sharing your location with emergency services...")
                    .setCancelable(false)
                    .create();
            progressDialog.show();

            if (isNetworkAvailable()) {
                Log.d(TAG, "Sending location to server: " + requestBody.toString());

                ApiClient.getClient().newCall(ApiClient.buildRequest("api/emergency/sos/location", "POST", body))
                        .enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(SOSLocationActivity.this, "Failed to share location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Failed to share location", e);
                                    // Still continue with SMS as backup
                                    sendLocationViaSMS();
                                });
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                runOnUiThread(() -> progressDialog.dismiss());

                                String responseBody = response.body().string();
                                Log.d(TAG, "Server response: " + responseBody);

                                if (response.isSuccessful()) {
                                    try {
                                        JSONObject jsonResponse = new JSONObject(responseBody);
                                        boolean success = jsonResponse.getBoolean("success");

                                        if (success) {
                                            locationId = jsonResponse.getString("location_id");
                                            isSharing = true;

                                            Log.d(TAG, "Successfully received location_id: " + locationId);

                                            runOnUiThread(() -> {
                                                Toast.makeText(SOSLocationActivity.this, "SOS location shared successfully", Toast.LENGTH_SHORT).show();

                                                // Show confirmation dialog
                                                new AlertDialog.Builder(SOSLocationActivity.this)
                                                        .setTitle("SOS Alert Sent")
                                                        .setMessage("Your location has been shared with emergency services. You are now in continuous location tracking mode.")
                                                        .setPositiveButton("OK", null)
                                                        .show();
                                            });
                                        } else {
                                            runOnUiThread(() -> {
                                                Toast.makeText(SOSLocationActivity.this, "Failed to share location", Toast.LENGTH_SHORT).show();
                                                // Try SMS as backup
                                                sendLocationViaSMS();
                                            });
                                        }
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error parsing response", e);
                                        runOnUiThread(() -> {
                                            Toast.makeText(SOSLocationActivity.this, "Error parsing server response", Toast.LENGTH_SHORT).show();
                                            // Try SMS as backup
                                            sendLocationViaSMS();
                                        });
                                    }
                                } else {
                                    runOnUiThread(() -> {
                                        Toast.makeText(SOSLocationActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                                        // Try SMS as backup
                                        sendLocationViaSMS();
                                    });
                                }
                            }
                        });
            } else {
                progressDialog.dismiss();
                sendLocationViaSMS();

                // Still set sharing to true for UI updates
                isSharing = true;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON", e);
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Try SMS as backup
            sendLocationViaSMS();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void sendLocationViaSMS() {
        try {
            String message = "SOS EMERGENCY: Lat: " + currentLocation.getLatitude() +
                    ", Lng: " + currentLocation.getLongitude() +
                    " - " + locationText.getText().toString();

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("EMERGENCY_NUMBER", null, message, null, null);

            Toast.makeText(this, "SOS sent via SMS", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "SMS sending failed", e);
        }
    }
    private void updateLocationOnServer() {
        if (locationId == null || currentLocation == null) {
            Log.e(TAG, "Cannot update location: locationId or currentLocation is null");
            if (locationId == null) {
                Log.e(TAG, "locationId is null");
            }
            if (currentLocation == null) {
                Log.e(TAG, "currentLocation is null");
            }
            return;
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("latitude", currentLocation.getLatitude());
            requestBody.put("longitude", currentLocation.getLongitude());

            // Get address if available
            if (!locationText.getText().toString().equals("Fetching your location...") &&
                    !locationText.getText().toString().equals("Address not found") &&
                    !locationText.getText().toString().equals("Unable to get address")) {
                requestBody.put("address", locationText.getText().toString());
            }

            Log.d(TAG, "Updating location on server for ID: " + locationId);
            Log.d(TAG, "Update data: " + requestBody.toString());

            // Send the request
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), requestBody.toString());

            String endpoint = "api/emergency/sos/location/" + locationId;
            Log.d(TAG, "Calling endpoint: " + endpoint);

            ApiClient.getClient().newCall(ApiClient.buildRequest(endpoint, "PUT", body))
                    .enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e(TAG, "Failed to update location: " + e.getMessage(), e);
                            // Not showing a UI error for updates to avoid flooding the user
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String responseBody = response.body().string();
                            if (response.isSuccessful()) {
                                Log.d(TAG, "Location updated successfully: " + responseBody);
                            } else {
                                Log.e(TAG, "Server error during location update: " + response.code() + " - " + responseBody);

                                // If we get a 404 Not Found, it means the location tracking session
                                // may have expired or been deleted - reset the state
                                if (response.code() == 404) {
                                    isSharing = false;
                                    locationId = null;

                                    // Try to start a new sharing session
                                    runOnUiThread(() -> {
                                        Toast.makeText(SOSLocationActivity.this,
                                                "Location tracking session expired. Restarting...",
                                                Toast.LENGTH_SHORT).show();
                                        shareLocationWithServer();
                                    });
                                }
                            }
                        }
                    });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for location update", e);
        }
    }

    private void stopLocationSharing() {
        if (currentDialogState != DIALOG_STATE_NONE || isFinishing() || isDestroyed()) {
            return;
        }

        try {
            currentDialogState = DIALOG_STATE_SHOWING;

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Stop SOS Location Sharing")
                    .setMessage("Are you sure you want to stop sharing your location with emergency services?")
                    .setPositiveButton("Stop Sharing", (dialogInterface, which) -> {
                        currentDialogState = DIALOG_STATE_NONE;
                        performStopSharing();
                    })
                    .setNegativeButton("Continue Sharing", (dialogInterface, which) -> {
                        currentDialogState = DIALOG_STATE_NONE;
                    })
                    .setOnCancelListener(dialog1 -> {
                        currentDialogState = DIALOG_STATE_NONE;
                    })
                    .create();

            dialog.show();
        } catch (Exception e) {
            currentDialogState = DIALOG_STATE_NONE;
            Log.e(TAG, "Error in stopLocationSharing", e);
        }
    }

    private void performStopSharing() {
        if (!isSharing || locationId == null) {
            finish();
            return;
        }

        try {
            ApiClient.getClient().newCall(ApiClient.buildRequest("api/emergency/sos/location/" + locationId + "/end", "PUT", null))
                    .enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e(TAG, "Failed to stop location sharing", e);
                            // Finish anyway to allow the user to exit
                            runOnUiThread(SOSLocationActivity.this::finish);
                        }

                        @Override
                        public void onResponse(Call call, Response response) {
                            runOnUiThread(SOSLocationActivity.this::finish);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in performStopSharing", e);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isSharing) {
            // Show dialog asking if they want to stop sharing
            Toast.makeText(this, "Location sharing is active. Please use STOP SHARING button to exit.", Toast.LENGTH_LONG).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Reset dialog state when activity resumes
            currentDialogState = DIALOG_STATE_NONE;

            if (checkLocationPermission()) {
                startLocationUpdates();
            }
            timerHandler.post(timerRunnable);
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            timerHandler.removeCallbacks(timerRunnable);
            // Don't stop location updates when paused to continue sharing
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG, "Window focus changed: " + hasFocus);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Log.d(TAG, "onDestroy called - stopping location updates");
            stopLocationUpdates();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }

    @Override
    public void finish() {
        if (isSharing && !isFinishing && currentDialogState == DIALOG_STATE_NONE) {
            stopLocationSharing();
        } else {
            super.finish();
        }
    }

    /**
     * Test if the server is reachable
     * @param onSuccess callback if server is reachable
     * @param onFailure callback if server is not reachable
     */
    private void testServerConnectivity(Runnable onSuccess, Runnable onFailure) {
        new Thread(() -> {
            try {
                // Get the base URL from resources
                String serverUrl = getResources().getString(R.string.api_base_url);

                // Parse the URL to get host and port
                String host;
                int port = 5000; // Default port

                if (serverUrl.startsWith("http://")) {
                    serverUrl = serverUrl.substring(7);
                } else if (serverUrl.startsWith("https://")) {
                    serverUrl = serverUrl.substring(8);
                }

                // Remove trailing slash if present
                if (serverUrl.endsWith("/")) {
                    serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
                }

                // Check if port is specified
                if (serverUrl.contains(":")) {
                    String[] parts = serverUrl.split(":");
                    host = parts[0];
                    if (parts.length > 1) {
                        String portStr = parts[1];
                        port = Integer.parseInt(portStr);
                    }
                } else {
                    host = serverUrl;
                }

                // Try to connect to the server
                java.net.Socket socket = new java.net.Socket();
                socket.connect(new java.net.InetSocketAddress(host, port), 3000);
                socket.close();

                // Connection successful
                runOnUiThread(onSuccess);
            } catch (Exception e) {
                Log.e(TAG, "Server connectivity test failed: " + e.getMessage());
                runOnUiThread(onFailure);
            }
        }).start();
    }

    /**
     * Check if Google Play Services is available
     * @return true if available, false otherwise
     */
    private boolean checkGooglePlayServices() {
        try {
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);

            if (resultCode != ConnectionResult.SUCCESS) {
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    // Don't show dialog here - just return false so we know services aren't available
                    return false;
                } else {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error checking Google Play Services", e);
            return false;
        }
    }

    /**
     * Set a default location for testing if we can't get the real location
     */
    private void setDefaultLocation() {
        try {
            // Use a default location (Mumbai, India coordinates for testing)
            Location defaultLocation = new Location("default");
            defaultLocation.setLatitude(19.0760);
            defaultLocation.setLongitude(72.8777);
            defaultLocation.setTime(System.currentTimeMillis());

            // Log the default location
            Log.d(TAG, "Setting default location: " + defaultLocation.getLatitude() + ", " + defaultLocation.getLongitude());

            // Set it as current location
            currentLocation = defaultLocation;

            // Update text views directly
            if (coordinatesText != null) {
                String coords = String.format(Locale.US, "Lat: %.6f, Lng: %.6f",
                        currentLocation.getLatitude(), currentLocation.getLongitude());
                coordinatesText.setText(coords);
            }

            if (locationText != null) {
                locationText.setText("Mumbai, Maharashtra, India (Default Location)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting default location", e);
        }
    }

    /**
     * Marks an SOS location on the map with a custom marker
     * @param latitude The latitude of the SOS location
     * @param longitude The longitude of the SOS location
     */
    private void markSOSLocation(double latitude, double longitude) {
        if (mMap == null) {
            Log.e(TAG, "Cannot mark SOS location - map is null");
            return;
        }

        try {
            // Clear previous markers
            mMap.clear();

            // Create the SOS location point
            LatLng sosPosition = new LatLng(latitude, longitude);

            // Add the SOS marker to the map - simplified to avoid potential crashes
            mMap.addMarker(new MarkerOptions()
                    .position(sosPosition)
                    .title("SOS Emergency")
                    .snippet("Lat: " + latitude + ", Lng: " + longitude)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Move camera to the SOS location with zoom
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sosPosition, 15f));

            // Add a circle around the SOS point
            try {
                mMap.addCircle(new CircleOptions()
                        .center(sosPosition)
                        .radius(200) // 200 meters radius
                        .strokeColor(Color.RED)
                        .strokeWidth(2)
                        .fillColor(Color.argb(40, 255, 0, 0))); // Semi-transparent red
            } catch (Exception e) {
                Log.e(TAG, "Could not add circle to map", e);
                // Continue without the circle if it fails
            }

            Log.d(TAG, "Marked SOS location at: " + latitude + ", " + longitude);
        } catch (Exception e) {
            Log.e(TAG, "Error marking SOS location", e);
        }
    }

    /**
     * Enhanced location tracking method for SOS emergencies
     * - Uses high accuracy location
     * - Implements continuous updates
     * - Shows visual accuracy indicators
     * - Robust error handling with fallbacks
     * - Zooms appropriately based on accuracy
     */
    private void enableMyLocation() {
        try {
            // Check if we have permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            // Enable location layer on map
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            }

            // Create a high-accuracy, fast-update location request for emergency situations
            LocationRequest sosLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5000)         // 5 seconds between updates
                    .setFastestInterval(3000)  // Fastest update interval is 3 seconds
                    .setMaxWaitTime(10000);    // Max wait time of 10 seconds

            // Create a one-time callback for initial position
            fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "SOS: Initial location acquired: " + 
                                  location.getLatitude() + ", " + location.getLongitude() + 
                                  " (accuracy: " + location.getAccuracy() + "m)");
                            
                            // Save this location
                            currentLocation = location;
                            
                            // Update UI with location
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            
                            // Clear previous markers
                            if (mMap != null) {
                                mMap.clear();
                                
                                // Add SOS marker
                                mMap.addMarker(new MarkerOptions()
                                        .position(userLocation)
                                        .title("SOS EMERGENCY")
                                        .snippet("Accuracy: " + Math.round(location.getAccuracy()) + "m")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                
                                // Add accuracy circle
                                mMap.addCircle(new CircleOptions()
                                        .center(userLocation)
                                        .radius(location.getAccuracy())
                                        .strokeColor(Color.RED)
                                        .strokeWidth(2)
                                        .fillColor(Color.argb(40, 255, 0, 0)));
                                
                                // Zoom level based on accuracy - closer for more accurate readings
                                float zoomLevel = calculateZoomLevel(location.getAccuracy());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, zoomLevel));
                            }
                            
                            // Update UI elements
                            if (coordinatesText != null) {
                                coordinatesText.setText(String.format(Locale.US, "Lat: %.6f, Lng: %.6f (±%dm)",
                                        location.getLatitude(), location.getLongitude(), 
                                        Math.round(location.getAccuracy())));
                            }
                            
                            // Get address for the location text
                            getAddressFromLocation(location);
                            
                            // Update server with this location if sharing
                            if (isSharing && isNetworkAvailable()) {
                                updateLocationOnServer();
                            }
                        } else {
                            Log.e(TAG, "SOS: Failed to get initial location");
                            Toast.makeText(SOSLocationActivity.this, 
                                    "Unable to get your location. Please try again or move to an open area.", 
                                    Toast.LENGTH_SHORT).show();
                            
                            // Show default or last known location
                            getLastKnownLocation();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "SOS: Error getting location", e);
                        Toast.makeText(SOSLocationActivity.this,
                                "Location error: " + e.getLocalizedMessage(), 
                                Toast.LENGTH_SHORT).show();
                        
                        // Show default or last known location
                        getLastKnownLocation();
                    });
                    
            // Set up ongoing location updates
            LocationCallback sosLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null || locationResult.getLocations().isEmpty()) {
                        Log.d(TAG, "SOS: No locations in update");
                        return;
                    }
                    
                    // Get the most recent location
                    Location location = locationResult.getLastLocation();
                    if (location == null) return;
                    
                    Log.d(TAG, "SOS: Location update: " + 
                          location.getLatitude() + ", " + location.getLongitude() + 
                          " (accuracy: " + location.getAccuracy() + "m)");
                    
                    // Check if the new location is significantly better than our current one
                    if (currentLocation == null || 
                            location.getAccuracy() < currentLocation.getAccuracy() - 20 || // Significantly more accurate
                            location.getTime() - currentLocation.getTime() > 60000) {      // At least a minute newer
                        
                        // Update our reference location
                        currentLocation = location;
                        
                        // Update UI with this better location
                        LatLng updatedLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        
                        if (mMap != null) {
                            // Clear previous markers and add new ones
                            mMap.clear();
                            
                            // Add SOS marker
                            mMap.addMarker(new MarkerOptions()
                                    .position(updatedLocation)
                                    .title("SOS EMERGENCY")
                                    .snippet("Accuracy: " + Math.round(location.getAccuracy()) + "m")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            
                            // Add accuracy circle
                            mMap.addCircle(new CircleOptions()
                                    .center(updatedLocation)
                                    .radius(location.getAccuracy())
                                    .strokeColor(Color.RED)
                                    .strokeWidth(2)
                                    .fillColor(Color.argb(40, 255, 0, 0)));
                                    
                            // Smoothly move camera only if accuracy is good
                            if (location.getAccuracy() < 50) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLng(updatedLocation));
                            }
                        }
                        
                        // Update UI elements
                        if (coordinatesText != null) {
                            coordinatesText.setText(String.format(Locale.US, "Lat: %.6f, Lng: %.6f (±%dm)",
                                    location.getLatitude(), location.getLongitude(), 
                                    Math.round(location.getAccuracy())));
                        }
                        
                        // Get address information only if significantly different
                        getAddressFromLocation(location);
                        
                        // Update server with this location if sharing
                        if (isSharing && isNetworkAvailable()) {
                            updateLocationOnServer();
                        }
                    }
                }
            };
            
            // Start receiving location updates
            fusedLocationClient.requestLocationUpdates(
                    sosLocationRequest, 
                    sosLocationCallback,
                    Looper.getMainLooper());
                    
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when enabling location", e);
            Toast.makeText(this, "Location permission required for SOS services", Toast.LENGTH_SHORT).show();
            
            // Try to request permissions again
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Error enabling location", e);
            Toast.makeText(this, "Location error: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Calculate appropriate zoom level based on accuracy
     * @param accuracyMeters Location accuracy in meters
     * @return Zoom level (higher number = closer zoom)
     */
    private float calculateZoomLevel(float accuracyMeters) {
        if (accuracyMeters <= 10) return 18f;  // Very accurate - zoom in close
        if (accuracyMeters <= 50) return 16f;  // Fairly accurate
        if (accuracyMeters <= 200) return 14f; // Moderately accurate
        if (accuracyMeters <= 500) return 12f; // Low accuracy
        return 10f; // Poor accuracy - zoom out to show larger area
    }
}