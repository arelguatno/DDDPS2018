package com.example.arel.myapplication.screens;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.arel.myapplication.BaseActivity;
import com.example.arel.myapplication.R;
import com.example.arel.myapplication.fragments.HomeFragment;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;


import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawer;
    private FirebaseAuth mAuth;
    private static final int REQUEST_INVITE = 0;

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }

        View header = navigationView.getHeaderView(0);
        TextView accountName =  header.findViewById(R.id.account_name);
        TextView accountEmail = header.findViewById(R.id.account_email);
        ImageView account_img =  header.findViewById(R.id.account_img);


        if (user != null) {
            accountName.setText(user.getDisplayName());
            accountEmail.setText(user.getEmail());

            getRegistrationToken(user);

            Glide.with(this).load(user.getPhotoUrl()).apply(RequestOptions.circleCropTransform()).into(account_img);

        }else{
            navigateToLogInScreen();
        }


//        saveGeoPointsToFirestore();  // One time only
//        addAccountHistoryData(); //Dummy data
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void addAccountHistoryData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> one = new HashMap<>();
        one.put("amount", 25);
        one.put("date", Instant.now().toEpochMilli());
        one.put("Description","Train ride");
        one.put("ride_destination","Ayala");
        one.put("ride_from","Cubao");
        one.put("type","Ride");
        db.collection("account_history").document(user.getUid()).collection("data").document()
                .set(one);
    }


    /**
     * Get the registration token of the device and save it in Firestore.
     * This is needed so we can send push notification to the user using Cloud Functions.
     * We will only get this when the user is already logged in.
     * @param user FirebaseUser object of the logged in user
     */
    private void getRegistrationToken(final FirebaseUser user){
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String token = instanceIdResult.getToken();
                Log.v("token", "token " + token);
                Map<String, Object> userToken = new HashMap<>();
                userToken.put("token", token);
                db.collection("users").document(user.getUid()).update(userToken).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.v("token", "token added to user profile");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("error", "error in updating registration token " + e.getMessage());
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("error", "failed to get registration token! " + e.getMessage());

            }
        });
    }


    private void saveGeoPointsToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> one = new HashMap<>();
        one.put("name", "North Avenue");
        one.put("index", 1);
        one.put("latlng", new GeoPoint(14.65217,121.032333));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(one);

        Map<String, Object> two = new HashMap<>();
        two.put("name", "Quezon Avenue");
        two.put("index", 2);
        two.put("latlng", new GeoPoint(14.642753700000002,121.0386944));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(two);

        Map<String, Object> three = new HashMap<>();
        three.put("name", "GMA Kamuning");
        three.put("index", 3);
        three.put("latlng", new GeoPoint(14.6352168,121.0433426));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(three);


        Map<String, Object> four = new HashMap<>();
        four.put("name", "Cubao");
        four.put("index", 4);
        four.put("latlng", new GeoPoint(14.6192501,121.0512024));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(four);


        Map<String, Object> five = new HashMap<>();
        five.put("name", "Santolan - Anapolis");
        five.put("index", 5);
        five.put("latlng", new GeoPoint(14.607330999999997,121.05642999999998));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(five);


        Map<String, Object> six = new HashMap<>();
        six.put("name", "Ortigas");
        six.put("index", 6);
        six.put("latlng", new GeoPoint(14.5878485,121.0567112));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(six);


        Map<String, Object> seven = new HashMap<>();
        seven.put("name", "Shaw Boulevard");
        seven.put("index", 7);
        seven.put("latlng", new GeoPoint(14.581178300000001,121.0536881));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(seven);


        Map<String, Object> eight = new HashMap<>();
        eight.put("name", "Boni Avenue");
        eight.put("index", 8);
        eight.put("latlng", new GeoPoint(14.5737139,121.04815389999999));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(eight);



        Map<String, Object> nine = new HashMap<>();
        nine.put("name", "Guadalupe");
        nine.put("index", 9);
        nine.put("latlng", new GeoPoint(14.5666455,121.0454599));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(nine);


        Map<String, Object> ten = new HashMap<>();
        ten.put("name", "Buendia");
        ten.put("index", 10);
        ten.put("latlng", new GeoPoint(14.554624,121.0345156));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(ten);


        Map<String, Object> eleven = new HashMap<>();
        eleven.put("name", "Ayala");
        eleven.put("index", 11);
        eleven.put("latlng", new GeoPoint(14.549190599999998,121.0279696));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(eleven);


        Map<String, Object> twelve = new HashMap<>();
        twelve.put("name", "Magallanes");
        twelve.put("index", 12);
        twelve.put("latlng", new GeoPoint(14.541993999999999,121.0194226));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(twelve);


        Map<String, Object> thirteen = new HashMap<>();
        thirteen.put("name", "Taft Avenue");
        thirteen.put("index", 13);
        thirteen.put("latlng", new GeoPoint(14.537669,121.00217470000001));
        db.collection("train_stations").document("mrt_line").collection("stations").document()
                .set(thirteen);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if (user != null) {
            Log.d("arel", "with user");
        }else{
            navigateToLogInScreen();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
        } else if (id == R.id.nav_sign_out) {
            FirebaseAuth.getInstance().signOut();
            mGoogleSignInClient.signOut();
            navigateToLogInScreen();
        } else if (id == R.id.nav_ride) {
            Intent intent = new Intent(this, RideNowActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_share){
            sendInvite();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void sendInvite(){
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    private void navigateToLogInScreen(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
