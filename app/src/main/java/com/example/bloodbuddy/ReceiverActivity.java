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
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.location.Location;
import android.os.Looper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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

public class ReceiverActivity extends AppCompatActivity {

    private static final String TAG = "ReceiverActivity";
    private EditText etName, etPhonenumber, etToWhomFor, etLocation;
    private Spinner spinnerDistrict, spinnerTaluk, spinnerBloodGroup;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private double currentLatitude  = 0.0;
    private double currentLongitude = 0.0;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_PICKER_REQUEST          = 2;

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

    private boolean isValidName(String name) {
        return name != null && name.trim().length() >= 3 && name.matches("^[a-zA-Z\\s]*$");
    }

    private boolean isValidMobile(String phone) {
        if (phone == null || phone.length() != 10) return false;
        if (!phone.matches("^[6-9]\\d{9}$")) return false;
        for (int i = 0; i <= 9; i++) {
            String dummy = String.format(Locale.getDefault(), "%d%d%d%d%d%d%d%d%d%d", i, i, i, i, i, i, i, i, i, i);
            if (phone.equals(dummy)) return false;
        }
        return true;
    }

    private void validateAndSubmit() {
        String name = etName.getText().toString().trim();
        String phoneNumber = etPhonenumber.getText().toString().trim();
        String district = spinnerDistrict.getSelectedItem().toString();
        String taluk = spinnerTaluk.getSelectedItem().toString();
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();
        String toWhomFor = etToWhomFor.getText().toString().trim();
        String locationText = etLocation.getText().toString().trim();

        if (!isValidName(name)) {
            etName.setError("Enter a valid name (min 3 chars, letters only)");
            etName.requestFocus();
            return;
        }

        if (!isValidMobile(phoneNumber)) {
            etPhonenumber.setError("Enter a valid 10-digit mobile number starting with 6-9");
            etPhonenumber.requestFocus();
            return;
        }

        if (toWhomFor.isEmpty()) {
            etToWhomFor.setError("Field is required");
            etToWhomFor.requestFocus();
            return;
        }

        if (locationText.isEmpty()) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            return;
        }

        if (district.equals("Select District") || taluk.equals("Select Taluk") || bloodGroup.equals("Select Blood Group")) {
            Toast.makeText(this, "Please select all options", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        // Warn if GPS wasn't captured, but still allow submit
        if (currentLatitude == 0.0 && currentLongitude == 0.0) {
            Toast.makeText(this, "⚠️ GPS not detected. Your request may not appear on the map accurately.", Toast.LENGTH_LONG).show();
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        // Use GPS coordinates directly (reliable) — no more geocoding typed text
        String receiverId = db.collection("receivers").document().getId();
        Receiver receiver = new Receiver(receiverId, userId, name, phoneNumber, district, taluk,
                bloodGroup, toWhomFor, locationText, currentLatitude, currentLongitude);

        db.collection("receivers").document(receiverId).set(receiver).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                broadcastSOS(bloodGroup, toWhomFor, currentLatitude, currentLongitude);
                showSuccessDialog();
            } else {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(ReceiverActivity.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void broadcastSOS(String bloodGroup, String patientName, double lat, double lng) {
        String topic = bloodGroup.replace("+", "_POSITIVE").replace("-", "_NEGATIVE");
        Log.d(TAG, "Broadcasting SOS to " + topic + " at (" + lat + ", " + lng + ")");
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        etLocation.setEnabled(false);
        etLocation.setText("");
        etLocation.setHint("📍 Detecting your location...");

        // Step 1: try cached location first (fast)
        fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            Location cached = task.isSuccessful() ? task.getResult() : null;
            if (cached != null) {
                onLocationObtained(cached);
            } else {
                requestFreshLocation(); // Step 2: no cache → fresh GPS fix
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocation() {
        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setInterval(0);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                if (!result.getLocations().isEmpty()) {
                    onLocationObtained(result.getLocations().get(0));
                } else {
                    runOnUiThread(() -> {
                        etLocation.setEnabled(true);
                        etLocation.setHint("Could not detect — please type your address");
                    });
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
    }

    private void onLocationObtained(Location location) {
        currentLatitude  = location.getLatitude();
        currentLongitude = location.getLongitude();

        // Step 3: reverse-geocode on background thread (never block UI)
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addrs = geocoder.getFromLocation(currentLatitude, currentLongitude, 1);
                runOnUiThread(() -> {
                    etLocation.setEnabled(true);
                    if (addrs != null && !addrs.isEmpty()) {
                        etLocation.setText(addrs.get(0).getAddressLine(0));
                    } else {
                        etLocation.setHint("📍 Location ready — edit if needed");
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Geocoder failed", e);
                runOnUiThread(() -> {
                    etLocation.setEnabled(true);
                    etLocation.setHint("📍 GPS ready — type address if needed");
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                etLocation.setEnabled(true);
                etLocation.setHint("Location denied — please type your address");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
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
