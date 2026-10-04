package com.example.campuspay;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyEventsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private RecyclerView recyclerView;
    private TextView tvEmpty;

    private MyEventAdapter adapter;

    private final List<MyEvent> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarMyEvents);

        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.myEventsRecyclerView);
        tvEmpty = findViewById(R.id.tvMyEventsEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MyEventAdapter(items);
        recyclerView.setAdapter(adapter);

        loadMyEvents();
    }

    private void loadMyEvents() {

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

                    if (registrationSnapshot.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    loadAttendanceAndBuildList(
                            userId,
                            registrationSnapshot.getDocuments()
                    );
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

    private void loadAttendanceAndBuildList(
            String userId,
            List<DocumentSnapshot> registrations
    ) {

        db.collection("attendance")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(attendanceSnapshot -> {

                    Set<String> attendedEventIds = new HashSet<>();

                    attendanceSnapshot.getDocuments().forEach(document -> {

                        String eventId = document.getString("eventId");

                        if (eventId != null) {
                            attendedEventIds.add(eventId);
                        }
                    });

                    buildList(registrations, attendedEventIds);
                })
                .addOnFailureListener(e -> {

                    buildList(registrations, new HashSet<>());

                    Toast.makeText(
                            this,
                            "Could not load attendance status",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void buildList(
            List<DocumentSnapshot> registrations,
            Set<String> attendedEventIds
    ) {

        items.clear();

        Set<String> seenEventIds = new HashSet<>();

        registrations.forEach(document -> {

            String eventId = document.getString("eventId");

            if (eventId == null || !seenEventIds.add(eventId)) {
                return;
            }

            String title = document.getString("eventTitle");

            if (title == null) {
                title = "Campus Event";
            }

            Long credits = document.getLong("credits");
            String date = document.getString("date");

            items.add(
                    new MyEvent(
                            eventId,
                            title,
                            date,
                            credits != null ? credits : 0L,
                            attendedEventIds.contains(eventId)
                    )
            );
        });

        if (items.isEmpty()) {
            showEmpty();
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        adapter.notifyDataSetChanged();

        enrichMissingDetails();
    }

    private void enrichMissingDetails() {

        for (int index = 0; index < items.size(); index++) {

            MyEvent item = items.get(index);

            if (item.getCredits() != 0L && item.getDate() != null) {
                continue;
            }

            final int position = index;
            final MyEvent current = item;

            db.collection("events")
                    .document(current.getEventId())
                    .get()
                    .addOnSuccessListener(document -> {

                        if (!document.exists()) {
                            return;
                        }

                        Long credits = document.getLong("credits");
                        String date = document.getString("date");

                        items.set(
                                position,
                                new MyEvent(
                                        current.getEventId(),
                                        current.getTitle(),
                                        date,
                                        credits != null ? credits : 0L,
                                        current.isAttended()
                                )
                        );

                        adapter.notifyItemChanged(position);
                    });
        }
    }

    private void showEmpty() {
        items.clear();
        adapter.notifyDataSetChanged();

        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
    }
}