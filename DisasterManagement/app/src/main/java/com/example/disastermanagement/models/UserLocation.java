package com.example.disastermanagement.models;

import java.io.Serializable;

public class UserLocation implements Serializable {

    private double lat;
    private double lon;
    private String name;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;

    public UserLocation() {
        // Required empty constructor
    }

    public UserLocation(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    // Getters and Setters
    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (name != null && !name.isEmpty()) {
            sb.append(name);
        }
        
        if (address != null && !address.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address);
        }
        
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        
        if (state != null && !state.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state);
        }
        
        if (country != null && !country.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        
        if (pincode != null && !pincode.isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(pincode);
        }
        
        // If all fields are empty, return coordinates
        if (sb.length() == 0) {
            return String.format("Lat: %.6f, Lon: %.6f", lat, lon);
        }
        
        return sb.toString();
    }
} 