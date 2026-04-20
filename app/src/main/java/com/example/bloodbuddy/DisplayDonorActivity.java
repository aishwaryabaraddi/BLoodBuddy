package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DisplayDonorActivity extends AppCompatActivity {

    private static final String TAG = "DisplayDonorActivity";
    private Spinner spinnerBloodGroup, spinnerDistrict, spinnerTaluk;
    private LinearLayout donorDetailsContainer;
    private FirebaseFirestore db;
    private List<Donor> allDonors;
    private static final int REQUEST_SMS_PERMISSION = 1;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_donor);

        // Initialize views
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        spinnerTaluk = findViewById(R.id.spinnerTaluk);
        donorDetailsContainer = findViewById(R.id.donorDetailsContainer);
        progressBar = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        allDonors = new ArrayList<>();

        setupSpinners();
        fetchDonorDetails(); // Fetch all initially

        findViewById(R.id.imageView7).setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        List<String> bloodGroups = Arrays.asList("Select Blood Group", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        setupAdapter(spinnerBloodGroup, bloodGroups);

        List<String> districts = Arrays.asList("Select District", "Bagalkot", "Bangalore Rural", "Bangalore Urban", "Belgaum", "Bellary", "Bidar", "Bijapur", "Chamarajanagar", "Chikballapur", "Chikmagalur", "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Gulbarga", "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysore", "Raichur", "Ramanagara", "Shimoga", "Tumkur", "Udupi", "Uttara Kannada", "Yadgir");
        setupAdapter(spinnerDistrict, districts);

        setupAdapter(spinnerTaluk, Arrays.asList("Select Taluk"));

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getId() == R.id.spinnerDistrict) {
                    updateTalukSpinner(parent.getItemAtPosition(position).toString());
                }
                filterDonors();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerBloodGroup.setOnItemSelectedListener(filterListener);
        spinnerDistrict.setOnItemSelectedListener(filterListener);
        spinnerTaluk.setOnItemSelectedListener(filterListener);
    }

    private void setupAdapter(Spinner spinner, List<String> data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void updateTalukSpinner(String selectedDistrict) {
        List<String> taluks;
        switch (selectedDistrict) {
            case "Bagalkot": taluks = Arrays.asList("Select Taluk", "Bagalkot", "Badami", "Bilagi", "Hungund", "Jamkhandi", "Mudhol"); break;
            case "Bangalore Rural": taluks = Arrays.asList("Select Taluk", "Devanahalli", "Doddaballapur", "Hosakote", "Nelamangala"); break;
            case "Bangalore Urban": taluks = Arrays.asList("Select Taluk", "Bangalore East", "Bangalore North", "Bangalore South", "Anekal", "Yelahanka"); break;
            case "Belgaum": taluks = Arrays.asList("Select Taluk", "Athani", "Bailhongal", "Belgaum", "Chikodi", "Gokak", "Hukkeri", "Khanapur", "Ramdurg", "Raibag", "Saundatti"); break;
            case "Tumkur": taluks = Arrays.asList("Select Taluk", "Tumkur", "Sira", "Tiptur", "Gubbi", "Madhugiri", "Kunigal", "Pavagada", "Koratagere", "Turuvekere"); break;
            default: taluks = Arrays.asList("Select Taluk"); break;
        }
        setupAdapter(spinnerTaluk, taluks);
    }

    private void fetchDonorDetails() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("donors").get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                allDonors.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    allDonors.add(document.toObject(Donor.class));
                }
                displayDonors(allDonors);
            } else {
                Log.e(TAG, "Error getting documents: ", task.getException());
                Toast.makeText(this, "Failed to fetch donors", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterDonors() {
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();
        String district = spinnerDistrict.getSelectedItem().toString();
        String taluk = spinnerTaluk.getSelectedItem().toString();

        List<Donor> filtered = new ArrayList<>();
        for (Donor d : allDonors) {
            boolean bMatch = bloodGroup.equals("Select Blood Group") || d.getBloodGroup().equals(bloodGroup);
            boolean dMatch = district.equals("Select District") || d.getDistrict().equals(district);
            boolean tMatch = taluk.equals("Select Taluk") || d.getTaluk().equals(taluk);

            if (bMatch && dMatch && tMatch) {
                filtered.add(d);
            }
        }
        displayDonors(filtered);
    }

    private void displayDonors(List<Donor> donors) {
        donorDetailsContainer.removeAllViews();
        if (donors.isEmpty()) {
            TextView noResults = new TextView(this);
            noResults.setText("No donors found for your selection.");
            noResults.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            noResults.setPadding(0, 50, 0, 0);
            donorDetailsContainer.addView(noResults);
            return;
        }

        for (Donor donor : donors) {
            addDonorDetailsToContainer(donor);
        }
    }

    private void addDonorDetailsToContainer(Donor donor) {
        View cardView = getLayoutInflater().inflate(R.layout.donor_details_card, null);

        ((TextView) cardView.findViewById(R.id.tvDonorName)).setText(donor.getName());
        ((TextView) cardView.findViewById(R.id.tvDonorPhoneNumber)).setText(donor.getPhoneNumber());
        ((TextView) cardView.findViewById(R.id.tvDonorBloodGroup)).setText(donor.getBloodGroup());
        ((TextView) cardView.findViewById(R.id.tvDonorDistrict)).setText(donor.getDistrict());
        ((TextView) cardView.findViewById(R.id.tvDonorTaluk)).setText(donor.getTaluk());
        ((TextView) cardView.findViewById(R.id.tvDonorLastDonated)).setText("Last Donated: " + (donor.getLastDonated().isEmpty() ? "Never" : donor.getLastDonated()));
        ((TextView) cardView.findViewById(R.id.tvDonorLocation)).setText(donor.getLocation());

        cardView.findViewById(R.id.btnMakeRequest).setOnClickListener(v -> makeRequest(donor));
        cardView.findViewById(R.id.imageButton).setOnClickListener(v -> dialPhoneNumber(donor.getPhoneNumber()));

        cardView.findViewById(R.id.imageButton2).setOnClickListener(v -> {
            String uri = String.format(Locale.ENGLISH, "google.navigation:q=%f,%f", donor.getLatitude(), donor.getLongitude());
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Maps app not installed", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 24);
        cardView.setLayoutParams(layoutParams);
        donorDetailsContainer.addView(cardView);
    }

    private void makeRequest(Donor donor) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        // Fetching requester info from Firestore (Standard migration)
        db.collection("users").document(firebaseUser.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                User requester = documentSnapshot.toObject(User.class);
                if (requester != null) {
                    checkAndSendSMS(donor, requester);
                }
            });
    }

    private void checkAndSendSMS(Donor donor, User requester) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
        } else {
            sendSMS(donor, requester);
        }
    }

    private void sendSMS(Donor donor, User requester) {
        String message = "Blood Buddy SOS: Hello " + donor.getName() + ", I need " + donor.getBloodGroup() + 
                         " blood immediately. Please contact me at " + requester.getPhone() + ". - Sent by " + requester.getName();

        try {
            SmsManager.getDefault().sendTextMessage(donor.getPhoneNumber(), null, message, null, null);
            Toast.makeText(this, "Emergency Request sent to " + donor.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "SMS failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void dialPhoneNumber(String phoneNumber) {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber)));
    }
}
