package com.example.bloodbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminFeedback extends AppCompatActivity {

    private static final String TAG = "AdminFeedback";
    private static final String FALLBACK_ADMIN_EMAIL = "viju.r@gmail.com";

    private RecyclerView recyclerView;
    private FeedbackAdapter feedbackAdapter;
    private List<Feedback> feedbackList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.retreive_feedback);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        feedbackList = new ArrayList<>();
        feedbackAdapter = new FeedbackAdapter(feedbackList);
        recyclerView.setAdapter(feedbackAdapter);

        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to access this page.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkAdminStatus(currentUser);

        // Back → AdminDashboardActivity (not DomainActivity)
        ImageView btnBack = findViewById(R.id.imageView7);
        btnBack.setOnClickListener(v -> goToDashboard());
    }

    @Override
    public void onBackPressed() {
        goToDashboard();
    }

    private void goToDashboard() {
        Intent intent = new Intent(AdminFeedback.this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void checkAdminStatus(FirebaseUser firebaseUser) {
        db.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isAdmin = false;
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.isAdmin()) isAdmin = true;
                    }
                    if (isAdmin || FALLBACK_ADMIN_EMAIL.equalsIgnoreCase(firebaseUser.getEmail())) {
                        fetchFeedbacks();
                    } else {
                        Toast.makeText(this, "Access denied.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (FALLBACK_ADMIN_EMAIL.equalsIgnoreCase(firebaseUser.getEmail())) {
                        fetchFeedbacks();
                    } else {
                        Toast.makeText(this, "Error verifying admin status.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void fetchFeedbacks() {
        db.collection("feedbacks")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        Toast.makeText(this, "Failed to load feedback.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    feedbackList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Feedback feedback = doc.toObject(Feedback.class);
                            feedbackList.add(feedback);
                        }
                    }
                    feedbackAdapter.notifyDataSetChanged();
                });
    }
}
