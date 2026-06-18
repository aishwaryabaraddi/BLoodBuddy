package com.example.bloodbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bloodbuddy.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import static com.example.bloodbuddy.ValidationUtils.*;

public class LoginActivity extends AppCompatActivity {

    private static final String ADMIN_EMAIL = "viju.r@gmail.com";

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is already logged in — route to correct screen
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            routeUser(currentUser);
        }

        setupListeners();
    }

    private void setupListeners() {
        binding.passwordVisibilityToggle.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                binding.loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.passwordVisibilityToggle.setImageResource(R.drawable.baseline_visibility_24);
            } else {
                binding.loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.passwordVisibilityToggle.setImageResource(R.drawable.baseline_visibility_off_24);
            }
            binding.loginPassword.setSelection(binding.loginPassword.length());
        });

        binding.loginButton.setOnClickListener(v -> loginUser());

        binding.forgotPassword.setOnClickListener(v -> 
            startActivity(new Intent(LoginActivity.this, ForgotPassword.class)));

        binding.signup.setOnClickListener(v -> 
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = binding.loginEmail.getText().toString().trim();
        String password = binding.loginPassword.getText().toString().trim();

        if (email.isEmpty()) {
            binding.loginEmail.setError("Email is required");
            binding.loginEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            binding.loginEmail.setError("Enter a valid email address");
            binding.loginEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.loginPassword.setError("Password is required");
            binding.loginPassword.requestFocus();
            return;
        }

        if (!isValidPassword(password)) {
            binding.loginPassword.setError("Minimum 6 characters required");
            binding.loginPassword.requestFocus();
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Success! Now verify the user exists in Firestore
                        verifyUserInFirestore();
                    } else {
                        setLoading(false);
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                        Log.e("LoginActivity", "Login failed: " + errorMessage);
                        Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void verifyUserInFirestore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        setLoading(false);
                        if (documentSnapshot.exists()) {
                            Toast.makeText(LoginActivity.this, "Welcome back to Blood Buddy", Toast.LENGTH_SHORT).show();
                            User profile = documentSnapshot.toObject(User.class);
                            boolean isAdmin = (profile != null && profile.isAdmin())
                                    || ADMIN_EMAIL.equalsIgnoreCase(user.getEmail());
                            goTo(isAdmin ? AdminDashboardActivity.class : DomainActivity.class);
                        } else {
                            Toast.makeText(LoginActivity.this, "User profile not found. Please register again.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Error fetching profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /** Route an already-authenticated user to the correct home screen. */
    private void routeUser(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    User profile = (doc != null && doc.exists()) ? doc.toObject(User.class) : null;
                    boolean isAdmin = (profile != null && profile.isAdmin())
                            || ADMIN_EMAIL.equalsIgnoreCase(user.getEmail());
                    goTo(isAdmin ? AdminDashboardActivity.class : DomainActivity.class);
                })
                .addOnFailureListener(e -> goTo(DomainActivity.class));
    }

    private void goTo(Class<?> dest) {
        Intent intent = new Intent(LoginActivity.this, dest);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        if (loading) {
            binding.loginButton.setEnabled(false);
            binding.loginButton.setText("Logging in...");
        } else {
            binding.loginButton.setEnabled(true);
            binding.loginButton.setText("Continue");
        }
    }
}
