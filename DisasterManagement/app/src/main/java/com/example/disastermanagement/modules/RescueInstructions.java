package com.example.disastermanagement.modules;



import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import com.example.disastermanagement.R;


public class RescueInstructions extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue);
    }

    // Handle phone call action
    public void makePhoneCall(View view) {
        String phoneNumber = "";

        // Check which icon is clicked and assign the respective phone number
        if (view.getId() == R.id.ambulanceIcon) {
            phoneNumber = "112"; // Ambulance Number
        } else if (view.getId() == R.id.policeIcon) {
            phoneNumber = "100"; // Police Number
        } else if (view.getId() == R.id.fireIcon) {
            phoneNumber = "101"; // Fire Department Number
        }

        // Create an intent to dial the number
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }
}
