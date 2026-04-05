package com.example.bloodbuddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class RequestFormActivity extends AppCompatActivity {

    private EditText editTextHospitalName;
    private EditText editTextName;
    private EditText editTextPhoneNumber;
    private Spinner spinnerBloodGroup;
    private Button buttonSubmit;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request_form_card);

        // Initialize views
        editTextHospitalName = findViewById(R.id.editTextHospitalName);
        editTextName = findViewById(R.id.editTextName);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        // Automatically fetch hospital name
        fetchHospitalName();

        // Populate the spinner with blood groups
        populateBloodGroups();
    }

    private void fetchHospitalName() {
        Intent intent = getIntent();
        String hospitalName = intent.getStringExtra("HOSPITAL_NAME");
        if (hospitalName != null) {
            editTextHospitalName.setText(hospitalName);
        }
    }

    private void populateBloodGroups() {
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adapter);
    }
}
