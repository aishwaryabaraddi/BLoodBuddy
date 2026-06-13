package com.example.bloodbuddy;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "BloodBuddyChannel";
    private static final String SOS_CHANNEL_ID = "BloodBuddySOSChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference donorsRef;
    private DatabaseReference eventsRef;
    private DatabaseReference receiversRef;
    private static final long NOTIFICATION_COOLDOWN = 60 * 60 * 1000; // 1 hour
    private long lastNotificationTime = 0;
    private static final float BUFFER_RADIUS = 500.0f; // 500 meters
    private static final float SOS_RADIUS = 5000.0f; // 5km for SOS

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel for foreground service
        createNotificationChannel();

        // Set up Firebase references
        donorsRef = FirebaseDatabase.getInstance().getReference("donors");
        eventsRef = FirebaseDatabase.getInstance().getReference("events");
        receiversRef = FirebaseDatabase.getInstance().getReference("receivers");

        // Set up location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Request location updates
        startLocationUpdates();

        // Listen for SOS requests
        listenForEmergencyRequests();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Regular channel
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "BloodBuddy General", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("General notifications for nearby donors and events");
            notificationManager.createNotificationChannel(channel);

            // SOS channel (High Importance)
            NotificationChannel sosChannel = new NotificationChannel(SOS_CHANNEL_ID, "BloodBuddy SOS", NotificationManager.IMPORTANCE_HIGH);
            sosChannel.setDescription("Emergency blood requests nearby");
            sosChannel.enableVibration(true);
            sosChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            notificationManager.createNotificationChannel(sosChannel);
        }
    }

    private void listenForEmergencyRequests() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Get current user's blood group from Firestore
        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userBloodGroup = documentSnapshot.getString("bloodGroup");
                        if (userBloodGroup != null) {
                            startSOSListener(userBloodGroup);
                        }
                    }
                });
    }

    private void startSOSListener(String bloodGroup) {
        receiversRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check if any NEW request matches blood group and distance
                if (ActivityCompat.checkSelfPermission(LocationService.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(LocationService.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Receiver receiver = snapshot.getValue(Receiver.class);
                                if (receiver != null && bloodGroup.equals(receiver.getBloodGroup())) {
                                    Location target = new Location("");
                                    target.setLatitude(receiver.getLatitude());
                                    target.setLongitude(receiver.getLongitude());

                                    float distance = location.distanceTo(target);
                                    if (distance <= SOS_RADIUS) {
                                        sendSOSNotification(receiver);
                                    }
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendSOSNotification(Receiver receiver) {
        Intent intent = new Intent(this, RequestListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "🚨 URGENT: Blood Needed Nearby!";
        String message = receiver.getBloodGroup() + " required for " + receiver.getToWhomFor() + " at " + receiver.getLocation();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SOS_CHANNEL_ID)
                .setSmallIcon(R.drawable.blood)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int)System.currentTimeMillis(), builder.build());
    }

    private void sendNotification(int donorCount, ArrayList<String> donorsList) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime >= NOTIFICATION_COOLDOWN) {
            Intent intent = new Intent(this, DonorsListActivity.class);
            intent.putStringArrayListExtra("donorsList", donorsList);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String message = donorCount + " nearby donor" + (donorCount > 1 ? "s" : "") + " found!";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.yo)
                    .setContentTitle("BloodBuddy")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, builder.build());

            lastNotificationTime = currentTime;
        }
    }


    private void sendEventNotification(String eventName, float distance) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime >= NOTIFICATION_COOLDOWN) {
            Intent intent = new Intent(this, EventDetailsActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String message = "Event " + eventName + " is happening nearby (" + Math.round(distance) + " meters away)!";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.blood)
                    .setContentTitle("BloodBuddy Event Alert")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(2, builder.build());

            lastNotificationTime = currentTime;
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted. Cannot start updates.");
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10 * 60 * 1000); // 10 minute interval
        locationRequest.setFastestInterval(5 * 60 * 1000); // 5 minutes
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                Log.d(TAG, "Location: " + location.getLatitude() + ", " + location.getLongitude());
                checkNearbyDonors(location);
                checkNearbyEvents(location);
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
                    String donorName = donorSnapshot.child("name").getValue(String.class);

                    if (latitude != null && longitude != null && donorName != null) {
                        Location donorLocation = new Location("");
                        donorLocation.setLatitude(latitude);
                        donorLocation.setLongitude(longitude);

                        float distance = userLocation.distanceTo(donorLocation);

                        if (distance <= BUFFER_RADIUS) {
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
                    String eventName = eventSnapshot.child("name").getValue(String.class);

                    if (latitude != null && longitude != null && eventName != null) {
                        Location eventLocation = new Location("");
                        eventLocation.setLatitude(latitude);
                        eventLocation.setLongitude(longitude);

                        float distance = userLocation.distanceTo(eventLocation);

                        if (distance <= BUFFER_RADIUS) {
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check for permissions before starting foreground
        boolean hasLocationPermission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasLocationPermission) {
            Log.e(TAG, "Cannot start Location Foreground Service: Permissions missing.");
            stopSelf();
            return START_NOT_STICKY;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.yo)
                .setContentTitle("BloodBuddy")
                .setContentText("BloodBuddy is running in background")
                .setPriority(NotificationCompat.PRIORITY_LOW);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(1, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service", e);
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
