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

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent>
            confirmLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts
                    .StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    finish();
                }
            });

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

        // Vendors have no wallet actions.
        if (mAuth.getCurrentUser() != null) {
            RoleGate.fetchRole(db, mAuth.getCurrentUser().getUid(), role -> {
                if (UserRole.isVendor(role)) {
                    Toast.makeText(this, "Sending is not available for vendors",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }
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
        btnSendCredits.setEnabled(true);

        if (querySnapshot.isEmpty()) {
            tilRecipientEmail.setError("No student found with this email");
            return;
        }

        DocumentSnapshot recipientDoc = querySnapshot.getDocuments().get(0);
        String recipientId = recipientDoc.getId();
        String recipientEmail = recipientDoc.getString("email");
        String recipientName = recipientDoc.getString("name");
        String amountText = etSendAmount.getText() != null
                ? etSendAmount.getText().toString().trim()
                : "";
        long amount = Long.parseLong(amountText);

        android.content.Intent intent =
                new android.content.Intent(this, ConfirmPaymentActivity.class);
        intent.putExtra(ConfirmPaymentActivity.EXTRA_RECIPIENT_ID,
                recipientId);
        intent.putExtra(ConfirmPaymentActivity.EXTRA_RECIPIENT_EMAIL,
                recipientEmail);
        intent.putExtra(ConfirmPaymentActivity.EXTRA_RECIPIENT_NAME,
                recipientName);
        intent.putExtra(ConfirmPaymentActivity.EXTRA_AMOUNT, amount);
        confirmLauncher.launch(intent);
    }
}