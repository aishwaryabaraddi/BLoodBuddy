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

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "BloodBuddySOSChannel";
    private static final double MAX_DISTANCE_KM = 25.0; // Standard proximity for blood donation

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            String type = data.get("type");
            if ("SOS".equals(type)) {
                handleSOSNotification(data);
            } else {
                sendNotification("Blood Buddy", data.get("message"));
            }
        }
    }

    private void handleSOSNotification(Map<String, String> data) {
        String latStr = data.get("lat");
        String lngStr = data.get("lng");

        if (latStr != null && lngStr != null) {
            double patientLat = Double.parseDouble(latStr);
            double patientLng = Double.parseDouble(lngStr);

            // Get donor's current location to check proximity
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(location.getLatitude(), location.getLongitude(), patientLat, patientLng, results);
                        float distanceInKm = results[0] / 1000;

                        if (distanceInKm <= MAX_DISTANCE_KM) {
                            sendNotification(data.get("title"), data.get("message") + " (Distance: " + String.format("%.1f", distanceInKm) + " km)");
                        } else {
                            Log.d(TAG, "Request ignored: Donor is " + distanceInKm + " km away.");
                        }
                    } else {
                        // If location is null, show notification anyway as fallback
                        sendNotification(data.get("title"), data.get("message"));
                    }
                });
            } else {
                // If permission not granted, show it just in case
                sendNotification(data.get("title"), data.get("message"));
            }
        }
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, RequestListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.blood)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Blood Emergency SOS", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
}
