package com.example.campuspay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
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

    private boolean isOrganizer = false;
    private boolean isStudent = true;
    private String currentUserId = null;
    private com.google.android.material.button.MaterialButton btnCreateEventInline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarEvents);

        toolbar.setNavigationOnClickListener(v -> finish());

        btnCreateEventInline = findViewById(R.id.btnCreateEventInline);
        btnCreateEventInline.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        RecyclerView recyclerView = findViewById(R.id.eventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EventAdapter(events, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resolveRoleThenLoad();
    }

    /** Role check drives Create button + per-row Edit/Delete visibility. */
    private void resolveRoleThenLoad() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String role = userDoc.getString("role");
                    isOrganizer = UserRole.isOrganizer(role);
                    isStudent = UserRole.isStudent(role);
                    adapter.setAdminMode(isOrganizer, currentUserId);
                    adapter.setStudentView(isStudent);
                    adapter.setUnrestricted(UserRole.isAdmin(role));
                    btnCreateEventInline.setVisibility(
                            isOrganizer ? View.VISIBLE : View.GONE);
                    loadEvents();
                })
                .addOnFailureListener(e -> {
                    // Fail closed: student view if role can't be verified.
                    isOrganizer = false;
                    isStudent = true;
                    adapter.setAdminMode(false, currentUserId);
                    adapter.setStudentView(true);
                    adapter.setUnrestricted(false);
                    btnCreateEventInline.setVisibility(View.GONE);
                    loadEvents();
                });
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

                        com.google.firebase.Timestamp deadline =
                                document.getTimestamp("deadline");

                        Event event = new Event(
                                document.getId(),
                                document.getString("title"),
                                document.getString("description"),
                                document.getLong("credits"),
                                document.getString("date"),
                                document.getString("createdBy"),
                                deadline != null
                                        ? deadline.toDate().getTime() : null,
                                document.getLong("capacity"),
                                document.getLong("registeredCount"),
                                document.getString("department"),
                                document.getString("audienceTag"),
                                document.getString("eligibilityNote")
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

        // Registration is students-only.
        if (!isStudent) {
            Toast.makeText(this, "Registration is for students",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!event.isRegistrationOpen()) {
            Toast.makeText(this,
                    "Registration is closed for this event",
                    Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        String registrationId =
                userId + "_" + event.getId();

        com.google.firebase.firestore.DocumentReference eventRef =
                db.collection("events").document(event.getId());
        com.google.firebase.firestore.DocumentReference registrationRef =
                db.collection("registrations").document(registrationId);

        // One atomic transaction: re-check deadline + seats against the
        // latest server state, block duplicates, create the registration
        // and take one seat. The security rules verify the seat counter
        // and the deadline again in the same commit.
        db.runTransaction(transaction -> {
                    DocumentSnapshot eventSnapshot =
                            transaction.get(eventRef);

                    if (!eventSnapshot.exists()) {
                        throw new com.google.firebase.firestore.FirebaseFirestoreException(
                                "Event no longer available",
                                com.google.firebase.firestore.FirebaseFirestoreException
                                        .Code.NOT_FOUND);
                    }

                    com.google.firebase.Timestamp deadline =
                            eventSnapshot.getTimestamp("deadline");
                    if (deadline != null
                            && System.currentTimeMillis()
                                    > deadline.toDate().getTime()) {
                        throw new com.google.firebase.firestore.FirebaseFirestoreException(
                                "Registration deadline has passed",
                                com.google.firebase.firestore.FirebaseFirestoreException
                                        .Code.FAILED_PRECONDITION);
                    }

                    Long capacity = eventSnapshot.getLong("capacity");
                    if (capacity != null) {
                        Long count =
                                eventSnapshot.getLong("registeredCount");
                        long taken = count != null ? count : 0L;
                        if (taken >= capacity) {
                            throw new com.google.firebase.firestore.FirebaseFirestoreException(
                                    "Event is full",
                                    com.google.firebase.firestore.FirebaseFirestoreException
                                            .Code.FAILED_PRECONDITION);
                        }
                        transaction.update(eventRef, "registeredCount",
                                com.google.firebase.firestore.FieldValue
                                        .increment(1));
                    }

                    DocumentSnapshot registrationSnapshot =
                            transaction.get(registrationRef);
                    if (registrationSnapshot.exists()) {
                        throw new com.google.firebase.firestore.FirebaseFirestoreException(
                                "Already registered for this event",
                                com.google.firebase.firestore.FirebaseFirestoreException
                                        .Code.ALREADY_EXISTS);
                    }

                    Map<String, Object> registration = new HashMap<>();

                    registration.put("userId", userId);
                    registration.put("eventId", event.getId());
                    registration.put("eventTitle", event.getTitle());
                    registration.put(
                            "registeredAt",
                            FieldValue.serverTimestamp()
                    );

                    transaction.set(registrationRef, registration);
                    return null;
                })
                .addOnSuccessListener(unused -> {

                    event.setRegistered(true);
                    registeredEventIds.add(event.getId());

                    // Reload so the seat counter reflects the taken seat.
                    loadEvents();

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

    @Override
    public void onEdit(Event event) {
        Intent intent = new Intent(this, CreateEventActivity.class);
        intent.putExtra(CreateEventActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    @Override
    public void onDelete(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete event?")
                .setMessage("Delete \"" + event.getTitle() + "\"?\n\n"
                        + "Students will no longer be able to register for it. "
                        + "Past registrations and rewards are kept for records.")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEvent(Event event) {
        db.collection("events")
                .document(event.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    events.remove(event);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Event deleted",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Delete failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    
}
