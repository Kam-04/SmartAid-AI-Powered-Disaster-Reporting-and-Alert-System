package com.example.disastermanagement.models;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Model class to represent cyclone prediction data from ML model
 */
public class CyclonePrediction {
    @SerializedName("success")
    private boolean success;

    @SerializedName("probability")
    private double probability;

    @SerializedName("wind_speed")
    private double windSpeed;

    @SerializedName("pressure")
    private double pressure;

    @SerializedName("state")
    private String state;

    @SerializedName("cyclone_class")
    private String cycloneClass;

    @SerializedName("time_frame")
    private String timeFrame;

    @SerializedName("risk_level")
    private String riskLevel;

    @SerializedName("affected_areas")
    private Map<String, Double> affectedAreas;

    @SerializedName("recommendations")
    private String[] recommendations;

    @SerializedName("is_cyclone_season")
    private boolean isCycloneSeason;
    
    @SerializedName("data_source")
    private String dataSource;

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCycloneClass() {
        return cycloneClass;
    }

    public void setCycloneClass(String cycloneClass) {
        this.cycloneClass = cycloneClass;
    }

    public String getTimeFrame() {
        return timeFrame;
    }

    public void setTimeFrame(String timeFrame) {
        this.timeFrame = timeFrame;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Map<String, Double> getAffectedAreas() {
        return affectedAreas;
    }

    public void setAffectedAreas(Map<String, Double> affectedAreas) {
        this.affectedAreas = affectedAreas;
    }

    public String[] getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String[] recommendations) {
        this.recommendations = recommendations;
    }

    public boolean isCycloneSeason() {
        return isCycloneSeason;
    }

    public void setCycloneSeason(boolean cycloneSeason) {
        isCycloneSeason = cycloneSeason;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
} 