package com.example.bloodbuddy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class DonorsListActivity extends AppCompatActivity {
    private RecyclerView recyclerViewDonors;
    private ArrayList<Donor> donorsList;
    private DatabaseReference donorsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donors_list);

        recyclerViewDonors = findViewById(R.id.donorsRecyclerView);
        recyclerViewDonors.setLayoutManager(new LinearLayoutManager(this));

        // Retrieve donors list from intent
        Intent intent = getIntent();
        ArrayList<String> detectedDonors = intent.getStringArrayListExtra("donorsList");

        // Set up Firebase reference
        donorsRef = FirebaseDatabase.getInstance().getReference().child("donors");

        // Retrieve and filter donors data from Firebase
        retrieveDonorsData(detectedDonors);
    }

    private void retrieveDonorsData(ArrayList<String> detectedDonors) {
        donorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                donorsList = new ArrayList<>();
                for (DataSnapshot donorSnapshot : dataSnapshot.getChildren()) {
                    String name = donorSnapshot.child("name").getValue(String.class);
                    String bloodGroup = donorSnapshot.child("bloodGroup").getValue(String.class);
                    String phoneNumber = donorSnapshot.child("phoneNumber").getValue(String.class);

                    if (name != null && bloodGroup != null && phoneNumber != null && detectedDonors.contains(name)) {
                        donorsList.add(new Donor(name, bloodGroup, phoneNumber));
                    }
                }
                // Set up RecyclerView adapter
                DonorsAdapter adapter = new DonorsAdapter(DonorsListActivity.this, donorsList);
                recyclerViewDonors.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error
                Toast.makeText(DonorsListActivity.this, "Failed to retrieve donor data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Optional: Handle call button click (example)
    public void onCallButtonClick(View view) {
        int position = recyclerViewDonors.getChildLayoutPosition(view);
        String phoneNumber = donorsList.get(position).getPhoneNumber();
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }
}
