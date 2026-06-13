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
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class LocationWorker extends Worker {
    private static final String TAG = "LocationWorker";
    private static final String CHANNEL_ID = "BloodBuddyChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private static final long NOTIFICATION_COOLDOWN = 60 * 60 * 1000; // 1 hour
    private static long lastNotificationTime = 0;
    private static final float BUFFER_RADIUS = 5000.0f; // 5km radius for better testing
    private Context context;

    public LocationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "BloodBuddy Notifications";
            String description = "Alerts for nearby blood donors and SOS requests";
            int importance = NotificationManager.IMPORTANCE_HIGH; 
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure();
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        checkNearbyDonors(location);
                    }
                });

        return Result.success();
    }

    private void checkNearbyDonors(Location userLocation) {
        db.collection("donors").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                int donorCount = 0;
                ArrayList<String> donorsList = new ArrayList<>();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Donor donor = document.toObject(Donor.class);
                    if (donor.getLatitude() != 0 && donor.getLongitude() != 0) {
                        Location donorLoc = new Location("");
                        donorLoc.setLatitude(donor.getLatitude());
                        donorLoc.setLongitude(donor.getLongitude());

                        float distance = userLocation.distanceTo(donorLoc);
                        if (distance <= BUFFER_RADIUS) {
                            donorCount++;
                            donorsList.add(donor.getName());
                        }
                    }
                }

                if (donorCount > 0) {
                    sendNotification(donorCount, donorsList);
                }
            }
        });
    }

    private void sendNotification(int donorCount, ArrayList<String> donorsList) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime >= NOTIFICATION_COOLDOWN) {
            Intent intent = new Intent(context, DomainActivity.class);
            intent.putStringArrayListExtra("donorsList", donorsList);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String message = donorCount + " donors are nearby and ready to help!";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.blood)
                    .setContentTitle("BloodBuddy: Nearby Donors Found")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(1, builder.build());
            }
            lastNotificationTime = currentTime;
        }
    }
}
