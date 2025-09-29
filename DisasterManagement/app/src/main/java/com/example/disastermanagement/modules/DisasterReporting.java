package com.example.disastermanagement.modules;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.disastermanagement.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.example.disastermanagement.api.ApiClient;
import com.example.disastermanagement.api.ApiService;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.util.Log;

public class DisasterReporting extends AppCompatActivity {

    private MultiAutoCompleteTextView multiDisasterName;
    private TextView textCurrentLocation;
    private EditText editDescription, editAffectedArea, editObservations;
    private Button btnUploadPhoto, btnUploadVideo, btnSubmitReport;

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_VIDEO_PICK = 3;

    private FusedLocationProviderClient fusedLocationClient;
    private Uri photoUri, videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disaster_reporting);

        // Initialize views
        multiDisasterName = findViewById(R.id.multiDisasterName);
        textCurrentLocation = findViewById(R.id.textCurrentLocation);
        editDescription = findViewById(R.id.editDescription);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);

        // Set up disaster names
        String[] disasters = {"Flood" ,"Earthquake"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, disasters);
        multiDisasterName.setAdapter(adapter);
        multiDisasterName.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        // Set threshold to show the dropdown immediately when clicked
        multiDisasterName.setThreshold(0);
        multiDisasterName.setOnClickListener(v -> multiDisasterName.showDropDown());
        multiDisasterName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) multiDisasterName.showDropDown();
        });

        // Add X icon to remove the selected text
        Drawable crossDrawable = getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel);
        crossDrawable.setBounds(0, 0, crossDrawable.getIntrinsicWidth(), crossDrawable.getIntrinsicHeight());
        multiDisasterName.setCompoundDrawables(null, null, crossDrawable, null);

        // Handle clicking the X icon to clear the text
        multiDisasterName.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getX() >= (multiDisasterName.getWidth() - multiDisasterName.getCompoundPaddingRight())) {
                    multiDisasterName.setText(""); // Clear the text when X is clicked
                    return true;
                }
            }
            return false;
        });

        // Location setup
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fetchCurrentLocation();

        // Upload Photo button
        btnUploadPhoto.setOnClickListener(v -> {
            Intent photoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoIntent.setType("image/*");
            startActivityForResult(photoIntent, REQUEST_IMAGE_PICK);
        });

        // Upload Video button
        btnUploadVideo.setOnClickListener(v -> {
            Intent videoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            videoIntent.setType("video/*");
            startActivityForResult(videoIntent, REQUEST_VIDEO_PICK);
        });

        // Submit Report button
        btnSubmitReport.setOnClickListener(v -> submitReport());
    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    String locationStr = "Lat: " + location.getLatitude()
                            + ", Lon: " + location.getLongitude();
                    textCurrentLocation.setText(locationStr);
                } else {
                    textCurrentLocation.setText("Unable to fetch location.");
                }
            }
        });
    }

    private void submitReport() {
        String disasterNames = multiDisasterName.getText().toString().trim();
        String location = textCurrentLocation.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        // Validation
        if (disasterNames.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        View progressOverlay = findViewById(R.id.progress_overlay);
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Submitting report...", Toast.LENGTH_SHORT).show();
        }

        try {
            // Parse location string "Lat: 12.34, Lon: 56.78" into coordinates
            double latitude = 0.0;
            double longitude = 0.0;
            
            if (location.contains("Lat:") && location.contains("Lon:")) {
                String[] parts = location.split(",");
                latitude = Double.parseDouble(parts[0].replace("Lat:", "").trim());
                longitude = Double.parseDouble(parts[1].replace("Lon:", "").trim());
            }

            // Create report object
            JsonObject reportJson = new JsonObject();
            reportJson.addProperty("disaster_type", disasterNames.toLowerCase());
            reportJson.addProperty("description", description);
            
            // Add location as an object with lat and lng properties
            JsonObject locationJson = new JsonObject();
            locationJson.addProperty("lat", latitude);
            locationJson.addProperty("lng", longitude);
            reportJson.add("location", locationJson);
            
            // Add user ID (in a real app, get from user session)
            reportJson.addProperty("user_id", "user123");
            
            // Add severity (could be from a dropdown in the future)
            reportJson.addProperty("severity", "medium");
            
            // Check if we have media to upload
            if (photoUri != null) {
                reportJson.addProperty("has_image", true);
            }
            
            if (videoUri != null) {
                reportJson.addProperty("has_video", true);
            }

            // Get API service
            ApiService apiService = ApiClient.getApiService(this);
            
            // Make API call
            Call<JsonObject> call = apiService.createDisasterReport(reportJson);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject result = response.body();
                        
                        if (result.has("success") && result.get("success").getAsBoolean()) {
                            String reportId = result.get("report_id").getAsString();
                            Toast.makeText(DisasterReporting.this, "Report submitted successfully!", Toast.LENGTH_LONG).show();
                            
                            // Upload media if available
                            if (photoUri != null || videoUri != null) {
                                uploadMedia(reportId);
                            } else {
                                finish(); // Close the form
                            }
                        } else {
                            Toast.makeText(DisasterReporting.this, "Error: " + result.get("message").getAsString(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(DisasterReporting.this, "Failed to submit report. Please try again.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    Toast.makeText(DisasterReporting.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            
        } catch (Exception e) {
            if (progressOverlay != null) {
                progressOverlay.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void uploadMedia(String reportId) {
        // Show progress
        View progressOverlay = findViewById(R.id.progress_overlay);
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        }
        
        Toast.makeText(this, "Uploading media files...", Toast.LENGTH_SHORT).show();
        
        // Create a thread to handle the uploads
        new Thread(() -> {
            boolean success = true;
            String errorMessage = "";
            
            try {
                // Upload photo if available
                if (photoUri != null) {
                    success = uploadFile(reportId, photoUri, "image");
                    if (!success) {
                        errorMessage = "Failed to upload photo";
                        throw new Exception(errorMessage);
                    }
                }
                
                // Upload video if available
                if (videoUri != null) {
                    success = uploadFile(reportId, videoUri, "video");
                    if (!success) {
                        errorMessage = "Failed to upload video";
                        throw new Exception(errorMessage);
                    }
                }
                
                // Update UI on success
                runOnUiThread(() -> {
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    Toast.makeText(DisasterReporting.this, "Media uploaded successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after successful upload
                });
                
            } catch (Exception e) {
                final String finalErrorMessage = errorMessage.isEmpty() ? e.getMessage() : errorMessage;
                
                // Update UI on failure
                runOnUiThread(() -> {
                    if (progressOverlay != null) {
                        progressOverlay.setVisibility(View.GONE);
                    }
                    Toast.makeText(DisasterReporting.this, "Error: " + finalErrorMessage, Toast.LENGTH_LONG).show();
                    // Still finish activity since the report was created
                    finish();
                });
            }
        }).start();
    }
    
    private boolean uploadFile(String reportId, Uri fileUri, String mediaType) throws IOException {
        // Convert content URI to file
        File file = getFileFromUri(fileUri);
        if (file == null) {
            return false;
        }
        
        // Create RequestBody from file
        RequestBody requestFile = RequestBody.create(
                MediaType.parse(getContentResolver().getType(fileUri)), 
                file);
                
        // Create MultipartBody.Part
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "file", 
                file.getName(), 
                requestFile);
                
        // Create RequestBody for media type
        RequestBody typeBody = RequestBody.create(
                MediaType.parse("text/plain"), 
                mediaType);
                
        // Get API service
        ApiService apiService = ApiClient.getApiService(this);
        
        // Make synchronous API call (we're already in a background thread)
        try {
            retrofit2.Response<JsonObject> response = apiService
                    .uploadReportMedia(reportId, filePart, typeBody)
                    .execute();
                    
            return response.isSuccessful();
        } catch (Exception e) {
            Log.e("DisasterReporting", "Error uploading file: " + e.getMessage());
            return false;
        }
    }
    
    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }
            
            // Create a temp file
            String fileName = getFileName(uri);
            File tempFile = File.createTempFile(
                    fileName.substring(0, fileName.lastIndexOf('.')),
                    "." + fileName.substring(fileName.lastIndexOf('.') + 1),
                    getCacheDir()
            );
            
            // Copy input stream to the temp file
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.close();
            inputStream.close();
            
            return tempFile;
            
        } catch (Exception e) {
            Log.e("DisasterReporting", "Error getting file from URI: " + e.getMessage());
            return null;
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("DisasterReporting", "Error getting filename: " + e.getMessage());
            }
        }
        
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                textCurrentLocation.setText("Permission denied. Can't fetch location.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (requestCode == REQUEST_IMAGE_PICK) {
                photoUri = selectedUri;
                Toast.makeText(this, "Photo selected!", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_VIDEO_PICK) {
                videoUri = selectedUri;
                Toast.makeText(this, "Video selected!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
