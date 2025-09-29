package com.example.disastermanagement.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class DashboardResponse {
    @SerializedName("recent_earthquakes")
    private List<EarthquakeData> recentEarthquakes;
    
    @SerializedName("active_alerts")
    private int activeAlerts;
    
    @SerializedName("affected_regions")
    private List<String> affectedRegions;
    
    @SerializedName("statistics")
    private Map<String, Integer> statistics;
    
    // Getters and Setters
    public List<EarthquakeData> getRecentEarthquakes() {
        return recentEarthquakes;
    }

    public void setRecentEarthquakes(List<EarthquakeData> recentEarthquakes) {
        this.recentEarthquakes = recentEarthquakes;
    }

    public int getActiveAlerts() {
        return activeAlerts;
    }

    public void setActiveAlerts(int activeAlerts) {
        this.activeAlerts = activeAlerts;
    }

    public List<String> getAffectedRegions() {
        return affectedRegions;
    }

    public void setAffectedRegions(List<String> affectedRegions) {
        this.affectedRegions = affectedRegions;
    }

    public Map<String, Integer> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Integer> statistics) {
        this.statistics = statistics;
    }
    
    // Helper methods to get specific statistics
    public int getTotalEarthquakes() {
        if (statistics != null && statistics.containsKey("total_earthquakes")) {
            return statistics.get("total_earthquakes");
        }
        return 0;
    }
    
    public int getReportsSubmitted() {
        if (statistics != null && statistics.containsKey("reports_submitted")) {
            return statistics.get("reports_submitted");
        }
        return 0;
    }
    
    public int getRegisteredUsers() {
        if (statistics != null && statistics.containsKey("registered_users")) {
            return statistics.get("registered_users");
        }
        return 0;
    }
} 