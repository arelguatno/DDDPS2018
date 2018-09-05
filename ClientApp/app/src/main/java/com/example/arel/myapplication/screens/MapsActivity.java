package com.example.arel.myapplication.screens;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arel.myapplication.R;
import com.example.arel.myapplication.StatationsDialog;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, StatationsDialog.StationsDialogListener {

    private GoogleMap mMap;
    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    private static final String TAG = "MapsActivity";
    MarkerOptions originMarker;
    MarkerOptions destinationMarker;
    private ImageView mGps;
    SupportMapFragment mapFragment;

    TextView input_origin, input_destination;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mGps = (ImageView) findViewById(R.id.ic_gps);

        input_origin = findViewById(R.id.input_origin);
        input_destination = findViewById(R.id.input_destination);

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(destinationMarker == null && originMarker == null){
                    return;
                }

                if(destinationMarker != null){
                    zoomStationsCamera(originMarker,destinationMarker);
                }else {
                    zoomStationsCamera(originMarker,originMarker);
                }
            }
        });

        getLocationPermission();
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            moveCameraLocationCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM);

                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCameraLocationCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void zoomStationsCamera(MarkerOptions origin, MarkerOptions destination) {
        if(origin.getPosition() == destination.getPosition()){
            moveCameraLocationCamera(origin.getPosition(), DEFAULT_ZOOM);
            return;
        }

        // start zooming the map to all markers
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        //the include method will calculate the min and max bound.
        builder.include(origin.getPosition());
        builder.include(destination.getPosition());

        LatLngBounds bounds = builder.build();

//        int width = getResources().getDisplayMetrics().widthPixels;
//        int height = getResources().getDisplayMetrics().heightPixels;
        int width = mapFragment.getView().getMeasuredWidth();
        int height = mapFragment.getView().getMeasuredHeight();

        int padding = (int) (width * 0.18); // offset from edges of the map 21% of screen

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);

        mMap.animateCamera(cu);
        // end of zooming
    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }


    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);

        }
    }

    public void toButton(View v) {
        StatationsDialog statationsDialog = new StatationsDialog();
        statationsDialog.setCancelable(false);

        Bundle args = new Bundle();
        args.putBoolean("isDestination", false);  // origin

        statationsDialog.setArguments(args);
        statationsDialog.show(getSupportFragmentManager(), "dialog");
    }

    public void fromButton(View v) {
        StatationsDialog statationsDialog = new StatationsDialog();
        statationsDialog.setCancelable(false);

        Bundle args = new Bundle();
        args.putBoolean("isDestination", true); // destination

        statationsDialog.setArguments(args);
        statationsDialog.show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void dialogReturnGeoPoint(GeoPoint geoPoint, boolean isDestination, String stationName) {

        // Start moving the camera, and add marker.,
        if (!isDestination) {   // origin

            mMap.clear(); // clear all markers

            if(destinationMarker != null){  // check if destination is not null then add again the marker
                mMap.addMarker(destinationMarker);
            }

            LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
            originMarker = new MarkerOptions().position(latLng)
                    .title(stationName)
                    .icon((BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            mMap.addMarker(originMarker);
            input_origin.setText(stationName);

        } else {  //destination
            mMap.clear();

            if(originMarker != null){  // check if origin is not null then add again the marker
                mMap.addMarker(originMarker);
            }

            LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
            destinationMarker = new MarkerOptions().position(latLng)
                    .title(stationName)
                    .icon((BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            mMap.addMarker(destinationMarker);
            input_destination.setText(stationName);
        }

        if(destinationMarker != null){
            zoomStationsCamera(originMarker,destinationMarker);
        }else {
            zoomStationsCamera(originMarker,originMarker);
        }

    }
}
