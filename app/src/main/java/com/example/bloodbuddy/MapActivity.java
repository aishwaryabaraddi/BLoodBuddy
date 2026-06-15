package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
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
    private android.widget.LinearLayout bloodGroupChipRow;
    private View legendIconCard;   // parent CardView of donor button
    private View requestIconCard;  // parent CardView of request button

    // Data — raw lists stored so blood-group filter can re-draw without a new Firestore fetch
    private final java.util.List<Donor>    allDonors   = new java.util.ArrayList<>();
    private final java.util.List<Receiver> allRequests = new java.util.ArrayList<>();
    private String selectedBloodGroup = "All";
    private double myLat = 0, myLon = 0;
    private boolean donorsLoaded   = false;
    private boolean requestsLoaded = false;
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
        bloodGroupChipRow = findViewById(R.id.blood_group_chip_row);

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

        // blood.png has no XML tint — apply white so it shows on the red (active) card bg
        ((android.widget.ImageView) findViewById(R.id.legendIconReceivers))
                .setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));

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
        setupBloodGroupFilter();
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

    // ─── Blood group filter chips ─────────────────────────────────────────────

    private void setupBloodGroupFilter() {
        String[] groups = {"All", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        float density = getResources().getDisplayMetrics().density;
        int hPad = Math.round(14 * density);
        int vPad = Math.round(6 * density);
        int gap  = Math.round(8 * density);

        for (String bg : groups) {
            TextView chip = new TextView(this);
            chip.setText(bg);
            chip.setTextSize(12);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setPadding(hPad, vPad, hPad, vPad);

            android.widget.LinearLayout.LayoutParams lp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, gap, 0);
            chip.setLayoutParams(lp);

            updateChipStyle(chip, bg.equals(selectedBloodGroup));

            chip.setOnClickListener(v -> {
                selectedBloodGroup = bg;
                for (int i = 0; i < bloodGroupChipRow.getChildCount(); i++) {
                    android.view.View child = bloodGroupChipRow.getChildAt(i);
                    if (child instanceof TextView) {
                        updateChipStyle((TextView) child,
                                ((TextView) child).getText().toString().equals(bg));
                    }
                }
                if (donorsLoaded && requestsLoaded) applyBloodGroupFilter();
            });

            bloodGroupChipRow.addView(chip);
        }
    }

    private void updateChipStyle(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.rounded_button);
            chip.getBackground().mutate().setTint(0xFFC62828);
            chip.setTextColor(0xFFFFFFFF);
        } else {
            chip.setBackgroundResource(R.drawable.rounded_button);
            chip.getBackground().mutate().setTint(0xFFF5F5F5);
            chip.setTextColor(0xFF424242);
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
                myLat = loc.getLatitude();
                myLon = loc.getLongitude();
                GeoPoint myPoint = new GeoPoint(myLat, myLon);
                mMapView.getController().animateTo(myPoint);
                mMapView.getController().setZoom(13.0);

                tvMapStats.setText("🔍 Finding donors & requests nearby...");
                loadDonors();
                loadRequests();
            } else {
                mapProgressBar.setVisibility(View.GONE);
                tvMapStats.setText("⚠️ Location unavailable. Enable GPS.");
                Toast.makeText(this, "Enable GPS to see nearby donors.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Firebase Loading ─────────────────────────────────────────────────────

    private void loadDonors() {
        db.collection("donors").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to load donors", task.getException());
                donorsLoaded = true;
                checkLoadingDone();
                return;
            }
            allDonors.clear();
            for (QueryDocumentSnapshot doc : task.getResult()) {
                Donor donor = doc.toObject(Donor.class);
                double dLat = donor.getLatitude();
                double dLon = donor.getLongitude();
                if (dLat == 0.0 && dLon == 0.0) continue;
                if (withinRadius(myLat, myLon, dLat, dLon)) {
                    allDonors.add(donor);
                }
            }
            donorsLoaded = true;
            checkLoadingDone();
        });
    }

    private void loadRequests() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        db.collection("receivers").whereEqualTo("active", true).get()
                .addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to load requests", task.getException());
                requestsLoaded = true;
                checkLoadingDone();
                return;
            }
            allRequests.clear();
            for (QueryDocumentSnapshot doc : task.getResult()) {
                Receiver receiver = doc.toObject(Receiver.class);
                if (myUid.equals(receiver.getUserId())) continue;
                double rLat = receiver.getLatitude();
                double rLon = receiver.getLongitude();
                if (rLat == 0.0 && rLon == 0.0) continue;
                if (withinRadius(myLat, myLon, rLat, rLon)) {
                    allRequests.add(receiver);
                }
            }
            requestsLoaded = true;
            checkLoadingDone();
        });
    }

    private void checkLoadingDone() {
        if (!donorsLoaded || !requestsLoaded) return;
        runOnUiThread(() -> {
            mapProgressBar.setVisibility(View.GONE);
            applyBloodGroupFilter();
        });
    }

    /** Re-draws markers based on the currently selected blood group chip. */
    private void applyBloodGroupFilter() {
        donorOverlay.getItems().clear();
        requestOverlay.getItems().clear();

        int dCount = 0, rCount = 0;
        for (Donor d : allDonors) {
            if ("All".equals(selectedBloodGroup) || selectedBloodGroup.equals(d.getBloodGroup())) {
                addMarker(donorOverlay, d.getLatitude(), d.getLongitude(),
                        d.getName(), d.getBloodGroup(), d.getPhoneNumber(), true);
                dCount++;
            }
        }
        for (Receiver r : allRequests) {
            if ("All".equals(selectedBloodGroup) || selectedBloodGroup.equals(r.getBloodGroup())) {
                addMarker(requestOverlay, r.getLatitude(), r.getLongitude(),
                        r.getName(), r.getBloodGroup(), r.getPhoneNumber(), false);
                rCount++;
            }
        }

        final int fd = dCount, fr = rCount;
        tvDonorCount.setText(fd + " Donors");
        tvRequestCount.setText(fr + " Requests");
        if (fd == 0 && fr == 0) {
            tvMapStats.setText("No results found within 50 km"
                    + ("All".equals(selectedBloodGroup) ? "" : " for " + selectedBloodGroup));
        } else {
            tvMapStats.setText("🩸 " + fd + " donor" + (fd != 1 ? "s" : "")
                    + "  •  🆘 " + fr + " request" + (fr != 1 ? "s" : "")
                    + " within 50 km");
        }
        mMapView.invalidate();
    }

    // ─── Map Markers ──────────────────────────────────────────────────────────

    private void addMarker(FolderOverlay overlay, double lat, double lon,
                           String name, String bloodGroup, String phone, boolean isDonor) {
        // Donors = green person pin (available to help)
        // Receivers = blood drop scaled to 40 dp (urgent SOS)
        Drawable icon = isDonor
                ? scaledIcon(R.drawable.baseline_person_pin_circle_24, 40, 0xFF4CAF50)
                : scaledIcon(R.drawable.blood, 40, 0);

        Marker marker = new Marker(mMapView);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        if (icon != null) marker.setIcon(icon);
        marker.setTitle(name); // needed by OSMDroid internally

        marker.setOnMarkerClickListener((m, mapView) -> {
            showPersonCard(name, bloodGroup, phone, isDonor);
            return true; // suppress default popup
        });

        overlay.add(marker);
    }

    /**
     * Scales any drawable (PNG or vector) to sizeDp × sizeDp.
     * For PNG files BitmapFactory is used directly (preserves colours).
     * For vector XML drawables the drawable is rendered to a canvas.
     * tintColor = 0 → no tint applied (use natural colour of PNG).
     */
    private Drawable scaledIcon(int resId, int sizeDp, int tintColor) {
        int px = Math.round(sizeDp * getResources().getDisplayMetrics().density);

        // Try as PNG/bitmap first
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), resId);
        if (bmp != null) {
            bmp = Bitmap.createScaledBitmap(bmp, px, px, true);
            return new BitmapDrawable(getResources(), bmp);
        }

        // Fall back: vector drawable → canvas
        Drawable d = ContextCompat.getDrawable(this, resId);
        if (d == null) return null;
        d = d.mutate();
        if (tintColor != 0) d.setTint(tintColor);
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, px, px);
        d.draw(c);
        return new BitmapDrawable(getResources(), b);
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

    // RED card   = filter ON  (markers showing)
    // GRAY card  = filter OFF (markers hidden)
    private static final int COLOR_ACTIVE   = 0xFFEC3E3E;
    private static final int COLOR_INACTIVE = 0xFFDDDDDD;
    private static final int COLOR_ICON_ON  = 0xFFFFFFFF;
    private static final int COLOR_ICON_OFF = 0xFF9E9E9E;

    private void toggleDonors() {
        donorsVisible = !donorsVisible;
        androidx.cardview.widget.CardView card =
                (androidx.cardview.widget.CardView) legendIconCard;
        android.widget.ImageView iv =
                (android.widget.ImageView) findViewById(R.id.legendIcon);
        if (donorsVisible) {
            if (!mMapView.getOverlays().contains(donorOverlay))
                mMapView.getOverlays().add(donorOverlay);
            card.setCardBackgroundColor(COLOR_ACTIVE);
            iv.setImageTintList(android.content.res.ColorStateList.valueOf(COLOR_ICON_ON));
            tvDonorCount.setTextColor(0xFFFFFFFF);
        } else {
            mMapView.getOverlays().remove(donorOverlay);
            card.setCardBackgroundColor(COLOR_INACTIVE);
            iv.setImageTintList(android.content.res.ColorStateList.valueOf(COLOR_ICON_OFF));
            tvDonorCount.setTextColor(0xFF9E9E9E);
        }
        mMapView.invalidate();
    }

    private void toggleRequests() {
        requestsVisible = !requestsVisible;
        androidx.cardview.widget.CardView card =
                (androidx.cardview.widget.CardView) requestIconCard;
        android.widget.ImageView iv =
                (android.widget.ImageView) findViewById(R.id.legendIconReceivers);
        if (requestsVisible) {
            if (!mMapView.getOverlays().contains(requestOverlay))
                mMapView.getOverlays().add(requestOverlay);
            card.setCardBackgroundColor(COLOR_ACTIVE);
            iv.setImageTintList(android.content.res.ColorStateList.valueOf(COLOR_ICON_ON));
            tvRequestCount.setTextColor(0xFFFFFFFF);
        } else {
            mMapView.getOverlays().remove(requestOverlay);
            card.setCardBackgroundColor(COLOR_INACTIVE);
            iv.setImageTintList(android.content.res.ColorStateList.valueOf(COLOR_ICON_OFF));
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
