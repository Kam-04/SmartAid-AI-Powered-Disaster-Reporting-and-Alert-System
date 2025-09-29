package com.example.disastermanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.disastermanagement.api.ConnectionChecker;
import com.example.disastermanagement.modules.AIAnalysis;
import com.example.disastermanagement.modules.CycloneAnalysis;
import com.example.disastermanagement.modules.DisasterReporting;
import com.example.disastermanagement.modules.DisasterRouting;
import com.example.disastermanagement.modules.EarthquakeAnalysis;
import com.example.disastermanagement.modules.EmergencyResponse;
import com.example.disastermanagement.modules.FloodAnalysis;
import com.example.disastermanagement.modules.MapsActivity;
import com.example.disastermanagement.modules.MonitoringDashboard;
import com.example.disastermanagement.modules.RescueInstructions;
import com.example.disastermanagement.modules.SOSLocationActivity;
import com.google.android.material.navigation.NavigationView;



public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private CardView cardReporting;
    private CardView cardEmergency;
    private CardView cardMonitoring;
    private CardView cardAnalysis;
    private CardView cardEarthquake;
    private CardView cardFlood;
    private CardView cardCyclone;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private RecyclerView newsRecyclerView;
    private ProgressBar progressBar;

    private Button btnGoToReporting;
    private Button btnGoEmergencySOS;

    private static final String API_KEY = "d00ddca93b1940069f071fd47e213549";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView.setNavigationItemSelectedListener(this);
        
        // Configure navigation view to display expandable items
        configureNavigationDrawer();

        initializeViews();
        setupButtonListeners();
        checkBackendConnection();
    }
    
    private void configureNavigationDrawer() {
        // Get analysis menu item
        MenuItem analysisMenuItem = navigationView.getMenu().findItem(R.id.nav_analysis);
        
        // Check if we should expand the disaster analysis submenu
        boolean shouldExpand = getIntent().getBooleanExtra("from_analysis", false);
        
        if (shouldExpand && analysisMenuItem != null) {
            // Make sure analysis menu is visible
            analysisMenuItem.setVisible(true);
            
            // Programmatically expand submenu
            navigationView.setItemIconTintList(null); // Enable icons
            Menu menu = navigationView.getMenu();
            
            // Navigate through the menu and find the Analysis item 
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getItemId() == R.id.nav_analysis) {
                    // Submenu is automatically displayed in Material components
                    SubMenu subMenu = item.getSubMenu();
                    if (subMenu != null) {
                        for (int j = 0; j < subMenu.size(); j++) {
                            subMenu.getItem(j).setVisible(true);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void initializeViews() {
        // Un-comment these lines to initialize the CardViews
        cardReporting = findViewById(R.id.cardRecentDisaster);

        btnGoToReporting = findViewById(R.id.disasterReporting);

        btnGoEmergencySOS = findViewById(R.id.btnEmergencySOS);

    }


    private void setupButtonListeners() {
        btnGoToReporting.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DisasterReporting.class))
        );

        btnGoEmergencySOS.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, EmergencyResponse.class))
        );

        // Set up click listeners for ML analysis cards if they exist
        if (cardEarthquake != null) {
            cardEarthquake.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, EarthquakeAnalysis.class);
                intent.putExtra("from_analysis", true);
                startActivity(intent);
            });
        }
        
        if (cardFlood != null) {
            cardFlood.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, FloodAnalysis.class);
                intent.putExtra("from_analysis", true);
                startActivity(intent);
            });
        }
        
        if (cardCyclone != null) {
            cardCyclone.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CycloneAnalysis.class);
                intent.putExtra("from_analysis", true);
                startActivity(intent);
            });
        }
    }


    public void onEmergencyStepsClick(View view) {
        Intent intent = new Intent(this, EmergencyStepsActivity.class);
        startActivity(intent);
    }

    private void checkBackendConnection() {
        ConnectionChecker.checkConnection(this, new ConnectionChecker.ConnectionListener() {
            @Override
            public void onConnected() {
                Toast.makeText(MainActivity.this, getString(R.string.connection_success), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectionFailed(String error) {
                Toast.makeText(MainActivity.this, getString(R.string.connection_failed, error), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void manualCheckConnection(View view) {
        Toast.makeText(this, getString(R.string.checking_connection), Toast.LENGTH_SHORT).show();
        checkBackendConnection();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;
        
        switch (id) {
            case R.id.nav_home:
                // Already on home screen
                break;
            case R.id.nav_reporting:
                intent = new Intent(this, DisasterReporting.class);
                break;
            case R.id.disasterReporting:
                intent = new Intent(this, EmergencyResponse.class);
                break;
            case R.id.nav_maps:
                intent = new Intent(this, MapsActivity.class);
                break;
            case R.id.nav_earthquake_analysis:
                intent = new Intent(this, EarthquakeAnalysis.class);
                intent.putExtra("from_analysis", true);
                break;
            case R.id.nav_flood_analysis:
                intent = new Intent(this, FloodAnalysis.class);
                intent.putExtra("from_analysis", true);
                break;
            case R.id.nav_rescue_instructions:
                intent = new Intent(this, RescueInstructions.class);
                break;
            case R.id.nav_sos:
                intent = new Intent(this, EmergencyResponse.class);
                break;
            case R.id.nav_monitoring:
                intent = new Intent(this, DisasterRouting.class);
                break;
        }
        
        if (intent != null) {
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
