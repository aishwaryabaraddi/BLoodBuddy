package com.example.bloodbuddy;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class Dashboard extends AppCompatActivity {

    private TextView textViewName, textViewEmail;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        textViewName = findViewById(R.id.textView9);
        textViewEmail = findViewById(R.id.textView11);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // Fetch user details from Cloud Firestore
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String name = document.getString("name");
                                String email = document.getString("email");

                                if (name != null) textViewName.setText(name);
                                if (email != null) textViewEmail.setText(email);
                            } else {
                                Log.e("Dashboard", "No such document in Firestore for UID: " + user.getUid());
                            }
                        } else {
                            Log.e("Dashboard", "Firestore fetch failed: ", task.getException());
                        }
                    });
        } else {
            Log.e("Dashboard", "Current user is null");
        }
    }
}
