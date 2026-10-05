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
    private TextInputLayout tilDeadline;
    private TextInputLayout tilCapacity;
    private TextInputLayout tilDepartment;
    private TextInputLayout tilEligibility;

    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private TextInputEditText etCredits;
    private TextInputEditText etDate;
    private TextInputEditText etDeadline;
    private TextInputEditText etCapacity;
    private TextInputEditText etEligibility;
    private android.widget.AutoCompleteTextView actvDepartment;
    private com.google.android.material.chip.ChipGroup chipGroupAudience;

    private MaterialButton btnPublish;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String editEventId = null;
    private long editRegisteredCount = 0;
    private long selectedDeadlineMillis = -1;

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
        tilDeadline = findViewById(R.id.tilEventDeadline);
        tilCapacity = findViewById(R.id.tilEventCapacity);
        tilDepartment = findViewById(R.id.tilEventDepartment);
        tilEligibility = findViewById(R.id.tilEventEligibility);

        etTitle = findViewById(R.id.etEventTitle);
        etDescription = findViewById(R.id.etEventDescription);
        etCredits = findViewById(R.id.etEventCredits);
        etDate = findViewById(R.id.etEventDate);
        etDeadline = findViewById(R.id.etEventDeadline);
        etCapacity = findViewById(R.id.etEventCapacity);
        etEligibility = findViewById(R.id.etEventEligibility);
        actvDepartment = findViewById(R.id.actvEventDepartment);
        chipGroupAudience = findViewById(R.id.chipGroupAudience);

        actvDepartment.setAdapter(new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                EventAudience.departments()));
        actvDepartment.setText(EventAudience.DEPARTMENT_ALL, false);

        etDate.setOnClickListener(v -> showDateTimePicker(false));
        etDeadline.setOnClickListener(v -> showDateTimePicker(true));

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

    /**
     * Date list first, then a 12-hour time list with AM/PM.
     * forDeadline picks the registration deadline, otherwise the
     * event day and time shown on the listing.
     */
    private void showDateTimePicker(boolean forDeadline) {
        java.util.Calendar now = java.util.Calendar.getInstance();

        new android.app.DatePickerDialog(
                this,
                (view, year, month, day) ->
                    new android.app.TimePickerDialog(
                            CreateEventActivity.this,
                            (timeView, hourOfDay, minute) -> {
                                long millis =
                                        EventDeadline.combineDateTimeMillis(
                                                year, month, day,
                                                hourOfDay, minute);
                                String text =
                                        EventDeadline.formatDateTime(millis);
                                if (forDeadline) {
                                    etDeadline.setText(text);
                                    selectedDeadlineMillis = millis;
                                    tilDeadline.setError(null);
                                } else {
                                    etDate.setText(text);
                                    tilDate.setError(null);
                                }
                            },
                            now.get(java.util.Calendar.HOUR_OF_DAY),
                            now.get(java.util.Calendar.MINUTE),
                            false).show(),
                now.get(java.util.Calendar.YEAR),
                now.get(java.util.Calendar.MONTH),
                now.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    /** Checks the chip matching the stored tag, defaulting to Open for All. */
    private void checkAudienceChip(String tag) {
        int checkedId = R.id.chipOpenForAll;
        if (EventAudience.TAG_MUST_JOIN.equals(tag)) {
            checkedId = R.id.chipMustJoin;
        } else if (EventAudience.TAG_HIGHLY_SUGGESTED.equals(tag)) {
            checkedId = R.id.chipHighlySuggested;
        }
        chipGroupAudience.check(checkedId);
    }

    private String selectedAudienceTag() {
        int checkedId = chipGroupAudience.getCheckedChipId();
        if (checkedId == R.id.chipMustJoin) {
            return EventAudience.TAG_MUST_JOIN;
        }
        if (checkedId == R.id.chipHighlySuggested) {
            return EventAudience.TAG_HIGHLY_SUGGESTED;
        }
        return EventAudience.TAG_OPEN_FOR_ALL;
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
                    com.google.firebase.Timestamp deadline =
                            doc.getTimestamp("deadline");
                    if (deadline != null) {
                        selectedDeadlineMillis =
                                deadline.toDate().getTime();
                        etDeadline.setText(EventDeadline.formatDateTime(
                                selectedDeadlineMillis));
                    } else {
                        etDeadline.setText("");
                    }
                    Long capacity = doc.getLong("capacity");
                    etCapacity.setText(capacity != null ? String.valueOf(capacity) : "");
                    Long count = doc.getLong("registeredCount");
                    editRegisteredCount = count != null ? count : 0;
                    String department = doc.getString("department");
                    actvDepartment.setText(
                            department != null && !department.isEmpty()
                                    ? department
                                    : EventAudience.DEPARTMENT_ALL,
                            false);
                    checkAudienceChip(doc.getString("audienceTag"));
                    etEligibility.setText(doc.getString("eligibilityNote"));
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
        String capacityRaw = etCapacity.getText() != null
                ? etCapacity.getText().toString().trim() : "";

        tilTitle.setError(null);
        tilDescription.setError(null);
        tilCredits.setError(null);
        tilDate.setError(null);
        tilDeadline.setError(null);
        tilCapacity.setError(null);

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

        long deadlineMillis = selectedDeadlineMillis;
        if (deadlineMillis <= 0) {
            tilDeadline.setError("Pick a deadline day and time");
            return;
        }

        long capacity;
        try {
            capacity = Long.parseLong(capacityRaw);
        } catch (NumberFormatException e) {
            tilCapacity.setError("Enter a valid number");
            etCapacity.requestFocus();
            return;
        }

        if (capacity < 1 || capacity > 1000) {
            tilCapacity.setError("Capacity must be between 1 and 1000");
            etCapacity.requestFocus();
            return;
        }

        String department = actvDepartment.getText() != null
                ? actvDepartment.getText().toString().trim() : "";
        if (!EventAudience.isValidDepartment(department)) {
            tilDepartment.setError("Pick a department from the list");
            return;
        }
        tilDepartment.setError(null);

        String eligibility = etEligibility.getText() != null
                ? etEligibility.getText().toString().trim() : "";
        if (eligibility.isEmpty()) {
            tilEligibility.setError(
                    "Tell students who should opt in and who can't");
            etEligibility.requestFocus();
            return;
        }
        if (eligibility.length() > 500) {
            tilEligibility.setError("Keep the note under 500 characters");
            etEligibility.requestFocus();
            return;
        }

        String audienceTag = selectedAudienceTag();

        boolean isEdit = editEventId != null && !editEventId.isEmpty();

        if (!isEdit && deadlineMillis <= System.currentTimeMillis()) {
            tilDeadline.setError("Deadline must be in the future");
            etDeadline.requestFocus();
            return;
        }

        if (isEdit && capacity < editRegisteredCount) {
            tilCapacity.setError("Cannot go below "
                    + editRegisteredCount + " registered");
            etCapacity.requestFocus();
            return;
        }

        if (isEdit) {
            updateEvent(title, description, credits, date,
                    deadlineMillis, capacity, department, audienceTag,
                    eligibility);
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
        event.put("deadline",
                new com.google.firebase.Timestamp(
                        new java.util.Date(deadlineMillis)));
        event.put("capacity", capacity);
        event.put("registeredCount", 0L);
        event.put("department", department);
        event.put("audienceTag", audienceTag);
        event.put("eligibilityNote", eligibility);

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
    private void updateEvent(String title, String description, long credits,
            String date, long deadlineMillis, long capacity,
            String department, String audienceTag, String eligibility) {
        btnPublish.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("credits", credits);
        updates.put("date", date);
        updates.put("deadline",
                new com.google.firebase.Timestamp(
                        new java.util.Date(deadlineMillis)));
        updates.put("capacity", capacity);
        updates.put("department", department);
        updates.put("audienceTag", audienceTag);
        updates.put("eligibilityNote", eligibility);
        // Backfill the counter on legacy events edited for the first time.
        updates.put("registeredCount", editRegisteredCount);

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
