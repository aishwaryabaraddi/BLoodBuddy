package com.example.bloodbuddy;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailsActivity";
    private DatabaseReference eventsRef;
    private TextView eventNameTextView;
    private TextView eventDateTextView;
    private TextView eventDescriptionTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Initialize Firebase reference
        eventsRef = FirebaseDatabase.getInstance().getReference().child("events");

        // Initialize views
        eventNameTextView = findViewById(R.id.eventNameTextView);
        eventDateTextView = findViewById(R.id.eventDateTextView);
        eventDescriptionTextView = findViewById(R.id.eventDescriptionTextView);

        // Retrieve event ID from intent
        String eventId = getIntent().getStringExtra("eventId");

        // Fetch event details from Firebase
        fetchEventDetails(eventId);
    }

    private void fetchEventDetails(String eventId) {
        eventsRef.child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve event details
                    String eventName = dataSnapshot.child("eventName").getValue(String.class);
                    String eventDate = dataSnapshot.child("eventDate").getValue(String.class);
                    String eventDescription = dataSnapshot.child("eventDescription").getValue(String.class);

                    // Update UI with event details
                    eventNameTextView.setText(eventName);
                    eventDateTextView.setText(eventDate);
                    eventDescriptionTextView.setText(eventDescription);
                } else {
                    Log.w(TAG, "Event not found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch event details: " + databaseError.getMessage());
            }
        });
    }
}
