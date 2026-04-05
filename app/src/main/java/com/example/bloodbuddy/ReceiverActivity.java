package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ReceiverActivity extends AppCompatActivity {

    private static final String TAG = "ReceiverActivity";
    private EditText etName, etPhonenumber,etToWhomFor, etLocation;
    private Spinner spinnerDistrict, spinnerTaluk, spinnerBloodGroup;
    private Button btnSubmit;
    private DatabaseReference databaseReference;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receiver);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("receivers");

        // Initialize views
        etName = findViewById(R.id.etName);
        etPhonenumber =findViewById(R.id.etPhonenumber);
        spinnerDistrict = findViewById(R.id.etDistrict);
        spinnerTaluk = findViewById(R.id.etTaluk);
        spinnerBloodGroup = findViewById(R.id.etBloodGroup);
        etToWhomFor = findViewById(R.id.etToWhomFor);
        etLocation = findViewById(R.id.etLocation);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Populate District Spinner
        List<String> districts = Arrays.asList("Select District", "Bagalkot", "Bangalore Rural", "Bangalore Urban", "Belgaum", "Bellary", "Bidar", "Bijapur", "Chamarajanagar", "Chikballapur", "Chikmagalur", "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Gulbarga", "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysore", "Raichur", "Ramanagara", "Shimoga", "Tumkur", "Udupi", "Uttara Kannada", "Yadgir");
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);

        // Populate Taluk Spinner based on selected District
        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDistrict = (String) parent.getItemAtPosition(position);
                updateTalukSpinner(selectedDistrict);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Populate Blood Group Spinner
        List<String> bloodGroups = Arrays.asList("Select Blood Group", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-");
        ArrayAdapter<String> bloodGroupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        bloodGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodGroupAdapter);

        // Set location
        setLocation();

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString().trim();
                String PhoneNumber = etPhonenumber.getText().toString().trim();
                String district = spinnerDistrict.getSelectedItem().toString();
                String taluk = spinnerTaluk.getSelectedItem().toString();
                String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();
                String toWhomFor = etToWhomFor.getText().toString().trim();
                String location = etLocation.getText().toString().trim();

                if (!name.isEmpty() && !district.equals("Select District") && !taluk.equals("Select Taluk") &&
                        !bloodGroup.equals("Select Blood Group") && !toWhomFor.isEmpty() && !location.isEmpty()) {

                    // Store the receiver in the database
                    String receiverId = databaseReference.push().getKey();
                    if (receiverId != null) {
                        // Get latitude and longitude for the location
                        Geocoder geocoder = new Geocoder(ReceiverActivity.this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(location, 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                double latitude = address.getLatitude();
                                double longitude = address.getLongitude();

                                // Create receiver object with latitude and longitude
                                Receiver receiver = new Receiver(receiverId, name,PhoneNumber, district, taluk, bloodGroup, toWhomFor, location, latitude, longitude);
                                databaseReference.child(receiverId).setValue(receiver).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            fetchDonorData();
                                        } else {
                                            Log.e(TAG, "Failed to save receiver data", task.getException());
                                            Toast.makeText(ReceiverActivity.this, "Failed to save receiver data", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            } else {
                                Toast.makeText(ReceiverActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(ReceiverActivity.this, "Geocoding failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Receiver ID is null");
                    }
                } else {
                    Toast.makeText(ReceiverActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // ImageView for going back to DomainActivity
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ImageView imageViewBack = findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to DomainActivity
                Intent intent = new Intent(ReceiverActivity.this, DomainActivity.class);
                startActivity(intent);
            }
        });
    }

        private void updateTalukSpinner(String selectedDistrict) {
        List<String> taluks;
            switch (selectedDistrict) {
                case "Bagalkot":
                    taluks = Arrays.asList("Select Taluk", "Bagalkot", "Badami", "Bilagi", "Hungund", "Jamkhandi", "Mudhol");
                    break;
                case "Bangalore Rural":
                    taluks = Arrays.asList("Select Taluk", "Devanahalli", "Doddaballapur", "Hosakote", "Nelamangala");
                    break;
                case "Bangalore Urban":
                    taluks = Arrays.asList("Select Taluk", "Bangalore East", "Bangalore North", "Bangalore South", "Anekal", "Yelahanka");
                    break;
                case "Belgaum":
                    taluks = Arrays.asList("Select Taluk", "Athani", "Bailhongal", "Belgaum", "Chikodi", "Gokak", "Hukkeri", "Khanapur", "Ramdurg", "Raibag", "Saundatti");
                    break;
                case "Bellary":
                    taluks = Arrays.asList("Select Taluk", "Sandur", "Hospet", "Siruguppa", "Kudligi", "Hagaribommanahalli");
                    break;
                case "Bidar":
                    taluks = Arrays.asList("Select Taluk", "Humnabad", "Bidar", "Bhalki", "Aurad", "Basavakalyan");
                    break;
                case "Bijapur":
                    taluks = Arrays.asList("Select Taluk", "Bijapur", "Basavana Bagewadi", "Sindagi", "Indi", "Muddebihal");
                    break;
                case "Chamarajanagar":
                    taluks = Arrays.asList("Select Taluk", "Chamarajanagar", "Gundlupet", "Kollegal", "Yelandur");
                    break;
                case "Chikballapur":
                    taluks = Arrays.asList("Select Taluk", "Chikballapur", "Chintamani", "Gauribidanur", "Bagepalli", "Sidlaghatta", "Gudibanda");
                    break;
                case "Chikmagalur":
                    taluks = Arrays.asList("Select Taluk", "Chikmagalur", "Kadur", "Koppa", "Mudigere", "Narasimharajapura", "Sringeri", "Tarikere");
                    break;
                case "Chitradurga":
                    taluks = Arrays.asList("Select Taluk", "Chitradurga", "Hiriyur", "Hosadurga", "Holalkere", "Molakalmuru", "Challakere");
                    break;
                case "Dakshina Kannada":
                    taluks = Arrays.asList("Select Taluk", "Mangalore", "Bantwal", "Belthangady", "Puttur", "Sullia", "Ullal", "Karkala", "Mudabidri", "Mulki", "Surathkal", "Moodabidri");
                    break;
                case "Davanagere":
                    taluks = Arrays.asList("Select Taluk", "Davanagere", "Channagiri", "Honnali", "Harihar", "Harapanahalli", "Jagalur");
                    break;
                case "Dharwad":
                    taluks = Arrays.asList("Select Taluk", "Dharwad", "Hubli", "Kalghatgi", "Kundgol", "Navalgund");
                    break;
                case "Gadag":
                    taluks = Arrays.asList("Select Taluk", "Gadag", "Mundargi", "Nargund", "Ron", "Shirhatti");
                    break;
                case "Gulbarga":
                    taluks = Arrays.asList("Select Taluk", "Kamalapur", "Shahbad", "Kalaburagi", "Aland", "Jewargi", "Afzalpur");
                    break;
                case "Hassan":
                    taluks = Arrays.asList("Select Taluk", "Arsikere", "Belur", "Channarayapatna", "Hassan", "Holenarsipur", "Sakleshpur", "Alur", "Arkalgud");
                    break;
                case "Haveri":
                    taluks = Arrays.asList("Select Taluk", "Hanagal", "Haveri", "Hirekerur", "Ranebennur", "Byadgi", "Savanur", "Shiggaon");
                    break;
                case "Kodagu":
                    taluks = Arrays.asList("Select Taluk", "Madikeri", "Somwarpet", "Virajpet");
                    break;
                case "Kolar":
                    taluks = Arrays.asList("Select Taluk", "Bangarapet", "Kolar", "Malur", "Mulbagal", "Srinivaspur");
                    break;
                case "Koppal":
                    taluks = Arrays.asList("Select Taluk", "Gangawati", "Koppal", "Kushtagi", "Yelburga");
                    break;
                case "Mandya":
                    taluks = Arrays.asList("Select Taluk", "Krishnarajpet", "Mandya", "Malavalli", "Nagamangala", "Pandavapura", "Srirangapatna", "Maddur");
                    break;
                case "Mysore":
                    taluks = Arrays.asList("Select Taluk", "Hunsur", "Krishnarajanagara", "Mysore", "Nanjangud", "Piriyapatna", "Tirumakudal Narsipur");
                    break;
                case "Raichur":
                    taluks = Arrays.asList("Select Taluk", "Devadurga", "Lingsugur", "Manvi", "Raichur", "Sindhanur");
                    break;
                case "Ramanagara":
                    taluks = Arrays.asList("Select Taluk", "Channapatna", "Kanakapura", "Magadi", "Ramanagaram");
                    break;
                case "Shimoga":
                    taluks = Arrays.asList("Select Taluk", "Bhadravathi", "Hosanagara", "Sagara", "Shikarpur", "Shimoga", "Sorab", "Tirthahalli");
                    break;
                case "Tumkur":
                    taluks = Arrays.asList("Select Taluk", "Tumkur", "Sira", "Tiptur", "Gubbi", "Madhugiri");
                    break;
                case "Udupi":
                    taluks = Arrays.asList("Select Taluk", "Karkala", "Kundapura", "Udupi");
                    break;
                case "Uttara Kannada":
                    taluks = Arrays.asList("Select Taluk", "Ankola", "Bhatkal", "Haliyal", "Karwar", "Kumta", "Mundgod", "Siddapur", "Sirsi", "Yellapur", "Dandeli");
                    break;
                case "Yadgir":
                    taluks = Arrays.asList("Select Taluk", "Shahpur", "Shorapur", "Yadgir");
                    break;
                default:
                    taluks = Arrays.asList("Select Taluk");
                    break;
            }
        ArrayAdapter<String> talukAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, taluks);
        talukAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaluk.setAdapter(talukAdapter);
    }

    private void setLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Location location = task.getResult();
                    Geocoder geocoder = new Geocoder(ReceiverActivity.this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            etLocation.setText(address.getAddressLine(0));
                        } else {
                            Toast.makeText(ReceiverActivity.this, "Unable to get address", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(ReceiverActivity.this, "Unable to get address", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ReceiverActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchDonorData() {
        DatabaseReference donorsRef = FirebaseDatabase.getInstance().getReference("donors");
        donorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Donor> donors = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Donor donor = snapshot.getValue(Donor.class);
                    if (donor != null) {
                        donors.add(donor);
                    }
                }
                if (!donors.isEmpty()) {
                    Intent intent = new Intent(ReceiverActivity.this, DisplayDonorActivity.class);
                    intent.putExtra("donors", (Serializable) donors);
                    startActivity(intent);
                } else {
                    Toast.makeText(ReceiverActivity.this, "No donors found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ReceiverActivity.this, "Failed to fetch donor data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
