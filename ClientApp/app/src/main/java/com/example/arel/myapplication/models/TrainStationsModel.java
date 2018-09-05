package com.example.arel.myapplication.models;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;

/**
 * Created by aguatno on 9/4/18.
 */

public class TrainStationsModel {
    GeoPoint geoPoint;
    Long index;
    String name;

    public TrainStationsModel(GeoPoint geoPoint, Long index, String name) {
        this.geoPoint = geoPoint;
        this.index = index;
        this.name = name;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public Long getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }
}


