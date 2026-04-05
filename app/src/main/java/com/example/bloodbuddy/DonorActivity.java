package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DonorActivity extends AppCompatActivity {

    private EditText etName, etPhoneNumber, etLocation;
    private TextView etLastDonated;
    private Spinner spinnerDistrict, spinnerTaluk, spinnerBloodGroup;
    private Button btnSubmit;
    private DatabaseReference databaseReference;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private double currentLatitude;
    private double currentLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("donors");

        // Initialize views
        etName = findViewById(R.id.etName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        spinnerDistrict = findViewById(R.id.etDistrict);
        spinnerTaluk = findViewById(R.id.etTaluk);
        etLastDonated = findViewById(R.id.etLastDonated);
        spinnerBloodGroup = findViewById(R.id.etBloodGroup);
        etLocation = findViewById(R.id.etLocation);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Populate District Spinner
        List<String> districts = Arrays.asList("Select District", "Bagalkot", "Bangalore Rural", "Bangalore Urban", "Belgaum", "Bellary", "Bidar", "Bijapur", "Chamarajanagar", "Chikballapur", "Chikmagalur", "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Gulbarga", "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysore", "Raichur", "Ramanagara", "Shimoga", "Tumkur", "Udupi", "Uttara Kannada", "Yadgir");
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);

        // Populate Blood Group Spinner
        List<String> bloodGroups = Arrays.asList("Select Blood Group", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-");
        ArrayAdapter<String> bloodGroupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        bloodGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodGroupAdapter);

        // Request location permissions and get current location
        requestLocationPermission();

        // Set date picker on Last Donated EditText
        etLastDonated.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // Set listener for district spinner to update taluk spinner
        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDistrict = (String) parent.getItemAtPosition(position);
                updateTalukSpinner(selectedDistrict, null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get user input
                String name = etName.getText().toString().trim();
                String phoneNumber = etPhoneNumber.getText().toString().trim();
                String district = spinnerDistrict.getSelectedItem().toString();
                String taluk = spinnerTaluk.getSelectedItem().toString();
                String lastDonated = etLastDonated.getText().toString().trim();
                String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();
                String location = etLocation.getText().toString().trim();

                // Validate user input
                if (!name.isEmpty() && isValidPhoneNumber(phoneNumber) && !district.equals("Select District") &&
                        !taluk.equals("Select Taluk") && !lastDonated.isEmpty() && !bloodGroup.equals("Select Blood Group")) {

                    // Store donor data in Firebase
                    String donorId = databaseReference.push().getKey();
                    if (donorId != null) {
                        Donor donor = new Donor(name, phoneNumber, district, taluk, lastDonated, bloodGroup, location, currentLatitude, currentLongitude);
                        databaseReference.child(donorId).setValue(donor);

                        // Show success message
                        Toast.makeText(DonorActivity.this, "Donor data saved", Toast.LENGTH_SHORT).show();

                        // Clear form fields
                        etName.setText("");
                        etPhoneNumber.setText("");
                        etLastDonated.setText("");
                        etLocation.setText("");
                        spinnerDistrict.setSelection(0);
                        spinnerTaluk.setSelection(0);
                        spinnerBloodGroup.setSelection(0);
                    }
                } else {
                    // Show error message if any field is empty
                    Toast.makeText(DonorActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
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
                Intent intent = new Intent(DonorActivity.this, DomainActivity.class);
                startActivity(intent);
            }
        });
    }

    // Function to validate phone number
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Check if phone number contains exactly 10 digits and is numeric
        return phoneNumber.length() == 10 && phoneNumber.matches("[0-9]+");
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create a new DatePickerDialog instance
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                DonorActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                        etLastDonated.setText(date);
                    }
                },
                year, month, day
        );

        // Set the maximum date to current date minus 90 days
        Calendar maxDateCalendar = Calendar.getInstance();
        maxDateCalendar.add(Calendar.DAY_OF_YEAR, -90);
        datePickerDialog.getDatePicker().setMaxDate(maxDateCalendar.getTimeInMillis());

        // Show the DatePickerDialog
        datePickerDialog.show();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, get current location
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get current location
                getCurrentLocation();
            } else {
                // Permission denied, show message
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Location location = task.getResult();
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();
                    // Fetch address from location coordinates
                    getAddressFromLocation(location.getLatitude(), location.getLongitude());
                } else {
                    // Log error or display toast message for failure
                    Log.e("LocationFetch", "Unable to fetch location: " + task.getException());
                    Toast.makeText(DonorActivity.this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Set location field with fetched address
                etLocation.setText(address.getAddressLine(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateTalukSpinner(String selectedDistrict, String selectedTaluk) {
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
        // Update the Taluk Spinner with the new list of taluks
        ArrayAdapter<String> talukAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, taluks);
        talukAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaluk.setAdapter(talukAdapter);

        // Set selected taluk if provided
        if (selectedTaluk != null) {
            setSpinnerSelection(spinnerTaluk, selectedTaluk);
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int position = 0; position < adapter.getCount(); position++) {
            if (adapter.getItem(position).equals(value)) {
                spinner.setSelection(position);
                return;
            }
        }
    }

    private void clearFormFields() {
        etName.setText("");
        etPhoneNumber.setText("");
        etLastDonated.setText("");
        etLocation.setText("");
        spinnerDistrict.setSelection(0);
        spinnerTaluk.setSelection(0);
        spinnerBloodGroup.setSelection(0);
    }
}

