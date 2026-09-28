package com.example.campuspay;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ScanQRActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions>
            qrLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {

                if (result.getContents() == null) {
                    Toast.makeText(
                            this,
                            "Scan cancelled",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                String qrData = result.getContents();

                processQRCode(qrData);
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ScanOptions options = new ScanOptions();

        options.setPrompt("Scan the event QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);

        qrLauncher.launch(options);
    }

    private void processQRCode(String qrData) {

        String prefix = "CAMPUSPAY_EVENT:";

        if (!qrData.startsWith(prefix)) {

            Toast.makeText(
                    this,
                    "Invalid CampusPay event QR",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String eventId = qrData.substring(prefix.length());

        checkEventRegistration(eventId);
    }

    private void checkEventRegistration(String eventId) {

        String userId =
                mAuth.getCurrentUser().getUid();

        String registrationId =
                userId + "_" + eventId;

        db.collection("registrations")
                .document(registrationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {

                        Toast.makeText(
                                this,
                                "You are not registered for this event",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    checkRewardClaimed(
                            userId,
                            eventId
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Verification failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void checkRewardClaimed(
            String userId,
            String eventId
    ) {

        String rewardId =
                userId + "_" + eventId;

        db.collection("attendance")
                .document(rewardId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        Toast.makeText(
                                this,
                                "Credits already claimed for this event",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    awardCredits(
                            userId,
                            eventId,
                            rewardId
                    );
                });
    }

    private void awardCredits(
            String userId,
            String eventId,
            String rewardId
    ) {

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDocument -> {

                    if (!eventDocument.exists()) {

                        Toast.makeText(
                                this,
                                "Event not found",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    Long credits =
                            eventDocument.getLong("credits");

                    String eventTitle =
                            eventDocument.getString("title");

                    if (credits == null) {
                        Toast.makeText(
                                this,
                                "Event reward not configured",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    db.collection("users")
                            .document(userId)
                            .update(
                                    "credits",
                                    FieldValue.increment(credits)
                            )
                            .addOnSuccessListener(unused -> {

                                saveAttendance(
                                        rewardId,
                                        userId,
                                        eventId,
                                        eventTitle,
                                        credits
                                );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Failed to award credits: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                });
    }

    private void saveAttendance(
            String rewardId,
            String userId,
            String eventId,
            String eventTitle,
            Long credits
    ) {

        java.util.Map<String, Object> attendance =
                new java.util.HashMap<>();

        attendance.put("userId", userId);
        attendance.put("eventId", eventId);
        attendance.put("eventTitle", eventTitle);
        attendance.put("creditsAwarded", credits);
        attendance.put(
                "attendedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("attendance")
                .document(rewardId)
                .set(attendance)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Attendance verified! "
                                    + credits
                                    + " Credits earned.",
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Attendance record failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
}