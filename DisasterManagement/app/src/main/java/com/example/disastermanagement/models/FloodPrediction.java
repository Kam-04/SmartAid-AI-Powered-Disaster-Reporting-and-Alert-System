package com.example.disastermanagement.models;

import java.util.List;
import java.util.Map;

/**
 * Model class to represent flood prediction data from ML model
 */
public class FloodPrediction {
    private String probability;
    private String predictedRainfall;
    private double confidence;
    private String timeFrame;
    private String riskLevel;
    private List<String> recommendations;
    private String dataSource;
    private Map<String, Double> historicalWaterLevels;
    private Map<String, Double> predictedWaterLevels;

    // Getters and Setters
    public String getProbability() {
        return probability;
    }

    public void setProbability(String probability) {
        this.probability = probability;
    }

    public String getPredictedRainfall() {
        return predictedRainfall;
    }

    public void setPredictedRainfall(String predictedRainfall) {
        this.predictedRainfall = predictedRainfall;
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

    public Map<String, Double> getHistoricalWaterLevels() {
        return historicalWaterLevels;
    }

    public void setHistoricalWaterLevels(Map<String, Double> historicalWaterLevels) {
        this.historicalWaterLevels = historicalWaterLevels;
    }

    public Map<String, Double> getPredictedWaterLevels() {
        return predictedWaterLevels;
    }

    public void setPredictedWaterLevels(Map<String, Double> predictedWaterLevels) {
        this.predictedWaterLevels = predictedWaterLevels;
    }
} 