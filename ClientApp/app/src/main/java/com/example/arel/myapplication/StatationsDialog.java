package com.example.arel.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arel.myapplication.models.TrainStationsModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.webianks.library.scroll_choice.ScrollChoice;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aguatno on 9/4/18.
 */

public class StatationsDialog extends AppCompatDialogFragment {
    private StationsDialogListener statationsDialog;
    List<String> datas = new ArrayList<>();

    ScrollChoice scrollChoice;
    int selectedStation = 0;
    String title;
    List<TrainStationsModel> trains = new ArrayList<>();
    boolean isDestination = false;

    private static final String TAG = "StatationsDialog";
    static int originSelected, destinationSelected = 0;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        isDestination = getArguments().getBoolean("isDestination");
        if (!isDestination) { // Get previous
            selectedStation = originSelected;
        } else {
            selectedStation = destinationSelected;
        }
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_select_stations_dialog, null);

        builder.setView(view)
                .setTitle(isDestination == false ? "Select origin" : "Select destination")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("Select", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Set station
                        if (!isDestination) {
                            originSelected = selectedStation;
                        } else {
                            destinationSelected = selectedStation;
                        }
                        statationsDialog.dialogReturnGeoPoint(trains.get(selectedStation).getGeoPoint(), isDestination,
                                trains.get(selectedStation).getName());

                    }
                });

        scrollChoice = view.findViewById(R.id.scroll_choice);
        loadDatas();

        scrollChoice.setOnItemSelectedListener(new ScrollChoice.OnItemSelectedListener() {
            @Override
            public void onItemSelected(ScrollChoice scrollChoice, int position, String name) {
                selectedStation = position;
            }
        });


        return builder.create();
    }

    private void loadDatas() {
        // Get All stations
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("train_stations").document("mrt_line").collection("stations").orderBy("index")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        trains.clear();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            TrainStationsModel trainStationsModel = new TrainStationsModel((GeoPoint) doc.get("latlng"),
                                    doc.getLong("index"),
                                    doc.getString("name")
                            );
                            trains.add(trainStationsModel);
                            datas.add(doc.getString("name"));
                            /*
                            * Possible bug on getGeoPoint =  doc.getGeoPoint("latlng");
                            * Thrown - Attempt to invoke virtual method 'double com.google.firebase.firestore.GeoPoint.getLongitude()' on a null object reference
                            *
                            * I found workaround to cast the doc.get to Geopoint
                            * (GeoPoint) doc.get("latlng");
                            * geoPoint().getLatitude;
                            * */
                        }
                        scrollChoice.addItems(datas, selectedStation);
                    }
                });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            statationsDialog = (StationsDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "must implement StationsDialogListener");
        }
    }

    public interface StationsDialogListener {
        void dialogReturnGeoPoint(GeoPoint geoPoint, boolean isDestination, String stationName);
    }
}
