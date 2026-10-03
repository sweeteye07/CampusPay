package com.example.campuspay;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EventsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private LinearLayout eventsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        eventsContainer = findViewById(R.id.eventsContainer);

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarEvents);

        toolbar.setNavigationOnClickListener(v -> finish());

        loadEvents();
    }

    private void loadEvents() {

        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    eventsContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {

                        TextView emptyMessage = new TextView(this);
                        emptyMessage.setText("No events available");
                        emptyMessage.setTextSize(18);

                        eventsContainer.addView(emptyMessage);

                        return;
                    }

                    querySnapshot.getDocuments().forEach(document -> {

                        String eventId = document.getId();
                        String title = document.getString("title");
                        String description = document.getString("description");
                        Long credits = document.getLong("credits");
                        String date = document.getString("date");

                        LinearLayout eventLayout = new LinearLayout(this);

                        eventLayout.setOrientation(
                                LinearLayout.VERTICAL
                        );

                        eventLayout.setPadding(
                                20,
                                20,
                                20,
                                20
                        );

                        TextView tvTitle = new TextView(this);

                        tvTitle.setText(title);
                        tvTitle.setTextSize(22);
                        tvTitle.setTextColor(Color.BLACK);

                        TextView tvDescription = new TextView(this);

                        tvDescription.setText(description);
                        tvDescription.setTextSize(16);

                        TextView tvCredits = new TextView(this);

                        tvCredits.setText(
                                "Earn " + credits + " Credits"
                        );

                        tvCredits.setTextSize(16);

                        TextView tvDate = new TextView(this);

                        tvDate.setText(
                                "Date: " + date
                        );

                        tvDate.setTextSize(16);

                        Button btnRegister = new Button(this);

                        Button btnShowQR = new Button(this);

                        btnShowQR.setText("Show Event QR");

                        btnShowQR.setOnClickListener(v -> {

                            android.content.Intent intent =
                                    new android.content.Intent(
                                            EventsActivity.this,
                                            EventQRActivity.class
                                    );

                            intent.putExtra("eventId", eventId);
                            intent.putExtra("eventTitle", title);

                            startActivity(intent);
                        });

                        checkRegistration(
                                eventId,
                                btnRegister
                        );

                        btnRegister.setOnClickListener(v ->
                                registerForEvent(
                                        eventId,
                                        title,
                                        btnRegister
                                )
                        );

                        eventLayout.addView(tvTitle);
                        eventLayout.addView(tvDescription);
                        eventLayout.addView(tvCredits);
                        eventLayout.addView(tvDate);
                        eventLayout.addView(btnRegister);
                        eventLayout.addView(btnShowQR);

                        LinearLayout.LayoutParams params =
                                new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                );

                        params.setMargins(
                                0,
                                0,
                                0,
                                24
                        );

                        eventsContainer.addView(
                                eventLayout,
                                params
                        );
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to load events: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void checkRegistration(
            String eventId,
            Button button
    ) {

        String userId =
                mAuth.getCurrentUser().getUid();

        String registrationId =
                userId + "_" + eventId;

        db.collection("registrations")
                .document(registrationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        button.setText("Registered");
                        button.setEnabled(false);

                    } else {

                        button.setText("Register");
                        button.setEnabled(true);
                    }
                });
    }

    private void registerForEvent(
            String eventId,
            String eventTitle,
            Button button
    ) {

        String userId =
                mAuth.getCurrentUser().getUid();

        String registrationId =
                userId + "_" + eventId;

        Map<String, Object> registration =
                new HashMap<>();

        registration.put(
                "userId",
                userId
        );

        registration.put(
                "eventId",
                eventId
        );

        registration.put(
                "eventTitle",
                eventTitle
        );

        registration.put(
                "registeredAt",
                FieldValue.serverTimestamp()
        );

        db.collection("registrations")
                .document(registrationId)
                .set(registration)
                .addOnSuccessListener(unused -> {

                    button.setText("Registered");
                    button.setEnabled(false);

                    Toast.makeText(
                            this,
                            "Registered for " + eventTitle,
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Registration failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
}