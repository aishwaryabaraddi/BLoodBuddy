package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.widget.ProgressBar;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NearbyHospitalsActivity extends AppCompatActivity {

    private static final String TAG = NearbyHospitalsActivity.class.getSimpleName();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MapView mapView;
    private GraphicsOverlay graphicsOverlay;
    private TextView textNearbyHospitals;
    private CardView hospitalDetailsCardView;
    private TextView hospitalNameTextView;
    private TextView hospitalDistanceTextView;
    private TextView hospitalAddressTextView;
    private TextView hospitalLatTextView;
    private TextView hospitalLonTextView;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FrameLayout requestFormContainer;
    private Button makeRequestButton;
    private ProgressBar progressBar;
    private String selectedHospitalName; // Store the selected hospital name

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_hospitals);

        // Initialize views
        mapView = findViewById(R.id.mapView);
        textNearbyHospitals = findViewById(R.id.textNearbyHospitals);
        hospitalDetailsCardView = findViewById(R.id.hospitalDetailsCardView);
        hospitalNameTextView = findViewById(R.id.hospitalNameTextView);
        hospitalDistanceTextView = findViewById(R.id.hospitalDistanceTextView);
        hospitalAddressTextView = findViewById(R.id.hospitalAddressTextView);
        hospitalLatTextView = findViewById(R.id.hospitalLatTextView);
        hospitalLonTextView = findViewById(R.id.hospitalLonTextView);
        requestFormContainer = findViewById(R.id.requestFormContainer);

        // Initialize progress bar
        ProgressBar progressBar = findViewById(R.id.progressBar);

        // Initialize map with a basemap
        ArcGISMap map = new ArcGISMap(Basemap.createTopographic());
        mapView.setMap(map);

        // Initialize graphics overlay
        graphicsOverlay = new GraphicsOverlay();
        mapView.getGraphicsOverlays().add(graphicsOverlay);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Request location permissions and get the current location
        requestLocationPermissions();

        // Show progress bar while fetching nearby hospitals
        progressBar.setVisibility(View.VISIBLE);

        // Fetch nearby hospitals data
        fetchNearbyHospitals(777689.62, 1448898.35);

