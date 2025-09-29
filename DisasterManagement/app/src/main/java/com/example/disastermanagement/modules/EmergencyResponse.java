package com.example.disastermanagement.modules;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.disastermanagement.R;
import com.example.disastermanagement.api.ApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.time.LocalDateTime;
import java.time.Duration;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This class manages emergency response operations and resource management
 * during disaster situations.
 */
public class EmergencyResponse extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private Map<String, EmergencyTeam> availableTeams;
    private Map<String, EmergencyTeam> deployedTeams;
    private Map<String, Resource> resources;
    private Map<String, EvacuationCenter> evacuationCenters;
    private Map<String, EmergencyOperation> activeOperations;

    private CardView cardSosLocation;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_response);

        this.availableTeams = new HashMap<>();
        this.deployedTeams = new HashMap<>();
        this.resources = new HashMap<>();
        this.evacuationCenters = new HashMap<>();
        this.activeOperations = new HashMap<>();

        // Initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize the UI components
        initializeUI();
    }

    /**
     * Initialize UI components and set up click listeners
     */
    private void initializeUI() {
        // Find the SOS Location button
        cardSosLocation = findViewById(R.id.cardSosLocation);

        // Set up click listener for SOS Location button
        cardSosLocation.setOnClickListener(v -> {
            // Show confirmation dialog
            new AlertDialog.Builder(EmergencyResponse.this)
                    .setTitle("Send Emergency SOS")
                    .setMessage("This will share your current location with emergency services. Continue?")
                    .setPositiveButton("Send SOS", (dialog, which) -> {
                        // Check location permission first
                        if (checkLocationPermission()) {
                            // Get and share location immediately
                            getAndShareLocation();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /**
     * Check if location permission is granted
     */
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

    /**
     * Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get and share location
                getAndShareLocation();
            } else {
                Toast.makeText(this, "Location permission is required for SOS services", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Get current location and share it with the server
     */
    private void getAndShareLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Show progress dialog
            AlertDialog progressDialog = new AlertDialog.Builder(this)
                    .setTitle("Sending SOS")
                    .setMessage("Getting your location...")
                    .setCancelable(false)
                    .create();
            progressDialog.show();

            // First check if the server is reachable
            Thread serverCheckThread = new Thread(() -> {
                boolean isServerReachable = isServerReachable();

                runOnUiThread(() -> {
                    if (!isServerReachable) {
                        progressDialog.dismiss();
                        showServerConnectionErrorDialog();
                        return;
                    }

                    // Server is reachable, proceed with getting location
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(this, location -> {
                                if (location != null) {
                                    // Share location with server
                                    shareLocationWithServer(location, progressDialog);
                                } else {
                                    progressDialog.dismiss();
                                    Toast.makeText(EmergencyResponse.this, "Cannot get current location. Try again.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(EmergencyResponse.this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
            });
            serverCheckThread.start();
        }
    }

    /**
     * Check if the server is reachable
     */
    private boolean isServerReachable() {
        try {
            // Get the base URL from ApiClient
            String serverUrl = getResources().getString(R.string.api_base_url);

            // Extract the host and port
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
                port = Integer.parseInt(parts[1]);
            } else {
                host = serverUrl;
            }

            // Try to establish socket connection
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), 5000);
            socket.close();
            return true;
        } catch (Exception e) {
            Log.e("EmergencyResponse", "Server check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Show dialog for server connection error
     */
    private void showServerConnectionErrorDialog() {
        String serverUrl = getResources().getString(R.string.api_base_url);

        new AlertDialog.Builder(this)
                .setTitle("Server Connection Error")
                .setMessage("Could not connect to emergency server at " + serverUrl +
                        "\n\nPlease check your connection settings and try again. " +
                        "You can still send an SMS emergency alert.")
                .setPositiveButton("Send SMS Alert", (dialog, which) -> {
                    sendSmsEmergencyAlert();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Send emergency alert via SMS
     */
    private void sendSmsEmergencyAlert() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            AlertDialog progressDialog = new AlertDialog.Builder(this)
                    .setTitle("Sending SMS Alert")
                    .setMessage("Getting your location...")
                    .setCancelable(false)
                    .create();
            progressDialog.show();

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        progressDialog.dismiss();
                        if (location != null) {
                            try {
                                // Check for SMS permission
                                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(this,
                                            new String[]{Manifest.permission.SEND_SMS}, 2);
                                    return;
                                }

                                // Send SMS
                                String message = "SOS EMERGENCY: Lat: " + location.getLatitude() +
                                        ", Lng: " + location.getLongitude();

                                android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
                                smsManager.sendTextMessage("EMERGENCY_NUMBER", null, message, null, null);

                                Toast.makeText(this, "SOS sent via SMS", Toast.LENGTH_SHORT).show();

                                // Show success dialog
                                showSuccessDialog(location);
                            } catch (Exception e) {
                                Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Cannot get location for SMS", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Share location with server
     */
    private void shareLocationWithServer(Location location, AlertDialog progressDialog) {
        try {
            progressDialog.setMessage("Sending your location to emergency services...");

            JSONObject requestBody = new JSONObject();
            requestBody.put("user_id", "user123"); // In a real app, use the actual user ID
            requestBody.put("latitude", location.getLatitude());
            requestBody.put("longitude", location.getLongitude());
            requestBody.put("disaster_type", "sos");
            requestBody.put("message", "Emergency SOS - Need immediate assistance");

            // Send the request
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), requestBody.toString());

            ApiClient.getClient().newCall(ApiClient.buildRequest("api/emergency/sos/location", "POST", body))
                    .enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(EmergencyResponse.this, "Failed to send SOS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try {
                                if (response.isSuccessful()) {
                                    JSONObject jsonResponse = new JSONObject(response.body().string());
                                    boolean success = jsonResponse.getBoolean("success");

                                    final String locationId = success ? jsonResponse.getString("location_id") : null;

                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        if (success) {
                                            // Save location ID for tracker activity
                                            Intent intent = getIntent();
                                            intent.putExtra("LOCATION_ID", locationId);

                                            // Show success dialog with location details
                                            showSuccessDialog(location);
                                        } else {
                                            Toast.makeText(EmergencyResponse.this, "Failed to send SOS", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(EmergencyResponse.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            } catch (JSONException e) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(EmergencyResponse.this, "Error processing response", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
        } catch (JSONException e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show success dialog with location details
     */
    private void showSuccessDialog(Location location) {
        new AlertDialog.Builder(this)
                .setTitle("SOS Sent Successfully")
                .setMessage("Your location has been shared with emergency services.\n\n" +
                        "Coordinates: " + location.getLatitude() + ", " + location.getLongitude() +
                        "\n\nHelp is on the way!")
//                .setPositiveButton("Open Location Tracker", (dialog, which) -> {
//                    // Start the SOS Location Activity for continuous tracking
//                    Intent sosIntent = new Intent(EmergencyResponse.this, SOSLocationActivity.class);
//                    // Pass the location and the fact that we're already sharing
//                    sosIntent.putExtra("ALREADY_SHARING", true);
//                    sosIntent.putExtra("LATITUDE", location.getLatitude());
//                    sosIntent.putExtra("LONGITUDE", location.getLongitude());
//                    // If we received a location ID from the server response, pass it
//                    try {
//                        if (getIntent().hasExtra("LOCATION_ID")) {
//                            String locationId = getIntent().getStringExtra("LOCATION_ID");
//                            sosIntent.putExtra("LOCATION_ID", locationId);
//                        }
//                    } catch (Exception e) {
//                        Log.e("EmergencyResponse", "Error getting location ID", e);
//                    }
//                    startActivity(sosIntent);
//                })
                .setNegativeButton("Close", null)
                .setCancelable(false)
                .show();
    }

    /**
     * Registers a new emergency response team
     * @param teamId Unique team identifier
     * @param teamName Team name
     * @param teamType Type of team (MEDICAL, RESCUE, FIRE, etc.)
     * @param members Number of team members
     * @param capabilities List of team capabilities
     * @return boolean indicating success
     */
    public boolean registerTeam(String teamId, String teamName, String teamType, int members, List<String> capabilities) {
        if (availableTeams.containsKey(teamId)) {
            return false;
        }

        EmergencyTeam team = new EmergencyTeam(teamId, teamName, teamType, members, capabilities);
        availableTeams.put(teamId, team);
        return true;
    }

    /**
     * Deploys a team to a disaster location
     * @param teamId ID of the team to deploy
     * @param disasterId ID of the disaster
     * @param location Map containing location coordinates
     * @param missionDetails Additional mission details
     * @return String operation ID if successful, null otherwise
     */
    public String deployTeam(String teamId, String disasterId, Map<String, Double> location,
                             Map<String, Object> missionDetails) {
        if (!availableTeams.containsKey(teamId)) {
            return null; // Team not available
        }

        EmergencyTeam team = availableTeams.get(teamId);
        availableTeams.remove(teamId);

        String operationId = UUID.randomUUID().toString();
        EmergencyOperation operation = new EmergencyOperation(
                operationId, disasterId, team, location, LocalDateTime.now(), missionDetails
        );

        activeOperations.put(operationId, operation);
        deployedTeams.put(teamId, team);

        return operationId;
    }

    /**
     * Completes an emergency operation and returns the team to available status
     * @param operationId ID of the operation to complete
     * @param operationReport Report on the operation outcomes
     * @return boolean indicating success
     */
    public boolean completeOperation(String operationId, Map<String, Object> operationReport) {
        if (!activeOperations.containsKey(operationId)) {
            return false;
        }

        EmergencyOperation operation = activeOperations.get(operationId);
        EmergencyTeam team = operation.getTeam();

        // Update operation status
        operation.setCompletionTime(LocalDateTime.now());
        operation.setOperationReport(operationReport);

        // Move team back to available
        deployedTeams.remove(team.getId());
        availableTeams.put(team.getId(), team);

        // Move operation to completed (in a real implementation)
        activeOperations.remove(operationId);

        return true;
    }

    /**
     * Allocates resources to an emergency operation
     * @param operationId ID of the operation
     * @param resourceAllocations Map of resource IDs to quantities
     * @return boolean indicating success
     */
    public boolean allocateResources(String operationId, Map<String, Integer> resourceAllocations) {
        if (!activeOperations.containsKey(operationId)) {
            return false;
        }

        EmergencyOperation operation = activeOperations.get(operationId);

        // Check if resources are available in sufficient quantities
        for (Map.Entry<String, Integer> entry : resourceAllocations.entrySet()) {
            String resourceId = entry.getKey();
            int quantity = entry.getValue();

            if (!resources.containsKey(resourceId) ||
                    resources.get(resourceId).getAvailableQuantity() < quantity) {
                return false; // Not enough resources
            }
        }

        // Allocate resources
        for (Map.Entry<String, Integer> entry : resourceAllocations.entrySet()) {
            String resourceId = entry.getKey();
            int quantity = entry.getValue();

            Resource resource = resources.get(resourceId);
            resource.allocate(quantity);
            operation.addResource(resourceId, quantity);
        }

        return true;
    }

    /**
     * Adds a new resource to inventory
     * @param resourceId Resource identifier
     * @param name Resource name
     * @param type Resource type
     * @param quantity Available quantity
     * @param unit Unit of measurement
     * @return boolean indicating success
     */
    public boolean addResource(String resourceId, String name, String type,
                               int quantity, String unit) {
        if (resources.containsKey(resourceId)) {
            // Update existing resource quantity
            Resource resource = resources.get(resourceId);
            resource.addQuantity(quantity);
            return true;
        }

        // Create new resource
        Resource resource = new Resource(resourceId, name, type, quantity, unit);
        resources.put(resourceId, resource);

        return true;
    }

    /**
     * Registers a new evacuation center
     * @param centerId Center identifier
     * @param name Center name
     * @param location Map of location coordinates
     * @param capacity Maximum capacity
     * @param facilities Available facilities
     * @return boolean indicating success
     */
    public boolean registerEvacuationCenter(String centerId, String name, Map<String, Double> location,
                                            int capacity, List<String> facilities) {
        if (evacuationCenters.containsKey(centerId)) {
            return false; // Center already exists
        }

        EvacuationCenter center = new EvacuationCenter(centerId, name, location, capacity, facilities);
        evacuationCenters.put(centerId, center);

        return true;
    }

    /**
     * Updates the current occupancy of an evacuation center
     * @param centerId Center identifier
     * @param currentOccupancy Current number of occupants
     * @return boolean indicating success
     */
    public boolean updateEvacuationCenterOccupancy(String centerId, int currentOccupancy) {
        if (!evacuationCenters.containsKey(centerId)) {
            return false;
        }

        EvacuationCenter center = evacuationCenters.get(centerId);
        if (currentOccupancy > center.getCapacity()) {
            return false; // Exceeds capacity
        }

        center.setCurrentOccupancy(currentOccupancy);
        return true;
    }

    /**
     * Gets available teams by type
     * @param teamType Type of team to filter by (optional, can be null)
     * @return List of available emergency teams
     */
    public List<EmergencyTeam> getAvailableTeams(String teamType) {
        List<EmergencyTeam> result = new ArrayList<>();

        for (EmergencyTeam team : availableTeams.values()) {
            if (teamType == null || team.getType().equals(teamType)) {
                result.add(team);
            }
        }

        return result;
    }

    /**
     * Gets nearby evacuation centers
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param maxDistance Maximum distance in kilometers
     * @return List of evacuation centers within the specified distance
     */
    public List<EvacuationCenter> getNearbyEvacuationCenters(double latitude, double longitude, double maxDistance) {
        List<EvacuationCenter> nearbyCenters = new ArrayList<>();

        for (EvacuationCenter center : evacuationCenters.values()) {
            Map<String, Double> centerLocation = center.getLocation();
            double centerLat = centerLocation.get("latitude");
            double centerLong = centerLocation.get("longitude");

            // Calculate distance using Haversine formula (simplified here)
            double distance = calculateDistance(latitude, longitude, centerLat, centerLong);

            if (distance <= maxDistance) {
                nearbyCenters.add(center);
            }
        }

        // Sort by distance
        nearbyCenters.sort((c1, c2) -> {
            Map<String, Double> loc1 = c1.getLocation();
            Map<String, Double> loc2 = c2.getLocation();

            double dist1 = calculateDistance(latitude, longitude,
                    loc1.get("latitude"), loc1.get("longitude"));
            double dist2 = calculateDistance(latitude, longitude,
                    loc2.get("latitude"), loc2.get("longitude"));

            return Double.compare(dist1, dist2);
        });

        return nearbyCenters;
    }

    /**
     * Generates an emergency response status report
     * @return Map containing status information
     */
    public Map<String, Object> getStatusReport() {
        Map<String, Object> report = new HashMap<>();

        report.put("totalTeams", availableTeams.size() + deployedTeams.size());
        report.put("availableTeams", availableTeams.size());
        report.put("deployedTeams", deployedTeams.size());
        report.put("activeOperations", activeOperations.size());
        report.put("evacuationCenters", evacuationCenters.size());

        // Calculate total evacuation capacity and current occupancy
        int totalCapacity = 0;
        int totalOccupancy = 0;
        for (EvacuationCenter center : evacuationCenters.values()) {
            totalCapacity += center.getCapacity();
            totalOccupancy += center.getCurrentOccupancy();
        }

        report.put("totalEvacuationCapacity", totalCapacity);
        report.put("currentEvacuationOccupancy", totalOccupancy);
        report.put("evacuationCapacityUsedPercent",
                totalCapacity > 0 ? (totalOccupancy * 100.0 / totalCapacity) : 0);

        // Resource summary
        Map<String, Integer> resourcesByType = new HashMap<>();
        for (Resource resource : resources.values()) {
            String type = resource.getType();
            int quantity = resource.getAvailableQuantity();

            resourcesByType.put(type, resourcesByType.getOrDefault(type, 0) + quantity);
        }
        report.put("resourcesByType", resourcesByType);

        return report;
    }

    // Private helper methods

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Simplified Haversine formula
        final int EARTH_RADIUS = 6371; // km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return EARTH_RADIUS * c;
    }

    // Inner classes for data structures

    public static class EmergencyTeam {
        private String id;
        private String name;
        private String type;
        private int members;
        private List<String> capabilities;

        public EmergencyTeam(String id, String name, String type, int members, List<String> capabilities) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.members = members;
            this.capabilities = capabilities;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public int getMembers() { return members; }
        public List<String> getCapabilities() { return capabilities; }
    }

    public static class Resource {
        private String id;
        private String name;
        private String type;
        private int totalQuantity;
        private int allocatedQuantity;
        private String unit;

        public Resource(String id, String name, String type, int quantity, String unit) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.totalQuantity = quantity;
            this.allocatedQuantity = 0;
            this.unit = unit;
        }

        public void allocate(int quantity) {
            if (quantity > getAvailableQuantity()) {
                throw new IllegalArgumentException("Not enough resources available");
            }
            allocatedQuantity += quantity;
        }

        public void deallocate(int quantity) {
            allocatedQuantity = Math.max(0, allocatedQuantity - quantity);
        }

        public void addQuantity(int quantity) {
            totalQuantity += quantity;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public int getTotalQuantity() { return totalQuantity; }
        public int getAllocatedQuantity() { return allocatedQuantity; }
        public int getAvailableQuantity() { return totalQuantity - allocatedQuantity; }
        public String getUnit() { return unit; }
    }

    public static class EvacuationCenter {
        private String id;
        private String name;
        private Map<String, Double> location;
        private int capacity;
        private int currentOccupancy;
        private List<String> facilities;

        public EvacuationCenter(String id, String name, Map<String, Double> location,
                                int capacity, List<String> facilities) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.capacity = capacity;
            this.currentOccupancy = 0;
            this.facilities = facilities;
        }

        public void setCurrentOccupancy(int occupancy) {
            this.currentOccupancy = occupancy;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public Map<String, Double> getLocation() { return location; }
        public int getCapacity() { return capacity; }
        public int getCurrentOccupancy() { return currentOccupancy; }
        public List<String> getFacilities() { return facilities; }
    }

    public static class EmergencyOperation {
        private String id;
        private String disasterId;
        private EmergencyTeam team;
        private Map<String, Double> location;
        private LocalDateTime startTime;
        private LocalDateTime completionTime;
        private Map<String, Object> missionDetails;
        private Map<String, Object> operationReport;
        private Map<String, Integer> allocatedResources;

        public EmergencyOperation(String id, String disasterId, EmergencyTeam team,
                                  Map<String, Double> location, LocalDateTime startTime,
                                  Map<String, Object> missionDetails) {
            this.id = id;
            this.disasterId = disasterId;
            this.team = team;
            this.location = location;
            this.startTime = startTime;
            this.missionDetails = missionDetails;
            this.allocatedResources = new HashMap<>();
        }

        public void setCompletionTime(LocalDateTime time) { this.completionTime = time; }
        public void setOperationReport(Map<String, Object> report) { this.operationReport = report; }
        public void addResource(String resourceId, int quantity) {
            allocatedResources.put(resourceId, allocatedResources.getOrDefault(resourceId, 0) + quantity);
        }

        public String getId() { return id; }
        public String getDisasterId() { return disasterId; }
        public EmergencyTeam getTeam() { return team; }
        public Map<String, Double> getLocation() { return location; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getCompletionTime() { return completionTime; }
        public Duration getDuration() {
            if (completionTime == null) {
                return Duration.between(startTime, LocalDateTime.now());
            }
            return Duration.between(startTime, completionTime);
        }
        public Map<String, Object> getMissionDetails() { return missionDetails; }
        public Map<String, Object> getOperationReport() { return operationReport; }
        public Map<String, Integer> getAllocatedResources() { return allocatedResources; }
    }
} 