package com.example.disastermanagement.modules;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.disastermanagement.R;

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class provides functionality for the disaster monitoring dashboard
 * that displays real-time disaster data, alerts, and status updates.
 */
public class MonitoringDashboard extends AppCompatActivity {
    private Map<String, List<Alert>> activeAlerts;
    private Map<String, DisasterEvent> ongoingDisasters;
    private Map<String, Double> thresholds;
    private List<String> monitoredRegions;
    private LocalDateTime lastUpdated;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring_dashboard);
        
        this.activeAlerts = new HashMap<>();
        this.ongoingDisasters = new HashMap<>();
        this.thresholds = new HashMap<>();
        this.monitoredRegions = new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();
        
        // Initialize default thresholds
        thresholds.put("flood_water_level", 5.0); // meters
        thresholds.put("earthquake_magnitude", 4.5); // Richter scale
        thresholds.put("cyclone_wind_speed", 120.0); // km/h
        thresholds.put("tsunami_wave_height", 2.0); // meters
    }
    
    /**
     * Updates the dashboard with new sensor data
     * @param sensorData Map containing the latest sensor readings
     * @return List of new alerts generated from the sensor data
     */
    public List<Alert> updateDashboard(Map<String, Object> sensorData) {
        List<Alert> newAlerts = processIncomingSensorData(sensorData);
        this.lastUpdated = LocalDateTime.now();
        
        // Add new alerts to the active alerts list
        for (Alert alert : newAlerts) {
            String region = alert.getRegion();
            if (!activeAlerts.containsKey(region)) {
                activeAlerts.put(region, new ArrayList<>());
            }
            activeAlerts.get(region).add(alert);
        }
        
        return newAlerts;
    }
    
    /**
     * Adds a new region to be monitored
     * @param regionName Name of the region
     * @param coordinates Map containing latitude and longitude
     * @param parameters Map of monitoring parameters specific to this region
     * @return boolean indicating success
     */
    public boolean addMonitoredRegion(String regionName, Map<String, Double> coordinates, 
                                     Map<String, Object> parameters) {
        if (monitoredRegions.contains(regionName)) {
            return false; // Region already exists
        }
        
        monitoredRegions.add(regionName);
        // In a real implementation, would store the coordinates and parameters
        // for this region in a database or appropriate data structure
        
        return true;
    }
    
    /**
     * Gets all active alerts for a specific region
     * @param regionName Name of the region to get alerts for
     * @return List of active alerts for the specified region
     */
    public List<Alert> getActiveAlertsForRegion(String regionName) {
        return activeAlerts.getOrDefault(regionName, new ArrayList<>());
    }
    
    /**
     * Updates a monitoring threshold
     * @param thresholdName Name of the threshold to update
     * @param newValue New threshold value
     * @return boolean indicating success
     */
    public boolean updateThreshold(String thresholdName, double newValue) {
        if (!thresholds.containsKey(thresholdName)) {
            return false;
        }
        
        thresholds.put(thresholdName, newValue);
        return true;
    }
    
    /**
     * Registers a new disaster event
     * @param disasterType Type of disaster
     * @param region Affected region
     * @param severity Severity level (1-5)
     * @param details Additional details about the disaster
     * @return String ID of the registered disaster
     */
    public String registerDisasterEvent(String disasterType, String region, 
                                       int severity, Map<String, Object> details) {
        String disasterId = UUID.randomUUID().toString();
        DisasterEvent event = new DisasterEvent(disasterId, disasterType, region, 
                                               severity, LocalDateTime.now(), details);
        
        ongoingDisasters.put(disasterId, event);
        
        // Generate high-priority alert for the new disaster
        Alert newDisasterAlert = new Alert(
            "DISASTER_DECLARED",
            "New " + disasterType + " disaster declared in " + region,
            "HIGH",
            region,
            details
        );
        
        if (!activeAlerts.containsKey(region)) {
            activeAlerts.put(region, new ArrayList<>());
        }
        activeAlerts.get(region).add(newDisasterAlert);
        
        return disasterId;
    }
    
    /**
     * Marks a disaster event as resolved
     * @param disasterId ID of the disaster to resolve
     * @param resolutionDetails Details about the resolution
     * @return boolean indicating success
     */
    public boolean resolveDisasterEvent(String disasterId, Map<String, Object> resolutionDetails) {
        if (!ongoingDisasters.containsKey(disasterId)) {
            return false;
        }
        
        DisasterEvent event = ongoingDisasters.get(disasterId);
        event.setResolved(true);
        event.setResolutionTime(LocalDateTime.now());
        event.setResolutionDetails(resolutionDetails);
        
        // Move from ongoing to historical (in a real implementation)
        ongoingDisasters.remove(disasterId);
        
        return true;
    }
    
    /**
     * Get a dashboard summary
     * @return Map containing summary of the dashboard state
     */
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("lastUpdated", lastUpdated.format(DateTimeFormatter.ISO_DATE_TIME));
        summary.put("monitoredRegionsCount", monitoredRegions.size());
        summary.put("activeAlertsCount", countTotalAlerts());
        summary.put("ongoingDisastersCount", ongoingDisasters.size());
        
        // Count alerts by priority
        Map<String, Integer> alertsByPriority = new HashMap<>();
        alertsByPriority.put("HIGH", 0);
        alertsByPriority.put("MEDIUM", 0);
        alertsByPriority.put("LOW", 0);
        
        for (List<Alert> regionAlerts : activeAlerts.values()) {
            for (Alert alert : regionAlerts) {
                String priority = alert.getPriority();
                alertsByPriority.put(priority, alertsByPriority.get(priority) + 1);
            }
        }
        summary.put("alertsByPriority", alertsByPriority);
        
        return summary;
    }
    
    // Private helper methods
    
    private List<Alert> processIncomingSensorData(Map<String, Object> sensorData) {
        List<Alert> newAlerts = new ArrayList<>();
        
        // Process sensor data and generate alerts based on thresholds
        // This is a simplified implementation
        
        if (sensorData.containsKey("water_level")) {
            double waterLevel = (double) sensorData.get("water_level");
            double threshold = thresholds.get("flood_water_level");
            
            if (waterLevel > threshold) {
                Map<String, Object> details = new HashMap<>();
                details.put("current_level", waterLevel);
                details.put("threshold", threshold);
                details.put("excess", waterLevel - threshold);
                
                Alert alert = new Alert(
                    "WATER_LEVEL_EXCEEDED",
                    "Water level has exceeded the threshold",
                    waterLevel > threshold * 1.5 ? "HIGH" : "MEDIUM",
                    (String) sensorData.get("region"),
                    details
                );
                
                newAlerts.add(alert);
            }
        }
        
        // Similar processing for other sensor types would go here
        
        return newAlerts;
    }
    
    private int countTotalAlerts() {
        int total = 0;
        for (List<Alert> alerts : activeAlerts.values()) {
            total += alerts.size();
        }
        return total;
    }
    
    // Inner classes for data structures
    
    public static class Alert {
        private String type;
        private String message;
        private String priority;
        private String region;
        private Map<String, Object> details;
        private LocalDateTime timestamp;
        
        public Alert(String type, String message, String priority, String region, 
                    Map<String, Object> details) {
            this.type = type;
            this.message = message;
            this.priority = priority;
            this.region = region;
            this.details = details;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getType() { return type; }
        public String getMessage() { return message; }
        public String getPriority() { return priority; }
        public String getRegion() { return region; }
        public Map<String, Object> getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    public static class DisasterEvent {
        private String id;
        private String type;
        private String region;
        private int severity;
        private LocalDateTime declarationTime;
        private LocalDateTime resolutionTime;
        private boolean resolved;
        private Map<String, Object> details;
        private Map<String, Object> resolutionDetails;
        
        public DisasterEvent(String id, String type, String region, int severity, 
                            LocalDateTime declarationTime, Map<String, Object> details) {
            this.id = id;
            this.type = type;
            this.region = region;
            this.severity = severity;
            this.declarationTime = declarationTime;
            this.details = details;
            this.resolved = false;
        }
        
        public void setResolved(boolean resolved) { this.resolved = resolved; }
        public void setResolutionTime(LocalDateTime time) { this.resolutionTime = time; }
        public void setResolutionDetails(Map<String, Object> details) { this.resolutionDetails = details; }
        
        public String getId() { return id; }
        public String getType() { return type; }
        public String getRegion() { return region; }
        public int getSeverity() { return severity; }
        public LocalDateTime getDeclarationTime() { return declarationTime; }
        public LocalDateTime getResolutionTime() { return resolutionTime; }
        public boolean isResolved() { return resolved; }
        public Map<String, Object> getDetails() { return details; }
        public Map<String, Object> getResolutionDetails() { return resolutionDetails; }
    }
} 