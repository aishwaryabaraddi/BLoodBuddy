package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final double BUFFER_DISTANCE_KM = 50.0; // Increased range for better visibility

    private RouteTask routeTask;
    private RouteParameters routeParameters;
    private MapView mMapView;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private GraphicsOverlay graphicsOverlay;
    private LinearLayout donorListLayout;
    private LinearLayout receiverListLayout;
    private ScrollView donorListScrollView;
    private ScrollView receiverListScrollView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        ArcGISRuntimeEnvironment.setApiKey("AAPK2ee3c403aac24a01a4368148cd8e0019e654qj508BtZjGPXLBCeSpBOyK4yRvxF8w3U46FYJhutXcTJcwTee8qTnV6P3oHy");
        mMapView = findViewById(R.id.mapView);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();

        graphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(graphicsOverlay);

        routeTask = new RouteTask(this, "https://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route");
        routeTask.loadAsync();
        routeTask.addDoneLoadingListener(() -> {
            if (routeTask.getLoadStatus() == LoadStatus.LOADED) {
                try {
                    routeParameters = routeTask.createDefaultParametersAsync().get();
                } catch (Exception e) {
                    Log.e(TAG, "Error creating default parameters: " + e.getMessage());
                }
            }
        });

        donorListLayout = findViewById(R.id.donorListLayout);
        receiverListLayout = findViewById(R.id.receiverListLayout);
        donorListScrollView = findViewById(R.id.donorListScrollView);
        receiverListScrollView = findViewById(R.id.receiverListScrollView);
        
        findViewById(R.id.legendIcon).setOnClickListener(view -> toggleDonorListVisibility());
        findViewById(R.id.legendIconReceivers).setOnClickListener(view -> toggleReceiverListVisibility());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchCurrentLocation();
        }

        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
        mMapView.setMap(map);

        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                android.graphics.Point screenPoint = new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY()));
                ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphics = mMapView.identifyGraphicsOverlayAsync(graphicsOverlay, screenPoint, 10.0, false);
                identifyGraphics.addDoneListener(() -> {
                    try {
                        IdentifyGraphicsOverlayResult result = identifyGraphics.get();
                        if (!result.getGraphics().isEmpty()) {
                            Graphic identifiedGraphic = result.getGraphics().get(0);
                            if (identifiedGraphic.getAttributes().containsKey("latitude")) {
                                showCallout(identifiedGraphic.getGeometry(), identifiedGraphic.getAttributes());
                            }
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error identifying graphic: " + ex.getMessage());
                    }
                });
                return super.onSingleTapConfirmed(e);
            }
        });

        findViewById(R.id.imageViewBack).setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (donorListScrollView.getVisibility() == View.VISIBLE) {
                    donorListScrollView.setVisibility(View.GONE);
                } else if (receiverListScrollView.getVisibility() == View.VISIBLE) {
                    receiverListScrollView.setVisibility(View.GONE);
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });
    }

    private void toggleDonorListVisibility() {
        donorListScrollView.setVisibility(donorListScrollView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        receiverListScrollView.setVisibility(View.GONE);
    }

    private void toggleReceiverListVisibility() {
        receiverListScrollView.setVisibility(receiverListScrollView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        donorListScrollView.setVisibility(View.GONE);
    }

    @Override protected void onResume() { super.onResume(); mMapView.resume(); }
    @Override protected void onPause() { super.onPause(); mMapView.pause(); }
    @Override protected void onDestroy() { super.onDestroy(); if (mMapView != null) mMapView.dispose(); }

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Location location = task.getResult();
                updateMapLocation(location.getLatitude(), location.getLongitude());
                createBufferAndQuery(location.getLatitude(), location.getLongitude());
                addCurrentLocationGraphic(location.getLatitude(), location.getLongitude());
            }
        });
    }

    private void updateMapLocation(double latitude, double longitude) {
        Point currentLocation = new Point(longitude, latitude, SpatialReferences.getWgs84());
        mMapView.setViewpoint(new Viewpoint(currentLocation, 50000));
    }

    private void createBufferAndQuery(double latitude, double longitude) {
        double bufferDistanceMeters = BUFFER_DISTANCE_KM * 1000.0;
        Point currentLocation = new Point(longitude, latitude, SpatialReferences.getWgs84());
        Polygon bufferGeometry = GeometryEngine.buffer(currentLocation, bufferDistanceMeters);
        queryDonorsInBuffer(bufferGeometry);
        queryReceiversInBuffer(bufferGeometry);
    }

    private void queryDonorsInBuffer(Polygon bufferGeometry) {
        db.collection("donors").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                donorListLayout.removeAllViews();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Donor donor = document.toObject(Donor.class);
                    Point donorLocation = new Point(donor.getLongitude(), donor.getLatitude(), SpatialReferences.getWgs84());
                    if (GeometryEngine.contains(bufferGeometry, donorLocation)) {
                        addDonorGraphic(donorLocation, donor);
                        addDonorToList(donor);
                    }
                }
            }
        });
    }

    private void queryReceiversInBuffer(Polygon bufferGeometry) {
        db.collection("receivers").whereEqualTo("active", true).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                receiverListLayout.removeAllViews();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Receiver receiver = document.toObject(Receiver.class);
                    Point receiverLocation = new Point(receiver.getLongitude(), receiver.getLatitude(), SpatialReferences.getWgs84());
                    if (GeometryEngine.contains(bufferGeometry, receiverLocation)) {
                        addReceiverGraphic(receiverLocation, receiver);
                        addReceiverToList(receiver);
                    }
                }
            }
        });
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void addDonorGraphic(Point donorLocation, Donor donor) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.baseline_person_pin_circle_24);
        PictureMarkerSymbol symbol = new PictureMarkerSymbol(new BitmapDrawable(getResources(), getBitmapFromDrawable(drawable)));
        symbol.loadAsync();
        symbol.addDoneLoadingListener(() -> {
            Graphic graphic = new Graphic(donorLocation, symbol);
            graphic.getAttributes().put("name", donor.getName());
            graphic.getAttributes().put("phone", donor.getPhoneNumber());
            graphic.getAttributes().put("bloodGroup", donor.getBloodGroup());
            graphic.getAttributes().put("latitude", donor.getLatitude());
            graphic.getAttributes().put("longitude", donor.getLongitude());
            graphicsOverlay.getGraphics().add(graphic);
        });
    }

    private void addReceiverGraphic(Point receiverLocation, Receiver receiver) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.blood);
        PictureMarkerSymbol symbol = new PictureMarkerSymbol(new BitmapDrawable(getResources(), getBitmapFromDrawable(drawable)));
        symbol.loadAsync();
        symbol.addDoneLoadingListener(() -> {
            Graphic graphic = new Graphic(receiverLocation, symbol);
            graphic.getAttributes().put("name", receiver.getName());
            graphic.getAttributes().put("phone", receiver.getPhoneNumber());
            graphic.getAttributes().put("bloodGroup", receiver.getBloodGroup());
            graphic.getAttributes().put("latitude", receiver.getLatitude());
            graphic.getAttributes().put("longitude", receiver.getLongitude());
            graphicsOverlay.getGraphics().add(graphic);
        });
    }

    private void addDonorToList(Donor donor) {
        View view = getLayoutInflater().inflate(R.layout.donor_item, null);
        ((TextView)view.findViewById(R.id.donorNameTextView)).setText(donor.getName());
        ((TextView)view.findViewById(R.id.donorBloodGroupTextView)).setText(donor.getBloodGroup());
        view.setOnClickListener(v -> updateMapLocation(donor.getLatitude(), donor.getLongitude()));
        donorListLayout.addView(view);
    }

    private void addReceiverToList(Receiver receiver) {
        View view = getLayoutInflater().inflate(R.layout.receiver_item, null);
        ((TextView)view.findViewById(R.id.receiverName)).setText(receiver.getName());
        ((TextView)view.findViewById(R.id.receiverBloodGroup)).setText(receiver.getBloodGroup());
        view.setOnClickListener(v -> updateMapLocation(receiver.getLatitude(), receiver.getLongitude()));
        receiverListLayout.addView(view);
    }

    private void addCurrentLocationGraphic(double latitude, double longitude) {
        Point currentLocation = new Point(longitude, latitude, SpatialReferences.getWgs84());
        Drawable drawable = ContextCompat.getDrawable(this, com.esri.arcgisruntime.R.drawable.arcgisruntime_location_display_default_symbol);
        PictureMarkerSymbol symbol = new PictureMarkerSymbol(new BitmapDrawable(getResources(), getBitmapFromDrawable(drawable)));
        symbol.loadAsync();
        symbol.addDoneLoadingListener(() -> graphicsOverlay.getGraphics().add(new Graphic(currentLocation, symbol)));
    }

    private void showCallout(com.esri.arcgisruntime.geometry.Geometry geometry, Map<String, Object> attributes) {
        Callout callout = mMapView.getCallout();
        callout.setLocation((Point) geometry);
        TextView tv = new TextView(this);
        tv.setText(String.format("Name: %s\nBlood: %s\nPhone: %s", attributes.get("name"), attributes.get("bloodGroup"), attributes.get("phone")));
        tv.setPadding(20, 20, 20, 20);
        tv.setBackgroundColor(Color.WHITE);
        callout.setContent(tv);
        callout.show();
    }
}
