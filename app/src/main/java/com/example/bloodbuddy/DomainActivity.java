package com.example.bloodbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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
    private static final String ADMIN_EMAIL = "viju.r@gmail.com";
    private static final String TAG = "DomainActivity";

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

        fetchCarouselFromFirestore();

        runnable = new Runnable() {
            @Override
            public void run() {
                if (imageAdapter.getItemCount() > 1) {
                    if (currentItem >= imageAdapter.getItemCount()) {
                        currentItem = 0;
                    }
                    viewPager.setCurrentItem(currentItem++, true);
                }
                handler.postDelayed(this, 4000);
            }
        };
        handler.postDelayed(runnable, 4000);

        findViewById(R.id.button1_container).setOnClickListener(v -> startActivity(new Intent(this, DonorActivity.class)));
        findViewById(R.id.button2_container).setOnClickListener(v -> startActivity(new Intent(this, ReceiverActivity.class)));
        findViewById(R.id.btn_menu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        ExtendedFloatingActionButton fabAiChat = findViewById(R.id.fab_ai_chat);
        fabAiChat.setOnClickListener(v -> startActivity(new Intent(DomainActivity.this, ChatAiActivity.class)));

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            else if (itemId == R.id.nav_Info) startActivity(new Intent(this, BloodInfoActivity.class));
            else if (itemId == R.id.nav_NearbyBloodbanks) startActivity(new Intent(this, NearbyHospitalsActivity.class));
            else if (itemId == R.id.nav_upload) startActivity(new Intent(this, UploadImage.class));
            else if (itemId == R.id.nav_feedback) startActivity(new Intent(this, AdminFeedback.class));
            else if (itemId == R.id.nav_admin_dashboard) startActivity(new Intent(this, AdminDashboardActivity.class));
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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            
            Menu navMenu = navigationView.getMenu();
            navMenu.findItem(R.id.nav_upload).setVisible(false);
            navMenu.findItem(R.id.nav_feedback).setVisible(false);
            navMenu.findItem(R.id.nav_admin_dashboard).setVisible(false);

            View headerView = navigationView.getHeaderView(0);
            TextView navName = headerView.findViewById(R.id.textView9);
            TextView navEmail = headerView.findViewById(R.id.textView11);
            ImageView navImage = headerView.findViewById(R.id.nav_header_image);
            TextView navInitial = headerView.findViewById(R.id.nav_header_initial);
            
            navEmail.setText(user.getEmail());

            db.collection("users").document(userId).addSnapshotListener((doc, error) -> {
                if (doc != null && doc.exists()) {
                    User userObj = doc.toObject(User.class);
                    if (userObj != null) {
                        // ── Backward-compat: old docs store 'isDonor', new ones store 'donor' ──
                        Boolean legacyDonor = doc.getBoolean("isDonor");
                        if (Boolean.TRUE.equals(legacyDonor)) userObj.setDonor(true);

                        ((TextView)findViewById(R.id.user_welcome)).setText("Hello, " + userObj.getName() + "!");
                        navName.setText("Welcome, " + userObj.getName());

                        // Handle Header Initial/Image
                        if (userObj.getName() != null && !userObj.getName().isEmpty()) {
                            navInitial.setText(userObj.getName().substring(0, 1).toUpperCase());
                        }

                        if (userObj.getImageUrl() != null && !userObj.getImageUrl().isEmpty()) {
                            navInitial.setVisibility(View.GONE);
                            navImage.setVisibility(View.VISIBLE);
                            navImage.setPadding(0,0,0,0);
                            Glide.with(DomainActivity.this)
                                    .load(userObj.getImageUrl())
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(navImage);
                        } else {
                            navInitial.setVisibility(View.VISIBLE);
                            navImage.setVisibility(View.GONE);
                        }

                        boolean isAdmin = userObj.isAdmin() || ADMIN_EMAIL.equalsIgnoreCase(user.getEmail());
                        navMenu.findItem(R.id.nav_upload).setVisible(isAdmin);
                        navMenu.findItem(R.id.nav_feedback).setVisible(isAdmin);
                        navMenu.findItem(R.id.nav_admin_dashboard).setVisible(isAdmin);

                        updateDonorStatus(userObj);
                        subscribeToBloodGroupTopic(userObj.getBloodGroup());
                    }
                }
            });
            listenForActiveRequest(userId);
        }
    }

    private void fetchCarouselFromFirestore() {
        db.collection("carousel_images").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }

            imageUris.clear();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    String url = doc.getString("url");
                    if (url != null) imageUris.add(url);
                }
            }
            
            if (imageUris.isEmpty()) {
                imageUris.add("https://i.ibb.co/v4mS888/blood-donation-tips.jpg");
            }
            
            imageAdapter.notifyDataSetChanged();
            currentItem = 0;
            viewPager.setCurrentItem(0, false);
        });
    }

    private void subscribeToBloodGroupTopic(String bloodGroup) {
        if (bloodGroup == null || bloodGroup.isEmpty() || bloodGroup.equals("Select Blood Group")) return;
        String topic = bloodGroup.replace("+", "_POSITIVE").replace("-", "_NEGATIVE");
        FirebaseMessaging.getInstance().subscribeToTopic(topic);
    }

    private static final long SOS_EXPIRY_MS = TimeUnit.HOURS.toMillis(72);

    private void listenForActiveRequest(String userId) {
        db.collection("receivers")
            .whereEqualTo("userId", userId)
            .whereEqualTo("active", true)
            .addSnapshotListener((value, error) -> {
                Receiver validRequest = null;
                if (value != null) {
                    long now = System.currentTimeMillis();
                    for (QueryDocumentSnapshot doc : value) {
                        Receiver r = doc.toObject(Receiver.class);
                        if (r != null && (now - r.getTimestamp()) <= SOS_EXPIRY_MS) {
                            validRequest = r;
                            break;
                        }
                    }
                }
                if (validRequest != null) {
                    updateActiveSOSCard(validRequest);
                    requestStatusCard.setVisibility(View.VISIBLE);
                } else {
                    requestStatusCard.setVisibility(View.GONE);
                }
            });
    }

    /** Populates the active-SOS card with live request data. */
    private void updateActiveSOSCard(Receiver r) {
        String bg         = r.getBloodGroup() != null ? r.getBloodGroup() : "";
        String patient    = r.getToWhomFor() != null  ? r.getToWhomFor()  : "patient";
        int    responders = r.getResponderIds() == null ? 0 : r.getResponderIds().size();

        requestStatusTitle.setText("🩸 " + bg + " needed for " + patient);

        if (responders > 0) {
            requestStatusSubtitle.setText(
                    "🙋 " + responders + " donor" + (responders > 1 ? "s" : "") +
                    " responded · Tap to manage →");
        } else {
            requestStatusSubtitle.setText("⏳ Looking for nearby donors... · Tap to manage →");
        }

        // Make the entire card tappable → jump straight to the SOS manager
        requestStatusCard.setClickable(true);
        requestStatusCard.setFocusable(true);
        requestStatusCard.setOnClickListener(v ->
                startActivity(new Intent(DomainActivity.this, RequestListActivity.class)));
    }

    private void updateDonorStatus(User user) {
        if (user == null || !user.isDonor()) {
            donorStatusCard.setVisibility(View.GONE);
            return;
        }
        donorStatusCard.setVisibility(View.VISIBLE);

        String bg = (user.getBloodGroup() != null && !user.getBloodGroup().isEmpty()
                && !user.getBloodGroup().startsWith("Select"))
                ? user.getBloodGroup() + " · " : "";
        donorStatusTitle.setText(bg + "Ready to Save Lives 🩸");

        String last = user.getLastDonationDate();
        if (last != null && !last.isEmpty()) {
            donorStatusSubtitle.setText("Last donated: " + last + " · Keep it up!");
        } else {
            donorStatusSubtitle.setText("You are registered as a donor. Thank you! 🙏");
        }

        // Tap donor card → open request list so they can respond to SOS
        donorStatusCard.setClickable(true);
        donorStatusCard.setFocusable(true);
        donorStatusCard.setOnClickListener(v ->
                startActivity(new Intent(DomainActivity.this, RequestListActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
