package com.example.disastermanagement.models;

import java.util.List;
import java.util.Map;

/**
 * Model class to represent earthquake prediction data
 */
public class EarthquakePrediction {
    private String magnitude;
    private double confidence;
    private String timeFrame;
    private String riskLevel;
    private List<String> recommendations;
    private String dataSource;
    private Map<String, Double> historicalMagnitudes;
    private Map<String, Double> predictedMagnitudes;

    // Getters and Setters
    public String getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(String magnitude) {
        this.magnitude = magnitude;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
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

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public Map<String, Double> getHistoricalMagnitudes() {
        return historicalMagnitudes;
    }

    public void setHistoricalMagnitudes(Map<String, Double> historicalMagnitudes) {
        this.historicalMagnitudes = historicalMagnitudes;
    }

    public Map<String, Double> getPredictedMagnitudes() {
        return predictedMagnitudes;
    }

    public void setPredictedMagnitudes(Map<String, Double> predictedMagnitudes) {
        this.predictedMagnitudes = predictedMagnitudes;
    }
} 