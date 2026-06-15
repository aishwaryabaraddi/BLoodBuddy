package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shows donors within 50 km of the current user, sorted nearest-first.
 * Blood group filter shows compatible donor types (not just exact match).
 * e.g. if you need B+ blood → also shows B- and O+/O- donors who can help.
 */
public class DisplayDonorActivity extends AppCompatActivity {

    private static final String TAG                = "DisplayDonorActivity";
    private static final int    LOCATION_PERM_CODE = 1;
    private static final int    SMS_PERM_CODE      = 2;
    private static final double RADIUS_KM          = 50.0;

    // Blood group compatibility: recipient → list of compatible donor types
    private static final Map<String, List<String>> COMPATIBLE;
    static {
        COMPATIBLE = new HashMap<>();
        COMPATIBLE.put("A+",  Arrays.asList("A+", "A-", "O+", "O-"));
        COMPATIBLE.put("A-",  Arrays.asList("A-", "O-"));
        COMPATIBLE.put("B+",  Arrays.asList("B+", "B-", "O+", "O-"));
        COMPATIBLE.put("B-",  Arrays.asList("B-", "O-"));
        COMPATIBLE.put("AB+", Arrays.asList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        COMPATIBLE.put("AB-", Arrays.asList("A-", "B-", "AB-", "O-"));
        COMPATIBLE.put("O+",  Arrays.asList("O+", "O-"));
        COMPATIBLE.put("O-",  Arrays.asList("O-"));
    }

    private Spinner       spinnerBloodGroup, spinnerDistrict, spinnerTaluk;
    private LinearLayout  donorDetailsContainer;
    private ProgressBar   progressBar;
    private TextView      tvLocationStatus;
    private FirebaseFirestore db;

    private List<Donor>   allDonors = new ArrayList<>();
    private double        myLat = 0, myLon = 0;
    private boolean       locationReady = false;

    private FusedLocationProviderClient fusedClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_donor);

        spinnerBloodGroup     = findViewById(R.id.spinnerBloodGroup);
        spinnerDistrict       = findViewById(R.id.spinnerDistrict);
        spinnerTaluk          = findViewById(R.id.spinnerTaluk);
        donorDetailsContainer = findViewById(R.id.donorDetailsContainer);
        progressBar           = findViewById(R.id.progressBar);
        tvLocationStatus      = findViewById(R.id.tvLocationStatus);   // may be null if not in layout

