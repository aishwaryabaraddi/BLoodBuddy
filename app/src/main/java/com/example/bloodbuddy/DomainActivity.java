package com.example.bloodbuddy;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DomainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    ViewPager2 viewPager;
    ImageAdapter imageAdapter;
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable runnable;
    int currentItem = 0;
    List<String> imageUris = new ArrayList<>(); // Use List<String> for URIs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_domain);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Set up ViewPager2 for image slideshow
        viewPager = findViewById(R.id.viewPager);
        imageAdapter = new ImageAdapter(this, imageUris);
        viewPager.setAdapter(imageAdapter);

        // Fetch image URIs from Firebase and update ViewPager2
        fetchImageUrisFromFirebase();

        // Automatic slide show
        runnable = new Runnable() {
            @Override
            public void run() {
                if (currentItem == imageAdapter.getItemCount()) {
                    currentItem = 0;
                }
                viewPager.setCurrentItem(currentItem++, true);
                handler.postDelayed(this, 3000); // 3 seconds delay
            }
        };
        handler.postDelayed(runnable, 3000);

        Button donorButton = findViewById(R.id.button1);
        donorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DomainActivity.this, DonorActivity.class);
                startActivity(intent);
            }
        });

        Button receiverButton = findViewById(R.id.button2);
        receiverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DomainActivity.this, ReceiverActivity.class);
                startActivity(intent);
            }
        });

        ImageView hamburgerIcon = findViewById(R.id.imageView);
        hamburgerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_profile) {
                    Intent profileIntent = new Intent(DomainActivity.this, ProfileActivity.class);
                    startActivity(profileIntent);
                } else if (itemId == R.id.nav_Info) {
                    Intent infoIntent = new Intent(DomainActivity.this, BloodInfoActivity.class);
                    startActivity(infoIntent);
                } else if (itemId == R.id.nav_NearbyBloodbanks) {
                    Intent nearbyIntent = new Intent(DomainActivity.this, NearbyHospitalsActivity.class);
                    startActivity(nearbyIntent);
                }
//                else if (itemId == R.id.nav_Myrequests) {
//                    Intent nearbyIntent = new Intent(DomainActivity.this, RequestActivity.class);
//                    startActivity(nearbyIntent);
//                }
                else if (itemId == R.id.nav_upload) {
                    Intent nearbyIntent = new Intent(DomainActivity.this, UploadImage.class);
                    startActivity(nearbyIntent);
                } else if (itemId == R.id.nav_feedback) {
                    Intent nearbyIntent = new Intent(DomainActivity.this, AdminFeedback.class);
                    startActivity(nearbyIntent);
                } else if (itemId == R.id.nav_LogOut) {
                    handleLogout(); // Handle logout here
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.footer);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_request_list) {
                    Intent requestListIntent = new Intent(DomainActivity.this, RequestListActivity.class);
                    startActivity(requestListIntent);
                    return true;
                } else if (itemId == R.id.navigation_donor_list) {
                    Intent mapIntent = new Intent(DomainActivity.this, DisplayDonorActivity.class);
                    startActivity(mapIntent);
                    return true;
                } else if (itemId == R.id.navigation_map) {
                    Intent mapIntent = new Intent(DomainActivity.this, MapActivity.class);
                    startActivity(mapIntent);
                    return true;
                } else if (itemId == R.id.navigation_feedback) {
                    Intent mapIntent = new Intent(DomainActivity.this, UserFeedback.class);
                    startActivity(mapIntent);
                    return true;
//                } else if (itemId == R.id.navigation_about) {
//                    Intent mapIntent = new Intent(DomainActivity.this, About.class);
//                    startActivity(mapIntent);
//                    return true;
                }
                return false;
            }
        });

        // Request location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Start Location Service
        Intent serviceIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("donorsList")) {
            ArrayList<String> donorsList = intent.getStringArrayListExtra("donorsList");
            displayDonors(donorsList);
        }

        // Schedule the periodic work for checking location
        PeriodicWorkRequest locationWorkRequest =
                new PeriodicWorkRequest.Builder(LocationWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "LocationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                locationWorkRequest);

        // Fetch user details and display in Navigation Drawer
        // Fetch user details and display in Navigation Drawer
        View headerView = navigationView.getHeaderView(0);
        TextView nameTextView = headerView.findViewById(R.id.textView9);
        TextView emailTextView = headerView.findViewById(R.id.textView11);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String hardcodedName = "Welcome to Bloodbuddy !!";
            String userEmail = user.getEmail();
            nameTextView.setText(hardcodedName);
            emailTextView.setText(userEmail);

            // Control visibility of upload menu item
            Menu menu = navigationView.getMenu();
            MenuItem uploadMenuItem = menu.findItem(R.id.nav_upload);
            if (userEmail.equals("asturekartik@gmail.com")) { // Replace with the specific user's email
                uploadMenuItem.setVisible(true);
            } else {
                uploadMenuItem.setVisible(false);
            }
        }
    }

    private void fetchImageUrisFromFirebase() {
        DatabaseReference imagesRef = FirebaseDatabase.getInstance().getReference("images");
        imagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                imageUris.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String imageUri = snapshot.getValue(String.class);
                    if (imageUri != null) {
                        imageUris.add(imageUri);
                    }
                }
                // Update the adapter with new image URIs
                imageAdapter.notifyDataSetChanged();
                // If ViewPager2 needs to refresh the images, you might need to set the adapter again
                // viewPager.setAdapter(imageAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Log the error to the console
                Log.e("FirebaseError", "Error fetching image URIs: " + databaseError.getMessage());

                // Optionally, notify the user
                Toast.makeText(DomainActivity.this, "Failed to load images. Please try again.", Toast.LENGTH_SHORT).show();
            }

        });
    }
    private void displayDonors(ArrayList<String> donorsList) {
        // Assuming you have a TextView or a RecyclerView to display the donors list
        TextView donorsTextView = findViewById(R.id.donorsRecyclerView);
        StringBuilder donorsDisplay = new StringBuilder("Detected Donors:\n");
        for (String donor : donorsList) {
            donorsDisplay.append(donor).append("\n");
        }
        donorsTextView.setText(donorsDisplay.toString());
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DomainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Finish current activity so user cannot navigate back to domain
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start location service
                Intent serviceIntent = new Intent(this, LocationService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                // Permission denied, handle accordingly
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app requires location access to function properly. Please grant the location permission.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ActivityCompat.requestPermissions(DomainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE);
                        } else {
                            ActivityCompat.requestPermissions(DomainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

}
