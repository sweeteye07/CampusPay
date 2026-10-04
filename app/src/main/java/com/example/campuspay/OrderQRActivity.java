package com.example.campuspay;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Student screen: shows the pickup QR (CAMPUSPAY_PICKUP:&lt;redemptionId&gt;)
 * for one pending order, for the organizer to scan at handover.
 */
public class OrderQRActivity extends AppCompatActivity {

    public static final String EXTRA_REDEMPTION_ID = "redemptionId";

    private static final int QR_SIZE = 600;

    private ImageView ivOrderQR;
    private TextView tvOrderQrTitle;
    private TextView tvOrderQrStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_qr);

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarOrderQr);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivOrderQR = findViewById(R.id.ivOrderQR);
        tvOrderQrTitle = findViewById(R.id.tvOrderQrTitle);
        tvOrderQrStatus = findViewById(R.id.tvOrderQrStatus);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please log in again",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        String redemptionId = getIntent().getStringExtra(EXTRA_REDEMPTION_ID);

        if (redemptionId == null || redemptionId.isEmpty()) {
            Toast.makeText(this, "Order not found",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String ownerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("redemptions")
                .document(redemptionId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()
                            || !ownerId.equals(doc.getString("userId"))) {
                        Toast.makeText(this, "Order not found",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Redemption order = Redemption.fromDocument(doc);

                    if (!order.isPending()) {
                        tvOrderQrStatus.setText("Already collected");
                        Toast.makeText(this, "Order already collected",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String title = order.getItemTitle() != null
                            ? order.getItemTitle() : "Reward";
                    tvOrderQrTitle.setText(title);
                    tvOrderQrStatus.setText("Show this to the organizer");

                    generateQRCode(order.pickupPayload());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Could not load order: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
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

            ivOrderQR.setImageBitmap(bitmap);

        } catch (WriterException e) {

            Toast.makeText(
                    this,
                    "Failed to generate QR",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
