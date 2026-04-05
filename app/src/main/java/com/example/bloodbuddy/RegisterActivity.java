package com.example.bloodbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private FirebaseAuth mAuth;
    private EditText registerName, registerEmail, registerPhone, registerPassword;
    private RadioGroup genderGroup;
    private RadioButton genderButton;
    private Spinner districtSpinner, talukSpinner, bloodGroupSpinner;
    private Button registerButton;
    private ProgressBar progressBar;
    private TextView loginLink;

    // Blood groups
    private String[] bloodGroups = {"Select Blood Group", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    // Karnataka districts and taluks
    private String[] districts = {"Select District", "Bagalkot", "Bangalore Rural", "Bangalore Urban", "Belgaum",
            "Bellary", "Bidar", "Bijapur", "Chamarajanagar", "Chikballapur", "Chikmagalur",
            "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Gulbarga",
            "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysore", "Raichur",
            "Ramanagara", "Shimoga", "Tumkur", "Udupi", "Uttara Kannada", "Yadgir"};

    private String[][] taluks = {
            {"Select Taluk"},  // placeholder for "Select District"
            {"Bagalkot", "Badami", "Bilagi", "Hungund", "Jamkhandi", "Mudhol"},
            {"Devanahalli", "Doddaballapur", "Hosakote", "Nelamangala"},
            {"Bangalore North", "Bangalore East", "Bangalore South", "Anekal"},
            {"Athani", "Bailhongal", "Belgaum", "Chikodi", "Gokak", "Hukkeri", "Khanapur", "Ramdurg", "Raibag", "Saundatti"},
            {"Bellary", "Siruguppa", "Hospet", "Kudligi", "Sandur", "Hadagali", "Hagaribommanahalli"},
            {"Humnabad", "Bidar", "Bhalki", "Aurad", "Basavakalyan"},
            {"Bijapur", "Basavana Bagewadi", "Sindagi", "Indi", "Muddebihal"},
            {"Chamarajanagar", "Gundlupet", "Kollegal", "Yelandur"},
            {"Chikballapur", "Chintamani", "Gauribidanur", "Bagepalli", "Sidlaghatta", "Gudibanda"},
            {"Chikmagalur", "Kadur", "Koppa", "Mudigere", "Narasimharajapura", "Sringeri", "Tarikere"},
            {"Chitradurga", "Hiriyur", "Hosadurga", "Holalkere", "Molakalmuru", "Challakere"},
            {"Mangalore", "Bantwal", "Belthangady", "Puttur", "Sullia", "Ullal"},
            {"Davanagere", "Channagiri", "Honnali", "Harihar", "Harapanahalli", "Jagalur"},
            {"Dharwad", "Hubli", "Kalghatgi", "Kundgol", "Navalgund"},
            {"Gadag", "Mundargi", "Nargund", "Ron", "Shirhatti"},
            {"Kamalapur", "Shahbad", "Kalaburagi", "Aland", "Jewargi", "Afzalpur"},
            {"Arsikere", "Belur", "Channarayapatna", "Hassan", "Holenarsipur", "Sakleshpur", "Alur", "Arkalgud"},
            {"Hanagal", "Haveri", "Hirekerur", "Ranebennur", "Byadgi", "Savanur", "Shiggaon"},
            {"Madikeri", "Somwarpet", "Virajpet"},
            {"Bangarapet", "Kolar", "Malur", "Mulbagal", "Srinivaspur"},
            {"Gangawati", "Koppal", "Kushtagi", "Yelburga"},
            {"Krishnarajpet", "Mandya", "Malavalli", "Nagamangala", "Pandavapura", "Srirangapatna", "Maddur"},
            {"Hunsur", "Krishnarajanagara", "Mysore", "Nanjangud", "Piriyapatna", "Tirumakudal Narsipur"},
            {"Devadurga", "Lingsugur", "Manvi", "Raichur", "Sindhanur"},
            {"Channapatna", "Kanakapura", "Magadi", "Ramanagaram"},
            {"Bhadravathi", "Hosanagara", "Sagara", "Shikarpur", "Shimoga", "Sorab", "Tirthahalli"},
            {"Tumkur", "Sira", "Tiptur", "Gubbi", "Madhugiri"},
            {"Karkala", "Kundapura", "Udupi"},
            {"Ankola", "Bhatkal", "Haliyal", "Karwar", "Kumta", "Mundgod", "Siddapur", "Sirsi", "Yellapur", "Dandeli"},
            {"Shahpur", "Shorapur", "Yadgir"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        registerName = findViewById(R.id.register_name);
        registerEmail = findViewById(R.id.register_email);
        registerPhone = findViewById(R.id.register_phone);
        registerPassword = findViewById(R.id.register_password);
        genderGroup = findViewById(R.id.gender_group);
        bloodGroupSpinner = findViewById(R.id.spinner_blood_group);
        districtSpinner = findViewById(R.id.spinner_district);
        talukSpinner = findViewById(R.id.spinner_taluk);
        registerButton = findViewById(R.id.register_button);
        progressBar = findViewById(R.id.progress_bar);
        loginLink = findViewById(R.id.login_link);

        // Blood group spinner
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bloodGroupSpinner.setAdapter(bloodAdapter);

        // District spinner
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        districtSpinner.setAdapter(districtAdapter);

        // Taluk spinner (default placeholder)
        ArrayAdapter<String> talukAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, taluks[0]);
        talukAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        talukSpinner.setAdapter(talukAdapter);

        // Update taluk spinner based on selected district
        districtSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ArrayAdapter<String> newTalukAdapter = new ArrayAdapter<>(RegisterActivity.this,
                        android.R.layout.simple_spinner_item, taluks[position]);
                newTalukAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                talukSpinner.setAdapter(newTalukAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        registerButton.setOnClickListener(v -> registerUser());

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = registerName.getText().toString().trim();
        String email = registerEmail.getText().toString().trim();
        String phone = registerPhone.getText().toString().trim();
        String password = registerPassword.getText().toString().trim();

        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        genderButton = findViewById(selectedGenderId);
        String gender = (genderButton == null) ? "" : genderButton.getText().toString().trim();

        String bloodGroup = bloodGroupSpinner.getSelectedItem().toString();
        String district = districtSpinner.getSelectedItem().toString();
        String taluk = talukSpinner.getSelectedItem().toString();

        // Validations
        if (name.isEmpty()) {
            registerName.setError("Name is required");
            registerName.requestFocus();
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            registerEmail.setError("Valid email is required");
            registerEmail.requestFocus();
            return;
        }

        if (!email.endsWith("@gmail.com")) {
            registerEmail.setError("Only Gmail addresses are allowed (e.g., user@gmail.com)");
            registerEmail.requestFocus();
            return;
        }

        if (phone.isEmpty() || !phone.matches("\\d{10}")) {
            registerPhone.setError("Enter a valid 10-digit phone number");
            registerPhone.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            registerPassword.setError("Password must be at least 6 characters");
            registerPassword.requestFocus();
            return;
        }

        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bloodGroup.equals("Select Blood Group")) {
            Toast.makeText(this, "Please select your blood group", Toast.LENGTH_SHORT).show();
            return;
        }

        if (district.equals("Select District")) {
            Toast.makeText(this, "Please select your district", Toast.LENGTH_SHORT).show();
            return;
        }

        if (taluk.equals("Select Taluk")) {
            Toast.makeText(this, "Please select your taluk", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        setLoading(true);

        final String finalBloodGroup = bloodGroup;
        final String finalDistrict = district;
        final String finalTaluk = taluk;
        final String finalGender = gender;

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Registration successful");
                        String userId = mAuth.getCurrentUser().getUid();

                        // Explicitly use your Firebase Realtime DB URL
                        DatabaseReference dbRef = FirebaseDatabase
                                .getInstance("https://bloodbuddy-26803-default-rtdb.firebaseio.com")
                                .getReference()
                                .child("users")
                                .child(userId);

                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("name", name);
                        userMap.put("email", email);
                        userMap.put("phone", phone);
                        userMap.put("gender", finalGender);
                        userMap.put("bloodGroup", finalBloodGroup);
                        userMap.put("district", finalDistrict);
                        userMap.put("taluk", finalTaluk);

                        dbRef.setValue(userMap).addOnCompleteListener(dbTask -> {
                            setLoading(false);
                            if (dbTask.isSuccessful()) {
                                Log.d(TAG, "User data saved to database");
                                Toast.makeText(RegisterActivity.this,
                                        "Registration successful! Please login.", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            } else {
                                Log.e(TAG, "Failed to save user data", dbTask.getException());
                                Toast.makeText(RegisterActivity.this,
                                        "Account created but profile save failed. Please update profile after login.",
                                        Toast.LENGTH_LONG).show();
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            }
                        });

                    } else {
                        setLoading(false);
                        Log.e(TAG, "Registration failed", task.getException());
                        if (task.getException() instanceof FirebaseAuthException) {
                            FirebaseAuthException authEx = (FirebaseAuthException) task.getException();
                            String errorCode = authEx.getErrorCode();
                            if (errorCode.equals("ERROR_EMAIL_ALREADY_IN_USE")) {
                                Toast.makeText(this, "This email is already registered. Please login.", Toast.LENGTH_LONG).show();
                            } else if (errorCode.equals("ERROR_WEAK_PASSWORD")) {
                                Toast.makeText(this, "Password is too weak. Use at least 6 characters.", Toast.LENGTH_LONG).show();
                            } else if (errorCode.equals("ERROR_INVALID_EMAIL")) {
                                Toast.makeText(this, "Invalid email address.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Registration failed: " + authEx.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(this, "Registration failed. Check your internet connection.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void setLoading(boolean loading) {
        if (loading) {
            registerButton.setEnabled(false);
            registerButton.setText("Creating Account...");
            progressBar.setVisibility(View.VISIBLE);
        } else {
            registerButton.setEnabled(true);
            registerButton.setText("CREATE ACCOUNT");
            progressBar.setVisibility(View.GONE);
        }
    }
}
