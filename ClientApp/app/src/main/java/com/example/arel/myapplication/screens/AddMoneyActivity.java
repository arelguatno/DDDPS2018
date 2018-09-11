package com.example.arel.myapplication.screens;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.arel.myapplication.BaseActivity;
import com.example.arel.myapplication.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.HashMap;
import java.util.Map;

import static com.example.arel.myapplication.Constants.TOP_UP;

public class AddMoneyActivity extends BaseActivity {
    public final static int QRcodeWidth = 500;

    /*
    * Attention: QR Code format
    *    uid:amount
    * */

    LinearLayout qr_code_layout;
    private EditText amount_editText;
    Bitmap bitmap;
    private ImageView iv;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_money);

        qr_code_layout = findViewById(R.id.qr_code_layout);
        amount_editText = findViewById(R.id.amount_editText);
        btn = findViewById(R.id.button3);
        iv = findViewById(R.id.iv);

        amount_editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qr_code_layout.setVisibility(View.GONE);
            }
        });
    }

    public void btn_done(View v) {
        if (amount_editText.getText().toString().trim().length() == 0) {
            Toast.makeText(this, R.string.enter_amout_string, Toast.LENGTH_SHORT).show();
        } else {

            if (getCurrentFocus() == amount_editText) {
                // Close soft keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }

            amount_editText.clearFocus();
            qr_code_layout.setVisibility(View.INVISIBLE);
            btn.refreshDrawableState();

            showProgressDialog("Please wait..");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    generateTransactionId().addOnCompleteListener(new OnCompleteListener<String>() {
                        @Override
                        public void onComplete(@NonNull Task<String> task) {
                            String docID = task.getResult();

                            try {
                                String finalQRText = TOP_UP + ":" + user.getUid() + ":" + amount_editText.getText().toString() + ":" + docID;           // QR format
                                bitmap = TextToImageEncode(finalQRText);
                                iv.setImageBitmap(bitmap);
                                qr_code_layout.requestFocus();
                                qr_code_layout.setVisibility(View.VISIBLE);
                                hideProgressDialog();
                            } catch (WriterException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                }
            }, 500);


        }
    }

    private Task<String> generateTransactionId() {

        DocumentReference ref = db.collection("account_history").document();
        String generatedDocID = ref.getId();
        FirebaseFunctions functions = FirebaseFunctions.getInstance("asia-northeast1");

        String registrationToken = FirebaseInstanceId.getInstance().getToken();
        Map<String, Object> data = new HashMap<>();
        data.put("iid", generatedDocID);

        return functions
                .getHttpsCallable("generateTransactionId")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        String result = (String) task.getResult().getData();
                        return result;
                    }
                });
    }

    private Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.DATA_MATRIX.QR_CODE,
                    QRcodeWidth, QRcodeWidth, null
            );

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {

                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.black) : getResources().getColor(R.color.white);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }
}
