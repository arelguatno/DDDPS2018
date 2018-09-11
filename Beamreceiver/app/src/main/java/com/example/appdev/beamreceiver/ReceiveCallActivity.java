package com.example.appdev.beamreceiver;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static android.nfc.NdefRecord.createMime;

public class ReceiveCallActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback   {

    private File mParentPath;
    // Incoming Intent
    private Intent mIntent;
    private static String TAG = "GateConfirmationActivity";

    NfcAdapter mNfcAdapter;
    TextView textView;
    String uid = "";
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        TextView textView = (TextView) findViewById(R.id.textView);
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Register callback
        mNfcAdapter.setNdefPushMessageCallback(this, this);

    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        String text = ("Beam me up, Android!\n\n" +
                "Beam Time: " + System.currentTimeMillis());
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] { createMime(
                        "application/vnd.com.example.android.beam", text.getBytes())
                        /**
                         * The Android Application Record (AAR) is commented out. When a device
                         * receives a push with an AAR in it, the application specified in the AAR
                         * is guaranteed to run. The AAR overrides the tag dispatch system.
                         * You can add it back in to guarantee that this
                         * activity starts when receiving a beamed message. For now, this code
                         * uses the tag dispatch system.
                        */
//                        ,NdefRecord.createApplicationRecord("com.example.appdev.beamreceiver")
                });
        return msg;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }



    void processIntent(Intent intent) {
        textView = (TextView) findViewById(R.id.textView);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
//        textView.setText(new String(msg.getRecords()[0].getPayload()));
        uid = new String(msg.getRecords()[0].getPayload());


        String[] str = uid.split(":");

        textView.setText(str[0].toString() + "\n" + // uid
                str[1].toString() + "\n" +  // doc ID
                str[2].toString() + "\n" +// origin station
                str[3].toString() + "\n" + //amount
                str[4].toString()); // entry or exit

        saveEntryPointToFirestore(str[0].toString(),  // uid
                str[1].toString(), // doc ID
                str[2].toString(), // origin station
                str[3].toString() , //amount
                Boolean.valueOf(str[4])); // entry or exit

    }

    private void saveEntryPointToFirestore(String user_uid, String docID, String origin, final String amount, boolean entry_exit) {
        Map<String, Object> city = new HashMap<>();
        if(entry_exit){
            city.put("amount", Integer.valueOf(amount));
            city.put("date", System.currentTimeMillis());
            city.put("description", "Ride");
            city.put("ride_destination", "-");
            city.put("ride_from", origin);
            city.put("type", "INTRANSIT");
            city.put("allow_entry", true); // Gate is clear

            db.collection("account_history").document(user_uid).collection("data").document(docID)
                    .set(city)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing document", e);
                        }
                    });
        }else{
            city.put("ride_destination", origin);
            city.put("type", "RIDE");
            city.put("allow_entry", true); // Gate is clear

            db.collection("account_history").document(user_uid).collection("data").document(docID)
                    .update(city)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing document", e);
                        }
                    });



            // query balance

            final DocumentReference sfDocRef = db.collection("account_profile").document(user_uid);

            db.runTransaction(new Transaction.Function<Void>() {
                @Override
                public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                    DocumentSnapshot snapshot = transaction.get(sfDocRef);
                    long deductedAmount = snapshot.getLong("balance") - Long.valueOf(amount);

                    if(deductedAmount > 0) {
                        transaction.update(sfDocRef, "balance", deductedAmount);
                    } else {
                        throw new FirebaseFirestoreException("Passenger doesn't have the balance to pass this gate",
                                FirebaseFirestoreException.Code.ABORTED);

                    }

                    // Success
                    return null;
                }
            }).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Transaction success!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Transaction failure.", e);
                }
            });
        }


    }

}
