package com.example.bloodbuddy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UploadImage extends AppCompatActivity {

    private static final int PICK_IMAGES_REQUEST = 1;
    private static final String ADMIN_EMAIL = "viju.r@gmail.com";
    private static final String IMGBB_API_KEY = "34bf0c768f425a2ee5ad2578961d3d23";
    private static final String TAG = "UploadImage";

    private MaterialButton btnAddLink, btnUploadGallery;
    private TextInputEditText etImageUrl;
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private RequestQueue requestQueue;
    private ProgressBar progressBar;
    private View urlInputContainer;
    private TextView tvToggleUrl;

    private List<String> imageUrls;
    private ImageAdapterU imageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_image);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        requestQueue = Volley.newRequestQueue(this);
        
        if (currentUser == null) {
            finish();
            return;
        }

        checkAdminAndInitialize(currentUser);

        btnUploadGallery = findViewById(R.id.button4);
        btnAddLink = findViewById(R.id.btn_add_link);
        etImageUrl = findViewById(R.id.et_image_url);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.upload_progress);
        urlInputContainer = findViewById(R.id.url_input_container);
        tvToggleUrl = findViewById(R.id.tv_toggle_url);

        imageUrls = new ArrayList<>();
        imageAdapter = new ImageAdapterU(this, imageUrls);
        setupRecyclerView();

        btnUploadGallery.setOnClickListener(v -> openGallery());

        tvToggleUrl.setOnClickListener(v -> {
            if (urlInputContainer.getVisibility() == View.GONE) {
                urlInputContainer.setVisibility(View.VISIBLE);
                tvToggleUrl.setText("Hide URL input");
            } else {
                urlInputContainer.setVisibility(View.GONE);
                tvToggleUrl.setText("Or use an image URL");
            }
        });

        btnAddLink.setOnClickListener(v -> {
            String url = etImageUrl.getText().toString().trim();
            if (!url.isEmpty()) addImageUrlToFirestore(url);
            else Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
        });

        loadImagesFromFirestore();
        findViewById(R.id.imageView10).setOnClickListener(v -> finish());
    }

    private void checkAdminAndInitialize(FirebaseUser firebaseUser) {
        db.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isAdmin = false;
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.isAdmin()) isAdmin = true;
                    }
                    if (!isAdmin && !ADMIN_EMAIL.equalsIgnoreCase(firebaseUser.getEmail())) {
                        Toast.makeText(UploadImage.this, "Admin access required", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uploadToImgBB(data.getData());
        }
    }

    private void uploadToImgBB(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        btnUploadGallery.setEnabled(false);
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://api.imgbb.com/1/upload?key=" + IMGBB_API_KEY,
                    response -> {
                        progressBar.setVisibility(View.GONE);
                        btnUploadGallery.setEnabled(true);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String url = jsonObject.getJSONObject("data").getString("url");
                            addImageUrlToFirestore(url);
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to parse response", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        btnUploadGallery.setEnabled(true);
                        Toast.makeText(this, "Upload failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("image", base64Image);
                    return params;
                }
            };
            requestQueue.add(stringRequest);
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            btnUploadGallery.setEnabled(true);
            Log.e(TAG, "Error uploading", e);
        }
    }

    private void addImageUrlToFirestore(String url) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("url", url);
        data.put("timestamp", System.currentTimeMillis());

        db.collection("carousel_images").document(id).set(data)
                .addOnSuccessListener(aVoid -> {
                    etImageUrl.setText("");
                    Toast.makeText(UploadImage.this, "Carousel updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(imageAdapter);
    }

    private void loadImagesFromFirestore() {
        db.collection("carousel_images").orderBy("timestamp").addSnapshotListener((value, error) -> {
            if (error != null) return;
            imageUrls.clear();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    String url = doc.getString("url");
                    if (url != null) imageUrls.add(url);
                }
            }
            imageAdapter.notifyDataSetChanged();
        });
    }
}
