package com.example.bloodbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RequestListActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private DatabaseReference receiverRef;
    private LinearLayout receiverDetailsLayout;
    private String currentUserName;
    private String currentUserPhoneNumber;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request_list);

        receiverRef = FirebaseDatabase.getInstance().getReference().child("receivers");
        receiverDetailsLayout = findViewById(R.id.receiverDetailsLayout);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            fetchCurrentUserData();
        }

        fetchReceiverDetails();

        findViewById(R.id.imageView8).setOnClickListener(v -> finish());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE);
        }

        loadRequests();
    }

    private void fetchReceiverDetails() {
        receiverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                receiverDetailsLayout.removeAllViews();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Receiver receiver = snapshot.getValue(Receiver.class);
                    if (receiver != null && receiver.isActive()) {
                        addReceiverDetailsToContainer(receiver);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void fetchCurrentUserData() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUserId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentUserName = dataSnapshot.child("name").getValue(String.class);
                currentUserPhoneNumber = dataSnapshot.child("phone").getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void addReceiverDetailsToContainer(Receiver receiver) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(24, 16, 24, 16);
        cardView.setLayoutParams(params);
        cardView.setRadius(16);
        cardView.setCardElevation(8);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        layout.addView(createTextView("Patient: " + receiver.getToWhomFor(), 18, true, Color.BLACK));
        layout.addView(createTextView("Required: " + receiver.getBloodGroup(), 16, true, Color.RED));
        layout.addView(createTextView("Location: " + receiver.getLocation(), 14, false, Color.DKGRAY));
        layout.addView(createTextView("Hospital: " + receiver.getDistrict() + ", " + receiver.getTaluk(), 14, false, Color.GRAY));

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setPadding(0, 20, 0, 0);

        Button callBtn = createButton("CALL", "#F5F5F5", Color.BLACK);
        callBtn.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + receiver.getPhoneNumber())));
        });

        Button donateBtn = createButton("ACCEPT SOS", "#C62828", Color.WHITE);
        
        // Check if already accepted
        if (receiver.getResponderIds() != null && receiver.getResponderIds().contains(currentUserId)) {
            donateBtn.setText("ACCEPTED");
            donateBtn.setEnabled(false);
            donateBtn.setBackgroundColor(Color.LTGRAY);
        }

        donateBtn.setOnClickListener(v -> handleDonateClick(receiver, donateBtn));

        btnLayout.addView(callBtn);
        btnLayout.addView(donateBtn);
        layout.addView(btnLayout);
        cardView.addView(layout);
        receiverDetailsLayout.addView(cardView);
    }

    private void handleDonateClick(Receiver receiver, Button btn) {
        if (currentUserId == null) return;
        
        // 1. Update Realtime Database (The Handshake)
        List<String> responders = receiver.getResponderIds();
        if (responders == null) responders = new ArrayList<>();
        if (!responders.contains(currentUserId)) {
            responders.add(currentUserId);
            receiverRef.child(receiver.getId()).child("responderIds").setValue(responders)
                .addOnSuccessListener(aVoid -> {
                    btn.setText("ACCEPTED");
                    btn.setEnabled(false);
                    btn.setBackgroundColor(Color.LTGRAY);
                    Toast.makeText(this, "Response Recorded! Thank you hero.", Toast.LENGTH_SHORT).show();
                    
                    // 2. Send SMS as Backup
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                        sendSms(receiver);
                    }
                });
        }
    }

    private void sendSms(Receiver receiver) {
        String message = "BloodBuddy: I'm coming to donate " + receiver.getBloodGroup() + 
                         " for " + receiver.getToWhomFor() + ". Contact: " + currentUserName + " (" + currentUserPhoneNumber + ")";
        try {
            SmsManager.getDefault().sendTextMessage(receiver.getPhoneNumber(), null, message, null, null);
        } catch (Exception e) {
            Log.e("SMS", "Failed", e);
        }
    }

    private TextView createTextView(String text, int size, boolean bold, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 4, 0, 4);
        return tv;
    }

    private Button createButton(String text, String bgColor, int textColor) {
        Button btn = new Button(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 120, 1.0f);
        p.setMargins(8, 0, 8, 0);
        btn.setLayoutParams(p);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setBackgroundColor(Color.parseColor(bgColor));
        btn.setTextSize(12);
        return btn;
    }
}
