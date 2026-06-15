package com.example.bloodbuddy;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminDonorsActivity extends AppCompatActivity {

    private static final String[] BLOOD_GROUPS =
            {"All", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};

    private LinearLayout chipRow, donorsListLayout;
    private ProgressBar progressBar;
    private TextView tvDonorCount;
    private FirebaseFirestore db;

    private List<Donor> allDonors = new ArrayList<>();
    private String selectedFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_donors);

        db = FirebaseFirestore.getInstance();

        chipRow         = findViewById(R.id.chipRowDonors);
        donorsListLayout = findViewById(R.id.donorsListLayout);
        progressBar     = findViewById(R.id.progressBar);
        tvDonorCount    = findViewById(R.id.tvDonorCount);

        findViewById(R.id.btnBack).setOnClickListener(v -> goToDashboard());

        setupChips();
        loadDonors();
    }

    private void setupChips() {
        for (String bg : BLOOD_GROUPS) {
            TextView chip = new TextView(this);
            chip.setText(bg);
            chip.setTextSize(12);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(6), dp(14), dp(6));
            chip.setTypeface(null, Typeface.BOLD);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(lp);

            updateChipStyle(chip, bg.equals(selectedFilter));

            chip.setOnClickListener(v -> {
                selectedFilter = bg;
                updateAllChipStyles();
                applyFilter();
            });

            chipRow.addView(chip);
        }
    }

    private void updateAllChipStyles() {
        for (int i = 0; i < chipRow.getChildCount(); i++) {
            TextView chip = (TextView) chipRow.getChildAt(i);
            updateChipStyle(chip, chip.getText().toString().equals(selectedFilter));
        }
    }

    private void updateChipStyle(TextView chip, boolean selected) {
        chip.setBackgroundResource(R.drawable.rounded_button);
        if (selected) {
            chip.getBackground().mutate().setTint(0xFFC62828);
            chip.setTextColor(Color.WHITE);
        } else {
            chip.getBackground().mutate().setTint(0xFFEEEEEE);
            chip.setTextColor(0xFF616161);
        }
    }

    private void loadDonors() {
        progressBar.setVisibility(View.VISIBLE);
        donorsListLayout.removeAllViews();

        db.collection("donors").get()
                .addOnSuccessListener(snap -> {
                    allDonors.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Donor d = doc.toObject(Donor.class);
                        if (d != null) {
                            d.setId(doc.getId());
                            allDonors.add(d);
                        }
                    }
                    // Sort alphabetically by name
                    allDonors.sort((a, b) -> {
                        String na = a.getName() != null ? a.getName() : "";
                        String nb = b.getName() != null ? b.getName() : "";
                        return na.compareToIgnoreCase(nb);
                    });
                    progressBar.setVisibility(View.GONE);
                    applyFilter();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load donors.", Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilter() {
        donorsListLayout.removeAllViews();
        List<Donor> filtered = new ArrayList<>();
        for (Donor d : allDonors) {
            if ("All".equals(selectedFilter)
                    || selectedFilter.equals(d.getBloodGroup())) {
                filtered.add(d);
            }
        }

        tvDonorCount.setText(filtered.size() + " donors");

        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No donors found for this blood group.");
            empty.setTextColor(0xFF9E9E9E);
            empty.setTextSize(13);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, 0);
            donorsListLayout.addView(empty);
            return;
        }

        for (Donor d : filtered) {
            donorsListLayout.addView(buildDonorCard(d));
        }
    }

    private View buildDonorCard(Donor d) {
        CardView card = new CardView(this);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setPadding(dp(14), dp(14), dp(14), dp(14));
        body.setGravity(Gravity.CENTER_VERTICAL);

        // Blood group circle badge
        TextView badge = new TextView(this);
        badge.setText(d.getBloodGroup() != null ? d.getBloodGroup() : "?");
        badge.setTextColor(0xFFC62828);
        badge.setTextSize(13);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.circle_button);
        badge.getBackground().mutate().setTint(0xFFFEEBEE);
        int bSize = dp(50);
        badge.setLayoutParams(new LinearLayout.LayoutParams(bSize, bSize));

        // Info column
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = makeText(d.getName() != null ? d.getName() : "Unknown", 14, 0xFF212121, true);
        TextView tvPhone = makeText(
                "📞 " + (d.getPhoneNumber() != null ? d.getPhoneNumber() : "—"), 12, 0xFF616161, false);

        String locationText = "";
        if (d.getDistrict() != null && !d.getDistrict().isEmpty()) {
            locationText = "📍 " + d.getDistrict();
            if (d.getTaluk() != null && !d.getTaluk().isEmpty())
                locationText += ", " + d.getTaluk();
        }
        TextView tvLocation = makeText(locationText.isEmpty() ? "📍 Location not set" : locationText,
                12, 0xFF616161, false);

        String lastDonated = d.getLastDonated() != null && !d.getLastDonated().isEmpty()
                ? "Last donated: " + d.getLastDonated() : "Last donated: Not recorded";
        TextView tvLast = makeText(lastDonated, 11, 0xFF9E9E9E, false);

        info.addView(tvName);
        info.addView(tvPhone);
        info.addView(tvLocation);
        info.addView(tvLast);

        // Call button
        Button btnCall = new Button(this);
        btnCall.setText("Call");
        btnCall.setTextColor(Color.WHITE);
        btnCall.setTextSize(12);
        btnCall.setBackgroundResource(R.drawable.rounded_button);
        btnCall.getBackground().mutate().setTint(0xFF2E7D32);

        LinearLayout.LayoutParams callLp = new LinearLayout.LayoutParams(dp(64), dp(36));
        btnCall.setLayoutParams(callLp);
        btnCall.setPadding(0, 0, 0, 0);

        String phone = d.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            btnCall.setOnClickListener(v -> {
                Intent call = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + phone));
                startActivity(call);
            });
        } else {
            btnCall.setAlpha(0.4f);
            btnCall.setEnabled(false);
        }

        body.addView(badge);
        body.addView(info);
        body.addView(btnCall);
        card.addView(body);
        return card;
    }

    private TextView makeText(String text, int sizeSp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(1), 0, dp(1));
        return tv;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        goToDashboard();
    }
}
