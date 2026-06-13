package com.example.bloodbuddy;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserFeedback extends AppCompatActivity {

    private TextInputEditText etName, etContact, etEmail, etFeedback;
    private TextInputLayout tilName, tilContact, tilEmail, tilFeedback;
    private MaterialButton btnSubmit;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        db = FirebaseFirestore.getInstance();

        tilName = findViewById(R.id.til_name);
        tilContact = findViewById(R.id.til_contact);
        tilEmail = findViewById(R.id.til_email);
        tilFeedback = findViewById(R.id.til_feedback);
        etName = findViewById(R.id.et_name);
        etContact = findViewById(R.id.et_contact);
        etEmail = findViewById(R.id.et_email);
        etFeedback = findViewById(R.id.et_feedback);
        btnSubmit = findViewById(R.id.btn_submit);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            etEmail.setText(user.getEmail());
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            etName.setText(doc.getString("name"));
                            etContact.setText(doc.getString("phone"));
                        }
                    });
        }

        btnSubmit.setOnClickListener(v -> submitFeedback());
        findViewById(R.id.imageViewBack).setOnClickListener(v -> finish());
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void submitFeedback() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }

        String name = etName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String feedbackMsg = etFeedback.getText().toString().trim();

        tilName.setError(null);
        tilContact.setError(null);
        tilEmail.setError(null);
        tilFeedback.setError(null);

        if (TextUtils.isEmpty(name) || name.length() < 3) {
            tilName.setError("Valid name required");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(contact) || contact.length() != 10
                || !contact.matches("^[6-9]\\d{9}$")) {
            tilContact.setError("Enter a valid 10-digit mobile number");
            etContact.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)
                || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(feedbackMsg)) {
            tilFeedback.setError("Please enter your message");
            etFeedback.requestFocus();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("SENDING...");

        // Safety timeout to reset button if Firestore hangs due to poor 3G connection
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!btnSubmit.isEnabled() && btnSubmit.getText().toString().equals("SENDING...")) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("SUBMIT FEEDBACK");
                Toast.makeText(this, "Submission taking too long. It will sync once connection improves.", Toast.LENGTH_SHORT).show();
            }
        }, 10000);

        String feedbackId = UUID.randomUUID().toString();
        Map<String, Object> fb = new HashMap<>();
        fb.put("feedbackId", feedbackId);
        fb.put("name", name);
        fb.put("contact", contact);
        fb.put("email", email);
        fb.put("feedback", feedbackMsg);
        fb.put("timestamp", System.currentTimeMillis());
        fb.put("userId", FirebaseAuth.getInstance().getUid());

        db.collection("feedbacks").document(feedbackId).set(fb)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Thank you! Feedback sent.", Toast.LENGTH_SHORT).show();
                etFeedback.setText("");
                btnSubmit.setEnabled(true);
                btnSubmit.setText("SUBMIT FEEDBACK");
            })
            .addOnFailureListener(e -> {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("SUBMIT FEEDBACK");
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
