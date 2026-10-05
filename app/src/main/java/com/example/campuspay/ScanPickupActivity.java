package com.example.campuspay;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Organizer screen. Scans a student's pickup QR
 * (CAMPUSPAY_PICKUP:&lt;redemptionId&gt;), confirms the order with the
 * buyer present, and marks it handed over in one transaction.
 */
public class ScanPickupActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String organizerId;
    private boolean isAdmin = false;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions>
            qrLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {

                if (result.getContents() == null) {
                    finish();
                    return;
                }

                handleScan(result.getContents().trim());
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        organizerId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(organizerId)
                .get()
                .addOnSuccessListener(userDoc -> {

                    String role = userDoc.getString("role");
                    if (!UserRole.isOrganizer(role)
                            && !UserRole.isVendor(role)) {
                        Toast.makeText(
                                this,
                                "Only staff and vendors can hand over orders",
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                        return;
                    }

                    isAdmin = UserRole.isAdmin(role);

                    startScan();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            this,
                            "Could not verify organizer: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    private void startScan() {

        ScanOptions options = new ScanOptions();

        options.setPrompt("Scan student pickup QR");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);

        qrLauncher.launch(options);
    }

    private void handleScan(String qrData) {

        if (!QrPayload.isPickupPayload(qrData)) {
            showMessage("Not a CampusPay pickup QR.");
            return;
        }

        String redemptionId = QrPayload.redemptionIdFrom(qrData);

        if (!QrPayload.isValidRedemptionId(redemptionId)) {
            showMessage("Not a valid pickup QR.");
            return;
        }

        lookupOrder(redemptionId);
    }

    private void lookupOrder(String redemptionId) {

        db.collection("redemptions")
                .document(redemptionId)
                .get()
                .addOnSuccessListener(orderDoc -> {

                    if (!orderDoc.exists()) {
                        showMessage("Order not found.");
                        return;
                    }

                    Redemption order = Redemption.fromDocument(orderDoc);

                    if (!order.isPending()) {
                        showMessage("\"" + order.getItemTitle()
                                + "\" was already handed over.");
                        return;
                    }

                    // Only the marketplace service provider who listed the
                    // item may hand it over (owner-less items: any organizer).
                    db.collection("market_items")
                            .document(order.getItemId())
                            .get()
                            .addOnSuccessListener(itemDoc -> {
                                if (!itemDoc.exists()) {
                                    showMessage("Item no longer listed.");
                                    return;
                                }
                                String createdBy = itemDoc.getString("createdBy");
                                if (!isAdmin && createdBy != null
                                        && !createdBy.equals(organizerId)) {
                                    showMessage("Only the item provider can hand over this order.");
                                    return;
                                }
                                lookupBuyer(order);
                            })
                            .addOnFailureListener(e ->
                                    showMessage("Lookup failed: "
                                            + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        showMessage("Lookup failed: " + e.getMessage()));
    }

    private void lookupBuyer(Redemption order) {

        db.collection("users")
                .document(order.getUserId())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String name = userDoc.getString("name");
                    String email = userDoc.getString("email");
                    String label = (name != null ? name : "Student")
                            + (email != null ? " (" + email + ")" : "");
                    confirmHandover(order, label);
                })
                .addOnFailureListener(e ->
                        showMessage("Lookup failed: "
                                + e.getMessage()));
    }

    private void confirmHandover(Redemption order, String label) {

        if (isFinishing()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Hand over item?")
                .setMessage(
                        "Hand over \"" + order.getItemTitle() + "\" to:\n\n"
                                + label
                                + "\n\nCheck that this matches the person in front of you."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Hand Over",
                        (dialog, which) -> handover(order, label)
                )
                .setNegativeButton(
                        "Skip",
                        (dialog, which) -> startScan()
                )
                .show();
    }

    private void handover(Redemption order, String label) {

        DocumentReference orderRef =
                db.collection("redemptions").document(order.getId());

        db.runTransaction(transaction -> {

                    DocumentSnapshot orderSnapshot =
                            transaction.get(orderRef);

                    if (!orderSnapshot.exists()) {
                        throw new FirebaseFirestoreException(
                                "Order not found",
                                FirebaseFirestoreException.Code.NOT_FOUND
                        );
                    }

                    if (!Redemption.STATUS_PENDING.equals(
                            orderSnapshot.getString("status"))) {
                        throw new FirebaseFirestoreException(
                                "Order was already handed over",
                                FirebaseFirestoreException.Code.ABORTED
                        );
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", Redemption.STATUS_HANDED);
                    updates.put("handedBy", organizerId);
                    updates.put("handedAt", FieldValue.serverTimestamp());

                    transaction.update(orderRef, updates);

                    return order.getItemTitle();
                })
                .addOnSuccessListener(title ->
                        showMessage("Handed over \"" + title
                                + "\" to " + label + ".")
                )
                .addOnFailureListener(e ->
                        showMessage("Handover failed: " + e.getMessage())
                );
    }

    /** Shows a result and lets the organizer scan the next pickup. */
    private void showMessage(String message) {

        if (isFinishing()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Scan next", (dialog, which) -> startScan())
                .setNegativeButton("Done", (dialog, which) -> finish())
                .show();
    }
}
