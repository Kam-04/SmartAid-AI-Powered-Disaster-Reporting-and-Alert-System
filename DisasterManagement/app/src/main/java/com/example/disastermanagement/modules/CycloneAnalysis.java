package com.example.disastermanagement.modules;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.disastermanagement.MainActivity;
import com.example.disastermanagement.R;
import com.example.disastermanagement.api.ApiClient;
import com.example.disastermanagement.api.ApiService;
import com.example.disastermanagement.models.CyclonePrediction;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CycloneAnalysis extends AppCompatActivity {

    private static final String TAG = "CycloneAnalysis";
    private boolean isOfflineMode = false;

    private Spinner stateSpinner;
    private Button btnPredict;
    private ProgressBar progressBar;
    private CardView predictionCard;
    private TextView tvNoData;
    private TextView tvPredictionResult, tvConfidence, tvTimeFrame, tvRiskLevel, tvRecommendations;
    private TextView tvWindSpeed, tvPressure, tvCycloneClass, tvOfflineMessage, tvDataSource;
    private TextView tvSeasonStatus;

    private PieChart pieChartAffectedAreas;
    // Commented out since not defined in layout
    // private BarChart barChartSeverity;

    private ApiService apiService;
    private List<String> states = new ArrayList<>();
    private String selectedState = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cyclone_analysis);

        Log.d(TAG, "Activity onCreate started");

        // Initialize all UI elements
        initializeViews();

        // Try to initialize API service
        try {
            ApiClient.resetApiClient(); // Reset client to ensure fresh connection
            apiService = ApiClient.getApiService(this);
            isOfflineMode = false;
            Log.d(TAG, "API Service initialized successfully");
            tvOfflineMessage.setVisibility(View.GONE);
        } catch (Exception e) {
            // Handle network initialization error gracefully
            Log.e(TAG, "Error initializing API Service: " + e.getMessage(), e);
            Toast.makeText(this, "Running in offline mode - backend server not available", Toast.LENGTH_LONG).show();
            isOfflineMode = true;
            apiService = null;
            tvOfflineMessage.setVisibility(View.VISIBLE);
        }

        // Set up listeners for buttons and spinner
        setupListeners();

        // Initialize states with default data
        setupDefaultStates();
    }

    private void initializeViews() {
        stateSpinner = findViewById(R.id.spinnerState);
        btnPredict = findViewById(R.id.btnPredict);
        progressBar = findViewById(R.id.progressBar);
        predictionCard = findViewById(R.id.cardPrediction);
        tvNoData = findViewById(R.id.tvNoData);
        tvPredictionResult = findViewById(R.id.tvPredictionResult);
        tvConfidence = findViewById(R.id.tvConfidence);
        tvTimeFrame = findViewById(R.id.tvTimeFrame);
        tvRiskLevel = findViewById(R.id.tvRiskLevel);
        tvRecommendations = findViewById(R.id.tvRecommendations);
        tvWindSpeed = findViewById(R.id.tvWindSpeed);
        tvPressure = findViewById(R.id.tvPressure);
        tvCycloneClass = findViewById(R.id.tvCycloneClass);
        tvSeasonStatus = findViewById(R.id.tvSeasonStatus);
        tvDataSource = findViewById(R.id.tvDataSource);

        // Add offline message TextView
        try {
            tvOfflineMessage = findViewById(R.id.tvOfflineMessage);
        } catch (Exception e) {
            Log.d(TAG, "Offline message TextView not found in layout");
            tvOfflineMessage = new TextView(this);
            tvOfflineMessage.setText("Offline Mode - Backend server not available");
            tvOfflineMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvOfflineMessage.setPadding(16, 16, 16, 16);
            tvOfflineMessage.setVisibility(View.GONE);
        }

        // Get charts if they exist
        try {
            pieChartAffectedAreas = findViewById(R.id.pieChartAffectedAreas);

            // Initialize charts with proper settings to ensure visibility
            if (pieChartAffectedAreas != null) {
                pieChartAffectedAreas.setDescription(null);
                pieChartAffectedAreas.setUsePercentValues(true);
                pieChartAffectedAreas.setDrawHoleEnabled(true);
                pieChartAffectedAreas.setHoleRadius(35f);
                pieChartAffectedAreas.setTransparentCircleRadius(40f);
                pieChartAffectedAreas.setRotationEnabled(true);
                pieChartAffectedAreas.setHighlightPerTapEnabled(true);
            }
        } catch (Exception e) {
            Log.d(TAG, "Charts not found in layout");
        }

        // Hide cards initially
        predictionCard.setVisibility(View.GONE);
        tvNoData.setVisibility(View.GONE);
    }

    private void setupDefaultStates() {
        // Clear existing states to avoid duplicates
        states.clear();

        // Add default states (cyclone-prone coastal states first)
        states.add("Select a State");
        states.add("Odisha");
        states.add("West Bengal");
        states.add("Andhra Pradesh");
        states.add("Tamil Nadu");
        states.add("Gujarat");
        states.add("Kerala");
        states.add("Maharashtra");
        states.add("Karnataka");
        states.add("Goa");
        
        // Set up spinner with these states
        updateStateSpinner();
    }

    private void updateStateSpinner() {
        // Create and set adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                states);
        stateSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        btnPredict.setOnClickListener(v -> {
            if (isOfflineMode) {
                showOfflineModeMessage();
            } else if (selectedState.equals("Select a State")) {
                Toast.makeText(this, "Please select a state first", Toast.LENGTH_SHORT).show();
            } else {
                generatePrediction();
            }
        });

        stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedState = states.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedState = "Select a State";
            }
        });
    }

    private void showOfflineModeMessage() {
        Toast.makeText(this,
                "This feature requires connection to the backend server. Currently in offline mode.",
                Toast.LENGTH_LONG).show();
    }

    private void generatePrediction() {
        if (apiService == null) {
            Toast.makeText(this, "Cannot connect to server", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        predictionCard.setVisibility(View.GONE);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("state", selectedState);

        apiService.predictCyclone(requestData).enqueue(new Callback<CyclonePrediction>() {
            @Override
            public void onResponse(Call<CyclonePrediction> call, Response<CyclonePrediction> response) {
                showProgress(false);

                if (response.isSuccessful() && response.body() != null) {
                    CyclonePrediction prediction = response.body();

                    if (prediction.isSuccess()) {
                        // Update UI with prediction
                        updatePredictionUI(prediction);
                    } else {
                        Toast.makeText(CycloneAnalysis.this,
                                "Server could not generate prediction",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CycloneAnalysis.this,
                            "Failed to generate prediction",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CyclonePrediction> call, Throwable t) {
                showProgress(false);
                Toast.makeText(CycloneAnalysis.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePredictionUI(CyclonePrediction prediction) {
        predictionCard.setVisibility(View.VISIBLE);

        // Format probability as percentage
        int probabilityPercent = (int) (prediction.getProbability() * 100);

        // Set prediction summary
        tvPredictionResult.setText(String.format("Probability of cyclone: %d%%", probabilityPercent));
        
        // Set cyclone meteorological data
        tvWindSpeed.setText(String.format("Wind Speed: %.1f km/h", prediction.getWindSpeed()));
        tvPressure.setText(String.format("Pressure: %.1f hPa", prediction.getPressure()));
        tvCycloneClass.setText(String.format("Classification: %s", prediction.getCycloneClass()));
        
        // Set season status
        tvSeasonStatus.setText(prediction.isCycloneSeason() ? 
                "Currently in cyclone season (high risk period)" :
                "Currently outside cyclone season (low risk period)");
        tvSeasonStatus.setTextColor(prediction.isCycloneSeason() ? 
                getResources().getColor(android.R.color.holo_red_dark) :
                getResources().getColor(android.R.color.holo_blue_dark));

        // Set confidence
        tvConfidence.setText(String.format("ML Model Confidence: %d%%",
                (int) (0.7 * 100))); // ML models use 0.7 confidence for now

        // Set time frame
        tvTimeFrame.setText(String.format("Time Frame: %s", prediction.getTimeFrame()));

        // Set risk level with appropriate color
        tvRiskLevel.setText(String.format("Risk Level: %s", prediction.getRiskLevel()));

        // Change risk level text color based on severity
        switch (prediction.getRiskLevel()) {
            case "Low":
                tvRiskLevel.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "Medium":
                tvRiskLevel.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "High":
                tvRiskLevel.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                break;
            case "Very High":
            case "Extreme":
                tvRiskLevel.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                break;
        }

        // Set recommendations
        if (prediction.getRecommendations() != null && prediction.getRecommendations().length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Based on ML model analysis:\n\n");
            for (String recommendation : prediction.getRecommendations()) {
                sb.append("• ").append(recommendation).append("\n");
            }
            tvRecommendations.setText(sb.toString().trim());
        } else {
            tvRecommendations.setText("No specific recommendations available.");
        }
        
        // If we have a data source field, display it
        if (prediction.getDataSource() != null) {
            tvDataSource.setText("Source: " + prediction.getDataSource());
            tvDataSource.setVisibility(View.VISIBLE);
        } else {
            tvDataSource.setVisibility(View.GONE);
        }
        
        // Update charts if they exist
        try {
            if (pieChartAffectedAreas != null && prediction.getAffectedAreas() != null) {
                updateAffectedAreasChart(prediction.getAffectedAreas());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating charts: " + e.getMessage(), e);
        }
    }
    
    private void updateAffectedAreasChart(Map<String, Double> affectedAreas) {
        if (pieChartAffectedAreas == null || affectedAreas == null || affectedAreas.isEmpty()) {
            return;
        }
        
        try {
            ArrayList<PieEntry> entries = new ArrayList<>();
            ArrayList<Integer> colors = new ArrayList<>();
            
            // Add entries for the chart
            for (Map.Entry<String, Double> area : affectedAreas.entrySet()) {
                if (area.getValue() > 0) {
                    entries.add(new PieEntry(area.getValue().floatValue(), area.getKey()));
                }
            }
            
            // Generate colors - using vibrant colors for cyclone impacted areas
            int[] colorArray = {
                getResources().getColor(android.R.color.holo_purple),
                getResources().getColor(android.R.color.holo_blue_dark),
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_red_dark),
                getResources().getColor(android.R.color.holo_orange_dark)
            };
            
            for (int i = 0; i < entries.size(); i++) {
                colors.add(colorArray[i % colorArray.length]);
            }
            
            PieDataSet dataSet = new PieDataSet(entries, "Affected Coastal Areas");
            dataSet.setColors(colors);
            dataSet.setSliceSpace(2f);
            dataSet.setValueTextSize(12f);
            
            PieData data = new PieData(dataSet);
            data.setValueFormatter(new PercentFormatter(pieChartAffectedAreas));
            
            pieChartAffectedAreas.setData(data);
            pieChartAffectedAreas.highlightValues(null);
            pieChartAffectedAreas.invalidate(); // Force redraw
            
            // Make sure chart is visible
            pieChartAffectedAreas.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error creating pie chart: " + e.getMessage(), e);
            pieChartAffectedAreas.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("from_analysis", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
} 