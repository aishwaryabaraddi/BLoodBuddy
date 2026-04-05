package com.example.bloodbuddy;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Dashboard extends AppCompatActivity {

    private TextView textViewName, textViewEmail;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);


        textViewName = findViewById(R.id.textView9); // Assuming this is the TextView for displaying name
        textViewEmail = findViewById(R.id.textView11); // Assuming this is the TextView for displaying email

        // Get the current user from Firebase Authentication
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Initialize Firebase Realtime Database reference
            usersRef = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());

            // Fetch user details from Firebase Database
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);

                        // Set the fetched data to TextViews
                        if (name != null) {
                            textViewName.setText(name);
                        }
                        if (email != null) {
                            textViewEmail.setText(email);
                        }
                    } else {
                        Log.e("Dashboard", "User data not found in database for UID: " + user.getUid());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("Dashboard", "Failed to fetch user data: " + databaseError.getMessage());
                }
            });
        } else {
            Log.e("Dashboard", "Current user is null");
        }
    }
}
