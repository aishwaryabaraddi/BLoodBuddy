package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.google.firebase.auth.FirebaseAuth;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_CODE = 1;
    private static final double RADIUS_KM = 50.0;

    // Map
    private MapView mMapView;
    private MyLocationNewOverlay myLocationOverlay;
    private FolderOverlay donorOverlay   = new FolderOverlay();
    private FolderOverlay requestOverlay = new FolderOverlay();
    private boolean donorsVisible   = true;
    private boolean requestsVisible = true;

    // UI
    private TextView tvMapStats;
    private TextView tvDonorCount;
    private TextView tvRequestCount;
    private CardView personDetailsCard;
    private TextView tvCardName, tvCardBloodBadge, tvCardType, tvCardPhone;
    private ProgressBar mapProgressBar;
    private View legendIconCard;   // parent CardView of donor button
    private View requestIconCard;  // parent CardView of request button

    // Data
    private int donorCount   = 0;
    private int requestCount = 0;
    private String currentPhone = "";

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        mMapView         = findViewById(R.id.mapView);
        tvMapStats       = findViewById(R.id.tvMapStats);
        tvDonorCount     = findViewById(R.id.tvDonorCount);
        tvRequestCount   = findViewById(R.id.tvRequestCount);
        personDetailsCard = findViewById(R.id.personDetailsCard);
        tvCardName       = findViewById(R.id.tvCardName);
        tvCardBloodBadge = findViewById(R.id.tvCardBloodBadge);
        tvCardType       = findViewById(R.id.tvCardType);
        tvCardPhone      = findViewById(R.id.tvCardPhone);
        mapProgressBar   = findViewById(R.id.mapProgressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();

        // OSMDroid setup
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setMultiTouchControls(true);
        mMapView.getController().setZoom(13.0);

        // My location overlay — custom red map-pin icon instead of default white arrow
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMapView);
        myLocationOverlay.enableMyLocation();
        setLocationPinIcon();
        mMapView.getOverlays().add(myLocationOverlay);

        // Add overlay groups (for toggle control)
        mMapView.getOverlays().add(donorOverlay);
        mMapView.getOverlays().add(requestOverlay);

        // Buttons
        findViewById(R.id.imageViewBack).setOnClickListener(v -> finish());
        personDetailsCard.setVisibility(View.GONE);

        // Filter buttons — toggle markers on/off
        legendIconCard  = (View) ((View) findViewById(R.id.legendIcon)).getParent();
        requestIconCard = (View) ((View) findViewById(R.id.legendIconReceivers)).getParent();

        findViewById(R.id.legendIcon).setOnClickListener(v -> toggleDonors());
        findViewById(R.id.legendIconReceivers).setOnClickListener(v -> toggleRequests());

        // Close detail card
        findViewById(R.id.btnCloseCard).setOnClickListener(v ->
                personDetailsCard.setVisibility(View.GONE));

        // Call button
        findViewById(R.id.btnCall).setOnClickListener(v -> {
            if (!currentPhone.isEmpty()) {
                Intent call = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + currentPhone));
                startActivity(call);
            }
        });

        setupBottomNav();
        checkPermissions();
    }

    // ─── Location pin icon ────────────────────────────────────────────────────

    /** Converts the @drawable/nearby vector to a red Bitmap and sets it as the
     *  "current location" marker on MyLocationNewOverlay. */
    private void setLocationPinIcon() {
        try {
            Drawable pin = ContextCompat.getDrawable(this, R.drawable.nearby);
            if (pin == null) return;
            pin = pin.mutate();
            pin.setTint(0xFFEC3E3E); // app red

            // 48 dp → pixels
            int px = Math.round(48 * getResources().getDisplayMetrics().density);
            Bitmap bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            pin.setBounds(0, 0, px, px);
            pin.draw(canvas);

            myLocationOverlay.setPersonIcon(bmp);
            // Hotspot: tip of the pin is at horizontal-center, bottom edge
            myLocationOverlay.setPersonHotspot(px / 2.0f, px);
        } catch (Exception e) {
            Log.w(TAG, "Could not set custom location icon", e);
            // Falls back to OSMDroid's default white arrow — no crash
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_map);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_request_list) {
                startActivity(new Intent(this, RequestListActivity.class)); finish();
            } else if (id == R.id.navigation_donor_list) {
                startActivity(new Intent(this, DisplayDonorActivity.class)); finish();
            } else if (id == R.id.navigation_feedback) {
                startActivity(new Intent(this, UserFeedback.class)); finish();
            }
            return true;
        });
    }

    // ─── Permissions & Location ───────────────────────────────────────────────

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            loadMap();
        }
    }

    @SuppressLint("MissingPermission")
    private void loadMap() {
        mapProgressBar.setVisibility(View.VISIBLE);
        tvMapStats.setVisibility(View.VISIBLE);
        tvMapStats.setText("📍 Locating you...");

        myLocationOverlay.enableFollowLocation();

        fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Location loc = task.getResult();
                GeoPoint myPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                mMapView.getController().animateTo(myPoint);
                mMapView.getController().setZoom(13.0);

                tvMapStats.setText("🔍 Finding donors & requests nearby...");
                loadDonors(loc.getLatitude(), loc.getLongitude());
                loadRequests(loc.getLatitude(), loc.getLongitude());
            } else {
                mapProgressBar.setVisibility(View.GONE);
                tvMapStats.setText("⚠️ Location unavailable. Enable GPS.");
                Toast.makeText(this, "Enable GPS to see nearby donors.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Firebase Loading ─────────────────────────────────────────────────────

    private void loadDonors(double myLat, double myLon) {
        db.collection("donors").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to load donors", task.getException());
                checkLoadingDone();
                return;
            }
            donorCount = 0;
            donorOverlay.getItems().clear();

            for (QueryDocumentSnapshot doc : task.getResult()) {
                Donor donor = doc.toObject(Donor.class);
                double dLat = donor.getLatitude();
                double dLon = donor.getLongitude();

                // Skip if no location stored
                if (dLat == 0.0 && dLon == 0.0) continue;

                if (withinRadius(myLat, myLon, dLat, dLon)) {
                    donorCount++;
                    addMarker(donorOverlay, dLat, dLon,
                            donor.getName(),
                            donor.getBloodGroup(),
                            donor.getPhoneNumber(),
                            true);
                }
            }
            runOnUiThread(() -> {
                tvDonorCount.setText(donorCount + " Donors");
                mMapView.invalidate();
                checkLoadingDone();
            });
        });
    }

    private void loadRequests(double myLat, double myLon) {
        db.collection("receivers")
                .whereEqualTo("active", true)
                .get()
                .addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to load requests", task.getException());
                checkLoadingDone();
                return;
            }
            requestCount = 0;
            requestOverlay.getItems().clear();

            for (QueryDocumentSnapshot doc : task.getResult()) {
                Receiver receiver = doc.toObject(Receiver.class);
                double rLat = receiver.getLatitude();
                double rLon = receiver.getLongitude();

                if (rLat == 0.0 && rLon == 0.0) continue;

                if (withinRadius(myLat, myLon, rLat, rLon)) {
                    requestCount++;
                    addMarker(requestOverlay, rLat, rLon,
                            receiver.getName(),
                            receiver.getBloodGroup(),
                            receiver.getPhoneNumber(),
                            false);
                }
            }
            runOnUiThread(() -> {
                tvRequestCount.setText(requestCount + " Requests");
                mMapView.invalidate();
                checkLoadingDone();
            });
        });
    }

    private void checkLoadingDone() {
        runOnUiThread(() -> {
            mapProgressBar.setVisibility(View.GONE);
            if (donorCount == 0 && requestCount == 0) {
                tvMapStats.setText("No donors or requests found within 50 km");
            } else {
                tvMapStats.setText("🩸 " + donorCount + " donor"
                        + (donorCount != 1 ? "s" : "")
                        + "  •  🆘 " + requestCount + " request"
                        + (requestCount != 1 ? "s" : "")
                        + " within 50 km");
            }
        });
    }

    // ─── Map Markers ──────────────────────────────────────────────────────────

    private void addMarker(FolderOverlay overlay, double lat, double lon,
                           String name, String bloodGroup, String phone, boolean isDonor) {
        int iconRes = isDonor
                ? R.drawable.baseline_person_pin_circle_24
                : R.drawable.blood;
        Drawable icon = ContextCompat.getDrawable(this, iconRes);

        Marker marker = new Marker(mMapView);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(icon);
        marker.setTitle(name); // needed by OSMDroid internally

        marker.setOnMarkerClickListener((m, mapView) -> {
            showPersonCard(name, bloodGroup, phone, isDonor);
            return true; // suppress default popup
        });

        overlay.add(marker);
    }

    // ─── Detail Card ──────────────────────────────────────────────────────────

    private void showPersonCard(String name, String blood, String phone, boolean isDonor) {
        tvCardName.setText(name);
        tvCardBloodBadge.setText(blood);
        tvCardType.setText(isDonor ? "🩸 Donor" : "🆘 Needs Blood");
        tvCardPhone.setText(phone);
        currentPhone = phone;
        personDetailsCard.setVisibility(View.VISIBLE);
    }

    // ─── Filter Toggles ───────────────────────────────────────────────────────

    private void toggleDonors() {
        donorsVisible = !donorsVisible;
        if (donorsVisible) {
            if (!mMapView.getOverlays().contains(donorOverlay))
                mMapView.getOverlays().add(donorOverlay);
            ((androidx.cardview.widget.CardView) legendIconCard)
                    .setCardBackgroundColor(getColor(android.R.color.white));
            ((android.widget.ImageView) findViewById(R.id.legendIcon))
                    .setImageTintList(android.content.res.ColorStateList.valueOf(0xFFEC3E3E));
            tvDonorCount.setTextColor(0xFF212121);
        } else {
            mMapView.getOverlays().remove(donorOverlay);
            ((androidx.cardview.widget.CardView) legendIconCard)
                    .setCardBackgroundColor(0xFF9E9E9E);
            ((android.widget.ImageView) findViewById(R.id.legendIcon))
                    .setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
            tvDonorCount.setTextColor(0xFF9E9E9E);
        }
        mMapView.invalidate();
    }

    private void toggleRequests() {
        requestsVisible = !requestsVisible;
        if (requestsVisible) {
            if (!mMapView.getOverlays().contains(requestOverlay))
                mMapView.getOverlays().add(requestOverlay);
            ((androidx.cardview.widget.CardView) requestIconCard)
                    .setCardBackgroundColor(getColor(android.R.color.white));
            tvRequestCount.setTextColor(0xFF212121);
        } else {
            mMapView.getOverlays().remove(requestOverlay);
            ((androidx.cardview.widget.CardView) requestIconCard)
                    .setCardBackgroundColor(0xFF9E9E9E);
            tvRequestCount.setTextColor(0xFF9E9E9E);
        }
        mMapView.invalidate();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean withinRadius(double lat1, double lon1, double lat2, double lon2) {
        double R    = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) <= RADIUS_KM;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean withinRadius(double lat1, double lon1, double lat2, double lon2) {
        double R    = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) <= RADIUS_KM;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        if (personDetailsCard.getVisibility() == View.VISIBLE) {
            personDetailsCard.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == LOCATION_PERMISSION_CODE
                && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            loadMap();
        }
    }

    @Override protected void onResume()  { super.onResume();  mMapView.onResume();  }
    @Override protected void onPause()   { super.onPause();   mMapView.onPause();   }
}
