package com.example.disastermanagement.api;

import com.example.disastermanagement.models.DashboardResponse;
import com.example.disastermanagement.models.DisasterData;
import com.example.disastermanagement.models.EarthquakeData;
import com.example.disastermanagement.models.EarthquakePrediction;
import com.example.disastermanagement.models.FloodPrediction;
import com.example.disastermanagement.models.CyclonePrediction;
import com.example.disastermanagement.models.HistoricalEarthquakeResponse;
import com.example.disastermanagement.models.UserLocation;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Part;
import retrofit2.http.PartMap;

public interface ApiService {

    // Basic connectivity test endpoint - using ResponseBody to handle any response format
    @GET("/")
    Call<ResponseBody> getApiStatus();

    // Status endpoint with detailed information
    @GET("/api/status")
    Call<Map<String, Object>> getDetailedStatus();

    // AI Analysis Module APIs - ML Prediction Endpoints
    @POST("/api/analysis/predict/flood")
    Call<FloodPrediction> predictFlood(@Body Map<String, Object> data);

    @POST("/api/analysis/predict/earthquake")
    Call<EarthquakePrediction> predictEarthquake(@Body Map<String, Object> data);

    @POST("/api/analysis/predict/cyclone")
    Call<CyclonePrediction> predictCyclone(@Body Map<String, Object> data);

    // New dashboard endpoint for all predictions
    @GET("/api/analysis/predictions/dashboard")
    Call<Map<String, Object>> getPredictionsDashboard();

    // Historical data endpoints
    @GET("/api/analysis/historical/flood")
    Call<Map<String, Object>> getHistoricalFloodData(@Query("state") String state);

    @GET("/api/analysis/historical/earthquake")
    Call<HistoricalEarthquakeResponse> getHistoricalEarthquakeData(@Query("state") String state);

    // Monitoring Module APIs
    @GET("/api/monitoring/dashboard")
    Call<DashboardResponse> getDashboardData();

    @GET("/api/monitoring/map")
    Call<Map<String, Object>> getMapData(@Query("type") String disasterType, @Query("state") String state);

    @GET("/api/monitoring/weather")
    Call<Map<String, Object>> getWeatherData(@Query("lat") double latitude, @Query("lon") double longitude);

    @GET("/api/monitoring/river-levels")
    Call<Map<String, Object>> getRiverLevels(@Query("state") String state);

    @GET("/api/monitoring/seismic-activity")
    Call<Map<String, Object>> getSeismicActivity(@Query("days") int days);

    // Emergency Module APIs
    @POST("/api/emergency/sos")
    Call<Map<String, Object>> createSosAlert(@Body Map<String, Object> sosData);

    @GET("/api/emergency/sos/{sosId}")
    Call<Map<String, Object>> getSosAlert(@Path("sosId") String sosId);

    @GET("/api/emergency/sos")
    Call<Map<String, Object>> listSosAlerts(@Query("status") String status, @Query("disaster_type") String disasterType);

    @GET("/api/emergency/evacuation-centers")
    Call<Map<String, Object>> getEvacuationCenters(@Query("state") String state);

    @GET("/api/emergency/emergency-contacts")
    Call<Map<String, Object>> getEmergencyContacts();

    @GET("/api/emergency/teams")
    Call<Map<String, Object>> getEmergencyTeams(@Query("status") String status, @Query("type") String teamType, @Query("state") String state);

    @GET("/api/emergency/safety-guidelines")
    Call<Map<String, Object>> getSafetyGuidelines(@Query("type") String disasterType);

    // Reporting Module APIs
    @POST("/api/reporting/report")
    Call<JsonObject> createDisasterReport(@Body JsonObject reportData);

    @Multipart
    @POST("/api/reporting/report/{reportId}/media")
    Call<JsonObject> uploadReportMedia(
            @Path("reportId") String reportId,
            @Part MultipartBody.Part media,
            @Part("type") RequestBody mediaType
    );

    @GET("/api/reporting/report/{reportId}")
    Call<Map<String, Object>> getDisasterReport(@Path("reportId") String reportId);

    @GET("/api/reporting/reports")
    Call<Map<String, Object>> listDisasterReports(@Query("status") String status, @Query("disaster_type") String disasterType, @Query("verified") Boolean verified);

    @POST("/api/reporting/report/{reportId}/verify")
    Call<Map<String, Object>> verifyDisasterReport(@Path("reportId") String reportId, @Body Map<String, Object> verificationData);

    @POST("/api/reporting/report/{reportId}/status")
    Call<Map<String, Object>> updateReportStatus(@Path("reportId") String reportId, @Body Map<String, Object> statusData);

    @GET("/api/reporting/statistics")
    Call<Map<String, Object>> getReportingStatistics(@Query("start_date") String startDate, @Query("end_date") String endDate);

}