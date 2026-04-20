package com.example.bloodbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DomainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ViewPager2 viewPager;
    ImageAdapter imageAdapter;
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable runnable;
    int currentItem = 0;
    List<String> imageUris = new ArrayList<>();

    private CardView donorStatusCard, requestStatusCard;
    private TextView donorStatusTitle, donorStatusSubtitle;
    private TextView requestStatusTitle, requestStatusSubtitle;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_domain);

        db = FirebaseFirestore.getInstance();
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        donorStatusCard = findViewById(R.id.donor_status_card);
        donorStatusTitle = findViewById(R.id.donor_status_title);
        donorStatusSubtitle = findViewById(R.id.donor_status_subtitle);

        requestStatusCard = findViewById(R.id.request_status_card);
        requestStatusTitle = findViewById(R.id.request_status_title);
        requestStatusSubtitle = findViewById(R.id.request_status_subtitle);

        navigationView.setBackgroundColor(Color.WHITE);
        navigationView.setItemIconTintList(ContextCompat.getColorStateList(this, R.color.red));
        navigationView.setItemTextColor(ContextCompat.getColorStateList(this, R.color.red));

        viewPager = findViewById(R.id.viewPager);
        imageAdapter = new ImageAdapter(this, imageUris);
        viewPager.setAdapter(imageAdapter);

        fetchImageUrisFromFirebase();

        runnable = new Runnable() {
            @Override
            public void run() {
                if (imageAdapter.getItemCount() > 0) {
                    if (currentItem >= imageAdapter.getItemCount()) {
                        currentItem = 0;
                    }
                    viewPager.setCurrentItem(currentItem++, true);
                }
                handler.postDelayed(this, 3000);
            }
        };
        handler.postDelayed(runnable, 3000);

        findViewById(R.id.button1_container).setOnClickListener(v -> startActivity(new Intent(this, DonorActivity.class)));
        findViewById(R.id.button2_container).setOnClickListener(v -> startActivity(new Intent(this, ReceiverActivity.class)));
        findViewById(R.id.btn_menu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Set up the AI Chat FAB listener
        FloatingActionButton fabAiChat = findViewById(R.id.fab_ai_chat);
        fabAiChat.setOnClickListener(v -> startActivity(new Intent(DomainActivity.this, ChatAiActivity.class)));

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            else if (itemId == R.id.nav_Info) startActivity(new Intent(this, BloodInfoActivity.class));
            else if (itemId == R.id.nav_NearbyBloodbanks) startActivity(new Intent(this, NearbyHospitalsActivity.class));
            else if (itemId == R.id.nav_upload) startActivity(new Intent(this, UploadImage.class));
            else if (itemId == R.id.nav_feedback) startActivity(new Intent(this, AdminFeedback.class));
            else if (itemId == R.id.nav_LogOut) handleLogout();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.footer);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_request_list) startActivity(new Intent(this, RequestListActivity.class));
            else if (itemId == R.id.navigation_donor_list) startActivity(new Intent(this, DisplayDonorActivity.class));
            else if (itemId == R.id.navigation_map) startActivity(new Intent(this, MapActivity.class));
            else if (itemId == R.id.navigation_feedback) startActivity(new Intent(this, UserFeedback.class));
            return true;
        });

        checkAndRequestPermissions();

        PeriodicWorkRequest locationWorkRequest = new PeriodicWorkRequest.Builder(LocationWorker.class, 15, TimeUnit.MINUTES).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("LocationWork", ExistingPeriodicWorkPolicy.KEEP, locationWorkRequest);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId).addSnapshotListener((doc, error) -> {
                if (doc != null && doc.exists()) {
                    User userObj = doc.toObject(User.class);
                    if (userObj != null) {
                        ((TextView)findViewById(R.id.user_welcome)).setText("Hello, " + userObj.getName() + "!");
                        updateDonorStatus(userObj);
                        subscribeToBloodGroupTopic(userObj.getBloodGroup());
                    }
                }
            });
            listenForActiveRequest(userId);
        }
    }

    private void subscribeToBloodGroupTopic(String bloodGroup) {
        if (bloodGroup == null || bloodGroup.isEmpty() || bloodGroup.equals("Select Blood Group")) return;
        
        // Sanitize topic name: "A+" -> "A_POSITIVE", "O-" -> "O_NEGATIVE"
        String topic = bloodGroup.replace("+", "_POSITIVE").replace("-", "_NEGATIVE");
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("FCM", "Subscribed to topic: " + topic);
                }
            });
    }

    private void listenForActiveRequest(String userId) {
        db.collection("receivers")
            .whereEqualTo("userId", userId)
            .whereEqualTo("active", true)
            .addSnapshotListener((value, error) -> {
                if (value != null && !value.isEmpty()) {
                    requestStatusCard.setVisibility(View.VISIBLE);
                    com.google.firebase.firestore.DocumentSnapshot doc = value.getDocuments().get(0);
                    Receiver receiver = doc.toObject(Receiver.class);
                    if (receiver != null) {
                        int helpCount = (receiver.getResponderIds() != null) ? receiver.getResponderIds().size() : 0;
                        if (helpCount > 0) {
                            requestStatusTitle.setText(helpCount + " Donors are coming to help!");
                            requestStatusSubtitle.setText("Contact details sent to your SMS.");
                        } else {
                            requestStatusTitle.setText("SOS Active: Finding Donors...");
                            requestStatusSubtitle.setText("We notified nearby " + receiver.getBloodGroup() + " donors.");
                        }
                    }
                } else {
                    requestStatusCard.setVisibility(View.GONE);
                }
            });
    }

    private void updateDonorStatus(User user) {
        if (user == null || !user.isDonor()) {
            donorStatusCard.setVisibility(View.GONE);
            return;
        }

        donorStatusCard.setVisibility(View.VISIBLE);
        String lastDonation = user.getLastDonationDate();
        
        if (lastDonation == null || lastDonation.isEmpty()) {
            donorStatusTitle.setText("Status: Ready to Save Lives");
            donorStatusTitle.setTextColor(Color.parseColor("#2E7D32"));
            donorStatusCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            donorStatusSubtitle.setText("You are eligible to donate.");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date lastDate = sdf.parse(lastDonation);
            long days = TimeUnit.DAYS.convert(new Date().getTime() - lastDate.getTime(), TimeUnit.MILLISECONDS);
            int waitDays = "Male".equalsIgnoreCase(user.getGender()) ? 90 : 120;
            
            if (days < waitDays) {
                donorStatusCard.setCardBackgroundColor(Color.parseColor("#FFF5F5"));
                donorStatusTitle.setText("Status: Recovering");
                donorStatusTitle.setTextColor(Color.RED);
                donorStatusSubtitle.setText("Eligible in " + (waitDays - days) + " days.");
            } else {
                donorStatusCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                donorStatusTitle.setText("Status: Eligible");
                donorStatusTitle.setTextColor(Color.parseColor("#2E7D32"));
                donorStatusSubtitle.setText("You are ready to donate again!");
            }
        } catch (ParseException e) {
            donorStatusCard.setVisibility(View.GONE);
        }
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void handleLogout() {
        // Unsubscribe from topic on logout to prevent notifications for other users
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String bloodGroup = doc.getString("bloodGroup");
                    if (bloodGroup != null) {
                        String topic = bloodGroup.replace("+", "_POSITIVE").replace("-", "_NEGATIVE");
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
                    }
                }
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(DomainActivity.this, LoginActivity.class));
                finish();
            });
        } else {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void fetchImageUrisFromFirebase() {
        FirebaseDatabase.getInstance().getReference("images").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                imageUris.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String uri = snapshot.getValue(String.class);
                    if (uri != null) imageUris.add(uri);
                }
                imageAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }
}
