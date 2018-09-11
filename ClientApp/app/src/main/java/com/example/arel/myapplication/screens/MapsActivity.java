package com.example.arel.myapplication.screens;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arel.myapplication.DataParser;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.maps.android.SphericalUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, StatationsDialog.StationsDialogListener {

    private GoogleMap mMap;
    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final int SECOND_ACTIVITY_REQUEST_CODE = 0;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private static final float DEFAULT_ZOOM = 15f;
    private static final int DEFAULT_CAMERA_SPEED = 3000;

    private static final String TAG = "MapsActivity";
    MarkerOptions originMarker;
    MarkerOptions destinationMarker;
    private ImageView ic_gps, ic_location;
    SupportMapFragment mapFragment;

    TextView input_origin, input_destination, fare_amout_textView,fare_textView,distance_textView;
    private LocationManager locationManager;
    boolean notification_notified = false;
    boolean gps_zoom = false;


    private static final int DEFAULT_NOTIF_DISTANCE = 2500;


    Button confirmButton;

    LatLng myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ic_gps = findViewById(R.id.ic_gps);
        ic_location = findViewById(R.id.ic_location);

        input_origin = findViewById(R.id.input_origin);
        input_destination = findViewById(R.id.input_destination);
        fare_amout_textView = findViewById(R.id.fare_amout_textView);

        fare_textView = findViewById(R.id.fare_textView);
        distance_textView = findViewById(R.id.distance_textView);

        confirmButton = findViewById(R.id.confirmButton);

        ic_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (destinationMarker == null && originMarker == null) {
                    return;
                }

                if (destinationMarker != null) {
                    gps_zoom = true;
                    zoomStationsCamera(originMarker, destinationMarker);
                } else {
                    gps_zoom = false;
                    zoomStationsCamera(originMarker, originMarker);
                }
            }
        });

        ic_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gps_zoom = false;
                getDeviceLocation();
            }
        });
        getLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    public void confirmButton (View v){
        //getDeviceLocation();

        // Open a custom dialog box

        // Check balance here


        input_origin.setClickable(false);
        input_destination.setClickable(false);
//        getDeviceLocation(); // TODO remove this line

        Intent intent = new Intent(this, GateConfirmationActivity.class);
        intent.putExtra("origin_marker", input_origin.getText().toString());
        intent.putExtra("amount_fare", 25);
        intent.putExtra("entry", true);
        notification_notified = false;
        startActivityForResult(intent, SECOND_ACTIVITY_REQUEST_CODE);
    }

    // This method is called when the second activity finishes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check that it is the SecondActivity with an OK result
        if (requestCode == SECOND_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) { // Activity.RESULT_OK

                // get String data from Intent
                if(data.getBooleanExtra("allow_entry",false)){
                    // you can enter
                    getDeviceLocation(); // start
                } if(data.getBooleanExtra("allow_exit",false)){
                    // you can enter
                    finish();
                }
            }
        }
    }

    private String formatNumber(double num){
        return String.format("%,.0f", num);
    }

    LocationListener locationListenerGPS=new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            double latitude=location.getLatitude();
            double longitude=location.getLongitude();

            if(!gps_zoom){
                LatLng latLng = new LatLng(latitude,longitude);
                moveCameraLocationCamera(latLng, DEFAULT_ZOOM);
            }

            LatLng latLng1 = new LatLng(latitude,longitude);
            int distance = (int) SphericalUtil.computeDistanceBetween(latLng1, destinationMarker.getPosition());
            distance_textView.setText(distance + "m");

            if(!notification_notified && distance < DEFAULT_NOTIF_DISTANCE) {
                notification_notified = true;
                sendNotification();
            }

            if (distance < 40){
                // You arrived
                Intent intent = new Intent(getApplicationContext(), GateConfirmationActivity.class);
                intent.putExtra("origin_marker", input_destination.getText().toString());
                intent.putExtra("amount_fare", 25);
                intent.putExtra("entry", false);
                startActivityForResult(intent, SECOND_ACTIVITY_REQUEST_CODE);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private Task<String> sendNotification() {
        FirebaseFunctions functions = FirebaseFunctions.getInstance(getString(R.string.asia_northeas1_string));
        // Create the arguments to the callable function.
        String registrationToken = FirebaseInstanceId.getInstance().getToken();
        Map<String, Object> data = new HashMap<>();
        data.put("registrationToken", registrationToken);

        return functions
                .getHttpsCallable("sendNewPushNotification")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        String result = (String) task.getResult().getData();
                        return result;
                    }
                });
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
        myLocation = new LatLng(latLng.latitude, latLng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), DEFAULT_CAMERA_SPEED, null);
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

        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;

                locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        2000,
                        10, locationListenerGPS);

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
            mMap.getUiSettings().setMyLocationButtonEnabled(false);


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

            int totalDistance = (int) SphericalUtil.computeDistanceBetween(originMarker.getPosition(), destinationMarker.getPosition());
            distance_textView.setText(formatNumber(totalDistance) + "m");
        }

        if(destinationMarker != null){
            // update fare amount
            fare_amout_textView.setText("25.00");
            zoomStationsCamera(originMarker,destinationMarker);

            String url = getUrl(originMarker.getPosition(), destinationMarker.getPosition());
            Log.d("onMapClick", url.toString());
            FetchUrl FetchUrl = new FetchUrl();

            // Start downloading json data from Google Directions API
            FetchUrl.execute(url);

            confirmButton.setEnabled(true);
            ic_gps.setVisibility(View.VISIBLE);
        }else {
            zoomStationsCamera(originMarker,originMarker);
        }

    }

    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // API key
        String api_key = "key=" + getResources().getString(R.string.google_maps_key);


        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + api_key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }

        private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

            // Parsing the data in non-ui thread
            @Override
            protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

                JSONObject jObject;
                List<List<HashMap<String, String>>> routes = null;

                try {
                    jObject = new JSONObject(jsonData[0]);
                    Log.d("ParserTask", jsonData[0].toString());
                    DataParser parser = new DataParser();
                    Log.d("ParserTask", parser.toString());

                    // Starts parsing data
                    routes = parser.parse(jObject);
                    Log.d("ParserTask", "Executing routes");
                    Log.d("ParserTask", routes.toString());

                } catch (Exception e) {
                    Log.d("ParserTask", e.toString());
                    e.printStackTrace();
                }
                return routes;
            }

            // Executes in UI thread, after the parsing process
            @Override
            protected void onPostExecute(List<List<HashMap<String, String>>> result) {
                ArrayList<LatLng> points;
                PolylineOptions lineOptions = null;

                // Traversing through all the routes
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList<>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    List<HashMap<String, String>> path = result.get(i);

                    // Fetching all the points in i-th route
                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(10);
                    lineOptions.color(Color.RED);

                    Log.d("onPostExecute", "onPostExecute lineoptions decoded");

                }

                // Drawing polyline in the Google Map for the i-th route
                if (lineOptions != null) {
                    mMap.addPolyline(lineOptions);
                } else {
                    Log.d("onPostExecute", "without Polylines drawn");
                }
            }
        }
    }

}
