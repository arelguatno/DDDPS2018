package com.example.arel.myapplication.models;

/**
 * Created by aguatno on 9/2/18.
 */

public class AccountHistoryModel {
    // Type
    private String type, description;
    private long amount;

    // "type: Load" doc variables
    private String load_source, load_location;
    private long load_date;

    // "type: Ride" doc variables
    private String ride_destination, ride_from;
    private long ride_date;

    public AccountHistoryModel() {

    }

    // Load constructor
    public AccountHistoryModel(String source, String location_textView, long date, long amount, String type, String description) {
        this.load_source = source;
        this.load_location = location_textView;
        this.amount = amount;
        this.load_date = date;
        this.type = type;
        this.description = description;
    }

    // Ride constructor
    public AccountHistoryModel(String ride_destination, String ride_from, long ride_amount, long ride_date, String type, String description, String type2) {
        this.ride_destination = ride_destination;
        this.ride_from = ride_from;
        this.amount = ride_amount;
        this.ride_date = ride_date;
        this.type = type;
        this.description=description;
    }

    public String getDescription() {
        return description;
    }

    public String getLoad_source() {
        return load_source;
    }

    public String getLoad_location() {
        return load_location;
    }

    public long getLoad_date() {
        return load_date;
    }

    public long getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getRide_destination() {
        return ride_destination;
    }

    public String getRide_from() {
        return ride_from;
    }

    public long getRide_date() {
        return ride_date;
    }
}
