package com.example.bloodbuddy;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
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

public class AdminUsersActivity extends AppCompatActivity {

    private LinearLayout usersListLayout;
    private ProgressBar progressBar;
    private TextView tvUserCount;
    private EditText etSearch;
    private FirebaseFirestore db;

    private List<User> allUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        db = FirebaseFirestore.getInstance();

        usersListLayout = findViewById(R.id.usersListLayout);
        progressBar     = findViewById(R.id.progressBar);
        tvUserCount     = findViewById(R.id.tvUserCount);
        etSearch        = findViewById(R.id.etSearch);

        findViewById(R.id.btnBack).setOnClickListener(v -> goToDashboard());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                applySearch(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadUsers();
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").get()
                .addOnSuccessListener(snap -> {
                    allUsers.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            u.setId(doc.getId());
                            allUsers.add(u);
                        }
                    }
                    allUsers.sort((a, b) -> {
                        String na = a.getName() != null ? a.getName() : "";
                        String nb = b.getName() != null ? b.getName() : "";
                        return na.compareToIgnoreCase(nb);
                    });
                    progressBar.setVisibility(View.GONE);
                    applySearch("");
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load users.", Toast.LENGTH_SHORT).show();
                });
    }

    private void applySearch(String query) {
        usersListLayout.removeAllViews();
        List<User> filtered = new ArrayList<>();
        String q = query.toLowerCase();

        for (User u : allUsers) {
            String name  = u.getName()  != null ? u.getName().toLowerCase()  : "";
            String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
            if (q.isEmpty() || name.contains(q) || email.contains(q)) {
                filtered.add(u);
            }
        }

        tvUserCount.setText(filtered.size() + " users");

        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No users found.");
            empty.setTextColor(0xFF9E9E9E);
            empty.setTextSize(13);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, 0);
            usersListLayout.addView(empty);
            return;
        }

        for (User u : filtered) {
            usersListLayout.addView(buildUserCard(u));
        }
    }

    private View buildUserCard(User u) {
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
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setPadding(dp(14), dp(12), dp(14), dp(12));
        body.setGravity(Gravity.CENTER_VERTICAL);

        // ── Initial avatar circle ─────────────────────────────────────────────
        String name = u.getName() != null ? u.getName() : "?";
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        int avatarColor = avatarColorFor(initial);

        TextView avatar = new TextView(this);
        avatar.setText(initial);
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(16);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.circle_button);
        avatar.getBackground().mutate().setTint(avatarColor);
        int aSize = dp(48);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(aSize, aSize));

        // ── Info column ───────────────────────────────────────────────────────
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // Name row + badges
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = makeText(name, 14, 0xFF212121, true);
        nameRow.addView(tvName);

        if (u.isDonor()) {
            TextView donorBadge = new TextView(this);
            donorBadge.setText("DONOR");
            donorBadge.setTextSize(9);
            donorBadge.setTypeface(null, Typeface.BOLD);
            donorBadge.setTextColor(Color.WHITE);
            donorBadge.setGravity(Gravity.CENTER);
            donorBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
            donorBadge.setBackgroundResource(R.drawable.rounded_button);
            donorBadge.getBackground().mutate().setTint(0xFFC62828);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeLp.setMargins(dp(6), 0, 0, 0);
            donorBadge.setLayoutParams(badgeLp);
            nameRow.addView(donorBadge);
        }

        if (u.isAdmin()) {
            TextView adminBadge = new TextView(this);
            adminBadge.setText("ADMIN");
            adminBadge.setTextSize(9);
            adminBadge.setTypeface(null, Typeface.BOLD);
            adminBadge.setTextColor(Color.WHITE);
            adminBadge.setGravity(Gravity.CENTER);
            adminBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
            adminBadge.setBackgroundResource(R.drawable.rounded_button);
            adminBadge.getBackground().mutate().setTint(0xFF5C6BC0);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeLp.setMargins(dp(4), 0, 0, 0);
            adminBadge.setLayoutParams(badgeLp);
            nameRow.addView(adminBadge);
        }

        info.addView(nameRow);

        String email = u.getEmail() != null ? u.getEmail() : "—";
        info.addView(makeText("✉ " + email, 12, 0xFF616161, false));

        String locationLine = "";
        if (u.getDistrict() != null && !u.getDistrict().isEmpty()) {
            locationLine = "📍 " + u.getDistrict();
        }
        if (!locationLine.isEmpty()) {
            info.addView(makeText(locationLine, 11, 0xFF9E9E9E, false));
        }

        // Blood group pill on the right
        if (u.getBloodGroup() != null && !u.getBloodGroup().isEmpty()) {
            TextView bgPill = new TextView(this);
            bgPill.setText(u.getBloodGroup());
            bgPill.setTextSize(12);
            bgPill.setTypeface(null, Typeface.BOLD);
            bgPill.setTextColor(0xFFC62828);
            bgPill.setGravity(Gravity.CENTER);
            bgPill.setPadding(dp(10), dp(4), dp(10), dp(4));
            bgPill.setBackgroundResource(R.drawable.rounded_button);
            bgPill.getBackground().mutate().setTint(0xFFFEEBEE);
            bgPill.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            body.addView(avatar);
            body.addView(info);
            body.addView(bgPill);
        } else {
            body.addView(avatar);
            body.addView(info);
        }

        card.addView(body);
        return card;
    }

    private int avatarColorFor(String initial) {
        int[] colors = {
            0xFFC62828, 0xFF1565C0, 0xFF2E7D32, 0xFF6A1B9A,
            0xFFE65100, 0xFF00695C, 0xFF283593, 0xFF4E342E
        };
        int idx = initial.isEmpty() ? 0 : (initial.charAt(0) % colors.length);
        return colors[Math.abs(idx)];
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
