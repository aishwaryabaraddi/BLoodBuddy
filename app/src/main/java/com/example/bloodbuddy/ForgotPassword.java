package com.example.bloodbuddy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import static com.example.bloodbuddy.ValidationUtils.*;

public class ForgotPassword extends AppCompatActivity {

    private EditText emailEditText;
    private Button submitButton;
    private TextView backToLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailEditText = findViewById(R.id.editTextTextEmailAddress);
        submitButton  = findViewById(R.id.button3);
        backToLogin   = findViewById(R.id.back_to_login);
        progressBar   = findViewById(R.id.forget_progress);
        mAuth         = FirebaseAuth.getInstance();

        submitButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Please enter your email address");
                emailEditText.requestFocus();
                return;
            }

            if (!isValidEmail(email)) {
                emailEditText.setError("Please enter a valid email address");
                emailEditText.requestFocus();
                return;
            }

            // ── Step 3: Send reset email ───────────────────────────
            sendPasswordResetEmail(email);
        });

        backToLogin.setOnClickListener(v -> finish());
    }

    private void sendPasswordResetEmail(String email) {
        // Show loading state
        setLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        // ── SUCCESS: Real email was sent by Firebase ───────
                        showSuccessDialog(email);

                    } else {
                        // ── FAILURE: Identify the exact reason ────────────
                        Exception exception = task.getException();

                        if (exception instanceof FirebaseAuthInvalidUserException) {
                            // Email not registered in this app
                            emailEditText.setError("No account found with this email. Please register first.");
                            emailEditText.requestFocus();
                            Toast.makeText(ForgotPassword.this,
                                    "❌ This email is not registered with Blood Buddy.",
                                    Toast.LENGTH_LONG).show();

                        } else if (exception != null &&
                                exception.getMessage() != null &&
                                exception.getMessage().contains("network")) {
                            // Network error
                            Toast.makeText(ForgotPassword.this,
                                    "❌ No internet connection. Please try again.",
                                    Toast.LENGTH_LONG).show();

                        } else {
                            // Generic fallback
                            String msg = (exception != null && exception.getMessage() != null)
                                    ? exception.getMessage()
                                    : "Something went wrong. Please try again.";
                            Toast.makeText(ForgotPassword.this,
                                    "❌ " + msg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Shows a professional success dialog — like real apps do (Gmail, Instagram, etc.)
     * Uses the security-friendly "if an account exists" wording because Firebase
     * Email Enumeration Protection is ON by default (always returns success).
     */
    private void showSuccessDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("📧 Reset Link Sent")
                .setMessage("If an account exists for:\n\n" + email
                        + "\n\n...a password reset link has been sent to that address.\n\n"
                        + "✅ Check your inbox and spam/junk folder.\n"
                        + "✅ Click the link in the email to set a new password.\n"
                        + "✅ The link expires in 1 hour.")
                .setCancelable(false)
                .setPositiveButton("Back to Login", (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(ForgotPassword.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNeutralButton("Resend", (dialog, which) -> {
                    dialog.dismiss();
                    sendPasswordResetEmail(email);
                })
                .show();
    }

    /** Shows/hides the loading spinner and disables/enables the button */
    private void setLoading(boolean loading) {
        if (loading) {
            submitButton.setEnabled(false);
            submitButton.setText("Sending...");
            progressBar.setVisibility(View.VISIBLE);
        } else {
            submitButton.setEnabled(true);
            submitButton.setText("SEND RESET LINK");
            progressBar.setVisibility(View.GONE);
        }
    }
}
