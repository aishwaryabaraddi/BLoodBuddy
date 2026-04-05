package com.example.bloodbuddy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class UploadImage extends AppCompatActivity {

    private static final int PICK_IMAGES_REQUEST = 1;

    private Button uploadButton;
    private ImageView addImageView;
    private RecyclerView recyclerView;
    private ArrayList<Uri> imageUris;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;

    private List<String> imageUrls;
    private ImageAdapterU imageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_image);

        uploadButton = findViewById(R.id.button4);
        addImageView = findViewById(R.id.imageView6);
        recyclerView = findViewById(R.id.recyclerView);
        imageUris = new ArrayList<>();

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference("images");
        databaseReference = FirebaseDatabase.getInstance().getReference("images");

        imageUrls = new ArrayList<>();
        imageAdapter = new ImageAdapterU(this, imageUrls);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        addImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        // Initialize RecyclerView with an adapter
        setupRecyclerView();

        // Load existing images from Firebase
        loadImagesFromFirebase();

        ImageView imageViewBack = findViewById(R.id.imageView10);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to DomainActivity
                Intent intent = new Intent(UploadImage.this, DomainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    imageUris.add(imageUri);
                    uploadImageToFirebase(imageUri);
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                imageUris.add(imageUri);
                uploadImageToFirebase(imageUri);
            }
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        StorageReference fileRef = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Save image URL to database
                    String imageUrl = uri.toString();
                    databaseReference.push().setValue(imageUrl);

                    // Add image URL to local list and update RecyclerView
                    imageUrls.add(imageUrl);
                    updateRecyclerView();
                }))
                .addOnFailureListener(e -> {
                    // Handle unsuccessful uploads
                    Toast.makeText(UploadImage.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UploadImage", "uploadImageToFirebase: ", e);
                });
    }

    private String getFileExtension(Uri uri) {
        String extension = "";
        String mimeType = getContentResolver().getType(uri);
        if (mimeType != null) {
            String[] parts = mimeType.split("/");
            extension = parts[parts.length - 1];
        }
        return extension;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(imageAdapter);
    }

    private void updateRecyclerView() {
        imageAdapter.notifyDataSetChanged();
    }

    private void loadImagesFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                imageUrls.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    String imageUrl = postSnapshot.getValue(String.class);
                    imageUrls.add(imageUrl);
                }
                updateRecyclerView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle database errors
                Toast.makeText(UploadImage.this, "Failed to load images: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("UploadImage", "onCancelled: ", databaseError.toException());
            }
        });
    }
}
