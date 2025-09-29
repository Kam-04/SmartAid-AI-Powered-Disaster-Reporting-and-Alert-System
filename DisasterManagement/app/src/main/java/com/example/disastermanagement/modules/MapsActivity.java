package com.example.disastermanagement.modules;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.disastermanagement.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.Task;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Map fragment
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Show legend
        showLegend();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Draw zones
        addRiskZones();
    }

    private void enableMyLocation() {
        try {
            mMap.setMyLocationEnabled(true);
            Task<Location> task = fusedLocationClient.getLastLocation();
            task.addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng you = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(you, 10));
                    mMap.addMarker(new MarkerOptions()
                            .position(you)
                            .title("You are here"));
                } else {
                    Toast.makeText(this,
                            "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Location permission issue.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addRiskZones() {
        // Earthquake (Delhi)
        PolygonOptions quake = new PolygonOptions()
                .add(new LatLng(28.7041, 77.1025),
                        new LatLng(28.6,   77.3),
                        new LatLng(28.9,   77.3),
                        new LatLng(28.9,   77.0))
                .strokeColor(0xFFFF0000)
                .fillColor(0x44FF0000);
        mMap.addPolygon(quake);

        // Flood (Lucknow)
        mMap.addCircle(new CircleOptions()
                .center(new LatLng(26.8467, 80.9462))
                .radius(5000)
                .strokeColor(0xFF0000FF)
                .fillColor(0x440000FF));

        // Weather (Mumbai)
        LatLng mumbai = new LatLng(19.0760, 72.8777);
        mMap.addMarker(new MarkerOptions()
                .position(mumbai)
                .title("Weather: Heavy Rain")
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE)));
    }

    private void showLegend() {
        LinearLayout legend = findViewById(R.id.legendContainer);
        legend.setVisibility(View.VISIBLE);

        // TextViews (IDs now exist in XML)
        ((TextView) findViewById(R.id.earthquakeLegend))
                .setText("Earthquake Zone");
        ((TextView) findViewById(R.id.floodLegend))
                .setText("Flood Zone");
        ((TextView) findViewById(R.id.weatherLegend))
                .setText("Weather (Heavy Rain)");
        ((TextView) findViewById(R.id.locationLegend))
                .setText("You are here");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            Toast.makeText(this,
                    "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
