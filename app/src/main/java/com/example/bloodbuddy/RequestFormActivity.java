package com.example.bloodbuddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static com.example.bloodbuddy.ValidationUtils.*;

public class RequestFormActivity extends AppCompatActivity {

    private EditText editTextHospitalName;
    private EditText editTextName;
    private EditText editTextPhoneNumber;
    private Spinner spinnerBloodGroup;
    private Button buttonSubmit;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request_form_card);

        db = FirebaseFirestore.getInstance();

        editTextHospitalName = findViewById(R.id.editTextHospitalName);
        editTextName = findViewById(R.id.editTextName);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        fetchHospitalName();
        populateBloodGroups();

        buttonSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void fetchHospitalName() {
        Intent intent = getIntent();
        String hospitalName = intent.getStringExtra("HOSPITAL_NAME");
        if (hospitalName != null) {
            editTextHospitalName.setText(hospitalName);
        }
    }

    private void populateBloodGroups() {
        String[] bloodGroups = {"Select Blood Group", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adapter);
    }

    private void validateAndSubmit() {
        String hospitalName = editTextHospitalName.getText().toString().trim();
        String name = editTextName.getText().toString().trim();
        String phone = editTextPhoneNumber.getText().toString().trim();
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();

        if (hospitalName.isEmpty()) {
            editTextHospitalName.setError("Hospital name is required");
            editTextHospitalName.requestFocus();
            return;
        }

        if (!isValidName(name)) {
            editTextName.setError("Enter a valid name (min 3 chars)");
            editTextName.requestFocus();
            return;
        }

        if (!isValidMobile(phone)) {
            editTextPhoneNumber.setError("Enter a valid 10-digit mobile number starting with 6-9");
            editTextPhoneNumber.requestFocus();
            return;
        }

        if (bloodGroup.equals("Select Blood Group")) {
            Toast.makeText(this, "Please select a blood group", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSubmit.setEnabled(false);
        buttonSubmit.setText("SUBMITTING...");

        String userId = FirebaseAuth.getInstance().getUid();
        String requestId = db.collection("hospitalRequests").document().getId();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("userId", userId);
        request.put("hospitalName", hospitalName);
        request.put("name", name);
        request.put("phone", phone);
        request.put("bloodGroup", bloodGroup);
        request.put("timestamp", System.currentTimeMillis());

        db.collection("hospitalRequests").document(requestId).set(request)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request submitted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    buttonSubmit.setEnabled(true);
                    buttonSubmit.setText("Submit");
                    Toast.makeText(this, "Submission failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
