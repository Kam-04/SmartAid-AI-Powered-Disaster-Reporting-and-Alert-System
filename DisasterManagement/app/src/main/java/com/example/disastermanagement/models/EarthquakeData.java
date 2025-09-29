package com.example.disastermanagement.models;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class EarthquakeData {
    @SerializedName("_id")
    private String id;
    
    @SerializedName("magnitude")
    private double magnitude;
    
    @SerializedName("place")
    private String place;
    
    @SerializedName("time")
    private String originTime;
    
    @SerializedName("latitude")
    private double latitude;
    
    @SerializedName("longitude")
    private double longitude;
    
    @SerializedName("depth")
    private int depth;
    
    @SerializedName("magnitude_type")
    private String magnitudeType;
    
    @SerializedName("location")
    private String location;
    
    @SerializedName("region")
    private String region;
    
    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }

    public String getMagnitudeType() {
        return magnitudeType;
    }

    public void setMagnitudeType(String magnitudeType) {
        this.magnitudeType = magnitudeType;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getOriginTime() {
        return originTime;
    }

    public void setOriginTime(String originTime) {
        this.originTime = originTime;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
    
    @Override
    public String toString() {
        return "Earthquake: " + magnitude + magnitudeType + " at " + location;
    }
} 