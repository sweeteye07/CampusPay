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

    private TextInputLayout tilTitle;
    private TextInputLayout tilDescription;
    private TextInputLayout tilCredits;
    private TextInputLayout tilDate;

    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private TextInputEditText etCredits;
    private TextInputEditText etDate;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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

        MaterialButton btnPublish = findViewById(R.id.btnPublishEvent);
        btnPublish.setOnClickListener(v -> publishEvent());
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

        if (date.isEmpty()) {
            tilDate.setError("Date is required");
            etDate.requestFocus();
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
}
