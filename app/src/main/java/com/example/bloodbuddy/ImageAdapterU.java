package com.example.bloodbuddy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class ImageAdapterU extends RecyclerView.Adapter<ImageAdapterU.ImageViewHolder> {

    private Context context;
    private List<String> imageUrls;
    private FirebaseFirestore db;

    public ImageAdapterU(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.domain1)
                .error(R.drawable.baseline_add_circle_24)
                .into(holder.imageView);

        holder.btnDelete.setOnClickListener(v -> {
            deleteImage(imageUrl);
        });
    }

    private void deleteImage(String urlToDelete) {
        db.collection("carousel_images")
                .whereEqualTo("url", urlToDelete)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete().addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Image removed from carousel", Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public FloatingActionButton btnDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
