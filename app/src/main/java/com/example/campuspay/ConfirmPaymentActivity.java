package com.example.campuspay;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Second step of sending credits: reviews recipient, amount and the
 * balance impact, then executes the transfer on explicit confirm.
 * Started by SendMoneyActivity after the recipient lookup.
 */
public class ConfirmPaymentActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPIENT_ID = "recipientId";
    public static final String EXTRA_RECIPIENT_EMAIL = "recipientEmail";
    public static final String EXTRA_RECIPIENT_NAME = "recipientName";
    public static final String EXTRA_AMOUNT = "amount";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private MaterialButton btnConfirm;

    private String senderId;
    private String recipientId;
    private String recipientEmail;
    private long amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_payment);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarConfirmPayment);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        senderId = mAuth.getCurrentUser().getUid();
        recipientId = getIntent().getStringExtra(EXTRA_RECIPIENT_ID);
        recipientEmail = getIntent().getStringExtra(EXTRA_RECIPIENT_EMAIL);
        String recipientName = getIntent().getStringExtra(EXTRA_RECIPIENT_NAME);
        amount = getIntent().getLongExtra(EXTRA_AMOUNT, -1);

        if (recipientId == null || recipientId.isEmpty() || amount <= 0) {
            Toast.makeText(this, "Invalid payment details",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Vendors have no wallet actions.
        RoleGate.fetchRole(db, senderId, role -> {
            if (UserRole.isVendor(role)) {
                Toast.makeText(this, "Sending is not available for vendors",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });

        ((TextView) findViewById(R.id.tvConfirmRecipientName)).setText(
                recipientName != null && !recipientName.isEmpty()
                        ? recipientName : "Student");
        ((TextView) findViewById(R.id.tvConfirmRecipientEmail)).setText(
                recipientEmail != null ? recipientEmail : "");
        ((TextView) findViewById(R.id.tvConfirmAmount)).setText(
                amount + " Credits");

        btnConfirm = findViewById(R.id.btnConfirmPayment);
        btnConfirm.setEnabled(false);

        findViewById(R.id.btnCancelPayment).setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> runTransferTransaction());

        loadBalance();
    }

    private void loadBalance() {
        db.collection("users")
                .document(senderId)
                .get()
                .addOnSuccessListener(doc -> {
                    Long balance = doc.getLong("credits");
                    long current = balance != null ? balance : 0L;

                    ((TextView) findViewById(R.id.tvConfirmBalance))
                            .setText(current + " Credits");

                    TextView tvRemaining =
                            findViewById(R.id.tvConfirmRemaining);
                    long remaining = current - amount;
                    tvRemaining.setText(remaining + " Credits");
                    if (remaining < 0) {
                        tvRemaining.setTextColor(0xFFC62828);
                        Toast.makeText(this,
                                "Insufficient balance. You have "
                                        + current + " credits.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        tvRemaining.setTextColor(0xFF2E7D32);
                        btnConfirm.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Could not load balance: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void runTransferTransaction() {
        com.google.firebase.firestore.DocumentReference senderRef =
                db.collection("users").document(senderId);
        com.google.firebase.firestore.DocumentReference recipientRef =
                db.collection("users").document(recipientId);

        btnConfirm.setEnabled(false);

        db.runTransaction(transaction -> {
                    DocumentSnapshot senderSnapshot =
                            transaction.get(senderRef);
                    DocumentSnapshot recipientSnapshot =
                            transaction.get(recipientRef);

                    if (!senderSnapshot.exists()
                            || !recipientSnapshot.exists()) {
                        throw new com.google.firebase.firestore.FirebaseFirestoreException(
                                "Student profile not found",
                                com.google.firebase.firestore.FirebaseFirestoreException
                                        .Code.NOT_FOUND
                        );
                    }

                    if (senderId.equals(recipientId)) {
                        throw new com.google.firebase.firestore.FirebaseFirestoreException(
                                "You cannot send credits to yourself",
                                com.google.firebase.firestore.FirebaseFirestoreException
                                        .Code.FAILED_PRECONDITION
                        );
                    }

                    Long senderBalance = senderSnapshot.getLong("credits");
                    long balance = senderBalance != null ? senderBalance : 0L;

                    if (balance < amount) {
                        throw new com.google.firebase.firestore.FirebaseFirestoreException(
                                "Insufficient balance. You have "
                                        + balance + " credits.",
                                com.google.firebase.firestore.FirebaseFirestoreException
                                        .Code.FAILED_PRECONDITION
                        );
                    }

                    String senderEmail =
                            mAuth.getCurrentUser().getEmail() != null
                                    ? mAuth.getCurrentUser().getEmail()
                                            .toLowerCase()
                                    : "student";

                    transaction.update(senderRef, "credits",
                            com.google.firebase.firestore.FieldValue
                                    .increment(-amount));
                    transaction.update(recipientRef, "credits",
                            com.google.firebase.firestore.FieldValue
                                    .increment(amount));

                    com.google.firebase.firestore.DocumentReference outRef =
                            db.collection("transactions").document();
                    java.util.Map<String, Object> outLedger =
                            new java.util.HashMap<>();
                    outLedger.put("type", "transfer");
                    outLedger.put("direction", "out");
                    outLedger.put("userId", senderId);
                    outLedger.put("counterpartyEmail", recipientEmail);
                    outLedger.put("credits", amount);
                    outLedger.put("description",
                            "Sent " + amount + " credits to "
                                    + recipientEmail);
                    outLedger.put("createdAt",
                            com.google.firebase.firestore.FieldValue
                                    .serverTimestamp());
                    transaction.set(outRef, outLedger);

                    com.google.firebase.firestore.DocumentReference inRef =
                            db.collection("transactions").document();
                    java.util.Map<String, Object> inLedger =
                            new java.util.HashMap<>();
                    inLedger.put("type", "transfer");
                    inLedger.put("direction", "in");
                    inLedger.put("userId", recipientId);
                    inLedger.put("counterpartyEmail", senderEmail);
                    inLedger.put("credits", amount);
                    inLedger.put("description",
                            "Received " + amount + " credits from "
                                    + senderEmail);
                    inLedger.put("createdAt",
                            com.google.firebase.firestore.FieldValue
                                    .serverTimestamp());
                    transaction.set(inRef, inLedger);

                    return outRef.getId();
                })
                .addOnSuccessListener(txId -> showReceipt(txId))
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    Toast.makeText(
                            this,
                            "Transfer failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    /** Fresh balance read, then the receipt screen. */
    private void showReceipt(String txId) {
        db.collection("users")
                .document(senderId)
                .get()
                .addOnSuccessListener(doc -> {
                    Long balance = doc.getLong("credits");
                    launchReceipt(txId, balance != null ? balance : -1L);
                })
                .addOnFailureListener(e -> launchReceipt(txId, -1L));
    }

    private void launchReceipt(String txId, long newBalance) {
        android.content.Intent intent = new android.content.Intent(
                this, PaymentSuccessActivity.class);
        intent.putExtra(PaymentSuccessActivity.EXTRA_AMOUNT, amount);
        intent.putExtra(PaymentSuccessActivity.EXTRA_RECIPIENT_NAME,
                getIntent().getStringExtra(EXTRA_RECIPIENT_NAME));
        intent.putExtra(PaymentSuccessActivity.EXTRA_RECIPIENT_EMAIL,
                recipientEmail);
        intent.putExtra(PaymentSuccessActivity.EXTRA_TX_ID, txId);
        intent.putExtra(PaymentSuccessActivity.EXTRA_TIMESTAMP,
                System.currentTimeMillis());
        intent.putExtra(PaymentSuccessActivity.EXTRA_NEW_BALANCE,
                newBalance);
        startActivity(intent);
        setResult(RESULT_OK);
        finish();
    }
}
