package com.example.arel.myapplication;

/**
 * Created by Arel on 9/2/2018.
 */

public class Constants {

    // Firestore collections
    public static final String USERS_STR = "users";
    public static final String ACCOUNT_PROFILE_STR = "account_profile";
    public static final String ACCOUNT_HISTORY_STR = "account_history";

    public enum AccountHistoryType {
        RIDE, LOAD, WELCOME
    }

    public static final String DATE_FORMAT = "MMM dd, yyyy";
}
