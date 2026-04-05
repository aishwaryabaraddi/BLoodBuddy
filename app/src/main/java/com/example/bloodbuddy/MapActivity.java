package com.example.bloodbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Geometry;
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
import com.esri.arcgisruntime.location.LocationDataSource;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Arrays;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
//    private static final String ROUTE_SERVICE_URL = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final double BUFFER_DISTANCE_KM = 20.0;

    private RouteTask routeTask;
    private RouteParameters routeParameters;
    private MapView mMapView;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference donorsRef;
    private DatabaseReference receiversRef;
    private GraphicsOverlay graphicsOverlay;
    private LinearLayout donorListLayout;
    private LinearLayout receiverListLayout;
    private ScrollView donorListScrollView;
    private ScrollView receiverListScrollView;
    private ImageView legendIconDonors;
    private ImageView legendIconReceivers;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        ArcGISRuntimeEnvironment.setApiKey("AAPK2ee3c403aac24a01a4368148cd8e0019e654qj508BtZjGPXLBCeSpBOyK4yRvxF8w3U46FYJhutXcTJcwTee8qTnV6P3oHy");
        mMapView = findViewById(R.id.mapView);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        donorsRef = FirebaseDatabase.getInstance().getReference().child("donors");
        receiversRef = FirebaseDatabase.getInstance().getReference().child("receivers");

        graphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(graphicsOverlay);

        // Initialize the RouteTask and RouteParameters
        routeTask = new RouteTask(this, "https://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route");
        routeTask.loadAsync();
        routeTask.addDoneLoadingListener(() -> {
            if (routeTask.getLoadStatus() == LoadStatus.LOADED) {
                try {
                    routeParameters = routeTask.createDefaultParametersAsync().get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // Handle the error if the route task fails to load
                Log.e("RouteTask", "Failed to load RouteTask");
            }
        });


        donorListLayout = findViewById(R.id.donorListLayout);
        receiverListLayout = findViewById(R.id.receiverListLayout);
        donorListScrollView = findViewById(R.id.donorListScrollView);
        receiverListScrollView = findViewById(R.id.receiverListScrollView);
        legendIconDonors = findViewById(R.id.legendIcon);
        legendIconReceivers = findViewById(R.id.legendIconReceivers);

        legendIconDonors.setOnClickListener(view -> toggleDonorListVisibility());
        legendIconReceivers.setOnClickListener(view -> toggleReceiverListVisibility());

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
                            if (identifiedGraphic.getAttributes().containsKey("longitude") && identifiedGraphic.getAttributes().containsKey("latitude")) {
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

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ImageView imageViewBack = findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to DomainActivity
                Intent intent = new Intent(MapActivity.this, DomainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void toggleDonorListVisibility() {
        if (donorListScrollView.getVisibility() == View.VISIBLE) {
            donorListScrollView.setVisibility(View.GONE);
        } else {
            donorListScrollView.setVisibility(View.VISIBLE);
        }
    }

    private void toggleReceiverListVisibility() {
        if (receiverListScrollView.getVisibility() == View.VISIBLE) {
            receiverListScrollView.setVisibility(View.GONE);
        } else {
            receiverListScrollView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.dispose();
        }
    }

    @Override
    public void onBackPressed() {
        if (donorListScrollView.getVisibility() == View.VISIBLE) {
            donorListScrollView.setVisibility(View.GONE);
        } else if (receiverListScrollView.getVisibility() == View.VISIBLE) {
            receiverListScrollView.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Location location = task.getResult();
                    updateMapLocation(location.getLatitude(), location.getLongitude());
                    createBufferAndQuery(location.getLatitude(), location.getLongitude());
                    addCurrentLocationGraphic(location.getLatitude(), location.getLongitude());
                } else {
                    Toast.makeText(MapActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateMapLocation(double latitude, double longitude) {
        double scale = 10000;
        Point currentLocation = new Point(longitude, latitude, SpatialReferences.getWgs84());
        Viewpoint currentViewpoint = new Viewpoint(currentLocation, scale);
        mMapView.setViewpoint(currentViewpoint);
    }

    private void createBufferAndQuery(double latitude, double longitude) {
        double bufferDistanceMeters = BUFFER_DISTANCE_KM * 1000.0;
        Point currentLocation = new Point(longitude, latitude, SpatialReferences.getWgs84());
        Polygon bufferGeometry = GeometryEngine.buffer(currentLocation, bufferDistanceMeters);
        queryDonorsInBuffer(bufferGeometry);
        queryReceiversInBuffer(bufferGeometry);
    }

    private void queryDonorsInBuffer(Polygon bufferGeometry) {
        Map<String, Donor> donorsMap = new HashMap<>();
        donorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                donorListLayout.removeAllViews();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Donor donor = snapshot.getValue(Donor.class);
                    if (donor != null) {
                        Point donorLocation = new Point(donor.getLongitude(), donor.getLatitude(), SpatialReferences.getWgs84());
                        if (GeometryEngine.contains(bufferGeometry, donorLocation)) {
                            donorsMap.put(snapshot.getKey(), donor);
                            addDonorGraphic(donorLocation, donor);
                            addDonorToList(donor);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Failed to query donors: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void queryReceiversInBuffer(Polygon bufferGeometry) {
        Map<String, Receiver> receiversMap = new HashMap<>();
        receiversRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                receiverListLayout.removeAllViews();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Receiver receiver = snapshot.getValue(Receiver.class);
                    if (receiver != null) {
                        Point receiverLocation = new Point(receiver.getLongitude(), receiver.getLatitude(), SpatialReferences.getWgs84());
                        if (GeometryEngine.contains(bufferGeometry, receiverLocation)) {
                            receiversMap.put(snapshot.getKey(), receiver);
                            addReceiverGraphic(receiverLocation, receiver);
                            addReceiverToList(receiver);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Failed to query receivers: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else {
            int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 100;
            int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 100;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }


    private void addDonorGraphic(Point donorLocation, Donor donor) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.baseline_person_pin_circle_24);
        Bitmap bitmap = getBitmapFromDrawable(drawable);
        PictureMarkerSymbol symbol = new PictureMarkerSymbol(new BitmapDrawable(getResources(), bitmap));
        symbol.loadAsync();
        symbol.addDoneLoadingListener(() -> {
            Graphic graphic = new Graphic(donorLocation, symbol);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("name", donor.getName());
            attributes.put("phone", donor.getPhoneNumber());
            attributes.put("bloodGroup", donor.getBloodGroup());
            attributes.put("latitude", donor.getLatitude());
            attributes.put("longitude", donor.getLongitude());
            graphic.getAttributes().putAll(attributes);
            graphicsOverlay.getGraphics().add(graphic);
        });
    }

    private void addReceiverGraphic(Point receiverLocation, Receiver receiver) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.info);
        Bitmap bitmap = getBitmapFromDrawable(drawable);
        PictureMarkerSymbol symbol = new PictureMarkerSymbol(new BitmapDrawable(getResources(), bitmap));
        symbol.loadAsync();
        symbol.addDoneLoadingListener(() -> {
            Graphic graphic = new Graphic(receiverLocation, symbol);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("name", receiver.getName());
            attributes.put("latitude", receiver.getLatitude());
            attributes.put("longitude", receiver.getLongitude());
            graphic.getAttributes().putAll(attributes);
            graphicsOverlay.getGraphics().add(graphic);
        });
    }

    private void addDonorToList(Donor donor) {
        // Create a new LinearLayout to hold donor details and buttons
        LinearLayout donorItemLayout = new LinearLayout(this);
        donorItemLayout.setOrientation(LinearLayout.VERTICAL);
        donorItemLayout.setPadding(10, 10, 10, 10);
        donorItemLayout.setBackgroundColor(Color.WHITE);

        // Get screen width in pixels
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;

        // Convert 50dp to pixels
        int marginInDp = (int) (100 * displayMetrics.density + 0.5f);

        // Set the width of the donorItemLayout
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                screenWidth - marginInDp, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 10);
        donorItemLayout.setLayoutParams(layoutParams);

        // Create a TextView for donor details
        TextView donorView = new TextView(this);
        donorView.setText("Name: " + donor.getName() +
                "\nContact: " + donor.getPhoneNumber() +
                "\nBlood Group: " + donor.getBloodGroup() +
                "\nLongitude: " + donor.getLongitude() +
                "\nLatitude: " + donor.getLatitude());
        donorView.setPadding(10, 10, 10, 10);
        donorItemLayout.addView(donorView);

        // Create a button for calling the donor
        Button callButton = new Button(this);
        callButton.setText("Call");
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + donor.getPhoneNumber()));
                startActivity(callIntent);
            }
        });
        donorItemLayout.addView(callButton);

        // Create a button for navigating to the donor's location
        Button navigateButton = new Button(this);
        navigateButton.setText("Navigate");
        navigateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Assuming you have the latitude and longitude of the donor's location
                double latitude = donor.getLatitude();
                double longitude = donor.getLongitude();
                String uri = "https://www.google.com/maps/dir/?api=1&destination=" + latitude + "," + longitude;
                Intent navigateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                navigateIntent.setPackage("com.google.android.apps.maps");
                startActivity(navigateIntent);
            }
        });
        donorItemLayout.addView(navigateButton);

        // Add the entire layout (details + buttons) to the main donor list layout
        donorListLayout.addView(donorItemLayout);
    }

    private void addReceiverToList(Receiver receiver) {
        LinearLayout receiverItemLayout = new LinearLayout(this);
        receiverItemLayout.setOrientation(LinearLayout.VERTICAL);
        receiverItemLayout.setPadding(10, 10, 10, 10);
        receiverItemLayout.setBackgroundColor(Color.WHITE);

        // Get screen width in pixels
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;

        // Convert 50dp to pixels
        int marginInDp = (int) (100 * displayMetrics.density + 0.5f);

        // Set the width of the receiverItemLayout
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                screenWidth - marginInDp, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 10);
        receiverItemLayout.setLayoutParams(layoutParams);

        // Create a TextView for receiver details
        TextView receiverView = new TextView(this);
        receiverView.setText("Name: " + receiver.getName() +
                "\nBlood Group: " + receiver.getBloodGroup() +
                "\nLocation: " + receiver.getLocation() +
                "\nLongitude: " + receiver.getLongitude() +
                "\nLatitude: " + receiver.getLatitude());
        receiverView.setPadding(10, 10, 10, 10);
        receiverItemLayout.addView(receiverView);

        // Create a button for navigating to the receiver's location
        Button navigateButton = new Button(this);
        navigateButton.setText("Navigate");
        navigateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Assuming you have the latitude and longitude of the receiver's location
                double latitude = receiver.getLatitude();
                double longitude = receiver.getLongitude();

                // Perform routing within the app using Esri ArcGIS SDK
                performRouting(latitude, longitude);
            }
        });
        receiverItemLayout.addView(navigateButton);

        // Add the entire layout (details + button) to the main receiver list layout
        receiverListLayout.addView(receiverItemLayout);
    }

    private void performRouting(double latitude, double longitude) {
        if (routeParameters == null) {
            Toast.makeText(this, "Route parameters not initialized. Please try again later.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Get the current location of the user (replace with actual user location)
            double userLongitude=0;
            double userLatitude=0;
            Point currentLocation = new Point(userLongitude, userLatitude, SpatialReferences.getWgs84());

            // Define the receiver location
            Point receiverLocation = new Point(longitude, latitude, SpatialReferences.getWgs84());

            // Create stops for the route
            List<Stop> stops = new ArrayList<>();
            stops.add(new Stop(currentLocation));
            stops.add(new Stop(receiverLocation));
            routeParameters.setStops(stops);

            // Solve the route
            ListenableFuture<RouteResult> routeResultFuture = routeTask.solveRouteAsync(routeParameters);
            routeResultFuture.addDoneListener(() -> {
                try {
                    RouteResult routeResult = routeResultFuture.get();
                    Route route = routeResult.getRoutes().get(0);

                    // Create a graphic for the route
                    SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 5.0f);
                    Graphic routeGraphic = new Graphic(route.getRouteGeometry(), routeSymbol);
                    graphicsOverlay.getGraphics().add(routeGraphic);

                    // Zoom to the route
                    mMapView.setViewpointGeometryAsync(route.getRouteGeometry().getExtent(), 100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addCurrentLocationGraphic(double latitude, double longitude) {
        Point currentLocation = new Point(longitude, latitude, SpatialReferences.getWgs84());
        Drawable drawable = ContextCompat.getDrawable(this, com.esri.arcgisruntime.R.drawable.arcgisruntime_location_display_default_symbol);
        Bitmap bitmap = getBitmapFromDrawable(drawable);
        PictureMarkerSymbol symbol = new PictureMarkerSymbol(new BitmapDrawable(getResources(), bitmap));
        symbol.loadAsync();
        symbol.addDoneLoadingListener(() -> {
            Graphic graphic = new Graphic(currentLocation, symbol);
            graphicsOverlay.getGraphics().add(graphic);
        });
    }

    private void showCallout(com.esri.arcgisruntime.geometry.Geometry geometry, Map<String, Object> attributes) {
        // Create a callout and set its content
        Callout callout = mMapView.getCallout();
        callout.setLocation((Point) geometry);
        callout.setContent(new androidx.appcompat.widget.AppCompatTextView(MapActivity.this) {{
            setText("Name: " + attributes.get("name") +
                    "\nPhone: " + attributes.get("phone") +
                    "\nBlood Group: " + attributes.get("bloodGroup") +
                    "\nLongitude: " + attributes.get("longitude") +
                    "\nLatitude: " + attributes.get("latitude"));
            setPadding(10, 10, 10, 10);
            setBackgroundColor(0xFFFFFFFF);
        }});
        callout.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                Log.e(TAG, "Location permission denied");
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

