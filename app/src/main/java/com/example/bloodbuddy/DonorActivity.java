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
import com.google.android.gms.location.LocationServices;
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

    private double currentLatitude;
    private double currentLongitude;
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
        String ageStr = etAge.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();
        String lastDonated = etLastDonated.getText().toString().trim();

        if (ageStr.isEmpty() || weightStr.isEmpty() || gender.equals("Select Gender")) {
            Toast.makeText(this, "Please fill in Age, Weight, and Gender", Toast.LENGTH_SHORT).show();
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
            .addOnSuccessListener(aVoid -> showSuccessDialog())
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
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
                getAddressFromLocation(currentLatitude, currentLongitude);
            }
        });
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                etLocation.setText(addresses.get(0).getAddressLine(0));
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder failed", e);
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
        setupAdapter(spinnerTaluk, taluks);
    }
}
