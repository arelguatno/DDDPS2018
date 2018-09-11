package com.example.arel.myapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import com.crashlytics.android.Crashlytics;
import com.example.arel.myapplication.Constants;
import com.example.arel.myapplication.R;
import com.example.arel.myapplication.adapters.AccountHistoryAdapter;
import com.example.arel.myapplication.models.AccountHistoryModel;
import com.example.arel.myapplication.screens.AddMoneyActivity;
import com.example.arel.myapplication.screens.MapsActivity;
import com.example.arel.myapplication.screens.RideNowActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "HomeFragment";
    FirebaseFirestore db = FirebaseFirestore.getInstance();


    Button btn_ride_now;
    Button btn_top_up;
    Button btn_scan;
    TextView balance_textView;
    private AdView mAdView;

    private List<AccountHistoryModel> accountHistoryModelList = new ArrayList<>();
    private RecyclerView recyclerView;
    private AccountHistoryAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        // Admob
        MobileAds.initialize(getActivity(), String.valueOf(R.string.ADMOB_APP_ID));
        mAdView = v.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        btn_ride_now = v.findViewById(R.id.btn_ride_now);
        btn_top_up = v.findViewById(R.id.btn_top_up);
        btn_scan = v.findViewById(R.id.btn_scan);
        balance_textView = v.findViewById(R.id.available_balance_amount_textView);

        btn_ride_now.setOnClickListener(this);
        btn_top_up.setOnClickListener(this);
        btn_scan.setOnClickListener(this);

        setAccountBalance();

        // RecyclerView
        recyclerView = v.findViewById(R.id.recycler_view);
        mAdapter = new AccountHistoryAdapter(accountHistoryModelList, getContext());
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        prepareAccountHistoryData();

        return v;
    }

    private void prepareAccountHistoryData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.ACCOUNT_HISTORY_STR).document(user.getUid()).collection("data").orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot snapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        accountHistoryModelList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String type = doc.getString("type").toUpperCase();

                            if(type.equalsIgnoreCase(Constants.AccountHistoryType.LOAD.toString())){

                                AccountHistoryModel accountHistoryModel = new AccountHistoryModel(doc.getString("source"),
                                        doc.getString("source_location"),
                                        doc.getLong("date"),
                                        doc.getLong("amount"),
                                        doc.getString("type"),
                                        doc.getString("description"));
                                accountHistoryModelList.add(accountHistoryModel);

                            } else if (type.equalsIgnoreCase(Constants.AccountHistoryType.RIDE.toString())){

                                AccountHistoryModel accountHistoryModel = new AccountHistoryModel(doc.getString("ride_destination"),
                                        doc.getString("ride_from"),
                                        doc.getLong("amount"),
                                        doc.getLong("date"),
                                        doc.getString("type"),
                                        doc.getString("description"),
                                        doc.getString("type"));
                                accountHistoryModelList.add(accountHistoryModel);

                            }else if(type.equalsIgnoreCase(Constants.AccountHistoryType.WELCOME.toString())) {
                                AccountHistoryModel accountHistoryModel = new AccountHistoryModel(doc.getString("source"),
                                        doc.getString("source_location"),
                                        doc.getLong("date"),
                                        doc.getLong("amount"),
                                        doc.getString("type"),
                                        doc.getString("description"));
                                accountHistoryModelList.add(accountHistoryModel);
                            } else{
                                // Other type
                            }

                        }
                        mAdapter.notifyDataSetChanged();

                    }
                });


    }

    private void setAccountBalance() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            final DocumentReference docRef = db.collection(Constants.ACCOUNT_PROFILE_STR).document(user.getUid());
            docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        balance_textView.setText("error");
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.d(TAG, "Current data: " + snapshot.getData());

                        balance_textView.setText(formatNumber(snapshot.getLong("balance")));

                    } else {
                        balance_textView.setText("null");
                        Crashlytics.log(Log.DEBUG, TAG, "Current data: null");
                    }
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ride_now:
                startActivity(new Intent(getActivity(), MapsActivity.class));
                break;
            case R.id.btn_top_up:
                startActivity(new Intent(getActivity(), AddMoneyActivity.class));
                break;
            case R.id.btn_scan:
                break;
            default:
                break;
        }

    }

    private String formatNumber(double num){
        return String.format("%,.2f", num);
    }
}
