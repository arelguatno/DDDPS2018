package com.example.arel.myapplication.screens;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.example.arel.myapplication.BaseActivity;
import com.example.arel.myapplication.R;

public class RideNowActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_ride);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

    }
}
