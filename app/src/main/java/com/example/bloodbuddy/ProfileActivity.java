package com.example.bloodbuddy;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    private TextView tvName, tvPhoneNumber, tvEmail, tvDistrict, tvTaluk;
    private ImageView imageView5, imageViewEdit;
    private Button button;
    private DatabaseReference database;
    private StorageReference storageReference;
    private Uri imageUri;
    private ProgressBar progressBar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize user profile views
        tvName = findViewById(R.id.tvName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvEmail = findViewById(R.id.tvEmail);
        tvDistrict = findViewById(R.id.tvDistrict);
        tvTaluk = findViewById(R.id.tvTaluk);
        imageView5 = findViewById(R.id.imageView5);
        button = findViewById(R.id.button);
        progressBar = findViewById(R.id.progressBar);
        imageViewEdit = findViewById(R.id.imageView11);

        database = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        button.setOnClickListener(v -> openGallery());

        fetchUserData();

        // Set OnClickListener for edit icon
        imageViewEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditDialog();
            }
        });

        // Back button functionality
        ImageView imageViewBack = findViewById(R.id.imageView9);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to DomainActivity
                Intent intent = new Intent(ProfileActivity.this, DomainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showEditDialog() {
        // Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        // Initialize the EditText fields
        EditText editTextName = dialogView.findViewById(R.id.editTextName);
        EditText editTextEmail = dialogView.findViewById(R.id.editTextEmail);
        EditText editTextPhone = dialogView.findViewById(R.id.editTextPhone);

        // Set current values to the EditText fields
        editTextName.setText(tvName.getText().toString());
        editTextEmail.setText(tvEmail.getText().toString());
        editTextPhone.setText(tvPhoneNumber.getText().toString());

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Get the updated values
                        String updatedName = editTextName.getText().toString().trim();
                        String updatedEmail = editTextEmail.getText().toString().trim();
                        String updatedPhone = editTextPhone.getText().toString().trim();

                        // Update UI
                        tvName.setText(updatedName);
                        tvEmail.setText(updatedEmail);
                        tvPhoneNumber.setText(updatedPhone);

                        // Update Firebase Authentication and Database
                        updateUserProfile(updatedName, updatedEmail, updatedPhone);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    private void updateUserProfile(String updatedName, String updatedEmail, String updatedPhone) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = database.child("users").child(userId);

            userRef.child("name").setValue(updatedName);
            userRef.child("email").setValue(updatedEmail);
            userRef.child("phone").setValue(updatedPhone)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            currentUser.updateEmail(updatedEmail)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(ProfileActivity.this, "Failed to update email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            uploadImage();
        }
    }

    private void uploadImage() {
        if (imageUri != null) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null) {
                String userId = currentUser.getUid();
                StorageReference fileReference = storageReference.child(userId + ".jpg");

                fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        database.child("users").child(userId).child("imageUrl").setValue(imageUrl);
                        Glide.with(ProfileActivity.this)
                                .load(imageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .into(imageView5);
                        Toast.makeText(ProfileActivity.this, "Upload successful", Toast.LENGTH_SHORT).show();
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private void fetchUserData() {
        progressBar.setVisibility(View.VISIBLE); // Show progress bar
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "Fetching data for user ID: " + userId);

            database.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    progressBar.setVisibility(View.GONE); // Hide progress bar
                    if (dataSnapshot.exists()) {
                        // Fetch user profile data
                        String nameFromDB = dataSnapshot.child("name").getValue(String.class);
                        String emailFromDB = dataSnapshot.child("email").getValue(String.class);
                        String phoneFromDB = dataSnapshot.child("phone").getValue(String.class);
                        String districtFromDB = dataSnapshot.child("district").getValue(String.class);
                        String talukFromDB = dataSnapshot.child("taluk").getValue(String.class);
                        String imageUrlFromDB = dataSnapshot.child("imageUrl").getValue(String.class);

                        // Set user profile data to TextViews
                        tvName.setText(nameFromDB);
                        tvEmail.setText(emailFromDB);
                        tvPhoneNumber.setText(phoneFromDB);
                        tvDistrict.setText(districtFromDB);
                        tvTaluk.setText(talukFromDB);

                        // Load user profile image
                        if (imageUrlFromDB != null && !imageUrlFromDB.isEmpty()) {
                            Glide.with(ProfileActivity.this)
                                    .load(imageUrlFromDB)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(imageView5);
                        } else {
                            // Set default profile image if imageUrlFromDB is null or empty
                            imageView5.setImageResource(R.drawable.default_profile_image);
                        }
                    } else {
                        Log.e(TAG, "User data does not exist for user ID: " + userId);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    progressBar.setVisibility(View.GONE); // Hide progress bar
                    Log.e(TAG, "Failed to fetch user data: " + databaseError.getMessage());
                }
            });
        }
    }
}
