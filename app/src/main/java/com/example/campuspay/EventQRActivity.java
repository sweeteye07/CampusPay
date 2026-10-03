package com.example.campuspay;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class EventQRActivity extends AppCompatActivity {

    private ImageView ivEventQR;
    private com.google.android.material.appbar.MaterialToolbar toolbarEventQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_qr);

        ivEventQR = findViewById(R.id.ivEventQR);
        toolbarEventQr = findViewById(R.id.toolbarEventQr);

        toolbarEventQr.setNavigationOnClickListener(v -> finish());

        String eventId = getIntent().getStringExtra("eventId");
        String eventTitle = getIntent().getStringExtra("eventTitle");

        if (eventId == null) {
            Toast.makeText(
                    this,
                    "Event information missing",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        toolbarEventQr.setTitle(eventTitle);

        String qrData = "CAMPUSPAY_EVENT:" + eventId;

        generateQRCode(qrData);
    }

    private void generateQRCode(String data) {

        QRCodeWriter writer = new QRCodeWriter();

        try {

            BitMatrix bitMatrix =
                    writer.encode(
                            data,
                            BarcodeFormat.QR_CODE,
                            600,
                            600
                    );

            Bitmap bitmap = Bitmap.createBitmap(
                    600,
                    600,
                    Bitmap.Config.RGB_565
            );

            for (int x = 0; x < 600; x++) {
                for (int y = 0; y < 600; y++) {

                    bitmap.setPixel(
                            x,
                            y,
                            bitMatrix.get(x, y)
                                    ? android.graphics.Color.BLACK
                                    : android.graphics.Color.WHITE
                    );
                }
            }

            ivEventQR.setImageBitmap(bitmap);

        } catch (WriterException e) {

            Toast.makeText(
                    this,
                    "Failed to generate QR",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}