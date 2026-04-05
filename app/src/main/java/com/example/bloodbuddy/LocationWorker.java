package com.example.bloodbuddy;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class LocationWorker extends Worker {
    private static final String TAG = "LocationWorker";
    private static final String CHANNEL_ID = "BloodBuddyChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference donorsRef;
    private static final long NOTIFICATION_COOLDOWN = 60 * 60 * 1000; // 1 hour
    private long lastNotificationTime = 0;
    private static final float BUFFER_RADIUS = 500.0f; // 500 meters
    private Context context;

    public LocationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        donorsRef = FirebaseDatabase.getInstance().getReference("donors");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "BloodBuddyChannel";
            String description = "Channel for BloodBuddy notifications";
            int importance = NotificationManager.IMPORTANCE_LOW; // Use IMPORTANCE_LOW to reduce noise
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions are not granted.");
            return Result.failure(); // Return failure result if permissions are not granted
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        checkNearbyDonors(location);
                    } else {
                        Log.w(TAG, "Location is null");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get location", e));

        return Result.success();
    }

    private void sendNotification(int donorCount, ArrayList<String> donorsList) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime >= NOTIFICATION_COOLDOWN) { // Check cooldown period
            Intent intent = new Intent(context, DonorsListActivity.class);
            intent.putStringArrayListExtra("donorsList", donorsList);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String message = donorCount + " nearby donor" + (donorCount > 1 ? "s" : "") + " found!";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.blood)
                    .setContentTitle("BloodBuddy")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_LOW) // Use PRIORITY_LOW to reduce noise
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Expandable notification style
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, builder.build());

            lastNotificationTime = currentTime; // Update last notification time
        }
    }

    private void checkNearbyDonors(Location userLocation) {
        donorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int donorCount = 0;
                ArrayList<String> donorsList = new ArrayList<>();

                for (DataSnapshot donorSnapshot : dataSnapshot.getChildren()) {
                    Double latitude = donorSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = donorSnapshot.child("longitude").getValue(Double.class);
                    String donorName = donorSnapshot.child("name").getValue(String.class); // Assuming donor has a name field

                    // Check for null values before using them
                    if (latitude != null && longitude != null && donorName != null) {
                        Location donorLocation = new Location("");
                        donorLocation.setLatitude(latitude);
                        donorLocation.setLongitude(longitude);

                        float distance = userLocation.distanceTo(donorLocation);

                        if (distance <= BUFFER_RADIUS) { // Within BUFFER_RADIUS meters buffer radius
                            donorCount++;
                            donorsList.add(donorName);
                        }
                    } else {
                        Log.w(TAG, "Donor location data is incomplete.");
                    }
                }

                if (donorCount > 0) {
                    sendNotification(donorCount, donorsList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read donor data.", databaseError.toException());
            }
        });
    }
}
