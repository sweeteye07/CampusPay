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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Organizer screen. The organizer picks an event, then scans each student's
 * "My QR" (CAMPUSPAY_STUDENT:<uid>). After the organizer confirms the
 * student's name, the credits are awarded in one transaction.
 */
public class ScanQRActivity extends AppCompatActivity {

    private static final String STUDENT_QR_PREFIX = "CAMPUSPAY_STUDENT:";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String organizerId;

    private String eventId;
    private String eventTitle;
    private long eventCredits;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions>
            qrLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {

                if (result.getContents() == null) {
                    finish();
                    return;
                }

                if (eventId == null) {
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

                    if (!"admin".equals(userDoc.getString("role"))) {
                        Toast.makeText(
                                this,
                                "Only organizers can scan attendance",
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                        return;
                    }

                    loadEvents();
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

    // ------------------------------------------------------------------
    // Step 1: pick the event
    // ------------------------------------------------------------------
    private void loadEvents() {

        db.collection("events")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<String> ids = new ArrayList<>();
                    List<String> titles = new ArrayList<>();
                    List<Long> creditValues = new ArrayList<>();
                    List<String> labels = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String createdBy = doc.getString("createdBy");

                        // Organizers can only reward their own events
                        // (events added by hand in the console have no owner)
                        if (createdBy != null
                                && !createdBy.equals(organizerId)) {
                            continue;
                        }

                        Long credits = doc.getLong("credits");
                        String title = doc.getString("title");

                        if (credits == null || credits <= 0 || title == null) {
                            continue;
                        }

                        ids.add(doc.getId());
                        titles.add(title);
                        creditValues.add(credits);
                        labels.add(title + "  (+" + credits + ")");
                    }

                    if (ids.isEmpty()) {
                        Toast.makeText(
                                this,
                                "No events available for you to reward",
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                        return;
                    }

                    if (isFinishing()) {
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Select event")
                            .setItems(
                                    labels.toArray(new String[0]),
                                    (dialog, which) -> {
                                        eventId = ids.get(which);
                                        eventTitle = titles.get(which);
                                        eventCredits = creditValues.get(which);
                                        startScan();
                                    }
                            )
                            .setOnCancelListener(dialog -> finish())
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            this,
                            "Could not load events: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    // ------------------------------------------------------------------
    // Step 2: scan a student
    // ------------------------------------------------------------------
    private void startScan() {

        ScanOptions options = new ScanOptions();

        options.setPrompt("Scan student QR for: " + eventTitle);
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);

        qrLauncher.launch(options);
    }

    private void handleScan(String qrData) {

        if (!qrData.startsWith(STUDENT_QR_PREFIX)) {
            showMessage("Not a CampusPay student QR.");
            return;
        }

        String studentId = qrData.substring(STUDENT_QR_PREFIX.length());

        if (!studentId.matches("[A-Za-z0-9]{1,128}")) {
            showMessage("Not a valid student QR.");
            return;
        }

        if (studentId.equals(organizerId)) {
            showMessage("You cannot award credits to yourself.");
            return;
        }

        lookupStudent(studentId);
    }

    // ------------------------------------------------------------------
    // Step 3: check the student, then confirm with the organizer
    // ------------------------------------------------------------------
    private void lookupStudent(String studentId) {

        String rewardId = studentId + "_" + eventId;

        db.collection("users")
                .document(studentId)
                .get()
                .addOnSuccessListener(userDoc -> {

                    if (!userDoc.exists()) {
                        showMessage("Student not found.");
                        return;
                    }

                    String name = userDoc.getString("name");
                    String email = userDoc.getString("email");
                    String label = (name != null ? name : "Student")
                            + (email != null ? " (" + email + ")" : "");

                    db.collection("registrations")
                            .document(rewardId)
                            .get()
                            .addOnSuccessListener(regDoc -> {

                                if (!regDoc.exists()) {
                                    showMessage(label
                                            + " is not registered for this event.");
                                    return;
                                }

                                db.collection("attendance")
                                        .document(rewardId)
                                        .get()
                                        .addOnSuccessListener(attDoc -> {

                                            if (attDoc.exists()) {
                                                showMessage(label
                                                        + " has already received credits for this event.");
                                                return;
                                            }

                                            confirmAward(studentId, label);
                                        })
                                        .addOnFailureListener(this::onLookupFailed);
                            })
                            .addOnFailureListener(this::onLookupFailed);
                })
                .addOnFailureListener(this::onLookupFailed);
    }

    private void confirmAward(String studentId, String label) {

        if (isFinishing()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Award credits?")
                .setMessage(
                        "Give " + eventCredits + " credits to:\n\n"
                                + label
                                + "\n\nfor \"" + eventTitle + "\".\n\n"
                                + "Check that this matches the person in front of you."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Award",
                        (dialog, which) -> awardCredits(studentId, label)
                )
                .setNegativeButton(
                        "Skip",
                        (dialog, which) -> startScan()
                )
                .show();
    }

    // ------------------------------------------------------------------
    // Step 4: award credits atomically
    // ------------------------------------------------------------------
    private void awardCredits(String studentId, String label) {

        String rewardId = studentId + "_" + eventId;

        DocumentReference attendanceRef =
                db.collection("attendance").document(rewardId);
        DocumentReference registrationRef =
                db.collection("registrations").document(rewardId);
        DocumentReference eventRef =
                db.collection("events").document(eventId);
        DocumentReference studentRef =
                db.collection("users").document(studentId);
        DocumentReference ledgerRef =
                db.collection("transactions").document();

        db.runTransaction(transaction -> {

                    DocumentSnapshot attendanceSnapshot =
                            transaction.get(attendanceRef);

                    if (attendanceSnapshot.exists()) {
                        throw new FirebaseFirestoreException(
                                "Reward already given for this event",
                                FirebaseFirestoreException.Code.ABORTED
                        );
                    }

                    DocumentSnapshot registrationSnapshot =
                            transaction.get(registrationRef);

                    if (!registrationSnapshot.exists()) {
                        throw new FirebaseFirestoreException(
                                "Student is not registered for this event",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION
                        );
                    }

                    DocumentSnapshot eventSnapshot =
                            transaction.get(eventRef);

                    if (!eventSnapshot.exists()) {
                        throw new FirebaseFirestoreException(
                                "Event not found",
                                FirebaseFirestoreException.Code.NOT_FOUND
                        );
                    }

                    Long credits = eventSnapshot.getLong("credits");
                    String title = eventSnapshot.getString("title");

                    if (credits == null || credits <= 0) {
                        throw new FirebaseFirestoreException(
                                "Event reward not configured",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION
                        );
                    }

                    DocumentSnapshot studentSnapshot =
                            transaction.get(studentRef);

                    if (!studentSnapshot.exists()) {
                        throw new FirebaseFirestoreException(
                                "Student profile not found",
                                FirebaseFirestoreException.Code.NOT_FOUND
                        );
                    }

                    Map<String, Object> attendance = new HashMap<>();
                    attendance.put("userId", studentId);
                    attendance.put("eventId", eventId);
                    attendance.put("eventTitle", title);
                    attendance.put("creditsAwarded", credits);
                    attendance.put("awardedBy", organizerId);
                    attendance.put("attendedAt", FieldValue.serverTimestamp());

                    transaction.set(attendanceRef, attendance);

                    // lastEventId tells the security rules which attendance
                    // record backs this balance change
                    transaction.update(
                            studentRef,
                            "credits", FieldValue.increment(credits),
                            "lastEventId", eventId
                    );

                    Map<String, Object> ledger = new HashMap<>();
                    ledger.put("type", "earn");
                    ledger.put("userId", studentId);
                    ledger.put("eventId", eventId);
                    ledger.put("eventTitle", title);
                    ledger.put("credits", credits);
                    ledger.put(
                            "description",
                            "Earned " + credits + " credits for " + title
                    );
                    ledger.put("createdAt", FieldValue.serverTimestamp());

                    transaction.set(ledgerRef, ledger);

                    return credits;
                })
                .addOnSuccessListener(credits ->
                        showMessage("Awarded " + credits
                                + " credits to " + label + ".")
                )
                .addOnFailureListener(e ->
                        showMessage("Award failed: " + e.getMessage())
                );
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private void onLookupFailed(Exception e) {
        showMessage("Lookup failed: " + e.getMessage());
    }

    /** Shows a result and lets the organizer scan the next student. */
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