package com.example.campuspay;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventsActivity extends AppCompatActivity
        implements EventAdapter.OnEventActionListener {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private EventAdapter adapter;

    private final List<Event> events = new ArrayList<>();
    private final Set<String> registeredEventIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarEvents);

        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.eventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EventAdapter(events, this);
        recyclerView.setAdapter(adapter);

        loadEvents();
    }

    private void loadEvents() {

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("registrations")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(registrationSnapshot -> {

                    registeredEventIds.clear();

                    registrationSnapshot.getDocuments().forEach(document -> {

                        String eventId = document.getString("eventId");

                        if (eventId != null) {
                            registeredEventIds.add(eventId);
                        }
                    });

                    loadEventList();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to load registrations: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void loadEventList() {

        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    events.clear();

                    if (querySnapshot.isEmpty()) {

                        adapter.notifyDataSetChanged();

                        Toast.makeText(
                                this,
                                "No events available",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    querySnapshot.getDocuments().forEach(document -> {

                        Event event = new Event(
                                document.getId(),
                                document.getString("title"),
                                document.getString("description"),
                                document.getLong("credits"),
                                document.getString("date")
                        );

                        event.setRegistered(
                                registeredEventIds.contains(event.getId())
                        );

                        events.add(event);
                    });

                    adapter.notifyDataSetChanged();
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

    @Override
    public void onRegister(Event event) {

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        String registrationId =
                userId + "_" + event.getId();

        Map<String, Object> registration = new HashMap<>();

        registration.put("userId", userId);
        registration.put("eventId", event.getId());
        registration.put("eventTitle", event.getTitle());
        registration.put(
                "registeredAt",
                FieldValue.serverTimestamp()
        );

        db.collection("registrations")
                .document(registrationId)
                .set(registration)
                .addOnSuccessListener(unused -> {

                    event.setRegistered(true);
                    registeredEventIds.add(event.getId());

                    adapter.notifyDataSetChanged();

                    Toast.makeText(
                            this,
                            "Registered for " + event.getTitle(),
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
