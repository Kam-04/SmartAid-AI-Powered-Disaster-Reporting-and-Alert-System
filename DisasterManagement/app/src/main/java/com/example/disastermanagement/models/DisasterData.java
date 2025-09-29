package com.example.disastermanagement.models;

import java.io.Serializable;
import java.util.List;

public class DisasterData implements Serializable {

    private String id;
    private String type; // "flood" or "earthquake"
    private UserLocation location;
    private String state;
    private String city;
    private String severity; // "High", "Medium", "Low"
    private String status;
    private String timestamp;
    
    // Common fields
    private double affectedRadius;
    private int affectedPopulation;
    
    // Flood specific fields
    private double waterLevel;
    private double rainfallMm;
    
    // Earthquake specific fields
    private double magnitude;
    private double depthKm;
    private int aftershocks;
    
    // For impact analysis
    private double probability;
    private String impactAnalysis;
    private List<String> mitigationRecommendations;

    public DisasterData() {
        // Required empty constructor
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UserLocation getLocation() {
        return location;
    }

    public void setLocation(UserLocation location) {
        this.location = location;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getAffectedRadius() {
        return affectedRadius;
    }

    public void setAffectedRadius(double affectedRadius) {
        this.affectedRadius = affectedRadius;
    }

    public int getAffectedPopulation() {
        return affectedPopulation;
    }

    public void setAffectedPopulation(int affectedPopulation) {
        this.affectedPopulation = affectedPopulation;
    }

    public double getWaterLevel() {
        return waterLevel;
    }

    public void setWaterLevel(double waterLevel) {
        this.waterLevel = waterLevel;
    }

    public double getRainfallMm() {
        return rainfallMm;
    }

    public void setRainfallMm(double rainfallMm) {
        this.rainfallMm = rainfallMm;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }

    public double getDepthKm() {
        return depthKm;
    }

    public void setDepthKm(double depthKm) {
        this.depthKm = depthKm;
    }

    public int getAftershocks() {
        return aftershocks;
    }

    public void setAftershocks(int aftershocks) {
        this.aftershocks = aftershocks;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public String getImpactAnalysis() {
        return impactAnalysis;
    }

    public void setImpactAnalysis(String impactAnalysis) {
        this.impactAnalysis = impactAnalysis;
    }

    public List<String> getMitigationRecommendations() {
        return mitigationRecommendations;
    }

    public void setMitigationRecommendations(List<String> mitigationRecommendations) {
        this.mitigationRecommendations = mitigationRecommendations;
    }
} 