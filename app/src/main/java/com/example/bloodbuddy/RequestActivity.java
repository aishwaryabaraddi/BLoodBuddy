package com.example.bloodbuddy;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RequestActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RequestAdapter adapter;
    private List<Request> requestList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();
        adapter = new RequestAdapter(requestList);
        recyclerView.setAdapter(adapter);

        // Fetch requests from Firebase
        fetchRequests();
    }

    private void fetchRequests() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("donors").child(userId).child("requests");

            requestsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    requestList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Request request = snapshot.getValue(Request.class);
                        requestList.add(request);
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(RequestActivity.this, "Failed to load requests.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}






























//
//import android.annotation.SuppressLint;
//import android.os.Bundle;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.HashMap;
//import java.util.Map;
////
//public class RequestActivity extends AppCompatActivity {
//    private static String donorName;
//    private static String donorPhoneNumber;
//    private static String requesterName;
//    private static String requesterPhoneNumber;
//    private static String requesterTaluk;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_request);
//
//        // Optionally, you can retrieve and display the stored details here if needed.
//        // Example:
//        // TextView donorNameTextView = findViewById(R.id.donorNameTextView);
//        // donorNameTextView.setText(donorName);
//    }
//
//    public static void storeRequestDetails(String donorName, String donorPhoneNumber, String requesterName, String requesterPhoneNumber, String requesterTaluk) {
//        RequestActivity.donorName = donorName;
//        RequestActivity.donorPhoneNumber = donorPhoneNumber;
//        RequestActivity.requesterName = requesterName;
//        RequestActivity.requesterPhoneNumber = requesterPhoneNumber;
//        RequestActivity.requesterTaluk = requesterTaluk;
//    }
//
//    // Optionally, add getters if you need to retrieve these details from other parts of the app.
//    public static String getDonorName() { return donorName; }
//    public static String getDonorPhoneNumber() { return donorPhoneNumber; }
//    public static String getRequesterName() { return requesterName; }
//    public static String getRequesterPhoneNumber() { return requesterPhoneNumber; }
//    public static String getRequesterTaluk() { return requesterTaluk; }
//}
//
//
//
//
//        // Retrieve donor ID from intent extras
//        String donorId = getIntent().getStringExtra("donorId");
//        if (donorId != null) {
//            // Handle sending user details to this donor
//            sendDetailsToDonor(donorId);
////        } else {
////            Toast.makeText(this, "Donor ID not found", Toast.LENGTH_SHORT).show();
////            finish(); // Finish activity if donor ID is not found
////        }
//        }
//    }
//
//    private void sendDetailsToDonor(String donorId) {
//        // Fetch current user's details from Firebase
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            String userId = currentUser.getUid();
//
//            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
//            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                    // Fetch user data
//                    String userName = dataSnapshot.child("name").getValue(String.class);
//                    String userEmail = dataSnapshot.child("email").getValue(String.class);
//                    // Fetch other user details as needed
//
//                    // Create a UserData object or use HashMap to store user details
//                    Map<String, Object> userData = new HashMap<>();
//                    userData.put("userName", userName);
//                    userData.put("userEmail", userEmail);
//                    // Add other user data
//
//                    // Send user details to the donor
//                    DatabaseReference donorRef = FirebaseDatabase.getInstance().getReference().child("donors").child(donorId);
//                    donorRef.child("requests").push().setValue(userData)
//                            .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                @Override
//                                public void onSuccess(Void aVoid) {
//                                    Toast.makeText(RequestActivity.this, "Request sent successfully", Toast.LENGTH_SHORT).show();
//                                    finish(); // Finish activity after successful request
//                                }
//                            })
//                            .addOnFailureListener(new OnFailureListener() {
//                                @Override
//                                public void onFailure(@NonNull Exception e) {
//                                    Toast.makeText(RequestActivity.this, "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError databaseError) {
//                    // Handle database error
//                    Toast.makeText(RequestActivity.this, "Failed to fetch user data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            });
//        } else {
//            // Handle the case where the user is not authenticated or currentUser is null
//            Toast.makeText(RequestActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
//        }
//    }
//}
