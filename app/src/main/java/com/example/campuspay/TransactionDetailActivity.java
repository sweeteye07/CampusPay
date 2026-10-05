package com.example.campuspay;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Full details for one ledger entry. Opened by tapping a row in
 * Credit History; everything is passed in extras, no extra read.
 */
public class TransactionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TX_ID = "txId";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_DIRECTION = "direction";
    public static final String EXTRA_CREDITS = "credits";
    public static final String EXTRA_DESCRIPTION = "description";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_COUNTERPARTY_NAME = "counterpartyName";
    public static final String EXTRA_COUNTERPARTY_EMAIL = "counterpartyEmail";
    public static final String EXTRA_TIMESTAMP = "timestamp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarTransactionDetail);
        toolbar.setNavigationOnClickListener(v -> finish());

        String type = getIntent().getStringExtra(EXTRA_TYPE);
        String direction = getIntent().getStringExtra(EXTRA_DIRECTION);
        long credits = getIntent().getLongExtra(EXTRA_CREDITS, 0);

        boolean outgoing = "transfer".equals(type) && "out".equals(direction)
                || "spend".equals(type);

        TextView tvAmount = findViewById(R.id.tvDetailAmount);
        tvAmount.setText((outgoing ? "-" : "+") + credits + " Credits");
        tvAmount.setTextColor(outgoing ? 0xFFC62828 : 0xFF2E7D32);

        ImageView ivIcon = findViewById(R.id.ivDetailIcon);
        if ("earn".equals(type)) {
            ivIcon.setImageResource(R.drawable.ic_event);
        } else if ("spend".equals(type)) {
            ivIcon.setImageResource(R.drawable.ic_wallet);
        } else if ("out".equals(direction)) {
            ivIcon.setImageResource(R.drawable.ic_logout);
        } else {
            ivIcon.setImageResource(R.drawable.ic_receive);
        }

        setRow(R.id.rowDetailType, R.id.tvDetailTypeValue,
                prettyType(type, direction));
        setRow(R.id.rowDetailTitle, R.id.tvDetailTitleValue,
                getIntent().getStringExtra(EXTRA_TITLE));

        String name = getIntent().getStringExtra(EXTRA_COUNTERPARTY_NAME);
        String email = getIntent().getStringExtra(EXTRA_COUNTERPARTY_EMAIL);
        if ("transfer".equals(type) && email != null && !email.isEmpty()) {
            ((TextView) findViewById(R.id.tvDetailCounterpartyLabel))
                    .setText("out".equals(direction) ? "To" : "From");
            ((TextView) findViewById(R.id.tvDetailCounterpartyValue))
                    .setText(name != null && !name.isEmpty() ? name : email);
            setRow(R.id.rowDetailEmail, R.id.tvDetailEmailValue, email);
        } else {
            findViewById(R.id.rowDetailCounterparty).setVisibility(View.GONE);
            findViewById(R.id.rowDetailEmail).setVisibility(View.GONE);
        }

        setRow(R.id.rowDetailDescription, R.id.tvDetailDescriptionValue,
                getIntent().getStringExtra(EXTRA_DESCRIPTION));

        long timestamp = getIntent().getLongExtra(EXTRA_TIMESTAMP, -1);
        setRow(R.id.rowDetailDate, R.id.tvDetailDateValue,
                timestamp > 0
                        ? new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                                Locale.getDefault()).format(
                                new java.util.Date(timestamp))
                        : null);

        setRow(R.id.rowDetailTxId, R.id.tvDetailTxIdValue,
                getIntent().getStringExtra(EXTRA_TX_ID));

        // Tint the header icon frame per direction.
        FrameLayout iconBg = findViewById(R.id.flDetailIconBg);
        iconBg.setBackgroundResource(outgoing
                ? R.drawable.bg_pill_danger
                : R.drawable.bg_pill_success);
    }

    private String prettyType(String type, String direction) {
        if ("earn".equals(type)) {
            return "Credits earned";
        }
        if ("spend".equals(type)) {
            return "Reward redeemed";
        }
        if ("transfer".equals(type)) {
            return "out".equals(direction) ? "Credits sent"
                    : "Credits received";
        }
        return "Credit transaction";
    }

    private void setRow(int rowId, int valueId, String value) {
        if (value == null || value.isEmpty()) {
            findViewById(rowId).setVisibility(View.GONE);
        } else {
            ((TextView) findViewById(valueId)).setText(value);
        }
    }
}
