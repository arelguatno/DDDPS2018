package com.example.arel.myapplication.adapters;

/**
 * Created by aguatno on 9/2/18.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.arel.myapplication.Constants;
import com.example.arel.myapplication.R;
import com.example.arel.myapplication.models.AccountHistoryModel;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AccountHistoryAdapter extends RecyclerView.Adapter<AccountHistoryAdapter.MyViewHolder> {

    private List<AccountHistoryModel> accountHistoryList;
    private Context context;


    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView  one_textView, two_textView, three_textView,four_textView;
        public ImageView icon1, icon2;

        public MyViewHolder(View view) {
            super(view);

            one_textView = view.findViewById(R.id.one_textView);
            two_textView = view.findViewById(R.id.two_textView);
            three_textView = view.findViewById(R.id.three_textView);
            four_textView = view.findViewById(R.id.four_textView);

            icon1 = view.findViewById(R.id.icon1);
            icon2 = view.findViewById(R.id.icon2);

        }
    }


    public AccountHistoryAdapter(List<AccountHistoryModel> moviesList, Context context) {
        this.accountHistoryList = moviesList;
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.account_history_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        AccountHistoryModel accountHistoryModel = accountHistoryList.get(position);
        String type = accountHistoryModel.getType().toUpperCase();


        // Load
        if (type.equalsIgnoreCase(Constants.AccountHistoryType.LOAD.toString())) {

//            holder.one_textView.setText(accountHistoryModel.getLoad_source());
            holder.one_textView.setText(context.getString(R.string.received_money_from_string));
            holder.three_textView.setText(accountHistoryModel.getLoad_location());

            try {
                holder.two_textView.setText(parseDate(accountHistoryModel.getLoad_date()));

            } catch (ParseException e) {
                Log.d("ERRRR", "There's an error with date" + e.getMessage());
            }

            holder.four_textView.setText(context.getString(R.string.php_amount,  formatNumber(accountHistoryModel.getAmount())));
            holder.four_textView.setTextColor(Color.BLUE);

            holder.icon1.setImageResource(R.mipmap.ic_top_up);
            holder.icon2.setImageResource(R.mipmap.ic_from_location);

        } else if (type.equalsIgnoreCase(Constants.AccountHistoryType.RIDE.toString())) {

            holder.one_textView.setText(accountHistoryModel.getRide_destination());
            holder.one_textView.setTextSize(16);
            holder.one_textView.setTextColor(Color.BLACK);

            holder.one_textView.setTypeface(Typeface.DEFAULT_BOLD);

            holder.three_textView.setText(accountHistoryModel.getRide_from());

            try {
                holder.two_textView.setText(parseDate(accountHistoryModel.getRide_date()));
            } catch (ParseException e) {
                Log.d("ERRRR", "There's an error with date" + e.getMessage());
            }
            holder.four_textView.setText(context.getString(R.string.php_amount,  formatNumber(accountHistoryModel.getAmount())));

            holder.icon1.setImageResource(R.mipmap.ic_from_location);
            holder.icon2.setImageResource(R.mipmap.ic_destination);

        } else if(type.equalsIgnoreCase(Constants.AccountHistoryType.WELCOME.toString())){

            holder.one_textView.setText(accountHistoryModel.getDescription());
            holder.one_textView.setTextSize(16);
            holder.one_textView.setTextColor(Color.BLACK);
            holder.one_textView.setTypeface(Typeface.DEFAULT_BOLD);

            try {
                holder.two_textView.setText(parseDate(accountHistoryModel.getLoad_date()));
            } catch (ParseException e) {
                Log.d("ERRRR", "There's an error with date" + e.getMessage());
            }

            holder.three_textView.setVisibility(View.GONE);
            holder.four_textView.setVisibility(View.GONE);

            holder.icon1.setImageResource(R.drawable.ic_train_black);
            holder.icon2.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return accountHistoryList.size();
    }

    private String parseDate(long epochSeconds) throws ParseException {
        Date updatedate = new Date(epochSeconds);
        SimpleDateFormat format = new SimpleDateFormat(Constants.DATE_FORMAT);

        return format.format(updatedate);
    }

    private String formatNumber(double num){
        return String.format("%,.2f", num);
    }
}