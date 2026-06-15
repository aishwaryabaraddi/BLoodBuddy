package com.example.bloodbuddy;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private EditText etName, etPhone;
    private Spinner spinnerState, spinnerDistrict, spinnerTaluk;
    private Button btnSave;
    private ProgressBar progressBar;
    private TextView tvEditProfileInitial;
    private FirebaseFirestore db;
    private DocumentReference userDocRef;

    private String[] states = {"Select State", "Karnataka", "Other"};
    private String[] districts = {"Select District", "Bagalkot", "Bangalore Rural", "Bangalore Urban", "Belgaum",
            "Bellary", "Bidar", "Bijapur", "Chamarajanagar", "Chikballapur", "Chikmagalur",
            "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Gulbarga",
            "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysore", "Raichur",
            "Ramanagara", "Shimoga", "Tumkur", "Udupi", "Uttara Kannada", "Yadgir"};

    private String[][] taluks = {
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
        setContentView(R.layout.activity_edit_profile);

        etName = findViewById(R.id.editTextName);
        etPhone = findViewById(R.id.editTextPhone);
        spinnerState = findViewById(R.id.spinnerState);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        spinnerTaluk = findViewById(R.id.spinnerTaluk);
        btnSave = findViewById(R.id.buttonSave);
        progressBar = findViewById(R.id.progressBar);
        tvEditProfileInitial = findViewById(R.id.tvEditProfileInitial);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        db = FirebaseFirestore.getInstance();
        userDocRef = db.collection("users").document(user.getUid());

        setupSpinners();
        loadUserData();

        btnSave.setOnClickListener(v -> saveProfileChanges());
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        setupAdapter(spinnerState, states);
        setupAdapter(spinnerDistrict, districts);
        setupAdapter(spinnerTaluk, taluks[0]);

        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < taluks.length) {
                    setupAdapter(spinnerTaluk, taluks[position]);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupAdapter(Spinner spinner, String[] data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.BLACK);
                ((TextView) v).setTextSize(16);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadUserData() {
        progressBar.setVisibility(View.VISIBLE);
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            progressBar.setVisibility(View.GONE);
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    etName.setText(user.getName());
                    etPhone.setText(user.getPhone());
                    
                    if (user.getName() != null && !user.getName().isEmpty()) {
                        tvEditProfileInitial.setText(user.getName().substring(0, 1).toUpperCase());
                    }

                    setSpinnerSelection(spinnerState, states, user.getState());
                    setSpinnerSelection(spinnerDistrict, districts, user.getDistrict());
                    spinnerDistrict.post(() -> {
                        int distPos = getPosition(districts, user.getDistrict());
                        if (distPos != -1 && distPos < taluks.length) {
                            setupAdapter(spinnerTaluk, taluks[distPos]);
                            setSpinnerSelection(spinnerTaluk, taluks[distPos], user.getTaluk());
                        }
                    });
                }
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Error loading user data", e);
        });
    }

    private void setSpinnerSelection(Spinner spinner, String[] array, String value) {
        if (value != null) {
            int position = getPosition(array, value);
            if (position != -1) {
                spinner.setSelection(position, false);
            }
        }
    }

    private int getPosition(String[] array, String value) {
        if (value == null) return -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(value)) return i;
        }
        return -1;
    }

    private void saveProfileChanges() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String state = spinnerState.getSelectedItem().toString();
        String district = spinnerDistrict.getSelectedItem().toString();
        String taluk = spinnerTaluk.getSelectedItem().toString();

        if (name.length() < 3) {
            etName.setError("Enter a valid name");
            return;
        }

        if (!phone.matches("^[6-9]\\d{9}$")) {
            etPhone.setError("Enter a valid 10-digit mobile number starting with 6-9");
            return;
        }

        if (state.equals("Select State") || district.equals("Select District") || taluk.equals("Select Taluk")) {
            Toast.makeText(this, "Please select all location fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("state", state);
        updates.put("district", district);
        updates.put("taluk", taluk);

        userDocRef.update(updates).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            if (task.isSuccessful()) {
                Toast.makeText(EditProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EditProfileActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
