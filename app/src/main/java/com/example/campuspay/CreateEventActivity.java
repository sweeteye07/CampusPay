package com.example.campuspay;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";

    private TextInputLayout tilTitle;
    private TextInputLayout tilDescription;
    private TextInputLayout tilCredits;
    private TextInputLayout tilDate;

    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private TextInputEditText etCredits;
    private TextInputEditText etDate;

    private MaterialButton btnPublish;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String editEventId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarCreateEvent);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilTitle = findViewById(R.id.tilEventTitle);
        tilDescription = findViewById(R.id.tilEventDescription);
        tilCredits = findViewById(R.id.tilEventCredits);
        tilDate = findViewById(R.id.tilEventDate);

        etTitle = findViewById(R.id.etEventTitle);
        etDescription = findViewById(R.id.etEventDescription);
        etCredits = findViewById(R.id.etEventCredits);
        etDate = findViewById(R.id.etEventDate);

        btnPublish = findViewById(R.id.btnPublishEvent);

        editEventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        boolean isEdit = editEventId != null && !editEventId.isEmpty();

        if (isEdit) {
            toolbar.setTitle("Edit Event");
            btnPublish.setText("Save Changes");
            btnPublish.setEnabled(false);
            loadEventForEdit(editEventId);
        }

        btnPublish.setOnClickListener(v -> publishEvent());
    }

    /** Pre-fill the form when opened from Manage Events. */
    private void loadEventForEdit(String eventId) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Event not found",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    if (etTitle.getText() == null) {
                        return;
                    }
                    etTitle.setText(doc.getString("title"));
                    etDescription.setText(doc.getString("description"));
                    Long credits = doc.getLong("credits");
                    etCredits.setText(credits != null ? String.valueOf(credits) : "");
                    etDate.setText(doc.getString("date"));
                    btnPublish.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Could not load event: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void publishEvent() {
        String title = etTitle.getText() != null
                ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null
                ? etDescription.getText().toString().trim() : "";
        String creditsRaw = etCredits.getText() != null
                ? etCredits.getText().toString().trim() : "";
        String date = etDate.getText() != null
                ? etDate.getText().toString().trim() : "";

        tilTitle.setError(null);
        tilDescription.setError(null);
        tilCredits.setError(null);
        tilDate.setError(null);

        if (title.isEmpty()) {
            tilTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (title.length() > 100) {
            tilTitle.setError("Title cannot exceed 100 characters");
            etTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            tilDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        long credits;
        try {
            credits = Long.parseLong(creditsRaw);
        } catch (NumberFormatException e) {
            tilCredits.setError("Enter a valid number");
            etCredits.requestFocus();
            return;
        }

        if (credits <= 0) {
            tilCredits.setError("Credits must be greater than 0");
            etCredits.requestFocus();
            return;
        }

        // The Firestore rules cap event rewards at 10000 credits
        if (credits > 10000) {
            tilCredits.setError("Credits cannot be more than 10000");
            etCredits.requestFocus();
            return;
        }

        if (date.isEmpty()) {
            tilDate.setError("Date is required");
            etDate.requestFocus();
            return;
        }

        boolean isEdit = editEventId != null && !editEventId.isEmpty();
        if (isEdit) {
            updateEvent(title, description, credits, date);
            return;
        }

        String creatorId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : null;

        Map<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("description", description);
        event.put("credits", credits);
        event.put("date", date);
        event.put("createdBy", creatorId);
        event.put("createdAt", FieldValue.serverTimestamp());

        db.collection("events")
                .add(event)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(
                            this,
                            "Event published",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Publish failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    /** Update path for Manage Events -> Edit. Keeps createdBy/createdAt intact. */
    private void updateEvent(String title, String description, long credits, String date) {
        btnPublish.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("credits", credits);
        updates.put("date", date);

        db.collection("events")
                .document(editEventId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Event updated",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPublish.setEnabled(true);
                    Toast.makeText(this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
