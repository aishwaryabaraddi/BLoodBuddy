package com.example.bloodbuddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextView tvName, tvPhoneNumber, tvEmail, tvState, tvDistrict, tvTaluk, tvProfileInitial;
    private ImageView imageViewEdit;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private ListenerRegistration userListener;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        tvName = findViewById(R.id.tvName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvEmail = findViewById(R.id.tvEmail);
        tvState = findViewById(R.id.tvState);
        tvDistrict = findViewById(R.id.tvDistrict);
        tvTaluk = findViewById(R.id.tvTaluk);
        tvProfileInitial = findViewById(R.id.tvProfileInitial);
        progressBar = findViewById(R.id.progressBar);
        imageViewEdit = findViewById(R.id.imageView11);

        db = FirebaseFirestore.getInstance();

        fetchUserData();

        imageViewEdit.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });

        // Back button
        findViewById(R.id.imageView9).setOnClickListener(v -> finish());
    }

    private void fetchUserData() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference docRef = db.collection("users").document(userId);
            userListener = docRef.addSnapshotListener((documentSnapshot, e) -> {
                if (isFinishing()) return;
                progressBar.setVisibility(View.GONE);
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        tvName.setText(user.getName());
                        tvEmail.setText(user.getEmail());
                        tvPhoneNumber.setText(user.getPhone());
                        tvState.setText(user.getState() != null ? user.getState() : "Karnataka");
                        tvDistrict.setText(user.getDistrict());
                        tvTaluk.setText(user.getTaluk());

                        // Show first letter of user name
                        if (user.getName() != null && !user.getName().isEmpty()) {
                            tvProfileInitial.setText(user.getName().substring(0, 1).toUpperCase());
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
        }
    }
}
