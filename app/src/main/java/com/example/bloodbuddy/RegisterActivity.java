package com.example.bloodbuddy;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbuddy.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final String[] bloodGroups = {"Select Blood Group", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    private final String[] districts = {"Select District", "Bagalkot", "Bangalore Rural", "Bangalore Urban", "Belgaum",
            "Bellary", "Bidar", "Bijapur", "Chamarajanagar", "Chikballapur", "Chikmagalur",
            "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Gulbarga",
            "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysore", "Raichur",
            "Ramanagara", "Shimoga", "Tumkur", "Udupi", "Uttara Kannada", "Yadgir"};

    private final String[][] taluks = {
            {"Select Taluk"},
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
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupSpinners();
        setupDatePicker();

        binding.registerButton.setOnClickListener(v -> validateAndRegister());
        binding.loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setupDatePicker() {
        binding.registerDob.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR) - 18; 
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        String dob = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (monthOfYear + 1), year1);
                        binding.registerDob.setText(dob);
                    }, year, month, day);
            
            Calendar maxDate = Calendar.getInstance();
            maxDate.add(Calendar.YEAR, -18);
            datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
            
            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.YEAR, -65);
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
            
            datePickerDialog.show();
        });
    }

    private void setupSpinners() {
        setupAdapter(binding.spinnerBloodGroup, bloodGroups);
        setupAdapter(binding.spinnerDistrict, districts);

        binding.spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < taluks.length) {
                    setupAdapter(binding.spinnerTaluk, taluks[position]);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupAdapter(android.widget.Spinner spinner, String[] data) {
        if (spinner == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_spinner_item, data) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(getResources().getColor(android.R.color.black));
                ((TextView) v).setTextSize(16);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private boolean isValidName(String name) {
        return name != null && name.trim().length() >= 3 && name.matches("^[a-zA-Z\\s]*$");
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.contains(".");
    }

    private boolean isValidMobile(String phone) {
        if (phone == null || phone.length() != 10) return false;
        if (!phone.matches("^[6-9]\\d{9}$")) return false;
        // Check for dummy repeating numbers like 0000000000, 1111111111, etc.
        for (int i = 0; i <= 9; i++) {
            String dummy = String.format(Locale.getDefault(), "%d%d%d%d%d%d%d%d%d%d", i, i, i, i, i, i, i, i, i, i);
            if (phone.equals(dummy)) return false;
        }
        return true;
    }

    private void validateAndRegister() {
        String name = binding.registerName.getText().toString().trim();
        String email = binding.registerEmail.getText().toString().trim();
        String phone = binding.registerPhone.getText().toString().trim();
        String dob = binding.registerDob.getText().toString().trim();
        String password = binding.registerPassword.getText().toString().trim();

        int selectedGenderId = binding.genderGroup.getCheckedRadioButtonId();
        RadioButton genderButton = findViewById(selectedGenderId);
        String gender = (genderButton == null) ? "" : genderButton.getText().toString().trim();

        String district = binding.spinnerDistrict.getSelectedItem().toString();
        String taluk = binding.spinnerTaluk.getSelectedItem().toString();
        String bloodGroup = binding.spinnerBloodGroup.getSelectedItem().toString();

        if (!isValidName(name)) {
            binding.registerName.setError("Enter a valid name (min 3 chars, letters only)");
            binding.registerName.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            binding.registerEmail.setError("Enter a valid email address (e.g. user@gmail.com)");
            binding.registerEmail.requestFocus();
            return;
        }

        if (!isValidMobile(phone)) {
            binding.registerPhone.setError("Enter a valid 10-digit mobile number starting with 6-9");
            binding.registerPhone.requestFocus();
            return;
        }

        if (dob.isEmpty()) {
            Toast.makeText(this, "Please select Date of Birth", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            binding.registerPassword.setError("Minimum 6 characters required");
            binding.registerPassword.requestFocus();
            return;
        }

        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return;
        }

        if (district.equals("Select District")) {
            Toast.makeText(this, "Please select district", Toast.LENGTH_SHORT).show();
            return;
        }

        if (taluk.equals("Select Taluk")) {
            Toast.makeText(this, "Please select taluk", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bloodGroup.equals("Select Blood Group")) {
            Toast.makeText(this, "Please select blood group", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            
                            User newUser = new User(userId, name, email, phone, "Karnataka", district, taluk, gender, bloodGroup, dob);
                            
                            db.collection("users").document(userId)
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        setLoading(false);
                                        Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, DomainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        setLoading(false);
                                        Toast.makeText(RegisterActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        firebaseUser.delete();
                                    });
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this, "Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        if (loading) {
            binding.registerButton.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.registerButton.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    }
}
