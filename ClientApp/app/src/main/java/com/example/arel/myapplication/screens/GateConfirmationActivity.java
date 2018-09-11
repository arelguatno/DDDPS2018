package com.example.arel.myapplication.screens;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arel.myapplication.BaseActivity;
import com.example.arel.myapplication.Constants;
import com.example.arel.myapplication.R;
import com.example.arel.myapplication.models.AccountHistoryModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.nfc.NdefRecord.createMime;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class GateConfirmationActivity extends BaseActivity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {

    NfcAdapter mNfcAdapter;
    private Uri[] mFileUris = new Uri[10];

    private FileUriCallback mFileUriCallback;
    private static String TAG = "GateConfirmationActivity";
    private String originMarker;
    private static String generatedDocID;
    private int amountFare;

    private ProgressBar gate_progressBar;
    private ImageView gate_imageView;
    boolean entry_exit;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gate_confirmation);

        originMarker = getIntent().getExtras().getString("origin_marker", "NONE");
        amountFare = getIntent().getExtras().getInt("amount_fare", 0);
        entry_exit = getIntent().getExtras().getBoolean("entry", false);


        gate_progressBar = findViewById(R.id.gate_progressBar);
        gate_imageView = findViewById(R.id.gate_check_image);

        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Register callback
        mNfcAdapter.setNdefPushMessageCallback(this, this);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        /*
         * Instantiate a new FileUriCallback to handle requests for
         * URIs
         */
        mFileUriCallback = new FileUriCallback();
        // Set the dynamic callback for URI requests.
        mNfcAdapter.setBeamPushUrisCallback(mFileUriCallback, this);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {

        if(entry_exit){
            DocumentReference ref = db.collection("account_history").document();
            generatedDocID = ref.getId();
        }

        Log.d("aguatno", generatedDocID);
        String text = (user.getUid() +
                ":" + generatedDocID +
                ":" + originMarker +
                ":" + amountFare +
                ":" + entry_exit);


        NdefMessage msg = new NdefMessage(
                new NdefRecord[]{createMime(
                        "application/vnd.com.example.android.beam", text.getBytes())
                        /**
                         * The Android Application Record (AAR) is commented out. When a device
                         * receives a push with an AAR in it, the application specified in the AAR
                         * is guaranteed to run. The AAR overrides the tag dispatch system.
                         * You can add it back in to guarantee that this
                         * activity starts when receiving a beamed message. For now, this code
                         * uses the tag dispatch system.
                        */
                        , NdefRecord.createApplicationRecord("com.example.appdev.beamreceiver")
                });
        // Save data to firestore
        // Check if user can enter the train platforn
        validateIfUserCanEnterThePlatform();

        return msg;
    }

    private void validateIfUserCanEnterThePlatform() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.ACCOUNT_HISTORY_STR).document(user.getUid()).collection("data").document(generatedDocID)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            Log.d(TAG, "Current data: " + snapshot.getData());
                            Log.d(TAG, "Current data: " + snapshot.getBoolean("allow_entry"));
                            if(snapshot.getBoolean("allow_entry")){
                                // can enter
                                gate_progressBar.setVisibility(View.GONE);
                                gate_imageView.setVisibility(View.VISIBLE);

                                // Close and return the callback
                                Intent intent = new Intent();
                                intent.putExtra("allow_entry", true);

                                if(entry_exit){
                                    intent.putExtra("allow_entry", true);
                                    intent.putExtra("allow_exit", false);
                                }else{
                                    intent.putExtra("allow_entry", false);
                                    intent.putExtra("allow_exit", true);
                                }

                                setResult(RESULT_OK, intent);
                                finish();

                            }else{
                                // not allowed to enter
                            }
                        } else {
                            Log.d(TAG, "Current data: null");
                        }
                    }
                });
    }


    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
//        textView = (TextView) findViewById(R.id.textView);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
//        textView.setText(new String(msg.getRecords()[0].getPayload()));
    }

    @Override
    public void onNdefPushComplete(NfcEvent nfcEvent) {
        Log.d("arelguatno", "Yehey");
    }

    private class FileUriCallback implements
            NfcAdapter.CreateBeamUrisCallback {
        public FileUriCallback() {

        }

        /**
         * Create content URIs as needed to share with another device
         */
        @Override
        public Uri[] createBeamUris(NfcEvent event) {
            return mFileUris;
        }
    }

}
