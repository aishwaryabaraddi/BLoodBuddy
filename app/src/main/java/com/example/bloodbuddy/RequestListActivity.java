package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RequestListActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private DatabaseReference receiverRef;
    private LinearLayout receiverDetailsLayout;
    private String currentUserName;
    private String currentUserPhoneNumber;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request_list);

        // Initialize Firebase Database reference
        receiverRef = FirebaseDatabase.getInstance().getReference().child("receivers");

        // Initialize receiverDetailsLayout
        receiverDetailsLayout = findViewById(R.id.receiverDetailsLayout);

        // Retrieve receiver data from Firebase
        fetchReceiverDetails();

        // Fetch current user data
        fetchCurrentUserData();

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ImageView imageViewBack = findViewById(R.id.imageView8);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to DomainActivity
                Intent intent = new Intent(RequestListActivity.this, DomainActivity.class);
                startActivity(intent);
            }
        });

        // Request SMS permissions if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE);
        }
    }

    private void fetchReceiverDetails() {
        receiverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                receiverDetailsLayout.removeAllViews(); // Clear existing views

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Receiver receiver = snapshot.getValue(Receiver.class);
                    if (receiver != null) {
                        addReceiverDetailsToContainer(receiver);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void fetchCurrentUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    currentUserName = dataSnapshot.child("name").getValue(String.class);
                    currentUserPhoneNumber = dataSnapshot.child("phone").getValue(String.class);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                }
            });
        }
    }

    private void addReceiverDetailsToContainer(Receiver receiver) {
        // Inflate card view for receiver details
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);

        // Create a CardView to wrap receiver details
        androidx.cardview.widget.CardView cardView = new androidx.cardview.widget.CardView(this);
        cardView.setLayoutParams(params);
        cardView.setRadius(8); // Set corner radius
        cardView.setCardBackgroundColor(getResources().getColor(android.R.color.white)); // Set card background color
        cardView.setCardElevation(4); // Set card elevation

        // Create a LinearLayout to hold receiver details
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(16, 16, 16, 16);

        // Create TextViews for each detail
        TextView tvName = createTextView("Name             : " + receiver.getName());
        TextView tvPhoneNumber = createTextView("Contact          : " + receiver.getPhoneNumber());
        TextView tvDistrict = createTextView("District           : " + receiver.getDistrict());
        TextView tvTaluk = createTextView("Taluk              : " + receiver.getTaluk());
        TextView tvBloodGroup = createTextView("Blood Group  : " + receiver.getBloodGroup());
        TextView tvToWhomFor = createTextView("To Whom For: " + receiver.getToWhomFor());
        TextView tvLocation = createTextView("Location        : " + receiver.getLocation());

        // Add TextViews to LinearLayout
        linearLayout.addView(tvName);
        linearLayout.addView(tvPhoneNumber);
        linearLayout.addView(tvDistrict);
        linearLayout.addView(tvTaluk);
        linearLayout.addView(tvBloodGroup);
        linearLayout.addView(tvToWhomFor);
        linearLayout.addView(tvLocation);

        // Create call button
        Button callButton = new Button(this);
        callButton.setText("Call");
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + receiver.getPhoneNumber()));
                startActivity(dialIntent);
            }
        });

        // Create donate button
        Button donateButton = new Button(this);
        donateButton.setText("Donate");
        donateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(RequestListActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    sendSms(receiver);
                } else {
                    Toast.makeText(RequestListActivity.this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Add buttons to LinearLayout
        linearLayout.addView(callButton);
        linearLayout.addView(donateButton);

        // Add LinearLayout to CardView
        cardView.addView(linearLayout);

        // Add CardView to receiverDetailsLayout
        receiverDetailsLayout.addView(cardView);
    }

    private void sendSms(Receiver receiver) {
        String phoneNumber = receiver.getPhoneNumber();
        String message = "I am ready to donate blood. Name: " + currentUserName + ", Phone: " + currentUserPhoneNumber;
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Toast.makeText(this, "SMS sent", Toast.LENGTH_SHORT).show();
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textView.setText(text);
        textView.setTextSize(16);
        return textView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
