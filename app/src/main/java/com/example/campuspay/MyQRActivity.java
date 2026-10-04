package com.example.campuspay;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Student screen: shows the student's personal QR
 * (CAMPUSPAY_STUDENT:<uid>) for the event organizer to scan.
 */
public class MyQRActivity extends AppCompatActivity {

    private static final int QR_SIZE = 600;

    private ImageView ivMyQR;
    private TextView tvMyQrName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_qr);

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarMyQr);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivMyQR = findViewById(R.id.ivMyQR);
        tvMyQrName = findViewById(R.id.tvMyQrName);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please log in again",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        generateQRCode(QrPayload.STUDENT_PREFIX + user.getUid());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null) {
                        tvMyQrName.setText(name);
                    }
                });
    }

    private void generateQRCode(String data) {

        try {

            BitMatrix bitMatrix = new QRCodeWriter().encode(
                    data,
                    BarcodeFormat.QR_CODE,
                    QR_SIZE,
                    QR_SIZE
            );

            int[] pixels = new int[QR_SIZE * QR_SIZE];

            for (int y = 0; y < QR_SIZE; y++) {
                for (int x = 0; x < QR_SIZE; x++) {
                    pixels[y * QR_SIZE + x] =
                            bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(
                    pixels,
                    QR_SIZE,
                    QR_SIZE,
                    Bitmap.Config.RGB_565
            );

            ivMyQR.setImageBitmap(bitmap);

        } catch (WriterException e) {

            Toast.makeText(
                    this,
                    "Failed to generate QR",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}