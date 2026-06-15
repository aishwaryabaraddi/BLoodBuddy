package com.example.bloodbuddy;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AdminDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String ADMIN_EMAIL = "viju.r@gmail.com";
    private static final long   EXPIRY_MS   = TimeUnit.HOURS.toMillis(72);

    private DrawerLayout drawerLayout;
    private TextView tvTotalRequests, tvActiveNow, tvViaApp, tvViaExternal;
    private TextView tvCancelled, tvFulfillmentRate, tvTotalDonors, tvTotalUsers;
    private LinearLayout recentActivityLayout, activeSosLayout;
    private ProgressBar  progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.admin_drawer_layout);

        tvTotalRequests   = findViewById(R.id.tvTotalRequests);
        tvActiveNow       = findViewById(R.id.tvActiveNow);
        tvViaApp          = findViewById(R.id.tvViaApp);
        tvViaExternal     = findViewById(R.id.tvViaExternal);
        tvCancelled       = findViewById(R.id.tvCancelled);
        tvFulfillmentRate = findViewById(R.id.tvFulfillmentRate);
        tvTotalDonors     = findViewById(R.id.tvTotalDonors);
        tvTotalUsers      = findViewById(R.id.tvTotalUsers);
        recentActivityLayout = findViewById(R.id.recentActivityLayout);
        activeSosLayout      = findViewById(R.id.activeSosLayout);
        progressBar          = findViewById(R.id.progressBar);

        // Hamburger opens drawer
        ImageView btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // "View All →" SOS link
        TextView tvViewAllSos = findViewById(R.id.tvViewAllSos);
        tvViewAllSos.setOnClickListener(v ->
                startActivity(new Intent(this, AdminSOSActivity.class)));

        // Wire navigation drawer
        NavigationView navView = findViewById(R.id.admin_nav_view);
        navView.setNavigationItemSelectedListener(this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        // Show admin email in drawer header
        View header = navView.getHeaderView(0);
        if (header != null) {
            TextView tvEmail = header.findViewById(R.id.tvAdminNavEmail);
            if (tvEmail != null) tvEmail.setText(user.getEmail());
        }

        verifyAdminAndLoad(user);
    }

    // ─── Navigation drawer ────────────────────────────────────────────────────

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        int id = item.getItemId();

        if (id == R.id.admin_nav_dashboard) {
            // already here — do nothing
        } else if (id == R.id.admin_nav_donors) {
            startActivity(new Intent(this, AdminDonorsActivity.class));
        } else if (id == R.id.admin_nav_sos) {
            startActivity(new Intent(this, AdminSOSActivity.class));
        } else if (id == R.id.admin_nav_users) {
            startActivity(new Intent(this, AdminUsersActivity.class));
        } else if (id == R.id.admin_nav_feedbacks) {
            startActivity(new Intent(this, AdminFeedback.class));
        } else if (id == R.id.admin_nav_carousel) {
            startActivity(new Intent(this, UploadImage.class));
        } else if (id == R.id.admin_nav_logout) {
            confirmLogout();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            confirmLogout();
        }
    }

    // ─── Auth check ───────────────────────────────────────────────────────────

    private void verifyAdminAndLoad(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    boolean isAdmin = false;
                    if (doc.exists()) {
                        User u = doc.toObject(User.class);
                        isAdmin = u != null && u.isAdmin();
                    }
                    if (isAdmin || ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
                        loadStats();
                    } else {
                        Toast.makeText(this, "Access denied.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
                        loadStats();
                    } else {
                        Toast.makeText(this, "Error verifying admin status.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    // ─── Data loading ─────────────────────────────────────────────────────────

    private void loadStats() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("receivers").get()
                .addOnSuccessListener(snap -> {
                    int total = 0, active = 0, appDonor = 0, external = 0, cancelled = 0;
                    long now = System.currentTimeMillis();
                    List<Receiver> recentClosed = new ArrayList<>();
                    List<Receiver> activeSosList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snap) {
                        Receiver r = doc.toObject(Receiver.class);
                        if (r == null) continue;
                        r.setId(doc.getId());
                        total++;

                        if (r.isActive()) {
                            boolean expired = (now - r.getTimestamp()) > EXPIRY_MS;
                            if (!expired) {
                                active++;
                                activeSosList.add(r);
                            }
                        } else {
                            String src = r.getFulfillmentSource();
                            if ("app_donor".equals(src))   appDonor++;
                            else if ("external".equals(src))  external++;
                            else if ("cancelled".equals(src)) cancelled++;
                            recentClosed.add(r);
                        }
                    }

                    recentClosed.sort((a, b) ->
                            Long.compare(b.getFulfilledAt(), a.getFulfilledAt()));
                    activeSosList.sort((a, b) ->
                            Long.compare(a.getTimestamp(), b.getTimestamp()));

                    int closed      = appDonor + external + cancelled;
                    int successRate = closed == 0 ? 0 : ((appDonor + external) * 100 / closed);

                    tvTotalRequests.setText(String.valueOf(total));
                    tvActiveNow.setText(String.valueOf(active));
                    tvViaApp.setText(String.valueOf(appDonor));
                    tvViaExternal.setText(String.valueOf(external));
                    tvCancelled.setText(String.valueOf(cancelled));
                    tvFulfillmentRate.setText(successRate + "%");

                    populateActiveSOS(activeSosList);
                    populateRecentActivity(
                            recentClosed.subList(0, Math.min(10, recentClosed.size())));

                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load request data.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });

        db.collection("donors").get()
                .addOnSuccessListener(snap -> tvTotalDonors.setText(String.valueOf(snap.size())));

        db.collection("users").get()
                .addOnSuccessListener(snap -> tvTotalUsers.setText(String.valueOf(snap.size())));
    }

    // ─── Active SOS section ───────────────────────────────────────────────────

    private void populateActiveSOS(List<Receiver> list) {
        activeSosLayout.removeAllViews();

        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("✅  No active SOS requests right now.");
            empty.setTextColor(0xFF2E7D32);
            empty.setTextSize(13);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(12), 0, dp(12));
            activeSosLayout.addView(empty);
            return;
        }

        for (Receiver r : list) {
            CardView card = new CardView(this);
            card.setRadius(dp(12));
            card.setCardElevation(dp(3));
            card.setCardBackgroundColor(0xFFFFF8E1);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(lp);

            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(14), dp(12), dp(14), dp(12));

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView badge = new TextView(this);
            badge.setText(r.getBloodGroup() != null ? r.getBloodGroup() : "?");
            badge.setTextColor(0xFFC62828);
            badge.setTextSize(12);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setBackgroundResource(R.drawable.circle_button);
            badge.getBackground().mutate().setTint(0xFFFEEBEE);
            int bSize = dp(44);
            badge.setLayoutParams(new LinearLayout.LayoutParams(bSize, bSize));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(12), 0, 0, 0);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvFor = new TextView(this);
            String loc = (r.getDistrict() != null && !r.getDistrict().isEmpty())
                    ? " · " + r.getDistrict() : "";
            tvFor.setText("For: " + (r.getToWhomFor() != null ? r.getToWhomFor() : "—") + loc);
            tvFor.setTextSize(13);
            tvFor.setTypeface(null, Typeface.BOLD);
            tvFor.setTextColor(0xFF212121);

            TextView tvMeta = new TextView(this);
            int responders = r.getResponderIds() == null ? 0 : r.getResponderIds().size();
            tvMeta.setText("⏱ " + timeAgo(r.getTimestamp())
                    + "  ·  " + responders + " donor"
                    + (responders != 1 ? "s" : "") + " responded");
            tvMeta.setTextSize(12);
            tvMeta.setTextColor(0xFF757575);
            tvMeta.setPadding(0, dp(2), 0, 0);

            info.addView(tvFor);
            info.addView(tvMeta);

            headerRow.addView(badge);
            headerRow.addView(info);
            body.addView(headerRow);

            Button btnExpire = new Button(this);
            btnExpire.setText("Force Close");
            btnExpire.setTextSize(11);
            btnExpire.setTextColor(Color.WHITE);
            btnExpire.setBackgroundResource(R.drawable.rounded_button);
            btnExpire.getBackground().mutate().setTint(0xFF9E9E9E);

            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            btnLp.setMargins(0, dp(10), 0, 0);
            btnLp.gravity = Gravity.END;
            btnExpire.setLayoutParams(btnLp);
            btnExpire.setPadding(dp(16), dp(4), dp(16), dp(4));
            btnExpire.setOnClickListener(v -> confirmForceClose(r, card));

            body.addView(btnExpire);
            card.addView(body);
            activeSosLayout.addView(card);
        }
    }

    private void confirmForceClose(Receiver r, CardView card) {
        new AlertDialog.Builder(this)
                .setTitle("Force Close SOS?")
                .setMessage("This will mark the SOS for \""
                        + (r.getToWhomFor() != null ? r.getToWhomFor() : "—")
                        + "\" as cancelled. Use only when the request is stale or invalid.")
                .setPositiveButton("Close It", (d, w) -> forceCloseRequest(r, card))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void forceCloseRequest(Receiver r, CardView card) {
        Map<String, Object> update = new HashMap<>();
        update.put("active", false);
        update.put("fulfillmentSource", "admin_closed");
        update.put("fulfilledAt", System.currentTimeMillis());

        db.collection("receivers").document(r.getId()).update(update)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Request closed.", Toast.LENGTH_SHORT).show();
                    activeSosLayout.removeView(card);
                    try {
                        int cur = Integer.parseInt(tvActiveNow.getText().toString());
                        if (cur > 0) tvActiveNow.setText(String.valueOf(cur - 1));
                    } catch (NumberFormatException ignored) {}
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ─── Recent activity feed ─────────────────────────────────────────────────

    private void populateRecentActivity(List<Receiver> list) {
        recentActivityLayout.removeAllViews();

        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No closed requests yet — activity will appear here.");
            empty.setTextColor(0xFF9E9E9E);
            empty.setTextSize(13);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(20), 0, dp(20));
            recentActivityLayout.addView(empty);
            return;
        }

        for (Receiver r : list) {
            CardView card = new CardView(this);
            card.setRadius(dp(12));
            card.setCardElevation(dp(2));
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(lp);

            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(14), dp(12), dp(14), dp(12));

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView badge = new TextView(this);
            badge.setText(r.getBloodGroup() != null ? r.getBloodGroup() : "?");
            badge.setTextColor(0xFFC62828);
            badge.setTextSize(12);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setBackgroundResource(R.drawable.circle_button);
            badge.getBackground().mutate().setTint(0xFFFEEBEE);
            int bSize = dp(44);
            badge.setLayoutParams(new LinearLayout.LayoutParams(bSize, bSize));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(12), 0, 0, 0);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvPatient = new TextView(this);
            String loc = (r.getDistrict() != null && !r.getDistrict().isEmpty())
                    ? " · " + r.getDistrict() : "";
            tvPatient.setText("For: " + (r.getToWhomFor() != null ? r.getToWhomFor() : "—") + loc);
            tvPatient.setTextSize(13);
            tvPatient.setTypeface(null, Typeface.BOLD);
            tvPatient.setTextColor(0xFF212121);

            TextView tvStatus = new TextView(this);
            String src = r.getFulfillmentSource();
            if ("app_donor".equals(src)) {
                tvStatus.setText("✅  Fulfilled via App Donor");
                tvStatus.setTextColor(0xFF2E7D32);
            } else if ("external".equals(src)) {
                tvStatus.setText("🏥  Fulfilled via Blood Bank / Hospital");
                tvStatus.setTextColor(0xFF1976D2);
            } else if ("admin_closed".equals(src)) {
                tvStatus.setText("🔒  Force-closed by Admin");
                tvStatus.setTextColor(0xFF9E9E9E);
            } else {
                tvStatus.setText("❌  Cancelled / No longer needed");
                tvStatus.setTextColor(0xFF9E9E9E);
            }
            tvStatus.setTextSize(12);
            tvStatus.setPadding(0, dp(2), 0, 0);

            TextView tvTime = new TextView(this);
            long ts = r.getFulfilledAt() > 0 ? r.getFulfilledAt() : r.getTimestamp();
            tvTime.setText(timeAgo(ts));
            tvTime.setTextSize(11);
            tvTime.setTextColor(0xFFBDBDBD);
            tvTime.setPadding(0, dp(2), 0, 0);

            info.addView(tvPatient);
            info.addView(tvStatus);
            info.addView(tvTime);

            headerRow.addView(badge);
            headerRow.addView(info);
            body.addView(headerRow);
            card.addView(body);
            recentActivityLayout.addView(card);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String timeAgo(long timestamp) {
        if (timestamp <= 0) return "unknown time";
        long diff = System.currentTimeMillis() - timestamp;
        long mins = diff / 60_000;
        if (mins < 1)  return "just now";
        if (mins < 60) return mins + "m ago";
        long hrs = mins / 60;
        if (hrs < 24)  return hrs + "h ago";
        return (hrs / 24) + "d ago";
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
