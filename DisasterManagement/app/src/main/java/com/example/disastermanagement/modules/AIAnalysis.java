package com.example.disastermanagement.modules;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.disastermanagement.R;
import com.google.android.material.textfield.TextInputEditText;

public class AIAnalysis extends AppCompatActivity {
    
    private RadioGroup radioGroupDisasterType;
    private RadioButton radioFlood, radioEarthquake;
    private LinearLayout layoutFloodInputs, layoutEarthquakeInputs;
    private Button buttonGetPrediction, buttonGetCurrentLocation;
    private CardView cardPredictionResults;
    private TextInputEditText editTextLatitude, editTextLongitude, editTextRainfall, editTextRiverLevel, 
                            editTextSeismicActivity, editTextFaultLineProximity;
    private TextView textProbability, textSeverity, textAreaImpact, textPopulationRisk, textRecommendations;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_analysis);
        
        // Initialize UI elements
        initUI();
        
        // Setup event handlers
        setupEventHandlers();
    }
    
    private void initUI() {
        radioGroupDisasterType = findViewById(R.id.radioGroupDisasterType);
        radioFlood = findViewById(R.id.radioFlood);
        radioEarthquake = findViewById(R.id.radioEarthquake);
        
        layoutFloodInputs = findViewById(R.id.layoutFloodInputs);
        layoutEarthquakeInputs = findViewById(R.id.layoutEarthquakeInputs);
        
        buttonGetPrediction = findViewById(R.id.buttonGetPrediction);
        buttonGetCurrentLocation = findViewById(R.id.buttonGetCurrentLocation);
        
        cardPredictionResults = findViewById(R.id.cardPredictionResults);
        
        editTextLatitude = findViewById(R.id.editTextLatitude);
        editTextLongitude = findViewById(R.id.editTextLongitude);
        editTextRainfall = findViewById(R.id.editTextRainfall);
        editTextRiverLevel = findViewById(R.id.editTextRiverLevel);
        editTextSeismicActivity = findViewById(R.id.editTextSeismicActivity);
        editTextFaultLineProximity = findViewById(R.id.editTextFaultLineProximity);
        
        textProbability = findViewById(R.id.textProbability);
        textSeverity = findViewById(R.id.textSeverity);
        textAreaImpact = findViewById(R.id.textAreaImpact);
        textPopulationRisk = findViewById(R.id.textPopulationRisk);
        textRecommendations = findViewById(R.id.textRecommendations);
    }
    
    private void setupEventHandlers() {
        radioGroupDisasterType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioFlood) {
                layoutFloodInputs.setVisibility(View.VISIBLE);
                layoutEarthquakeInputs.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioEarthquake) {
                layoutFloodInputs.setVisibility(View.GONE);
                layoutEarthquakeInputs.setVisibility(View.VISIBLE);
            }
        });
        
        buttonGetPrediction.setOnClickListener(v -> {
            // Simulate prediction
            simulatePrediction();
        });
        
        buttonGetCurrentLocation.setOnClickListener(v -> {
            // Simulate getting current location
            editTextLatitude.setText("28.6139");
            editTextLongitude.setText("77.2090");
        });
    }
    
    private void simulatePrediction() {
        // Show the prediction results card
        cardPredictionResults.setVisibility(View.VISIBLE);
        
        // Set simulated values
        textProbability.setText("75%");
        textSeverity.setText("High");
        textAreaImpact.setText("45.2 sq km");
        textPopulationRisk.setText("25,000");
        textRecommendations.setText("• Evacuate low-lying areas\n• Avoid flood waters\n• Monitor local alerts");
    }
} 