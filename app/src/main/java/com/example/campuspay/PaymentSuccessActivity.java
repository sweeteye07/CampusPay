package com.example.campuspay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Receipt shown after a successful transfer: amount, recipient,
 * date, transaction ID and the sender's new balance.
 */
public class PaymentSuccessActivity extends AppCompatActivity {

    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_RECIPIENT_NAME = "recipientName";
    public static final String EXTRA_RECIPIENT_EMAIL = "recipientEmail";
    public static final String EXTRA_TX_ID = "txId";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_NEW_BALANCE = "newBalance";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        long amount = getIntent().getLongExtra(EXTRA_AMOUNT, -1);
        String name = getIntent().getStringExtra(EXTRA_RECIPIENT_NAME);
        String email = getIntent().getStringExtra(EXTRA_RECIPIENT_EMAIL);
        String txId = getIntent().getStringExtra(EXTRA_TX_ID);
        long timestamp = getIntent()
                .getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis());
        long newBalance = getIntent().getLongExtra(EXTRA_NEW_BALANCE, -1);

        if (amount <= 0) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvSuccessAmount))
                .setText(amount + " Credits");
        ((TextView) findViewById(R.id.tvSuccessRecipient))
                .setText(name != null && !name.isEmpty() ? name : "Student");
        ((TextView) findViewById(R.id.tvSuccessEmail))
                .setText(email != null ? email : "");
        ((TextView) findViewById(R.id.tvSuccessDate)).setText(
                new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                        Locale.getDefault()).format(
                        new java.util.Date(timestamp)));
        ((TextView) findViewById(R.id.tvSuccessTxId))
                .setText(txId != null ? txId : "—");
        ((TextView) findViewById(R.id.tvSuccessBalance)).setText(
                newBalance >= 0 ? newBalance + " Credits" : "—");

        findViewById(R.id.btnSuccessDone).setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnSuccessHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });
    }
}