        db          = FirebaseFirestore.getInstance();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        setupSpinners();
        requestLocation();
        findViewById(R.id.imageView7).setOnClickListener(v -> finish());
    }

    // ─── Location ─────────────────────────────────────────────────────────────

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERM_CODE);
        } else {
            fetchLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        fusedClient.getLastLocation().addOnCompleteListener(task -> {
            Location loc = (task.isSuccessful()) ? task.getResult() : null;
            if (loc != null) {
                myLat = loc.getLatitude();
                myLon = loc.getLongitude();
                locationReady = true;
            }
            // Load donors regardless — if no GPS, skip distance filter
            fetchDonorDetails();
        });
    }

    // ─── Data loading ──────────────────────────────────────────────────────────

    private void fetchDonorDetails() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("donors").get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error fetching donors", task.getException());
                Toast.makeText(this, "Failed to load donors", Toast.LENGTH_SHORT).show();
                return;
            }
            allDonors.clear();
            for (QueryDocumentSnapshot doc : task.getResult()) {
                allDonors.add(doc.toObject(Donor.class));
            }

            if (locationReady) {
                // Sort by distance to current user
                Collections.sort(allDonors, (a, b) -> {
                    double da = haversine(myLat, myLon, a.getLatitude(), a.getLongitude());
                    double db2 = haversine(myLat, myLon, b.getLatitude(), b.getLongitude());
                    return Double.compare(da, db2);
                });
            }

            filterDonors();
        });
    }

    // ─── Filtering ────────────────────────────────────────────────────────────

    private void filterDonors() {
        String selectedBloodGroup = spinnerBloodGroup.getSelectedItem().toString();
        String district           = spinnerDistrict.getSelectedItem().toString();
        String taluk              = spinnerTaluk.getSelectedItem().toString();

        // Build compatible donor types for selected blood group (recipient perspective)
        List<String> compatibleTypes = null;
        if (!selectedBloodGroup.equals("Select Blood Group")) {
            compatibleTypes = COMPATIBLE.get(selectedBloodGroup);
        }
        final List<String> compat = compatibleTypes;

        List<Donor> filtered = new ArrayList<>();
        for (Donor d : allDonors) {
            // Blood group: show compatible types if a specific group is selected
            boolean bgMatch = (compat == null) || compat.contains(d.getBloodGroup());
            // District / taluk: exact match if selected (null-safe — donor may lack these fields)
            boolean dMatch  = district.equals("Select District") || district.equals(d.getDistrict());
            boolean tMatch  = taluk.equals("Select Taluk")       || taluk.equals(d.getTaluk());
            // Distance: only include if within 50 km (skip check if no GPS)
            boolean inRange = !locationReady
                    || (d.getLatitude() == 0 && d.getLongitude() == 0)   // donor didn't share GPS → still show
                    || haversine(myLat, myLon, d.getLatitude(), d.getLongitude()) <= RADIUS_KM;

            if (bgMatch && dMatch && tMatch && inRange) filtered.add(d);
        }

        // Update compatibility hint below spinner
        if (compat != null && compat.size() > 1 && tvLocationStatus != null) {
            tvLocationStatus.setText("Showing donors who can give to " + selectedBloodGroup
                    + " (compatible types: " + String.join(", ", compat) + ")");
            tvLocationStatus.setVisibility(View.VISIBLE);
        } else if (tvLocationStatus != null) {
            tvLocationStatus.setVisibility(View.GONE);
        }

        displayDonors(filtered, selectedBloodGroup);
    }

    private void displayDonors(List<Donor> donors, String selectedGroup) {
        donorDetailsContainer.removeAllViews();

        if (donors.isEmpty()) {
            TextView noResults = new TextView(this);
            String msg = locationReady
                    ? "No donors found within 50 km for your filter."
                    : "No donors found for your selection.";
            noResults.setText(msg);
            noResults.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            noResults.setPadding(0, 50, 0, 0);
            noResults.setTextColor(0xFF757575);
            donorDetailsContainer.addView(noResults);
            return;
        }

        for (Donor donor : donors) {
            addDonorCard(donor, selectedGroup);
        }
    }

    private void addDonorCard(Donor donor, String selectedGroup) {
        View cardView = getLayoutInflater().inflate(R.layout.donor_details_card, null);

        ((TextView) cardView.findViewById(R.id.tvDonorName)).setText(donor.getName());
        ((TextView) cardView.findViewById(R.id.tvDonorPhoneNumber)).setText(donor.getPhoneNumber());
        ((TextView) cardView.findViewById(R.id.tvDonorBloodGroup)).setText(donor.getBloodGroup());
        ((TextView) cardView.findViewById(R.id.tvDonorDistrict)).setText(donor.getDistrict());
        ((TextView) cardView.findViewById(R.id.tvDonorTaluk)).setText(donor.getTaluk());
        String lastDonated = donor.getLastDonated();
        ((TextView) cardView.findViewById(R.id.tvDonorLastDonated))
                .setText("Last donated: " + (lastDonated == null || lastDonated.isEmpty() ? "Never" : lastDonated));

        // Distance badge
        TextView tvLocation = cardView.findViewById(R.id.tvDonorLocation);
        if (locationReady && (donor.getLatitude() != 0 || donor.getLongitude() != 0)) {
            double km = haversine(myLat, myLon, donor.getLatitude(), donor.getLongitude());
            tvLocation.setText("📍 " + donor.getLocation()
                    + "  •  " + String.format(Locale.getDefault(), "%.1f km away", km));
        } else {
            tvLocation.setText("📍 " + donor.getLocation());
        }

        // Compatibility hint on blood group badge
        if (!selectedGroup.equals("Select Blood Group")
                && !donor.getBloodGroup().equals(selectedGroup)) {
            ((TextView) cardView.findViewById(R.id.tvDonorBloodGroup))
                    .setText(donor.getBloodGroup() + " ✓ compatible");
        }

        cardView.findViewById(R.id.btnMakeRequest).setOnClickListener(v -> makeRequest(donor));
        cardView.findViewById(R.id.imageButton).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + donor.getPhoneNumber()))));
        cardView.findViewById(R.id.imageButton2).setOnClickListener(v -> {
            String uri = String.format(Locale.ENGLISH,
                    "google.navigation:q=%f,%f", donor.getLatitude(), donor.getLongitude());
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps?q="
                                + donor.getLatitude() + "," + donor.getLongitude())));
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 24);
        cardView.setLayoutParams(lp);
        donorDetailsContainer.addView(cardView);
    }

    // ─── SMS request ──────────────────────────────────────────────────────────

    private void makeRequest(Donor donor) {
        new AlertDialog.Builder(this)
                .setTitle("Send Blood Request?")
                .setMessage("An SMS will be sent to " + donor.getName()
                        + " (" + donor.getBloodGroup() + ") requesting blood donation.\n\nProceed?")
                .setPositiveButton("Send", (d, w) -> {
                    FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                    if (me == null) return;
                    db.collection("users").document(me.getUid()).get()
                            .addOnSuccessListener(doc -> {
                                User requester = doc.toObject(User.class);
                                if (requester != null) sendSMS(donor, requester);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendSMS(Donor donor, User requester) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERM_CODE);
            return;
        }
        String msg = "Blood Buddy SOS: Hello " + donor.getName()
                + ", I urgently need " + donor.getBloodGroup() + " blood. "
                + "Please contact " + requester.getName()
                + " at " + requester.getPhone() + ". Thank you!";
        try {
            SmsManager.getDefault().sendTextMessage(donor.getPhoneNumber(), null, msg, null, null);
            Toast.makeText(this, "Emergency request sent to " + donor.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "SMS failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Haversine ────────────────────────────────────────────────────────────

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R    = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─── Spinners ─────────────────────────────────────────────────────────────

    private void setupSpinners() {
        List<String> bloodGroups = Arrays.asList(
                "Select Blood Group", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        setupAdapter(spinnerBloodGroup, bloodGroups);

        List<String> districts = Arrays.asList("Select District", "Bagalkot", "Bangalore Rural",
                "Bangalore Urban", "Belgaum", "Bellary", "Bidar", "Bijapur", "Chamarajanagar",
                "Chikballapur", "Chikmagalur", "Chitradurga", "Dakshina Kannada", "Davanagere",
                "Dharwad", "Gadag", "Gulbarga", "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal",
                "Mandya", "Mysore", "Raichur", "Ramanagara", "Shimoga", "Tumkur", "Udupi",
                "Uttara Kannada", "Yadgir");
        setupAdapter(spinnerDistrict, districts);
        setupAdapter(spinnerTaluk, Arrays.asList("Select Taluk"));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (parent.getId() == R.id.spinnerDistrict)
                    updateTalukSpinner(parent.getItemAtPosition(pos).toString());
                if (locationReady || !allDonors.isEmpty()) filterDonors();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerBloodGroup.setOnItemSelectedListener(listener);
        spinnerDistrict.setOnItemSelectedListener(listener);
        spinnerTaluk.setOnItemSelectedListener(listener);
    }

    private void setupAdapter(Spinner spinner, List<String> data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void updateTalukSpinner(String district) {
        List<String> taluks;
        switch (district) {
            case "Bagalkot":         taluks = Arrays.asList("Select Taluk", "Bagalkot", "Badami", "Bilagi", "Hungund", "Jamkhandi", "Mudhol"); break;
            case "Bangalore Rural":  taluks = Arrays.asList("Select Taluk", "Devanahalli", "Doddaballapur", "Hosakote", "Nelamangala"); break;
            case "Bangalore Urban":  taluks = Arrays.asList("Select Taluk", "Anekal", "Bangalore East", "Bangalore North", "Bangalore South", "Yelahanka"); break;
            case "Belgaum":          taluks = Arrays.asList("Select Taluk", "Athani", "Bailhongal", "Belgaum", "Chikodi", "Gokak", "Hukkeri", "Khanapur", "Raibag", "Ramdurg", "Saundatti"); break;
            case "Bellary":          taluks = Arrays.asList("Select Taluk", "Bellary", "Hadagali", "Hagaribommanahalli", "Hospet", "Kudligi", "Sandur", "Siruguppa"); break;
            case "Bidar":            taluks = Arrays.asList("Select Taluk", "Aurad", "Basavakalyan", "Bhalki", "Bidar", "Humnabad"); break;
            case "Bijapur":          taluks = Arrays.asList("Select Taluk", "Basavana Bagewadi", "Bijapur", "Indi", "Muddebihal", "Sindagi"); break;
            case "Chamarajanagar":   taluks = Arrays.asList("Select Taluk", "Chamarajanagar", "Gundlupet", "Kollegal", "Yelandur"); break;
            case "Chikballapur":     taluks = Arrays.asList("Select Taluk", "Bagepalli", "Chikballapur", "Chintamani", "Gauribidanur", "Gudibanda", "Sidlaghatta"); break;
            case "Chikmagalur":      taluks = Arrays.asList("Select Taluk", "Chikmagalur", "Kadur", "Koppa", "Mudigere", "Narasimharajapura", "Sringeri", "Tarikere"); break;
            case "Chitradurga":      taluks = Arrays.asList("Select Taluk", "Challakere", "Chitradurga", "Hiriyur", "Holalkere", "Hosadurga", "Molakalmuru"); break;
            case "Dakshina Kannada": taluks = Arrays.asList("Select Taluk", "Bantwal", "Belthangady", "Mangalore", "Puttur", "Sullia", "Ullal"); break;
            case "Davanagere":       taluks = Arrays.asList("Select Taluk", "Channagiri", "Davanagere", "Harapanahalli", "Harihar", "Honnali", "Jagalur"); break;
            case "Dharwad":          taluks = Arrays.asList("Select Taluk", "Dharwad", "Hubli", "Kalghatgi", "Kundgol", "Navalgund"); break;
            case "Gadag":            taluks = Arrays.asList("Select Taluk", "Gadag", "Mundargi", "Nargund", "Ron", "Shirhatti"); break;
            case "Gulbarga":         taluks = Arrays.asList("Select Taluk", "Afzalpur", "Aland", "Jewargi", "Kalaburagi", "Kamalapur", "Shahbad"); break;
            case "Hassan":           taluks = Arrays.asList("Select Taluk", "Alur", "Arakalagudu", "Arsikere", "Belur", "Channarayapatna", "Hassan", "Holenarsipur", "Sakleshpur"); break;
            case "Haveri":           taluks = Arrays.asList("Select Taluk", "Byadgi", "Hanagal", "Haveri", "Hirekerur", "Ranebennur", "Savanur", "Shiggaon"); break;
            case "Kodagu":           taluks = Arrays.asList("Select Taluk", "Madikeri", "Somwarpet", "Virajpet"); break;
            case "Kolar":            taluks = Arrays.asList("Select Taluk", "Bangarapet", "Kolar", "Malur", "Mulbagal", "Srinivaspur"); break;
            case "Koppal":           taluks = Arrays.asList("Select Taluk", "Gangawati", "Koppal", "Kushtagi", "Yelburga"); break;
            case "Mandya":           taluks = Arrays.asList("Select Taluk", "Krishnarajpet", "Maddur", "Malavalli", "Mandya", "Nagamangala", "Pandavapura", "Srirangapatna"); break;
            case "Mysore":           taluks = Arrays.asList("Select Taluk", "Hunsur", "Krishnarajanagara", "Mysore", "Nanjangud", "Piriyapatna", "Tirumakudal Narsipur"); break;
            case "Raichur":          taluks = Arrays.asList("Select Taluk", "Devadurga", "Lingsugur", "Manvi", "Raichur", "Sindhanur"); break;
            case "Ramanagara":       taluks = Arrays.asList("Select Taluk", "Channapatna", "Kanakapura", "Magadi", "Ramanagaram"); break;
            case "Shimoga":          taluks = Arrays.asList("Select Taluk", "Bhadravathi", "Hosanagara", "Sagara", "Shikarpur", "Shimoga", "Sorab", "Tirthahalli"); break;
            case "Tumkur":           taluks = Arrays.asList("Select Taluk", "Gubbi", "Koratagere", "Kunigal", "Madhugiri", "Pavagada", "Sira", "Tiptur", "Tumkur", "Turuvekere"); break;
            case "Udupi":            taluks = Arrays.asList("Select Taluk", "Karkala", "Kundapura", "Udupi"); break;
            case "Uttara Kannada":   taluks = Arrays.asList("Select Taluk", "Ankola", "Bhatkal", "Dandeli", "Haliyal", "Karwar", "Kumta", "Mundgod", "Siddapur", "Sirsi", "Yellapur"); break;
            case "Yadgir":           taluks = Arrays.asList("Select Taluk", "Shahpur", "Shorapur", "Yadgir"); break;
            default:                 taluks = Arrays.asList("Select Taluk"); break;
        }
        setupAdapter(spinnerTaluk, taluks);
    }

    // ─── Permissions ──────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == LOCATION_PERM_CODE && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else if (code == LOCATION_PERM_CODE) {
            // No GPS — still load donors (without distance filter)
            fetchDonorDetails();
        }
    }
}
