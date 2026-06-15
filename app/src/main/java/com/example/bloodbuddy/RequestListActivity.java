package com.example.bloodbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Shows two sections:
 *   1. MY ACTIVE SOS  — the current user's own blood requests (with "Blood Arranged" / "Cancel" buttons)
 *   2. COMMUNITY SOS  — other users' active requests (with "Accept SOS" / "Call" buttons)
 *
 * Data source: Firestore "receivers" collection  (NOT Realtime DB — fixes the data-source mismatch bug)
 * Auto-expiry : requests older than 72 hours shown as expired and hidden from community feed.
 */
public class RequestListActivity extends AppCompatActivity {

    private static final int    SMS_PERMISSION_CODE = 1;
    private static final long   EXPIRY_MS = TimeUnit.HOURS.toMillis(72); // 72-hour SOS window

    private LinearLayout myRequestsLayout, communityLayout;
    private LinearLayout sectionMine, layoutEmpty;
    private TextView     tvCommunitySectionLabel;
    private ProgressBar  progressBar;
    private BottomNavigationView bottomNav;

    private FirebaseFirestore db;
    private String currentUserId = "";
    private ListenerRegistration listenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request_list);

        db = FirebaseFirestore.getInstance();
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me != null) currentUserId = me.getUid();

        myRequestsLayout       = findViewById(R.id.myRequestsLayout);
        communityLayout        = findViewById(R.id.receiverDetailsLayout);
        sectionMine            = findViewById(R.id.sectionMine);
        layoutEmpty            = findViewById(R.id.layoutEmpty);
        tvCommunitySectionLabel= findViewById(R.id.tvCommunitySectionLabel);
        progressBar            = findViewById(R.id.progressBar);

        findViewById(R.id.imageView8).setOnClickListener(v -> finish());

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_request_list);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_map) {
                startActivity(new Intent(this, MapActivity.class)); finish();
            } else if (id == R.id.navigation_donor_list) {
                startActivity(new Intent(this, DisplayDonorActivity.class)); finish();
            } else if (id == R.id.navigation_feedback) {
                startActivity(new Intent(this, UserFeedback.class)); finish();
            }
            return true;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }

        loadRequests();
    }

    // ─── Data ─────────────────────────────────────────────────────────────────

    private void loadRequests() {
        progressBar.setVisibility(View.VISIBLE);

        // Real-time listener so UI refreshes when someone closes their SOS
        listenerReg = db.collection("receivers")
                .whereEqualTo("active", true)
                .addSnapshotListener((snapshots, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null || snapshots == null) {
                        showEmpty();
                        return;
                    }

                    myRequestsLayout.removeAllViews();
                    communityLayout.removeAllViews();
                    int communityCount = 0;
                    int myCount = 0;

                    long now = System.currentTimeMillis();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Receiver r = doc.toObject(Receiver.class);

                        // Auto-expiry: treat requests older than 72 h as expired (without writing to DB)
                        boolean expired = (now - r.getTimestamp()) > EXPIRY_MS;

                        if (r.getUserId() != null && r.getUserId().equals(currentUserId)) {
                            // MY request
                            myCount++;
                            addMyRequestCard(r, expired);
                        } else if (!expired) {
                            // Community SOS — only active, non-expired
                            communityCount++;
                            addCommunityCard(r);
                        }
                    }

                    // Section visibility
                    sectionMine.setVisibility(myCount > 0 ? View.VISIBLE : View.GONE);
                    tvCommunitySectionLabel.setVisibility(communityCount > 0 ? View.VISIBLE : View.GONE);
                    layoutEmpty.setVisibility((myCount + communityCount == 0) ? View.VISIBLE : View.GONE);
                });
    }

    // ─── My Request Card ──────────────────────────────────────────────────────

    private void addMyRequestCard(Receiver r, boolean expired) {
        CardView card = makeCard();
        LinearLayout body = makeCardBody(card);

        // Header row: blood badge + info
        LinearLayout header = makeRow();
        TextView badge = makeBloodBadge(r.getBloodGroup());
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);
        LinearLayout.LayoutParams iLP = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(iLP);

        TextView tvName = makeText("For: " + r.getToWhomFor(), 15, true, 0xFF212121);
        TextView tvLoc  = makeText("📍 " + r.getLocation(), 12, false, 0xFF757575);
        TextView tvTime = makeText(timeAgo(r.getTimestamp()), 11, false, 0xFF9E9E9E);
        info.addView(tvName);
        info.addView(tvLoc);
        info.addView(tvTime);

        // Status chip
        String statusText;
        int statusColor;
        if (expired) {
            statusText  = "⏰ Expired";
            statusColor = 0xFF9E9E9E;
        } else {
            int responders = r.getResponderIds() == null ? 0 : r.getResponderIds().size();
            statusText  = responders > 0
                    ? "🙋 " + responders + " donor" + (responders > 1 ? "s" : "") + " responded"
                    : "⏳ Waiting for donors…";
            statusColor = responders > 0 ? 0xFF4CAF50 : 0xFFF57C00;
        }
        TextView tvStatus = makeText(statusText, 12, true, statusColor);
        tvStatus.setPadding(0, dp(6), 0, 0);

        header.addView(badge);
        header.addView(info);
        body.addView(header);
        body.addView(tvStatus);

        // Action buttons — only if still active
        if (!expired) {
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFFEEEEEE);
            LinearLayout.LayoutParams dLP = (LinearLayout.LayoutParams) divider.getLayoutParams();
            dLP.setMargins(0, dp(12), 0, dp(12));
            body.addView(divider);

            LinearLayout btnRow = makeRow();

            Button btnArranged = makeButton("✅  Blood Arranged", "#4CAF50", Color.WHITE);
            btnArranged.setOnClickListener(v -> showFulfillmentDialog(r));

            Button btnCancel = makeButton("✕  Cancel SOS", "#F5F5F5", Color.DKGRAY);
            btnCancel.setOnClickListener(v -> closeRequest(r, "cancelled", null));

            btnRow.addView(btnArranged);
            btnRow.addView(btnCancel);
            body.addView(btnRow);
        }

        card.addView(body);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);
        myRequestsLayout.addView(card);
    }

    // ─── Community Card ───────────────────────────────────────────────────────

    private void addCommunityCard(Receiver r) {
        CardView card = makeCard();
        LinearLayout body = makeCardBody(card);

        // Header
        LinearLayout header = makeRow();
        TextView badge = makeBloodBadge(r.getBloodGroup());
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);
        LinearLayout.LayoutParams iLP = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(iLP);

        info.addView(makeText("For: " + r.getToWhomFor(), 15, true, 0xFF212121));
        info.addView(makeText("📍 " + r.getLocation(), 12, false, 0xFF757575));
        info.addView(makeText("🏥 " + r.getDistrict() + (r.getTaluk().isEmpty() ? "" : ", " + r.getTaluk()),
                12, false, 0xFF9E9E9E));
        info.addView(makeText(timeAgo(r.getTimestamp()), 11, false, 0xFFBDBDBD));

        // Already accepted?
        boolean accepted = r.getResponderIds() != null
                && r.getResponderIds().contains(currentUserId);

        header.addView(badge);
        header.addView(info);
        body.addView(header);

        // Buttons
        View divider = new View(this);
        divider.setBackgroundColor(0xFFEEEEEE);
        LinearLayout.LayoutParams dLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dLP.setMargins(0, dp(12), 0, dp(12));
        divider.setLayoutParams(dLP);
        body.addView(divider);

        LinearLayout btnRow = makeRow();

        Button btnCall = makeButton("📞  Call", "#F5F5F5", Color.DKGRAY);
        btnCall.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + r.getPhoneNumber()))));

        Button btnAccept;
        if (accepted) {
            btnAccept = makeButton("✅  Accepted", "#E8F5E9", 0xFF388E3C);
            btnAccept.setEnabled(false);
        } else {
            btnAccept = makeButton("🩸  Accept SOS", "#C62828", Color.WHITE);
            btnAccept.setOnClickListener(v -> acceptSOS(r, btnAccept));
        }

        btnRow.addView(btnCall);
        btnRow.addView(btnAccept);
        body.addView(btnRow);

        card.addView(body);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);
        communityLayout.addView(card);
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private void acceptSOS(Receiver r, Button btn) {
        if (currentUserId.isEmpty()) return;

        java.util.List<String> ids = r.getResponderIds() == null
                ? new java.util.ArrayList<>() : new java.util.ArrayList<>(r.getResponderIds());
        if (ids.contains(currentUserId)) return;
        ids.add(currentUserId);

        db.collection("receivers").document(r.getId())
                .update("responderIds", ids)
                .addOnSuccessListener(v -> {
                    btn.setText("✅  Accepted");
                    btn.setEnabled(false);
                    btn.setBackgroundColor(0xFFE8F5E9);
                    btn.setTextColor(0xFF388E3C);
                    notifyRequesterBySms(r);
                    Toast.makeText(this,
                            "🙏 Accepted! The requester has been notified.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void notifyRequesterBySms(Receiver r) {
        String phone = r.getPhoneNumber();
        if (phone == null || phone.isEmpty()) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) return;
        try {
            String msg = "Good news! A donor has responded to your BloodBuddy SOS request for "
                    + r.getBloodGroup() + " blood. Please open the Blood Buddy app to manage your request.";
            SmsManager.getDefault().sendTextMessage(phone, null, msg, null, null);
        } catch (Exception ignored) {}
    }

    /** Fulfillment options dialog — tracks HOW blood was arranged for admin analytics */
    private void showFulfillmentDialog(Receiver r) {
        new AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog)
                .setTitle("✅  Blood Arranged?")
                .setMessage("Help us track how the blood was arranged. This improves the community.")
                .setPositiveButton("👤 Through a Blood Buddy Donor", (d, w) ->
                        closeRequest(r, "app_donor", currentUserId))
                .setNeutralButton("🏥 Blood Bank / Hospital", (d, w) ->
                        closeRequest(r, "external", null))
                .setNegativeButton("✕ Cancel / Not Needed Anymore", (d, w) ->
                        closeRequest(r, "cancelled", null))
                .setCancelable(true)
                .show();
    }

    private void closeRequest(Receiver r, String source, String donorUid) {
        Map<String, Object> update = new HashMap<>();
        update.put("active",            false);
        update.put("fulfillmentSource", source);
        update.put("fulfilledAt",       System.currentTimeMillis());
        if (donorUid != null) update.put("fulfilledByUserId", donorUid);

        db.collection("receivers").document(r.getId())
                .update(update)
                .addOnSuccessListener(v -> {
                    String msg = source.equals("app_donor")
                            ? "🎉 Great! Marked as fulfilled via app donor."
                            : source.equals("external")
                                ? "✅ Marked fulfilled. Stay safe!"
                                : "SOS cancelled.";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void showEmpty() {
        sectionMine.setVisibility(View.GONE);
        tvCommunitySectionLabel.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    private String timeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long mins = diff / 60_000;
        if (mins < 1)  return "just now";
        if (mins < 60) return mins + " min" + (mins > 1 ? "s" : "") + " ago";
        long hrs = mins / 60;
        if (hrs < 24)  return hrs + " hr" + (hrs > 1 ? "s" : "") + " ago";
        long days = hrs / 24;
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ─── View factories ───────────────────────────────────────────────────────

    private CardView makeCard() {
        CardView c = new CardView(this);
        c.setRadius(dp(16));
        c.setCardElevation(dp(3));
        c.setCardBackgroundColor(Color.WHITE);
        return c;
    }

    private LinearLayout makeCardBody(CardView card) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        return l;
    }

    private LinearLayout makeRow() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private TextView makeBloodBadge(String bloodGroup) {
        TextView t = new TextView(this);
        t.setText(bloodGroup);
        t.setTextColor(0xFFC62828);
        t.setTextSize(13);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundResource(R.drawable.circle_button);
        t.getBackground().mutate().setTint(0xFFFEEBEE);
        int s = dp(48);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return t;
    }

    private TextView makeText(String text, int sizeSp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }

    private Button makeButton(String text, String hexBg, int textColor) {
        Button b = new Button(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(40), 1f);
        p.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(p);
        b.setText(text);
        b.setTextColor(textColor);
        b.setTextSize(11);
        b.setPadding(0, 0, 0, 0);
        b.setBackgroundColor(Color.parseColor(hexBg));
        b.setAllCaps(false);
        return b;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerReg != null) listenerReg.remove();
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
    }
}
