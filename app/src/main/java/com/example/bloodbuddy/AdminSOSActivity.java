package com.example.bloodbuddy;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AdminSOSActivity extends AppCompatActivity {

    private static final long EXPIRY_MS = TimeUnit.HOURS.toMillis(72);

    private static final String F_ALL        = "All";
    private static final String F_ACTIVE     = "Active";
    private static final String F_EXPIRED    = "Expired";
    private static final String F_VIA_APP    = "Via App";
    private static final String F_VIA_BANK   = "Via Blood Bank";
    private static final String F_CANCELLED  = "Cancelled";
    private static final String F_FORCE      = "Force Closed";

    private static final String[] FILTERS =
            {F_ALL, F_ACTIVE, F_EXPIRED, F_VIA_APP, F_VIA_BANK, F_CANCELLED, F_FORCE};

    private LinearLayout chipRow, sosListLayout;
    private ProgressBar progressBar;
    private TextView tvSosCount;
    private FirebaseFirestore db;

    private List<Receiver> allRequests = new ArrayList<>();
    private String selectedFilter = F_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_sos);

        db = FirebaseFirestore.getInstance();

        chipRow      = findViewById(R.id.chipRowSos);
        sosListLayout = findViewById(R.id.sosListLayout);
        progressBar  = findViewById(R.id.progressBar);
        tvSosCount   = findViewById(R.id.tvSosCount);

        findViewById(R.id.btnBack).setOnClickListener(v -> goToDashboard());

        setupChips();
        loadRequests();
    }

    private void setupChips() {
        for (String f : FILTERS) {
            TextView chip = new TextView(this);
            chip.setText(f);
            chip.setTextSize(11);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            chip.setTypeface(null, Typeface.BOLD);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(lp);

            updateChipStyle(chip, f.equals(selectedFilter));
            chip.setOnClickListener(v -> {
                selectedFilter = f;
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

    private void loadRequests() {
        progressBar.setVisibility(View.VISIBLE);
        sosListLayout.removeAllViews();

        db.collection("receivers").get()
                .addOnSuccessListener(snap -> {
                    allRequests.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Receiver r = doc.toObject(Receiver.class);
                        if (r != null) {
                            r.setId(doc.getId());
                            allRequests.add(r);
                        }
                    }
                    // Sort newest first
                    allRequests.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    progressBar.setVisibility(View.GONE);
                    applyFilter();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load requests.", Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilter() {
        sosListLayout.removeAllViews();
        long now = System.currentTimeMillis();
        List<Receiver> filtered = new ArrayList<>();

        for (Receiver r : allRequests) {
            String status = getStatus(r, now);
            if (F_ALL.equals(selectedFilter) || selectedFilter.equals(status)) {
                filtered.add(r);
            }
        }

        tvSosCount.setText(filtered.size() + " requests");

        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No requests found for this filter.");
            empty.setTextColor(0xFF9E9E9E);
            empty.setTextSize(13);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, 0);
            sosListLayout.addView(empty);
            return;
        }

        for (Receiver r : filtered) {
            sosListLayout.addView(buildSOSCard(r, getStatus(r, now)));
        }
    }

    private String getStatus(Receiver r, long now) {
        if (!r.isActive()) {
            String src = r.getFulfillmentSource();
            if ("admin_closed".equals(src)) return F_FORCE;
            if ("app_donor".equals(src))    return F_VIA_APP;
            if ("external".equals(src))     return F_VIA_BANK;
            return F_CANCELLED;
        }
        boolean expired = (now - r.getTimestamp()) > EXPIRY_MS;
        return expired ? F_EXPIRED : F_ACTIVE;
    }

    private View buildSOSCard(Receiver r, String status) {
        long now = System.currentTimeMillis();
        boolean isActive = F_ACTIVE.equals(status);

        CardView card = new CardView(this);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));
        int cardBg = isActive ? 0xFFFFF8E1 : Color.WHITE;
        card.setCardBackgroundColor(cardBg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(12), dp(14), dp(12));

        // ── Top row: badge + info + status badge ─────────────────────────────
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Blood group circle
        TextView bgBadge = new TextView(this);
        bgBadge.setText(r.getBloodGroup() != null ? r.getBloodGroup() : "?");
        bgBadge.setTextColor(0xFFC62828);
        bgBadge.setTextSize(12);
        bgBadge.setTypeface(null, Typeface.BOLD);
        bgBadge.setGravity(Gravity.CENTER);
        bgBadge.setBackgroundResource(R.drawable.circle_button);
        bgBadge.getBackground().mutate().setTint(0xFFFEEBEE);
        int bSize = dp(48);
        bgBadge.setLayoutParams(new LinearLayout.LayoutParams(bSize, bSize));

        // Info column
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(10), 0, 0, 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        String loc = (r.getDistrict() != null && !r.getDistrict().isEmpty())
                ? " · " + r.getDistrict() : "";
        TextView tvFor = makeText("For: " + (r.getToWhomFor() != null ? r.getToWhomFor() : "—") + loc,
                13, 0xFF212121, true);
        TextView tvName = makeText("👤 " + (r.getName() != null ? r.getName() : "—"),
                12, 0xFF616161, false);
        TextView tvTime = makeText("🕐 " + timeAgo(r.getTimestamp()), 12, 0xFF757575, false);

        int responders = r.getResponderIds() == null ? 0 : r.getResponderIds().size();
        TextView tvResp = makeText(responders + " donor" + (responders != 1 ? "s" : "") + " responded",
                11, 0xFF9E9E9E, false);

        info.addView(tvFor);
        info.addView(tvName);
        info.addView(tvTime);
        info.addView(tvResp);

        // Detail row for all closed requests
        String detail = fulfillmentDetail(status);
        if (detail != null) {
            int detailColor;
            switch (status) {
                case F_VIA_APP:   detailColor = 0xFF2E7D32; break;
                case F_VIA_BANK:  detailColor = 0xFF1565C0; break;
                case F_FORCE:     detailColor = 0xFF5C6BC0; break;
                default:          detailColor = 0xFF757575; break;
            }
            TextView tvDetail = makeText(detail, 11, detailColor, false);
            tvDetail.setPadding(0, dp(4), 0, 0);
            info.addView(tvDetail);
        }

        // Status badge
        TextView statusBadge = new TextView(this);
        statusBadge.setText(statusLabel(status));
        statusBadge.setTextSize(10);
        statusBadge.setTypeface(null, Typeface.BOLD);
        statusBadge.setTextColor(Color.WHITE);
        statusBadge.setGravity(Gravity.CENTER);
        statusBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
        statusBadge.setBackgroundResource(R.drawable.rounded_button);
        statusBadge.getBackground().mutate().setTint(statusColor(status));
        statusBadge.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        topRow.addView(bgBadge);
        topRow.addView(info);
        topRow.addView(statusBadge);
        body.addView(topRow);

        // ── Force Close button (only for active requests) ─────────────────────
        if (isActive) {
            Button btnClose = new Button(this);
            btnClose.setText("Force Close");
            btnClose.setTextSize(11);
            btnClose.setTextColor(Color.WHITE);
            btnClose.setBackgroundResource(R.drawable.rounded_button);
            btnClose.getBackground().mutate().setTint(0xFF9E9E9E);

            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            btnLp.setMargins(0, dp(10), 0, 0);
            btnLp.gravity = Gravity.END;
            btnClose.setLayoutParams(btnLp);
            btnClose.setPadding(dp(16), dp(4), dp(16), dp(4));
            btnClose.setOnClickListener(v -> confirmForceClose(r, card));
            body.addView(btnClose);
        }

        card.addView(body);
        return card;
    }

    private void confirmForceClose(Receiver r, CardView card) {
        new AlertDialog.Builder(this)
                .setTitle("Force Close SOS?")
                .setMessage("This will mark the SOS for \""
                        + (r.getToWhomFor() != null ? r.getToWhomFor() : "—")
                        + "\" as cancelled.")
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
                    Toast.makeText(this, "Request force-closed.", Toast.LENGTH_SHORT).show();
                    r.setActive(false);
                    r.setFulfillmentSource("admin_closed");
                    applyFilter(); // refresh list
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String statusLabel(String status) {
        switch (status) {
            case F_ACTIVE:   return "🔴 ACTIVE";
            case F_EXPIRED:  return "⏰ EXPIRED";
            case F_VIA_APP:  return "✅ APP DONOR";
            case F_VIA_BANK: return "🏥 BLOOD BANK";
            case F_FORCE:    return "🔒 ADMIN CLOSED";
            default:         return "❌ NOT NEEDED";
        }
    }

    private int statusColor(String status) {
        switch (status) {
            case F_ACTIVE:   return 0xFFF57C00; // amber
            case F_EXPIRED:  return 0xFF9E9E9E; // grey
            case F_VIA_APP:  return 0xFF2E7D32; // green
            case F_VIA_BANK: return 0xFF1565C0; // blue
            case F_FORCE:    return 0xFF5C6BC0; // indigo
            default:         return 0xFF757575; // dark grey
        }
    }

    private String fulfillmentDetail(String status) {
        switch (status) {
            case F_VIA_APP:   return "User confirmed: blood arranged through a Blood Buddy donor";
            case F_VIA_BANK:  return "User confirmed: arranged via blood bank / hospital";
            case F_CANCELLED: return "User marked: no longer needed / patient recovered";
            case F_FORCE:     return "Closed by admin — request was stale or invalid";
            default:          return null;
        }
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

    private String timeAgo(long timestamp) {
        if (timestamp <= 0) return "—";
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
