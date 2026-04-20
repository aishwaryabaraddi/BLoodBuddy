package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReceiverActivity extends AppCompatActivity {

    private static final String TAG = "ReceiverActivity";
    private EditText etName, etPhonenumber, etToWhomFor, etLocation;
    private Spinner spinnerDistrict, spinnerTaluk, spinnerBloodGroup;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receiver);

        db = FirebaseFirestore.getInstance();
        etName = findViewById(R.id.etName);
        etPhonenumber = findViewById(R.id.etPhonenumber);
        spinnerDistrict = findViewById(R.id.etDistrict);
        spinnerTaluk = findViewById(R.id.etTaluk);
        spinnerBloodGroup = findViewById(R.id.etBloodGroup);
        etToWhomFor = findViewById(R.id.etToWhomFor);
        etLocation = findViewById(R.id.etLocation);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupSpinners();
        requestLocationPermission();

        findViewById(R.id.imageViewBack).setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void setupSpinners() {
        List<String> districts = Arrays.asList("Select District", "Bagalkot", "Bangalore Rural", "Bangalore Urban", "Belgaum", "Bellary", "Bidar", "Bijapur", "Chamarajanagar", "Chikballapur", "Chikmagalur", "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Gulbarga", "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysore", "Raichur", "Ramanagara", "Shimoga", "Tumkur", "Udupi", "Uttara Kannada", "Yadgir");
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);

        List<String> bloodGroups = Arrays.asList("Select Blood Group", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-");
        ArrayAdapter<String> bloodGroupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        bloodGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodGroupAdapter);

        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTalukSpinner(districts.get(position));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void validateAndSubmit() {
        String name = etName.getText().toString().trim();
        String phoneNumber = etPhonenumber.getText().toString().trim();
        String district = spinnerDistrict.getSelectedItem().toString();
        String taluk = spinnerTaluk.getSelectedItem().toString();
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();
        String toWhomFor = etToWhomFor.getText().toString().trim();
        String locationText = etLocation.getText().toString().trim();

        if (name.isEmpty() || phoneNumber.isEmpty() || toWhomFor.isEmpty() || locationText.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (district.equals("Select District") || taluk.equals("Select Taluk") || bloodGroup.equals("Select Blood Group")) {
            Toast.makeText(this, "Please select all options", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationText, 1);
            double latitude = 0, longitude = 0;
            if (addresses != null && !addresses.isEmpty()) {
                latitude = addresses.get(0).getLatitude();
                longitude = addresses.get(0).getLongitude();
            }

            String receiverId = db.collection("receivers").document().getId();
            Receiver receiver = new Receiver(receiverId, userId, name, phoneNumber, district, taluk, bloodGroup, toWhomFor, locationText, latitude, longitude);
            
            final double finalLat = latitude;
            final double finalLng = longitude;
            db.collection("receivers").document(receiverId).set(receiver).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    broadcastSOS(bloodGroup, toWhomFor, finalLat, finalLng);
                    showSuccessDialog();
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(ReceiverActivity.this, "Request failed", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            Toast.makeText(this, "Location error", Toast.LENGTH_SHORT).show();
        }
    }

    private void broadcastSOS(String bloodGroup, String patientName, double lat, double lng) {
        String topic = bloodGroup.replace("+", "_POSITIVE").replace("-", "_NEGATIVE");
        
        // This is a placeholder. In a standard app, you'd call a Cloud Function.
        // For a demonstration of the logic:
        Log.d(TAG, "Broadcasting SOS to " + topic + " at (" + lat + ", " + lng + ")");
        
        // We will include lat/lng in the data payload so the receiving devices can filter by distance.
        JSONObject payload = new JSONObject();
        try {
            JSONObject data = new JSONObject();
            data.put("title", "URGENT: " + bloodGroup + " Needed!");
            data.put("message", patientName + " needs your help. Tap to see location.");
            data.put("lat", String.valueOf(lat));
            data.put("lng", String.valueOf(lng));
            data.put("type", "SOS");
            
            payload.put("to", "/topics/" + topic);
            payload.put("data", data);
            
            // Note: Sending directly from app is insecure (requires Server Key).
            // This logic represents what the backend would send.
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showSuccessDialog() {
        progressBar.setVisibility(View.GONE);
        btnSubmit.setEnabled(true);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        dialog.findViewById(R.id.btnGotIt).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(ReceiverActivity.this, DomainActivity.class));
            finish();
        });

        dialog.findViewById(R.id.btnGoHome).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        etLocation.setText(addresses.get(0).getAddressLine(0));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Geocoder failed", e);
                }
            }
        });
    }

    private void updateTalukSpinner(String district) {
        List<String> taluks;
        switch (district) {
            case "Bagalkot": taluks = Arrays.asList("Select Taluk", "Bagalkot", "Badami", "Bilagi", "Hungund", "Jamkhandi", "Mudhol"); break;
            case "Bangalore Rural": taluks = Arrays.asList("Select Taluk", "Devanahalli", "Doddaballapur", "Hosakote", "Nelamangala"); break;
            case "Bangalore Urban": taluks = Arrays.asList("Select Taluk", "Bangalore East", "Bangalore North", "Bangalore South", "Anekal", "Yelahanka"); break;
            case "Belgaum": taluks = Arrays.asList("Select Taluk", "Athani", "Bailhongal", "Belgaum", "Chikodi", "Gokak", "Hukkeri", "Khanapur", "Ramdurg", "Raibag", "Saundatti"); break;
            case "Tumkur": taluks = Arrays.asList("Select Taluk", "Tumkur", "Sira", "Tiptur", "Gubbi", "Madhugiri", "Kunigal", "Pavagada", "Koratagere", "Turuvekere"); break;
            default: taluks = Arrays.asList("Select Taluk"); break;
        }
        ArrayAdapter<String> talukAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, taluks);
        talukAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaluk.setAdapter(talukAdapter);
    }
}
