package com.example.campuspay;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class SendMoneyActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextInputLayout tilRecipientEmail;
    private TextInputLayout tilSendAmount;
    private TextInputEditText etRecipientEmail;
    private TextInputEditText etSendAmount;
    private MaterialButton btnSendCredits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarSendMoney);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilRecipientEmail = findViewById(R.id.tilRecipientEmail);
        tilSendAmount = findViewById(R.id.tilSendAmount);
        etRecipientEmail = findViewById(R.id.etRecipientEmail);
        etSendAmount = findViewById(R.id.etSendAmount);
        btnSendCredits = findViewById(R.id.btnSendCredits);

        btnSendCredits.setOnClickListener(v -> sendCredits());
    }

    private void sendCredits() {
        tilRecipientEmail.setError(null);
        tilSendAmount.setError(null);

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String recipientEmail = etRecipientEmail.getText() != null
                ? etRecipientEmail.getText().toString().trim().toLowerCase()
                : "";
        String amountText = etSendAmount.getText() != null
                ? etSendAmount.getText().toString().trim()
                : "";

        if (recipientEmail.isEmpty()) {
            tilRecipientEmail.setError("Enter recipient email");
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            tilSendAmount.setError("Enter a valid amount");
            return;
        }

        if (amount <= 0) {
            tilSendAmount.setError("Amount must be greater than 0");
            return;
        }

        String senderEmail = mAuth.getCurrentUser().getEmail() != null
                ? mAuth.getCurrentUser().getEmail().toLowerCase()
                : "";

        if (recipientEmail.equals(senderEmail)) {
            tilRecipientEmail.setError("You cannot send credits to yourself");
            return;
        }

        btnSendCredits.setEnabled(false);

        db.collection("users")
                .whereEqualTo("email", recipientEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(this::onRecipientFound)
                .addOnFailureListener(e -> {
                    btnSendCredits.setEnabled(true);
                    Toast.makeText(this,
                            "Lookup failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void onRecipientFound(QuerySnapshot querySnapshot) {
        if (querySnapshot.isEmpty()) {
            btnSendCredits.setEnabled(true);
            tilRecipientEmail.setError("No student found with this email");
            return;
        }

        DocumentSnapshot recipientDoc = querySnapshot.getDocuments().get(0);
        String recipientId = recipientDoc.getId();
        String recipientEmail = recipientDoc.getString("email");
        String amountText = etSendAmount.getText() != null
                ? etSendAmount.getText().toString().trim()
                : "";
        long amount = Long.parseLong(amountText);

        runTransferTransaction(mAuth.getCurrentUser().getUid(), recipientId,
                recipientEmail, amount);
    }

    private void runTransferTransaction(
            String senderId,
            String recipientId,
            String recipientEmail,
            long amount
    ) {
        com.google.firebase.firestore.DocumentReference senderRef =
                db.collection("users").document(senderId);
        com.google.firebase.firestore.DocumentReference recipientRef =
                db.collection("users").document(recipientId);

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

                    return amount;
                })
                .addOnSuccessListener(sent -> {
                    btnSendCredits.setEnabled(true);
                    android.widget.Toast.makeText(
                                    this,
                                    "Sent " + sent + " credits to "
                                            + recipientEmail,
                                    android.widget.Toast.LENGTH_LONG
                            ).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSendCredits.setEnabled(true);
                    android.widget.Toast.makeText(
                            this,
                            "Transfer failed: " + e.getMessage(),
                            android.widget.Toast.LENGTH_LONG
                    ).show();
                });
    }
}