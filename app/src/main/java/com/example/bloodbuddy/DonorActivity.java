package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import android.os.Looper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DonorActivity extends AppCompatActivity {

    private static final String TAG = "DonorActivity";
    private EditText etName, etPhoneNumber, etLocation, etLastDonated, etAge, etWeight, etDonationCount;
    private Spinner spinnerDistrict, spinnerTaluk, spinnerBloodGroup, spinnerGender;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_PICKER_REQUEST          = 2;

    private double currentLatitude  = 0.0;
    private double currentLongitude = 0.0;
    private LocationCallback locationCallback;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        etName = findViewById(R.id.etName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        spinnerDistrict = findViewById(R.id.etDistrict);
        spinnerTaluk = findViewById(R.id.etTaluk);
        etLastDonated = findViewById(R.id.etLastDonated);
        spinnerBloodGroup = findViewById(R.id.etBloodGroup);
        etLocation = findViewById(R.id.etLocation);
        etAge = findViewById(R.id.etAge);
        etWeight = findViewById(R.id.etWeight);
        etDonationCount = findViewById(R.id.etDonationCount);
        spinnerGender = findViewById(R.id.spinnerGender);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupSpinners();
        requestLocationPermission();

        etLastDonated.setOnClickListener(v -> showDatePickerDialog());
        // Tap the location field → open Swiggy/Zomato-style location picker
        etLocation.setFocusable(false);
        etLocation.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(this, LocationPickerActivity.class),
                        LOCATION_PICKER_REQUEST));
        findViewById(R.id.imageViewBack).setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> showHealthQuestionnaire());
    }

    private void setupSpinners() {
        List<String> bloodGroups = Arrays.asList("Select Blood Group", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-");
        setupAdapter(spinnerBloodGroup, bloodGroups);

        List<String> genders = Arrays.asList("Select Gender", "Male", "Female", "Other");
        setupAdapter(spinnerGender, genders);

        List<String> districts = Arrays.asList("Select District", "Bagalkot", "Bangalore Rural", "Bangalore Urban", "Belgaum", "Bellary", "Bidar", "Bijapur", "Chamarajanagar", "Chikballapur", "Chikmagalur", "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Gulbarga", "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysore", "Raichur", "Ramanagara", "Shimoga", "Tumkur", "Udupi", "Uttara Kannada", "Yadgir");
        setupAdapter(spinnerDistrict, districts);

        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTalukSpinner(districts.get(position));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupAdapter(Spinner spinner, List<String> data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.BLACK);
                ((TextView) v).setTextSize(16);
                return v;
            }
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.BLACK);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private boolean isValidName(String name) {
        return name != null && name.trim().length() >= 3 && name.matches("^[a-zA-Z\\s]*$");
    }

    private boolean isValidMobile(String phone) {
        if (phone == null || phone.length() != 10) return false;
        if (!phone.matches("^[6-9]\\d{9}$")) return false;
        return !phone.matches("(\\d)\\1{9}");
    }

    private void showHealthQuestionnaire() {
        // Pre-validate basic info before showing questionnaire
        if (!validateBasicInfo()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_health_check, null);
        builder.setView(view);

        CheckBox cb1 = view.findViewById(R.id.cb_no_diseases);
        CheckBox cb2 = view.findViewById(R.id.cb_no_medication);
        CheckBox cb3 = view.findViewById(R.id.cb_no_tattoos);
        CheckBox cb4 = view.findViewById(R.id.cb_healthy_feel);

        builder.setPositiveButton("Verify & Submit", (dialog, which) -> {
            if (cb1.isChecked() && cb2.isChecked() && cb3.isChecked() && cb4.isChecked()) {
                submitDonorData();
            } else {
                Toast.makeText(this, "You must meet all health criteria to donate blood.", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private boolean validateBasicInfo() {
        String name = etName.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();
        String lastDonated = etLastDonated.getText().toString().trim();
        String district = spinnerDistrict.getSelectedItem().toString();
        String taluk = spinnerTaluk.getSelectedItem().toString();
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();

        if (!isValidName(name)) {
            etName.setError("Enter a valid name (min 3 chars, letters only)");
            etName.requestFocus();
            return false;
        }

        if (!isValidMobile(phone)) {
            etPhoneNumber.setError("Enter a valid 10-digit mobile number starting with 6-9");
            etPhoneNumber.requestFocus();
            return false;
        }

        if (ageStr.isEmpty() || weightStr.isEmpty() || gender.equals("Select Gender")) {
            Toast.makeText(this, "Please fill in Age, Weight, and Gender", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (district.equals("Select District") || taluk.equals("Select Taluk") || bloodGroup.equals("Select Blood Group")) {
            Toast.makeText(this, "Please select District, Taluk and Blood Group", Toast.LENGTH_SHORT).show();
            return false;
        }

        int age = Integer.parseInt(ageStr);
        if (age < 18 || age > 65) {
            Toast.makeText(this, "Ineligible: Age must be between 18 and 65.", Toast.LENGTH_LONG).show();
            return false;
        }

        double weight = Double.parseDouble(weightStr);
        if (weight < 45) {
            Toast.makeText(this, "Ineligible: Weight must be at least 45kg.", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!lastDonated.isEmpty()) {
            try {
                Date lastDate = dateFormat.parse(lastDonated);
                long diffInMillis = Math.abs(new Date().getTime() - lastDate.getTime());
                long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);

                // Medical Standard: 90 days for Men, 120 days for Women
                int requiredDays = gender.equals("Male") ? 90 : 120;
                if (diffInDays < requiredDays) {
                    Toast.makeText(this, "Ineligible: You must wait " + requiredDays + " days between donations. Remaining: " + (requiredDays - diffInDays) + " days.", Toast.LENGTH_LONG).show();
                    return false;
                }
            } catch (ParseException e) {
                return false;
            }
        }
        return true;
    }

    private void submitDonorData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        String donorId = currentUser.getUid();
        Donor donor = new Donor(donorId, 
            etName.getText().toString(), 
            etPhoneNumber.getText().toString(),
            spinnerDistrict.getSelectedItem().toString(),
            spinnerTaluk.getSelectedItem().toString(),
            etLastDonated.getText().toString(),
            spinnerBloodGroup.getSelectedItem().toString(),
            etLocation.getText().toString(),
            spinnerGender.getSelectedItem().toString(),
            etAge.getText().toString(),
            etWeight.getText().toString(),
            etDonationCount.getText().toString(),
            "Verified Healthy",
            currentLatitude, currentLongitude);

        db.collection("donors").document(donorId).set(donor)
            .addOnSuccessListener(aVoid -> {
                // Update User flag in firestore
                db.collection("users").document(donorId).update("donor", true);
                showSuccessDialog();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(this, "Submission failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showSuccessDialog() {
        progressBar.setVisibility(View.GONE);
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.findViewById(R.id.btnGotIt).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();

        // Pop animation on the checkmark icon
        android.view.View icon = dialog.findViewById(R.id.ivIcon);
        icon.setScaleX(0f);
        icon.setScaleY(0f);
        icon.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(450)
                .setStartDelay(80)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.8f))
                .start();
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            etLastDonated.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                etLocation.setHint("Tap here to search your location");
                Toast.makeText(this, "Tap the location field to search.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            currentLatitude  = data.getDoubleExtra(LocationPickerActivity.EXTRA_LAT, 0.0);
            currentLongitude = data.getDoubleExtra(LocationPickerActivity.EXTRA_LON, 0.0);
            String address   = data.getStringExtra(LocationPickerActivity.EXTRA_ADDRESS);
            etLocation.setText(address);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        etLocation.setEnabled(false);
        etLocation.setText("");
        etLocation.setHint("📍 Detecting your location...");

        // Step 1: try fast cached location first
        fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            Location cached = (task.isSuccessful()) ? task.getResult() : null;
            if (cached != null) {
                onLocationObtained(cached);
            } else {
                // Step 2: no cache → request fresh GPS fix
                requestFreshLocation();
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
                    runOnUiThread(() -> onLocationFailed());
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
    }

    private void onLocationObtained(Location location) {
        currentLatitude  = location.getLatitude();
        currentLongitude = location.getLongitude();

        // Step 3: reverse-geocode in background (never block UI thread)
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addrs = geocoder.getFromLocation(currentLatitude, currentLongitude, 1);
                runOnUiThread(() -> {
                    etLocation.setEnabled(true);
                    if (addrs != null && !addrs.isEmpty()) {
                        etLocation.setText(addrs.get(0).getAddressLine(0));
                    } else {
                        etLocation.setHint("📍 Location detected (edit if needed)");
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

    private void onLocationFailed() {
        etLocation.setEnabled(true);
        etLocation.setHint("Could not detect — please type your address");
        Toast.makeText(this, "GPS unavailable. Please type your address.", Toast.LENGTH_SHORT).show();
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
        setupAdapter(spinnerTaluk, taluks);
    }
}
