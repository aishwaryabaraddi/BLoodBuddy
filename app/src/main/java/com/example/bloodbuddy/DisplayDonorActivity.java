package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DisplayDonorActivity extends AppCompatActivity {

    private Spinner spinnerBloodGroup, spinnerDistrict, spinnerTaluk;
    private LinearLayout donorDetailsContainer;
    private DatabaseReference databaseReference;
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
        progressBar = findViewById(R.id.progressBar); // Initialize ProgressBar


        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("donors");

        // Set up spinners
        setupSpinners();

        // Initialize allDonors list
        allDonors = new ArrayList<>();

        // Fetch and display all donors initially
        fetchDonorDetails();

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ImageView imageViewBack = findViewById(R.id.imageView7);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to DomainActivity
                Intent intent = new Intent(DisplayDonorActivity.this, DomainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupSpinners() {
        // Set up spinner for blood groups
        List<String> bloodGroups = Arrays.asList("Select Blood Group", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        ArrayAdapter<String> bloodGroupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        bloodGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodGroupAdapter);

        // Set up spinner for districts
        List<String> districts = Arrays.asList("Select District", "Bagalkot", "Bangalore Rural", "Bangalore Urban", "Belgaum", "Bellary", "Bidar", "Bijapur", "Chamarajanagar", "Chikballapur", "Chikmagalur", "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Gulbarga", "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysore", "Raichur", "Ramanagara", "Shimoga", "Tumkur", "Udupi", "Uttara Kannada", "Yadgir");
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);

        // Set up spinner for taluks
        ArrayAdapter<String> talukAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList("Select Taluk"));
        talukAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaluk.setAdapter(talukAdapter);

        // Set event listeners for spinners
        spinnerBloodGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterDonors();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDistrict = (String) parent.getItemAtPosition(position);
                updateTalukSpinner(selectedDistrict);
                filterDonors();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        spinnerTaluk.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterDonors();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
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

    private void fetchDonorDetails() {
        // Show progress bar
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allDonors.clear();
                donorDetailsContainer.removeAllViews(); // Clear existing donor details

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Donor donor = snapshot.getValue(Donor.class);
                    if (donor != null) {
                        allDonors.add(donor);
                        addDonorDetailsToContainer(donor);
                    }
                }
                filterDonors(); // Ensure to filter after fetching

                // Hide progress bar after fetching completes
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
                // Hide progress bar on error as well
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void filterDonors() {
        if (allDonors == null) {
            return; // Handle case where allDonors is not initialized
        }

        String selectedBloodGroup = spinnerBloodGroup.getSelectedItem().toString();
        String selectedDistrict = spinnerDistrict.getSelectedItem().toString();
        String selectedTaluk = spinnerTaluk.getSelectedItem().toString();

        donorDetailsContainer.removeAllViews(); // Clear existing donor details

        for (Donor donor : allDonors) {
            boolean bloodGroupMatch = selectedBloodGroup.equals("Select Blood Group") || donor.getBloodGroup().equals(selectedBloodGroup);
            boolean districtMatch = selectedDistrict.equals("Select District") || donor.getDistrict().equals(selectedDistrict);
            boolean talukMatch = selectedTaluk.equals("Select Taluk") || donor.getTaluk().equals(selectedTaluk);

            if (bloodGroupMatch && districtMatch && talukMatch) {
                addDonorDetailsToContainer(donor);
            }
        }
    }

    private void addDonorDetailsToContainer(Donor donor) {
        // Inflate the CardView layout from XML
        View cardView = getLayoutInflater().inflate(R.layout.donor_details_card, null);

        // Initialize views inside the CardView
        TextView tvDonorName = cardView.findViewById(R.id.tvDonorName);
        TextView tvDonorPhoneNumber = cardView.findViewById(R.id.tvDonorPhoneNumber);
        TextView tvDonorBloodGroup = cardView.findViewById(R.id.tvDonorBloodGroup);
        TextView tvDonorDistrict = cardView.findViewById(R.id.tvDonorDistrict);
        TextView tvDonorTaluk = cardView.findViewById(R.id.tvDonorTaluk);
        TextView tvDonorLastDonated = cardView.findViewById(R.id.tvDonorLastDonated);
        TextView tvDonorLocation = cardView.findViewById(R.id.tvDonorLocation);
        Button btnMakeRequest = cardView.findViewById(R.id.btnMakeRequest);

        // Set donor details to the TextViews
        tvDonorName.setText("Name               : " + donor.getName());
        tvDonorPhoneNumber.setText("Phone Number  : " + donor.getPhoneNumber());
        tvDonorBloodGroup.setText("Blood Group       : " + donor.getBloodGroup());
        tvDonorDistrict.setText("District                : " + donor.getDistrict());
        tvDonorTaluk.setText("Taluk                   : " + donor.getTaluk());
        tvDonorLastDonated.setText("Last Donated     : " + donor.getLastDonated());
        tvDonorLocation.setText("Location             : " + donor.getLocation());

        btnMakeRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeRequest(donor);
            }
        });

        // Set click listener for phone icon to dial donor's phone number
        ImageView phoneIcon = cardView.findViewById(R.id.imageButton);
        phoneIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = donor.getPhoneNumber();
                dialPhoneNumber(phoneNumber);
            }
        });

        ImageView navigateIcon = cardView.findViewById(R.id.imageButton2);
        navigateIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Assuming you have the latitude and longitude of the donor's location
                double latitude = donor.getLatitude();
                double longitude = donor.getLongitude();
                String uri = "https://www.google.com/maps/dir/?api=1&destination=" + latitude + "," + longitude;
                Intent navigateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                navigateIntent.setPackage("com.google.android.apps.maps");
                startActivity(navigateIntent);
            }
        });

        // Add spacing between CardViews (optional)
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 16); // Adjust bottom margin as needed
        cardView.setLayoutParams(layoutParams);

        // Add the CardView to the donorDetailsContainer LinearLayout
        donorDetailsContainer.addView(cardView);
    }

    private void makeRequest(Donor donor) {
        // Fetch current user details
        getCurrentUser(new UserCallback() {
            @Override
            public void onUserFetched(User requester) {
                // Check SMS permission
                if (ContextCompat.checkSelfPermission(DisplayDonorActivity.this, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Permission not granted, request it
                    ActivityCompat.requestPermissions(DisplayDonorActivity.this,
                            new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
                } else {
                    // Permission already granted, send SMS
                    sendSMS(donor, requester);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(DisplayDonorActivity.this, "Failed to fetch user details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendSMS(Donor donor, User requester) {
        String phoneNumber = donor.getPhoneNumber();
        String message = "Blood Buddy Request: Hello, I need your blood donation. Please contact me.\n\n";

        // Adding requester details to the message
        message += "Requester Details:\n";
        message += "Name: " + requester.getName() + "\n";
        message += "Phone Number: " + requester.getPhone() + "\n";
        message += "Taluk: " + requester.getTaluk() + "\n";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            // Display toast message with donor details
            String toastMessage = "SMS sent successfully to " + donor.getName() + " (" + donor.getPhoneNumber() + ")";
            Toast.makeText(DisplayDonorActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(DisplayDonorActivity.this, "Failed to send SMS.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    private void getCurrentUser(UserCallback callback) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        callback.onUserFetched(user);
                    } else {
                        callback.onError(new Exception("User not found"));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    callback.onError(databaseError.toException());
                }
            });
        } else {
            callback.onError(new Exception("User not logged in"));
        }
    }

//    private void storeRequestInDonorAccount(Donor donor, User requester) {
//        DatabaseReference donorRef = FirebaseDatabase.getInstance().getReference("donors").child(donor.getid()).child("requests");
//
//        // Create a Request object
//        String message = "Blood Buddy Request: Hello, I need your blood donation. Please contact me.";
//        Request request = new Request(requester.getName(), requester.getPhone(), requester.getTaluk(), message);
//
//        // Store the request under the donor's requests
//        donorRef.push().setValue(request)
//                .addOnSuccessListener(aVoid -> Toast.makeText(DisplayDonorActivity.this, "Request stored successfully.", Toast.LENGTH_SHORT).show())
//                .addOnFailureListener(e -> Toast.makeText(DisplayDonorActivity.this, "Failed to store request.", Toast.LENGTH_SHORT).show());
//    }


//    private void saveRequestToDonorAccount(Donor donor, User requester, String message) {
//        DatabaseReference donorRef = FirebaseDatabase.getInstance().getReference("donors").child(donor.getid()).child("requests");
//        String requestId = donorRef.push().getKey();
//
//        Request request = new Request(requester.getName(), requester.getPhone(), requester.getTaluk(), message);
//
//        donorRef.child(requestId).setValue(request)
//                .addOnSuccessListener(aVoid -> {
//                    Toast.makeText(DisplayDonorActivity.this, "Request saved to donor's account.", Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(DisplayDonorActivity.this, "Failed to save request to donor's account.", Toast.LENGTH_SHORT).show();
//                });
//    }

    private interface UserCallback {
        void onUserFetched(User user);
        void onError(Exception e);
    }


    //    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_SMS_PERMISSION) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission granted, send SMS
//                sendSMS(selectedDonor); // Assuming selectedDonor is your Donor object
//            } else {
//                // Permission denied, show appropriate message or handle gracefully
//                Toast.makeText(DisplayDonorActivity.this, "SMS permission denied. Cannot send SMS.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
    private void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}
