package com.example.disastermanagement.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Model class for historical earthquake data API response
 */
public class HistoricalEarthquakeResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("state")
    private String state;

    @SerializedName("total_earthquakes")
    private int totalEarthquakes;

    @SerializedName("average_magnitude")
    private double averageMagnitude;

    @SerializedName("magnitude_distribution")
    private Map<String, Integer> magnitudeDistribution;

    @SerializedName("affected_areas")
    private List<String> affectedAreas;

    // Support both field names for backward compatibility
    @SerializedName(value = "earthquakes", alternate = {"data"})
    private List<EarthquakeData> earthquakes;

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getTotalEarthquakes() {
        return totalEarthquakes;
    }

    public void setTotalEarthquakes(int totalEarthquakes) {
        this.totalEarthquakes = totalEarthquakes;
    }

    public double getAverageMagnitude() {
        return averageMagnitude;
    }

    public void setAverageMagnitude(double averageMagnitude) {
        this.averageMagnitude = averageMagnitude;
    }

    public Map<String, Integer> getMagnitudeDistribution() {
        return magnitudeDistribution;
    }

    public void setMagnitudeDistribution(Map<String, Integer> magnitudeDistribution) {
        this.magnitudeDistribution = magnitudeDistribution;
    }

    public List<String> getAffectedAreas() {
        return affectedAreas;
    }

    public void setAffectedAreas(List<String> affectedAreas) {
        this.affectedAreas = affectedAreas;
    }

    public List<EarthquakeData> getEarthquakes() {
        return earthquakes;
    }

    public void setEarthquakes(List<EarthquakeData> earthquakes) {
        this.earthquakes = earthquakes;
    }
} 