//        makeRequestButton = findViewById(R.id.makeRequestButton);
//        makeRequestButton.setOnClickListener(v -> showRequestForm());

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ImageView imageViewBack = findViewById(R.id.imageView10);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to DomainActivity
                Intent intent = new Intent(NearbyHospitalsActivity.this, DomainActivity.class);
                startActivity(intent);
            }
        });
    }


    private void showRequestForm() {
        hospitalDetailsCardView.setVisibility(View.GONE); // Hide the CardView
        requestFormContainer.setVisibility(View.VISIBLE); // Show the FrameLayout
        View requestFormView = getLayoutInflater().inflate(R.layout.request_form_card, null);
        requestFormContainer.removeAllViews();
        requestFormContainer.addView(requestFormView);

        Button submitButton = requestFormView.findViewById(R.id.buttonSubmit);
        submitButton.setOnClickListener(v -> {
            // Display toast message
            Toast.makeText(NearbyHospitalsActivity.this, "Request submitted successfully", Toast.LENGTH_SHORT).show();
            // Hide the request form container
            requestFormContainer.setVisibility(View.GONE);
        });

        // Set the hospital name in the request form
        EditText editTextHospitalName = requestFormView.findViewById(R.id.editTextHospitalName);
        editTextHospitalName.setText(selectedHospitalName); // Use selectedHospitalName
    }


    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // Stop location updates
                        fusedLocationClient.removeLocationUpdates(locationCallback);

                        // Zoom to current location
                        Point currentLocation = new Point(longitude, latitude, SpatialReference.create(4326));
                        mapView.setViewpoint(new Viewpoint(currentLocation, 10000));

                        // Add current location marker to the map
                        addCurrentLocationMarker(currentLocation);

                        // Fetch nearby hospitals
                        fetchNearbyHospitals(longitude, latitude);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void addCurrentLocationMarker(Point location) {
        BitmapDrawable bitmapDrawable = BitmapUtils.getBitmapDrawable(getApplicationContext(), com.esri.arcgisruntime.R.drawable.arcgisruntime_location_display_default_symbol);
        PictureMarkerSymbol currentLocationSymbol = new PictureMarkerSymbol(bitmapDrawable);
        currentLocationSymbol.setWidth(32);
        currentLocationSymbol.setHeight(32);

        Graphic graphic = new Graphic(location, currentLocationSymbol);
        graphicsOverlay.getGraphics().add(graphic);
    }

    private void fetchNearbyHospitals(double longitude, double latitude) {
        // Construct the URL with the provided latitude and longitude
        String apiUrl = "https://kgis.ksrsac.in:9000/NearbyAssets/ws/v2/getNearbyAssetData?code=1201&lon=" + longitude + "&lat=" + latitude;

        // Add this before making the HTTPS connection
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Perform API call to get nearby hospital data
        new Thread(() -> {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String response = streamToString(in); // Convert InputStream to String
                    handleHospitalResponse(response); // Handle JSON response parsing and UI update for hospitals
                    hideProgressBar(); // Hide progress bar after fetching data
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching nearby hospital data: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void hideProgressBar() {
        runOnUiThread(() -> {
            ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);
        });
    }

    // Helper method to handle JSON response parsing and UI update for hospitals
    private void handleHospitalResponse(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject hospital = jsonArray.getJSONObject(i);
                    double utmX = hospital.getDouble("Lon");
                    double utmY = hospital.getDouble("Lat");
                    String name = hospital.getString("assetName");
                    double distanceMeters = hospital.getDouble("distance");
                    String address = hospital.getString("address");
                    double distance = distanceMeters / 1000;

                    Log.d(TAG, "Hospital: " + name + ", UTM_X: " + utmX + ", UTM_Y: " + utmY);

                    // Convert UTM to LatLon
                    Point utmPoint = new Point(utmX, utmY, SpatialReference.create(32643));
                    Point latLonPoint = (Point) GeometryEngine.project(utmPoint, SpatialReference.create(4326));

                    double latitude = latLonPoint.getY();
                    double longitude = latLonPoint.getX();

                    // Convert vector drawable to bitmap drawable
                    BitmapDrawable bitmapDrawable = BitmapUtils.getBitmapDrawable(getApplicationContext(), R.drawable.hospital);
                    int width = 64;
                    int height = 64;
                    bitmapDrawable.setBounds(0, 0, width, height);

                    // Create a PictureMarkerSymbol for the hospital icon
                    PictureMarkerSymbol hospitalSymbol = new PictureMarkerSymbol(bitmapDrawable);
                    hospitalSymbol.setWidth(width);
                    hospitalSymbol.setHeight(height);

                    int finalI = i;
                    runOnUiThread(() -> {
                        if (graphicsOverlay != null && mapView != null) {
                            Graphic graphic = new Graphic(latLonPoint, hospitalSymbol);
                            graphicsOverlay.getGraphics().add(graphic);
                            Log.d(TAG, "Added Hospital Graphic at LatLon coordinates: " + latitude + ", " + longitude);

                            // Set attributes for the graphic
                            graphic.getAttributes().put("name", name);
                            graphic.getAttributes().put("distance", distance);
                            graphic.getAttributes().put("address", address);
                            graphic.getAttributes().put("Lon", longitude);
                            graphic.getAttributes().put("Lat", latitude);

                            // Add click listener for the graphic
                            mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mapView) {
                                @Override
                                public boolean onSingleTapConfirmed(MotionEvent e) {
                                    android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
                                    ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphic = mapView.identifyGraphicsOverlayAsync(graphicsOverlay, screenPoint, 10, false);

                                    identifyGraphic.addDoneListener(() -> {
                                        try {
                                            IdentifyGraphicsOverlayResult result = identifyGraphic.get();
                                            if (!result.getGraphics().isEmpty()) {
                                                Graphic identifiedGraphic = result.getGraphics().get(0);
                                                String hospitalName = identifiedGraphic.getAttributes().get("name").toString();
                                                String hospitalAddress = identifiedGraphic.getAttributes().get("address").toString();
                                                double hospitalDistance = (double) identifiedGraphic.getAttributes().get("distance");
                                                double hospitalLat = (double) identifiedGraphic.getAttributes().get("Lat");
                                                double hospitalLon = (double) identifiedGraphic.getAttributes().get("Lon");

                                                showHospitalDetails(hospitalName, hospitalDistance, hospitalAddress, hospitalLat, hospitalLon);
                                            }
                                        } catch (InterruptedException | ExecutionException ex) {
                                            ex.printStackTrace();
                                        }
                                    });
                                    return super.onSingleTapConfirmed(e);
                                }
                            });
                        } else {
                            Log.e(TAG, "GraphicsOverlay or MapView is not properly initialized.");
                        }

                        // Display information for the first hospital
                        if (finalI == 0) {
                            showNearbyHospitals(name, distance);
                        }
                    });
                }
            } else {
                Log.d(TAG, "No nearby hospitals found.");
                // Handle case where no nearby hospitals are found
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response for hospitals: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showHospitalDetails(String name, double distance, String address, double lat, double lon) {
        runOnUiThread(() -> {
            hospitalNameTextView.setText(name);
            hospitalDistanceTextView.setText("Distance: " + distance + " km");
            hospitalAddressTextView.setText(address);
            hospitalLatTextView.setText("Latitude: " + lat);
            hospitalLonTextView.setText("Longitude: " + lon);

            ImageView hospitalIconImageView = hospitalDetailsCardView.findViewById(R.id.hospitalIconImageView);

            // Set hospital icon to ImageView
            hospitalIconImageView.setImageResource(R.drawable.baseline_directions_24);
            hospitalDetailsCardView.setVisibility(View.VISIBLE);

            // Set click listener to start navigation
            hospitalIconImageView.setOnClickListener(v -> {
                String uri = "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lon;
                Intent navigateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                navigateIntent.setPackage("com.google.android.apps.maps");
                if (navigateIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(navigateIntent);
                } else {
                    // Fallback: Open in any available map application if Google Maps is not available
                    Uri fallbackUri = Uri.parse("geo:" + lat + "," + lon);
                    Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, fallbackUri);
                    startActivity(fallbackIntent);
                }
            });
        });
    }

    // Helper method to convert InputStream to String
    private String streamToString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }

    // Example method to update UI with nearby hospitals information
    private void showNearbyHospitals(String name, double distance) {
        runOnUiThread(() -> {
            if (textNearbyHospitals != null) {
                // Update the TextView with nearby hospitals name and distance
                String infoText = "Nearby Hospital: " + name + "    " + "Distance: " + distance + " km";
                textNearbyHospitals.setText(infoText);
                textNearbyHospitals.setVisibility(View.VISIBLE); // Make the TextView visible
            } else {
                Log.e(TAG, "textNearbyHospitals TextView is null. Cannot set text.");
                // Handle the case where textNearbyHospitals is null, possibly by checking layout inflation or context issues
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.dispose();
        }
    }

    @Override
    public void onBackPressed() {
        // Handle back press to hide requestFormContainer if it's visible
        if (requestFormContainer != null && requestFormContainer.getVisibility() == View.VISIBLE) {
            requestFormContainer.setVisibility(View.GONE);
        }
        // Handle back press to hide hospitalDetailsCardView if it's visible
        else if (hospitalDetailsCardView != null && hospitalDetailsCardView.getVisibility() == View.VISIBLE) {
            hospitalDetailsCardView.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                // Handle permission denied
                Log.e(TAG, "Location permission denied.");
                // Optionally, you can show a message to the user indicating why location is needed and request again
            }
        }
    }
}
