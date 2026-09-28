package com.example.campuspay;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyEventsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView tvMyEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvMyEvents = findViewById(R.id.tvMyEvents);

        loadMyEvents();
    }

    private void loadMyEvents() {

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("registrations")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        tvMyEvents.setText("You have not registered for any events yet.");
                        return;
                    }

                    StringBuilder events = new StringBuilder();

                    querySnapshot.getDocuments().forEach(document -> {

                        String title =
                                document.getString("eventTitle");

                        events.append("• ")
                                .append(title)
                                .append("\n\n");
                    });

                    tvMyEvents.setText(events.toString());
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
}