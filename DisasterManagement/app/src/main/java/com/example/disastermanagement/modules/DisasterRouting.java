package com.example.disastermanagement.modules;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.example.disastermanagement.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;

public class DisasterRouting extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FloatingActionButton fabLocateMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disaster_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fabLocateMe = findViewById(R.id.fab_locate_me);
        fabLocateMe.setOnClickListener(v -> checkLocationPermissionAndShowLocation());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // First, ensure we have location permission
        checkLocationPermissionAndShowLocation();

        // Then add all disaster areas + nearby hospital/shelters
        addDisasterAndShelterMarkers();
    }

    private void checkLocationPermissionAndShowLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            showUserLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void showUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.addMarker(new MarkerOptions()
                                .position(userLatLng)
                                .title("You are here"));
                        // Zoom out to show all regions if desired:
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 5));
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                showUserLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addDisasterAndShelterMarkers() {
        // Define your disaster-prone areas:
        List<LatLng> proneAreas = Arrays.asList(
                new LatLng(26.2006, 92.9376),
                new LatLng(25.0961, 85.3131),
                new LatLng(30.0668, 79.0193),
                new LatLng(20.9517, 85.0985),
                new LatLng(22.2587, 71.1924),
                new LatLng(22.9868, 87.8550),
                new LatLng(11.7401, 92.6586),
                new LatLng(19.0760, 72.8777),
                new LatLng(13.0827, 80.2707),
                new LatLng(8.5241, 76.9366)
        );

        // Loop over each area:
        for (LatLng area : proneAreas) {

            mMap.addMarker(new MarkerOptions()
                    .position(area)
                    .title("Disaster Prone Area")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            LatLng hospital = new LatLng(area.latitude + 0.01, area.longitude + 0.01);
            mMap.addMarker(new MarkerOptions()
                    .position(hospital)
                    .title("Nearby Hospital")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            // 3️⃣ Place a "Shelter" marker just SW of the area:
            LatLng shelter = new LatLng(area.latitude - 0.01, area.longitude - 0.01);
            mMap.addMarker(new MarkerOptions()
                    .position(shelter)
                    .title("Nearby Shelter")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            mMap.addPolyline(new PolylineOptions()
                    .add(area, hospital)
                    .width(5)
                    .color(ContextCompat.getColor(this, R.color.teal_700)));

            mMap.addPolyline(new PolylineOptions()
                    .add(area, shelter)
                    .width(5)
                    .color(ContextCompat.getColor(this, R.color.purple_500)));
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(22.9734, 78.6569), 4.5f));
    }
}
