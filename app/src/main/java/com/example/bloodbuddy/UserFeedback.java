package com.example.bloodbuddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserFeedback extends AppCompatActivity {

    private EditText etName, etContact, etEmail, etFeedback;
    private Button btnSubmit;

    private DatabaseReference feedbackDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // Initialize Firebase Database reference
        feedbackDatabase = FirebaseDatabase.getInstance().getReference("feedback");

        etName = findViewById(R.id.et_name);
        etContact = findViewById(R.id.et_contact);
        etEmail = findViewById(R.id.et_email);
        etFeedback = findViewById(R.id.et_feedback);
        btnSubmit = findViewById(R.id.btn_submit);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitFeedback();
            }
        });

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) ImageView imageViewBack = findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to DomainActivity
                Intent intent = new Intent(UserFeedback.this, DomainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void submitFeedback() {
        String name = etName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String feedback = etFeedback.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(contact)) {
            etContact.setError("Contact is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(feedback)) {
            etFeedback.setError("Feedback is required");
            return;
        }

        String feedbackId = feedbackDatabase.push().getKey();
        Feedback fb = new Feedback(feedbackId, name, contact, email, feedback);
        feedbackDatabase.child(feedbackId).setValue(fb);

        Toast.makeText(this, "Feedback submitted successfully", Toast.LENGTH_SHORT).show();

        // Clear the input fields
        etName.setText("");
        etContact.setText("");
        etEmail.setText("");
        etFeedback.setText("");
    }
}
