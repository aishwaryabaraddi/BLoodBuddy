package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Swiggy/Zomato-style location picker.
 * • "Use my current location" button (GPS auto-detect + reverse-geocode)
 * • Search box with real-time Nominatim (OpenStreetMap) autocomplete
 * • Returns: EXTRA_LAT, EXTRA_LON, EXTRA_ADDRESS via setResult(RESULT_OK)
 */
public class LocationPickerActivity extends AppCompatActivity {

    public static final String EXTRA_ADDRESS = "loc_address";
    public static final String EXTRA_LAT     = "loc_lat";
    public static final String EXTRA_LON     = "loc_lon";

    private static final int  LOCATION_PERMISSION_CODE = 100;
    private static final int  DEBOUNCE_MS              = 400;   // wait 400 ms after last keystroke
    private static final String TAG = "LocationPicker";

    private EditText    etSearch;
    private ProgressBar progressBar;
    private TextView    tvCurrentAddress;
    private TextView    tvSectionLabel;
    private RecyclerView rvResults;
    private ResultAdapter adapter;
    private final List<LocationResult> results = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private final Handler  debounceHandler  = new Handler(Looper.getMainLooper());
    private       Runnable searchRunnable;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etSearch         = findViewById(R.id.etSearch);
        progressBar      = findViewById(R.id.progressBar);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        tvSectionLabel   = findViewById(R.id.tvSectionLabel);
        rvResults        = findViewById(R.id.rvResults);

        adapter = new ResultAdapter(results, this::onLocationSelected);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rvResults.setAdapter(adapter);

        findViewById(R.id.imageViewBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCurrentLocation).setOnClickListener(v -> requestCurrentLocation());

        // Populate the "Use current location" subtitle if GPS is already allowed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            preloadCurrentAddress();
        }

        // Debounced Nominatim search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                debounceHandler.removeCallbacks(searchRunnable);
                String q = s.toString().trim();
                if (q.length() < 3) {
                    results.clear();
                    adapter.notifyDataSetChanged();
                    tvSectionLabel.setVisibility(View.GONE);
                    return;
                }
                searchRunnable = () -> searchNominatim(q);
                debounceHandler.postDelayed(searchRunnable, DEBOUNCE_MS);
            }
        });

        // Open keyboard immediately
        etSearch.requestFocus();
    }

    // ─── Current location ─────────────────────────────────────────────────────

    private void requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            useCurrentLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void preloadCurrentAddress() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc == null) return;
            new Thread(() -> {
                try {
                    Geocoder gc = new Geocoder(this, Locale.getDefault());
                    List<Address> addrs = gc.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                    if (addrs != null && !addrs.isEmpty()) {
                        String addr = addrs.get(0).getAddressLine(0);
                        runOnUiThread(() -> tvCurrentAddress.setText(addr));
                    }
                } catch (Exception ignored) {}
            }).start();
        });
    }

    @SuppressLint("MissingPermission")
    private void useCurrentLocation() {
        progressBar.setVisibility(View.VISIBLE);
        fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                android.location.Location loc = task.getResult();
                reverseGeocodeAndReturn(loc.getLatitude(), loc.getLongitude());
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this,
                        "GPS unavailable — search for your location below.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reverseGeocodeAndReturn(double lat, double lon) {
        new Thread(() -> {
            try {
                Geocoder gc = new Geocoder(this, Locale.getDefault());
                List<Address> addrs = gc.getFromLocation(lat, lon, 1);
                String address = (addrs != null && !addrs.isEmpty())
                        ? addrs.get(0).getAddressLine(0)
                        : String.format(Locale.getDefault(), "%.5f, %.5f", lat, lon);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    returnResult(lat, lon, address);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    returnResult(lat, lon,
                            String.format(Locale.getDefault(), "%.5f, %.5f", lat, lon));
                });
            }
        }).start();
    }

    // ─── Nominatim search ─────────────────────────────────────────────────────

    private void searchNominatim(String query) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                // countrycodes=in limits to India; remove for worldwide
                String urlStr = "https://nominatim.openstreetmap.org/search?q="
                        + encoded + "&format=json&addressdetails=1&limit=6&countrycodes=in";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", getPackageName()); // required by Nominatim ToS

                if (conn.getResponseCode() != 200) {
                    conn.disconnect();
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                conn.disconnect();

                JSONArray arr = new JSONArray(sb.toString());
                List<LocationResult> fresh = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    fresh.add(new LocationResult(
                            obj.getString("display_name"),
                            Double.parseDouble(obj.getString("lat")),
                            Double.parseDouble(obj.getString("lon"))
                    ));
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    results.clear();
                    results.addAll(fresh);
                    adapter.notifyDataSetChanged();
                    tvSectionLabel.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
                });

            } catch (Exception e) {
                Log.e(TAG, "Nominatim error", e);
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        }).start();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void onLocationSelected(LocationResult r) {
        returnResult(r.lat, r.lon, r.displayName);
    }

    private void returnResult(double lat, double lon, String address) {
        Intent data = new Intent();
        data.putExtra(EXTRA_LAT, lat);
        data.putExtra(EXTRA_LON, lon);
        data.putExtra(EXTRA_ADDRESS, address);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == LOCATION_PERMISSION_CODE && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            useCurrentLocation();
        }
    }

    // ─── Data model ───────────────────────────────────────────────────────────

    static class LocationResult {
        final String displayName;
        final double lat, lon;

        LocationResult(String displayName, double lat, double lon) {
            this.displayName = displayName;
            this.lat  = lat;
            this.lon  = lon;
        }
    }

    // ─── RecyclerView adapter ─────────────────────────────────────────────────

    static class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.VH> {

        interface OnPick { void pick(LocationResult r); }

        private final List<LocationResult> items;
        private final OnPick listener;

        ResultAdapter(List<LocationResult> items, OnPick listener) {
            this.items    = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_location_result, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int pos) {
            LocationResult item = items.get(pos);
            // Split "Primary Area, Rest of address"
            String[] parts = item.displayName.split(",", 2);
            holder.tvPrimary.setText(parts[0].trim());
            holder.tvSecondary.setText(parts.length > 1 ? parts[1].trim() : "");
            holder.itemView.setOnClickListener(v -> listener.pick(item));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvPrimary, tvSecondary;
            VH(View v) {
                super(v);
                tvPrimary   = v.findViewById(R.id.tvPrimary);
                tvSecondary = v.findViewById(R.id.tvSecondary);
            }
        }
    }
}
