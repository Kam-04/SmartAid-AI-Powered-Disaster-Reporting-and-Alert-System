package com.example.disastermanagement.modules;

import android.content.Intent;
import android.graphics.Color;
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
import com.example.disastermanagement.models.FloodPrediction;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FloodAnalysis extends AppCompatActivity {

    private static final String TAG = "FloodAnalysis";
    private boolean isOfflineMode = false;

    private Spinner stateSpinner;
    private Button btnPredict;
    private ProgressBar progressBar;
    private CardView predictionCard;
    private TextView tvNoData;
    private TextView tvPredictionResult, tvConfidence, tvTimeFrame, tvRiskLevel, tvRecommendations;
    private TextView tvOfflineMessage, tvDataSource;

    private LineChart lineChartHistorical, lineChartPrediction;
    private ApiService apiService;
    private List<String> states = new ArrayList<>();
    private String selectedState = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flood_analysis);

        Log.d(TAG, "Activity onCreate started");

        // Initialize all UI elements
        initializeViews();

        // Try to initialize API service - but don't break the app if it fails
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

        // Initialize line charts
        try {
            lineChartHistorical = findViewById(R.id.lineChartHistorical);
            lineChartPrediction = findViewById(R.id.lineChartPrediction);

            // Configure historical data chart
            if (lineChartHistorical != null) {
                configureLineChart(lineChartHistorical, "Historical Flood Data");
            }

            // Configure prediction chart
            if (lineChartPrediction != null) {
                configureLineChart(lineChartPrediction, "Predicted Flood Risk");
            }
        } catch (Exception e) {
            Log.d(TAG, "Charts not found in layout");
        }

        // Hide cards initially
        predictionCard.setVisibility(View.GONE);
        tvNoData.setVisibility(View.GONE);
    }

    private void configureLineChart(LineChart chart, String description) {
        try {
            // Basic chart configuration
            Description desc = new Description();
            desc.setText(description);
            chart.setDescription(desc);
            chart.setDrawGridBackground(false);
            chart.setTouchEnabled(true);
            chart.setDragEnabled(true);
            chart.setScaleEnabled(true);
            chart.setPinchZoom(true);
            chart.setNoDataText("Loading data...");
            chart.setNoDataTextColor(Color.BLACK);

            // Configure X axis
            XAxis xAxis = chart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format("%.0f", value);
                }
            });

            // Configure Y axis
            YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format("%.1f", value);
                }
            });

            YAxis rightAxis = chart.getAxisRight();
            rightAxis.setEnabled(false);

            // Add empty data to prevent "No Chart Data Available" message
            LineData emptyData = new LineData();
            chart.setData(emptyData);
            chart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error configuring chart: " + e.getMessage(), e);
        }
    }

    private void setupDefaultStates() {
        try {
            // Clear existing states to avoid duplicates
            states.clear();

            // Add default states (high flood risk states first)
            states.add("Select a State");
            states.add("Assam");
            states.add("Bihar");
            states.add("West Bengal");
            states.add("Uttar Pradesh");
            states.add("Odisha");
            states.add("Kerala");
            states.add("Maharashtra");
            states.add("Gujarat");
            states.add("Andhra Pradesh");
            states.add("Tamil Nadu");
            states.add("Karnataka");
            
            // Set up spinner with these states
            updateStateSpinner();
        } catch (Exception e) {
            Log.e(TAG, "Error in setupDefaultStates: " + e.getMessage(), e);
        }
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
        requestData.put("algorithm", "lstm"); // Using LSTM for better time series prediction

        apiService.predictFlood(requestData).enqueue(new Callback<FloodPrediction>() {
            @Override
            public void onResponse(Call<FloodPrediction> call, Response<FloodPrediction> response) {
                showProgress(false);

                if (response.isSuccessful() && response.body() != null) {
                    FloodPrediction prediction = response.body();
                    updatePredictionUI(prediction);
                } else {
                    Toast.makeText(FloodAnalysis.this,
                            "Failed to generate prediction",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FloodPrediction> call, Throwable t) {
                showProgress(false);
                Toast.makeText(FloodAnalysis.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePredictionUI(FloodPrediction prediction) {
        predictionCard.setVisibility(View.VISIBLE);

        // Update prediction result
        tvPredictionResult.setText(prediction.getRiskLevel() + " flood risk");
        tvConfidence.setText(String.format("Confidence: %.1f%%", prediction.getConfidence() * 100));
        tvTimeFrame.setText("Time Frame: " + prediction.getTimeFrame());
        tvRiskLevel.setText("Risk Level: " + prediction.getRiskLevel());
        
        // Format recommendations as a single string
        StringBuilder recommendationsText = new StringBuilder();
        if (prediction.getRecommendations() != null) {
            for (String rec : prediction.getRecommendations()) {
                recommendationsText.append("• ").append(rec).append("\n");
            }
        }
        tvRecommendations.setText(recommendationsText.toString().trim());
        
        tvDataSource.setText("Data Source: " + prediction.getDataSource());

        // Update historical data chart
        if (lineChartHistorical != null && prediction.getHistoricalWaterLevels() != null) {
            updateHistoricalChart(prediction.getHistoricalWaterLevels());
        }

        // Update prediction chart
        if (lineChartPrediction != null && prediction.getPredictedWaterLevels() != null) {
            updatePredictionChart(prediction.getPredictedWaterLevels());
        }
    }

    private void updateHistoricalChart(Map<String, Double> historicalData) {
        try {
            if (lineChartHistorical == null || historicalData == null || historicalData.isEmpty()) {
                Log.e(TAG, "Cannot update historical chart: chart or data is null");
                return;
            }

            List<Entry> entries = new ArrayList<>();
            int index = 0;
            for (Map.Entry<String, Double> entry : historicalData.entrySet()) {
                entries.add(new Entry(index++, entry.getValue().floatValue()));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Historical Water Level");
            dataSet.setColor(Color.BLUE);
            dataSet.setCircleColor(Color.BLUE);
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(4f);
            dataSet.setDrawValues(false);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            LineData data = new LineData(dataSet);
            lineChartHistorical.setData(data);
            lineChartHistorical.invalidate();
            lineChartHistorical.animateY(1000);
        } catch (Exception e) {
            Log.e(TAG, "Error updating historical chart: " + e.getMessage(), e);
        }
    }

    private void updatePredictionChart(Map<String, Double> predictionData) {
        try {
            if (lineChartPrediction == null || predictionData == null || predictionData.isEmpty()) {
                Log.e(TAG, "Cannot update prediction chart: chart or data is null");
                return;
            }

            List<Entry> entries = new ArrayList<>();
            int index = 0;
            for (Map.Entry<String, Double> entry : predictionData.entrySet()) {
                entries.add(new Entry(index++, entry.getValue().floatValue()));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Predicted Water Level");
            dataSet.setColor(Color.RED);
            dataSet.setCircleColor(Color.RED);
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(4f);
            dataSet.setDrawValues(false);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            LineData data = new LineData(dataSet);
            lineChartPrediction.setData(data);
            lineChartPrediction.invalidate();
            lineChartPrediction.animateY(1000);
        } catch (Exception e) {
            Log.e(TAG, "Error updating prediction chart: " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnPredict.setEnabled(!show);
    }
} 