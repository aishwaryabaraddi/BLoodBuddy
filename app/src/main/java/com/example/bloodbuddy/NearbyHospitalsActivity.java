package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NearbyHospitalsActivity extends AppCompatActivity {

    private static final String TAG = "NearbyHospitalsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int SEARCH_RADIUS_METERS = 5000; // 5 km radius

    private MapView mapView;
    private TextView textNearbyHospitals;
    private MaterialCardView hospitalDetailsCardView;
    private TextView hospitalNameTextView;
    private TextView hospitalDistanceTextView;
    private TextView hospitalAddressTextView;
    private TextView hospitalLatTextView;
    private TextView hospitalLonTextView;
    private ProgressBar progressBar;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private MyLocationNewOverlay myLocationOverlay;

    private double myLat = 0, myLon = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Configure OSMDroid before setContentView
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_hospitals);

        mapView               = findViewById(R.id.mapView);
        textNearbyHospitals   = findViewById(R.id.textNearbyHospitals);
        hospitalDetailsCardView  = findViewById(R.id.hospitalDetailsCardView);
        hospitalNameTextView     = findViewById(R.id.hospitalNameTextView);
        hospitalDistanceTextView = findViewById(R.id.hospitalDistanceTextView);
        hospitalAddressTextView  = findViewById(R.id.hospitalAddressTextView);
        hospitalLatTextView      = findViewById(R.id.hospitalLatTextView);
        hospitalLonTextView      = findViewById(R.id.hospitalLonTextView);
        progressBar              = findViewById(R.id.progressBar);

        // Setup OSMDroid (free, no watermark, works worldwide)
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Bottom nav
        findViewById(R.id.nav_donors).setOnClickListener(v -> {
            startActivity(new Intent(this, DisplayDonorActivity.class));
            finish();
        });
        findViewById(R.id.nav_chat).setOnClickListener(v ->
                startActivity(new Intent(this, ChatAiActivity.class)));
        findViewById(R.id.imageView10).setOnClickListener(v -> finish());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermissions();
    }

    // ─── Location ─────────────────────────────────────────────────────────────

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            setupMyLocationOverlay();
            getCurrentLocation();
        }
    }

    private void setupMyLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        progressBar.setVisibility(View.VISIBLE);
        textNearbyHospitals.setText("📍 Getting your location...");
        textNearbyHospitals.setVisibility(View.VISIBLE);

        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000)
                .setNumUpdates(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLocations().isEmpty()) return;
                android.location.Location loc = locationResult.getLocations().get(0);
                myLat = loc.getLatitude();
                myLon = loc.getLongitude();
                fusedLocationClient.removeLocationUpdates(locationCallback);

                GeoPoint myPoint = new GeoPoint(myLat, myLon);
                mapView.getController().animateTo(myPoint);
                mapView.getController().setZoom(14.5);

                fetchNearbyHospitals(myLat, myLon);
            }
        };
        fusedLocationClient.requestLocationUpdates(req, locationCallback, null);
    }

    // ─── Overpass API (OpenStreetMap) — 3 mirrors for reliability ────────────

    // Multiple mirrors: if one is down/blocked, the next is tried automatically
    private static final String[] OVERPASS_MIRRORS = {
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.openstreetmap.fr/api/interpreter"
    };

    private void fetchNearbyHospitals(double lat, double lon) {
        String query = "[out:json][timeout:20];"
                + "(node[\"amenity\"~\"hospital|clinic\"](around:" + SEARCH_RADIUS_METERS + "," + lat + "," + lon + ");"
                + "way[\"amenity\"~\"hospital|clinic\"](around:" + SEARCH_RADIUS_METERS + "," + lat + "," + lon + "););"
                + "out center;";

        new Thread(() -> tryNextMirror(query, 0)).start();
    }

    private void tryNextMirror(String query, int mirrorIndex) {
        if (mirrorIndex >= OVERPASS_MIRRORS.length) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                textNearbyHospitals.setText("⚠️ Could not load hospitals. Check internet.");
                Toast.makeText(this, "All hospital data sources unavailable. Try again.", Toast.LENGTH_LONG).show();
            });
            return;
        }

        try {
            URL url = new URL(OVERPASS_MIRRORS[mirrorIndex]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            byte[] body = ("data=" + URLEncoder.encode(query, "UTF-8")).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                conn.disconnect();
                Log.w(TAG, "Mirror " + mirrorIndex + " returned " + statusCode + ", trying next...");
                tryNextMirror(query, mirrorIndex + 1);
                return;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            conn.disconnect();
            handleHospitalResponse(sb.toString());

        } catch (Exception e) {
            Log.w(TAG, "Mirror " + mirrorIndex + " failed: " + e.getMessage() + ", trying next...");
            tryNextMirror(query, mirrorIndex + 1);
        }
    }

    private void handleHospitalResponse(String response) {
        try {
            JSONObject json     = new JSONObject(response);
            JSONArray  elements = json.getJSONArray("elements");

            List<JSONObject> hospitals = new ArrayList<>();
            for (int i = 0; i < elements.length(); i++) {
                JSONObject el = elements.getJSONObject(i);
                double hLat, hLon;

                String type = el.getString("type");
                if (type.equals("node")) {
                    hLat = el.getDouble("lat");
                    hLon = el.getDouble("lon");
                } else if (el.has("center")) {
                    hLat = el.getJSONObject("center").getDouble("lat");
                    hLon = el.getJSONObject("center").getDouble("lon");
                } else continue;

                JSONObject tags    = el.optJSONObject("tags");
                String name        = tagOr(tags, "name", "Unknown Hospital");
                String phone       = tagOr(tags, "phone",
                                    tagOr(tags, "contact:phone", "N/A"));
                String emergency   = tagOr(tags, "emergency", "unknown");
                String amenity     = tagOr(tags, "amenity", "hospital");

                double dist = haversineKm(myLat, myLon, hLat, hLon);

                JSONObject h = new JSONObject();
                h.put("name", name);
                h.put("lat",  hLat);
                h.put("lon",  hLon);
                h.put("dist", dist);
                h.put("phone", phone);
                h.put("emergency", emergency);
                h.put("amenity", amenity);
                hospitals.add(h);
            }

            // Sort closest first
            hospitals.sort((a, b) -> {
                try { return Double.compare(a.getDouble("dist"), b.getDouble("dist")); }
                catch (JSONException e) { return 0; }
            });

            final List<JSONObject> sorted = hospitals;
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (sorted.isEmpty()) {
                    textNearbyHospitals.setText("No hospitals found within 5 km");
                    return;
                }
                try {
                    String nearest   = sorted.get(0).getString("name");
                    double nearDist  = sorted.get(0).getDouble("dist");
                    textNearbyHospitals.setText("🏥 " + sorted.size()
                            + " found  •  Nearest: " + nearest
                            + " (" + String.format("%.1f", nearDist) + " km)");

                    for (JSONObject h : sorted) addHospitalMarker(h);
                    mapView.invalidate();
                } catch (JSONException e) {
                    Log.e(TAG, "UI update error", e);
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error", e);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                textNearbyHospitals.setText("⚠️ Error loading hospital data");
            });
        }
    }

    // ─── Map markers ──────────────────────────────────────────────────────────

    private void addHospitalMarker(JSONObject h) {
        try {
            double lat  = h.getDouble("lat");
            double lon  = h.getDouble("lon");
            String name = h.getString("name");
            double dist = h.getDouble("dist");
            String phone     = h.getString("phone");
            String emergency = h.getString("emergency");
            String amenity   = h.getString("amenity");

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(lat, lon));
            marker.setTitle(name);
            marker.setSnippet(String.format("%.1f", dist) + " km away");
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_local_hospital_24));
            marker.setOnMarkerClickListener((m, mapV) -> {
                showHospitalDetails(name, dist, phone, emergency, amenity, lat, lon);
                return true; // suppress default popup
            });
            mapView.getOverlays().add(marker);

        } catch (JSONException e) {
            Log.e(TAG, "Marker error", e);
        }
    }

    // ─── Hospital details card ─────────────────────────────────────────────────

    private void showHospitalDetails(String name, double dist, String phone,
                                     String emergency, String amenity, double lat, double lon) {
        hospitalNameTextView.setText(name);
        hospitalDistanceTextView.setText(String.format("%.1f", dist) + " km away");

        // Build info line
        String type = "clinic".equalsIgnoreCase(amenity) ? "🩺 Clinic" : "🏥 Hospital";
        String emgInfo = "yes".equalsIgnoreCase(emergency) ? "  •  🚨 Emergency dept"
                       : "no".equalsIgnoreCase(emergency)  ? "  •  No emergency dept" : "";
        String phoneInfo = !"N/A".equals(phone) ? "\n📞 " + phone : "";
        hospitalAddressTextView.setText(type + emgInfo + phoneInfo);

        hospitalLatTextView.setText("Lat: " + String.format("%.5f", lat));
        hospitalLonTextView.setText("Lon: " + String.format("%.5f", lon));

        hospitalDetailsCardView.setVisibility(View.VISIBLE);

        // Close
        findViewById(R.id.btnCloseCard).setOnClickListener(v ->
                hospitalDetailsCardView.setVisibility(View.GONE));

        // Navigate via Google Maps
        findViewById(R.id.hospitalIconImageView).setOnClickListener(v -> {
            String uri = "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lon;
            Intent nav = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            nav.setPackage("com.google.android.apps.maps");
            if (nav.resolveActivity(getPackageManager()) != null) {
                startActivity(nav);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + lat + "," + lon)));
            }
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String tagOr(JSONObject tags, String key, String fallback) {
        if (tags == null) return fallback;
        return tags.optString(key, fallback);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == LOCATION_PERMISSION_REQUEST_CODE) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                setupMyLocationOverlay();
                getCurrentLocation();
            } else {
                progressBar.setVisibility(View.GONE);
                textNearbyHospitals.setText("⚠️ Location permission needed to find hospitals");
                textNearbyHospitals.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (hospitalDetailsCardView.getVisibility() == View.VISIBLE) {
            hospitalDetailsCardView.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onResume()  { super.onResume();  mapView.onResume();  }
    @Override protected void onPause()   { super.onPause();   mapView.onPause();   }
}
