package com.example.bloodbuddy;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etName, etPhone;
    private Spinner spinnerState, spinnerDistrict, spinnerTaluk;
    private Button btnSave;
    private ProgressBar progressBar;
    private ImageView profileImageView;
    private FirebaseFirestore db;
    private DocumentReference userDocRef;
    private StorageReference storageReference;
    private Uri croppedImageUri;

    private String[] states = {"Select State", "Karnataka", "Other"};
    private String[] districts = {"Select District", "Bagalkot", "Bangalore Rural", "Bangalore Urban", "Belgaum",
            "Bellary", "Bidar", "Bijapur", "Chamarajanagar", "Chikballapur", "Chikmagalur",
            "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Gulbarga",
            "Hassan", "Haveri", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysore", "Raichur",
            "Ramanagara", "Shimoga", "Tumkur", "Udupi", "Uttara Kannada", "Yadgir"};

    private String[][] taluks = {
            {"Select Taluk"},
            {"Bagalkot", "Badami", "Bilagi", "Hungund", "Jamkhandi", "Mudhol"},
            {"Devanahalli", "Doddaballapur", "Hosakote", "Nelamangala"},
            {"Bangalore North", "Bangalore East", "Bangalore South", "Anekal"},
            {"Athani", "Bailhongal", "Belgaum", "Chikodi", "Gokak", "Hukkeri", "Khanapur", "Ramdurg", "Raibag", "Saundatti"},
            {"Bellary", "Siruguppa", "Hospet", "Kudligi", "Sandur", "Hadagali", "Hagaribommanahalli"},
            {"Humnabad", "Bidar", "Bhalki", "Aurad", "Basavakalyan"},
            {"Bijapur", "Basavana Bagewadi", "Sindagi", "Indi", "Muddebihal"},
            {"Chamarajanagar", "Gundlupet", "Kollegal", "Yelandur"},
            {"Chikballapur", "Chintamani", "Gauribidanur", "Bagepalli", "Sidlaghatta", "Gudibanda"},
            {"Chikmagalur", "Kadur", "Koppa", "Mudigere", "Narasimharajapura", "Sringeri", "Tarikere"},
            {"Chitradurga", "Hiriyur", "Hosadurga", "Holalkere", "Molakalmuru", "Challakere"},
            {"Mangalore", "Bantwal", "Belthangady", "Puttur", "Sullia", "Ullal"},
            {"Davanagere", "Channagiri", "Honnali", "Harihar", "Harapanahalli", "Jagalur"},
            {"Dharwad", "Hubli", "Kalghatgi", "Kundgol", "Navalgund"},
            {"Gadag", "Mundargi", "Nargund", "Ron", "Shirhatti"},
            {"Kamalapur", "Shahbad", "Kalaburagi", "Aland", "Jewargi", "Afzalpur"},
            {"Arsikere", "Belur", "Channarayapatna", "Hassan", "Holenarsipur", "Sakleshpur", "Alur", "Arkalgud"},
            {"Hanagal", "Haveri", "Hirekerur", "Ranebennur", "Byadgi", "Savanur", "Shiggaon"},
            {"Madikeri", "Somwarpet", "Virajpet"},
            {"Bangarapet", "Kolar", "Malur", "Mulbagal", "Srinivaspur"},
            {"Gangawati", "Koppal", "Kushtagi", "Yelburga"},
            {"Krishnarajpet", "Mandya", "Malavalli", "Nagamangala", "Pandavapura", "Srirangapatna", "Maddur"},
            {"Hunsur", "Krishnarajanagara", "Mysore", "Nanjangud", "Piriyapatna", "Tirumakudal Narsipur"},
            {"Devadurga", "Lingsugur", "Manvi", "Raichur", "Sindhanur"},
            {"Channapatna", "Kanakapura", "Magadi", "Ramanagaram"},
            {"Bhadravathi", "Hosanagara", "Sagara", "Shikarpur", "Shimoga", "Sorab", "Tirthahalli"},
            {"Tumkur", "Sira", "Tiptur", "Gubbi", "Madhugiri"},
            {"Karkala", "Kundapura", "Udupi"},
            {"Ankola", "Bhatkal", "Haliyal", "Karwar", "Kumta", "Mundgod", "Siddapur", "Sirsi", "Yellapur", "Dandeli"},
            {"Shahpur", "Shorapur", "Yadgir"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etName = findViewById(R.id.editTextName);
        etPhone = findViewById(R.id.editTextPhone);
        spinnerState = findViewById(R.id.spinnerState);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        spinnerTaluk = findViewById(R.id.spinnerTaluk);
        btnSave = findViewById(R.id.buttonSave);
        progressBar = findViewById(R.id.progressBar);
        profileImageView = findViewById(R.id.edit_profile_image);
        findViewById(R.id.cv_profile_image).setOnClickListener(v -> openGallery());
        ImageView backButton = findViewById(R.id.backButton);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        db = FirebaseFirestore.getInstance();
        userDocRef = db.collection("users").document(user.getUid());
        storageReference = FirebaseStorage.getInstance().getReference("profile_images");

        setupSpinners();
        loadUserData();

        btnSave.setOnClickListener(v -> saveProfileChanges());
        backButton.setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        setupAdapter(spinnerState, states);
        setupAdapter(spinnerDistrict, districts);
        setupAdapter(spinnerTaluk, taluks[0]);

        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < taluks.length) {
                    setupAdapter(spinnerTaluk, taluks[position]);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupAdapter(Spinner spinner, String[] data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.BLACK);
                ((TextView) v).setTextSize(16);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadUserData() {
        progressBar.setVisibility(View.VISIBLE);
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            progressBar.setVisibility(View.GONE);
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    etName.setText(user.getName());
                    etPhone.setText(user.getPhone());
                    setSpinnerSelection(spinnerState, states, user.getState());
                    setSpinnerSelection(spinnerDistrict, districts, user.getDistrict());
                    spinnerDistrict.post(() -> {
                        int distPos = getPosition(districts, user.getDistrict());
                        if (distPos != -1 && distPos < taluks.length) {
                            setupAdapter(spinnerTaluk, taluks[distPos]);
                            setSpinnerSelection(spinnerTaluk, taluks[distPos], user.getTaluk());
                        }
                    });
                    String imageUrl = documentSnapshot.getString("imageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(EditProfileActivity.this)
                                .load(imageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.account)
                                .into(profileImageView);
                    }
                }
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Error loading user data", e);
        });
    }

    private void setSpinnerSelection(Spinner spinner, String[] array, String value) {
        if (value != null) {
            int position = getPosition(array, value);
            if (position != -1) {
                spinner.setSelection(position, false);
            }
        }
    }

    private int getPosition(String[] array, String value) {
        if (value == null) return -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(value)) return i;
        }
        return -1;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                Uri sourceUri = data.getData();
                if (sourceUri != null) {
                    startCrop(sourceUri);
                }
            } else if (requestCode == UCrop.REQUEST_CROP) {
                croppedImageUri = UCrop.getOutput(data);
                if (croppedImageUri != null) {
                    Glide.with(this)
                            .load(croppedImageUri)
                            .apply(RequestOptions.circleCropTransform())
                            .into(profileImageView);
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Toast.makeText(this, cropError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCrop(@NonNull Uri uri) {
        String destinationFileName = UUID.randomUUID().toString() + ".jpg";
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.colorAccent));

        UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)))
                .withAspectRatio(1, 1)
                .withMaxResultSize(1000, 1000)
                .withOptions(options)
                .start(this);
    }

    private void saveProfileChanges() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String state = spinnerState.getSelectedItem().toString();
        String district = spinnerDistrict.getSelectedItem().toString();
        String taluk = spinnerTaluk.getSelectedItem().toString();

        if (name.isEmpty() || phone.length() != 10 || state.equals("Select State") || district.equals("Select District") || taluk.equals("Select Taluk")) {
            Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        if (croppedImageUri != null) {
            Log.d(TAG, "New image selected, starting upload...");
            uploadImageWithDetails(name, phone, state, district, taluk);
        } else {
            Log.d(TAG, "No new image, updating details only...");
            updateUserDetails(name, phone, state, district, taluk, null);
        }
    }

    private void uploadImageWithDetails(String name, String phone, String state, String district, String taluk) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        StorageReference fileReference = storageReference.child(userId + ".jpg");

        Log.d(TAG, "Uploading image to: " + fileReference.toString());
        fileReference.putFile(croppedImageUri).addOnSuccessListener(taskSnapshot -> {
            Log.d(TAG, "Upload successful, getting download URL...");
            fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                Log.d(TAG, "Download URL: " + imageUrl);
                updateUserDetails(name, phone, state, district, taluk, imageUrl);
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Failed to get download URL", e);
                Toast.makeText(EditProfileActivity.this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Image Upload Failed", e);
            Toast.makeText(EditProfileActivity.this, "Image Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUserDetails(String name, String phone, String state, String district, String taluk, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("state", state);
        updates.put("district", district);
        updates.put("taluk", taluk);
        if (imageUrl != null) {
            updates.put("imageUrl", imageUrl);
        }

        Log.d(TAG, "Updating Firestore for user: " + userDocRef.getPath());
        userDocRef.update(updates).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Log.d(TAG, "Firestore update successful");
                Toast.makeText(EditProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Log.e(TAG, "Firestore update failed", task.getException());
                Toast.makeText(EditProfileActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
