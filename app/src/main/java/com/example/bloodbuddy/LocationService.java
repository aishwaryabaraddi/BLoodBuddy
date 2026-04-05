package com.example.bloodbuddy;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "BloodBuddyChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference donorsRef;
    private DatabaseReference eventsRef;
    private static final long NOTIFICATION_COOLDOWN = 60 * 60 * 1000; // 1 hour
    private long lastNotificationTime = 0;
    private static final float BUFFER_RADIUS = 500.0f; // 500 meters

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel for foreground service
        createNotificationChannel();

        // Set up Firebase references
        donorsRef = FirebaseDatabase.getInstance().getReference("donors");
        eventsRef = FirebaseDatabase.getInstance().getReference("events");

        // Set up location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Request location updates
        startLocationUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "BloodBuddyChannel";
            String description = "Channel for BloodBuddy notifications";
            int importance = NotificationManager.IMPORTANCE_LOW; // Use IMPORTANCE_LOW to reduce noise
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(int donorCount, ArrayList<String> donorsList) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime >= NOTIFICATION_COOLDOWN) { // Check cooldown period
            Intent intent = new Intent(this, DonorsListActivity.class);
            intent.putStringArrayListExtra("donorsList", donorsList); // Pass donors list to the activity
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String message = donorCount + " nearby donor" + (donorCount > 1 ? "s" : "") + " found!";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.yo)
                    .setContentTitle("BloodBuddy")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_LOW) // Use PRIORITY_LOW to reduce noise
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Expandable notification style
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, builder.build());

            lastNotificationTime = currentTime; // Update last notification time
        }
    }


    private void sendEventNotification(String eventName, float distance) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime >= NOTIFICATION_COOLDOWN) { // Check cooldown period
            Intent intent = new Intent(this, EventDetailsActivity.class); // Assume you have an activity to show event details
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String message = "Event " + eventName + " is happening nearby (" + Math.round(distance) + " meters away)!";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.blood) // Replace with your app's icon
                    .setContentTitle("BloodBuddy Event Alert")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_LOW) // Use PRIORITY_LOW to reduce noise
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Expandable notification style
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(2, builder.build()); // Use a different ID for event notifications

            lastNotificationTime = currentTime; // Update last notification time
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10 * 60 * 1000); // 10 minute interval
        locationRequest.setFastestInterval(5 * 60 * 1000); // 5 minutes
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handle case where permissions are not granted
            Log.e(TAG, "Location permissions not granted.");
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                Log.d(TAG, "Location: " + location.getLatitude() + ", " + location.getLongitude());
                checkNearbyDonors(location);
                checkNearbyEvents(location); // Add this line
            }
        }
    };

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

    private void checkNearbyEvents(Location userLocation) {
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    Double latitude = eventSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = eventSnapshot.child("longitude").getValue(Double.class);
                    String eventName = eventSnapshot.child("name").getValue(String.class); // Assuming event has a name field

                    if (latitude != null && longitude != null && eventName != null) {
                        Location eventLocation = new Location("");
                        eventLocation.setLatitude(latitude);
                        eventLocation.setLongitude(longitude);

                        float distance = userLocation.distanceTo(eventLocation);

                        if (distance <= BUFFER_RADIUS) { // Within BUFFER_RADIUS meters buffer radius
                            sendEventNotification(eventName, distance);
                        }
                    } else {
                        Log.w(TAG, "Event location data is incomplete.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read event data.", databaseError.toException());
            }
        });
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create a notification for the foreground service
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.yo)
                .setContentTitle("BloodBuddy")
                .setContentText("BloodBuddy is running")
                .setPriority(NotificationCompat.PRIORITY_LOW); // Use PRIORITY_LOW to reduce noise

        startForeground(1, builder.build());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop location updates when the service is destroyed
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}

