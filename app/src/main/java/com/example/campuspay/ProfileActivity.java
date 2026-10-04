package com.example.campuspay;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Student profile screen.
 *
 * Shows the student's identity (avatar initials, name, email, role),
 * account details (balance) and the "Your Activity" section (events
 * registered, events attended, credits earned, credits spent, current
 * balance and attendance rate). The display name can be
 * edited here, which is allowed by the security rules for a user's
 * own document.
 */
public class ProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView tvProfileInitials;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileRole;
    private TextView tvAccountEmail;
    private TextView tvAccountRole;
    private TextView tvAccountBalance;
    private TextView tvStatRegistered;
    private TextView tvStatAttended;
    private TextView tvStatEarned;
    private TextView tvStatSpent;
    private TextView tvStatBalance;
    private TextView tvStatAttendanceRate;
    private TextView tvAttendanceLabel;
    private android.widget.ProgressBar pbAttendanceRate;

    private MaterialButton btnScanAttendance;
    private View cardYourActivity;

    private String userId;
    private String currentName = "";
    private boolean isOrganizer = false;

    private int registeredCount = 0;
    private int attendedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarProfile);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvProfileInitials = findViewById(R.id.tvProfileInitials);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvAccountEmail = findViewById(R.id.tvAccountEmail);
        tvAccountRole = findViewById(R.id.tvAccountRole);
        tvAccountBalance = findViewById(R.id.tvAccountBalance);
        tvStatRegistered = findViewById(R.id.tvStatRegistered);
        tvStatAttended = findViewById(R.id.tvStatAttended);
        tvStatEarned = findViewById(R.id.tvStatEarned);
        tvStatSpent = findViewById(R.id.tvStatSpent);
        tvStatBalance = findViewById(R.id.tvStatBalance);
        tvStatAttendanceRate = findViewById(R.id.tvStatAttendanceRate);
        tvAttendanceLabel = findViewById(R.id.tvAttendanceLabel);
        pbAttendanceRate = findViewById(R.id.pbAttendanceRate);
        cardYourActivity = findViewById(R.id.cardYourActivity);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = user.getUid();

        findViewById(R.id.btnEditName)
                .setOnClickListener(v -> showEditNameDialog());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            mAuth.signOut();

            Intent intent = new Intent(
                    ProfileActivity.this,
                    MainActivity.class
            );
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Organizers only: scan a student's QR to award attendance
        btnScanAttendance = findViewById(R.id.btnScanAttendance);
        btnScanAttendance.setOnClickListener(v ->
                startActivity(new Intent(
                        ProfileActivity.this, ScanQRActivity.class)));

        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh after returning from events / transfers.
        // Stats reload is triggered from renderProfile once the
        // student role is confirmed, so organizers skip those reads.
        if (userId != null) {
            loadProfile();
        }
    }

    // ------------------------------------------------------------------
    // Profile
    // ------------------------------------------------------------------
    private void loadProfile() {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(this::renderProfile)
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not load profile: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show());
    }

    private void renderProfile(DocumentSnapshot doc) {

        if (!doc.exists()) {
            Toast.makeText(this, "Profile not found",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String name = doc.getString("name");
        String email = doc.getString("email");
        String role = doc.getString("role");
        Long credits = doc.getLong("credits");

        boolean organizer = UserRole.isOrganizer(role);
        isOrganizer = organizer;

        currentName = name != null ? name : "";

        String nameText = currentName.isEmpty()
                ? "CampusPay User" : currentName;
        String emailText = email != null ? email : "—";
        String roleLabel = organizer ? "Organizer" : "Student";

        // The scanner entry point only exists for organizers.
        // Your Activity is the mirror image: students only.
        btnScanAttendance.setVisibility(
                isOrganizer ? View.VISIBLE : View.GONE);
        cardYourActivity.setVisibility(
                isOrganizer ? View.GONE : View.VISIBLE);

        tvProfileName.setText(nameText);
        tvProfileInitials.setText(initialsOf(currentName));
        tvProfileEmail.setText(emailText);
        tvProfileRole.setText(roleLabel);

        tvAccountEmail.setText(emailText);
        tvAccountRole.setText(roleLabel);
        tvAccountBalance.setText(
                credits != null ? credits + " Credits" : "0 Credits");
        tvStatBalance.setText(String.valueOf(credits != null ? credits : 0));

        // Students only: organizers skip the activity reads entirely.
        if (!isOrganizer) {
            loadStats();
        }
    }

    /** First letters of up to two words of the display name. */
    private String initialsOf(String name) {

        if (name == null || name.trim().isEmpty()) {
            return "CP";
        }

        String[] words = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < words.length && i < 2; i++) {
            initials.append(Character.toUpperCase(words[i].charAt(0)));
        }

        return initials.toString();
    }

    // ------------------------------------------------------------------
    // Activity stats
    // ------------------------------------------------------------------
    private void loadStats() {

        if (isOrganizer) {
            return;
        }

        db.collection("registrations")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    registeredCount = snap.size();
                    tvStatRegistered.setText(String.valueOf(registeredCount));
                    renderAttendanceRate();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not load registrations",
                                Toast.LENGTH_SHORT
                        ).show());

        db.collection("attendance")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {

                    long earned = 0;

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Long credits = doc.getLong("creditsAwarded");
                        earned += credits != null ? credits : 0;
                    }

                    attendedCount = snap.size();
                    tvStatAttended.setText(String.valueOf(attendedCount));
                    tvStatEarned.setText(String.valueOf(earned));
                    renderAttendanceRate();
                    loadWalletStats();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not load attendance",
                                Toast.LENGTH_SHORT
                        ).show());
    }

    /**
     * Wallet side of Your Activity: credits earned (earn + received),
     * credits spent (sent), derived from the same ledger as History.
     */
    private void loadWalletStats() {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    long earned = 0;
                    long spent = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        CreditTransaction tx =
                                CreditTransaction.fromDocument(doc);
                        long credits = tx.getCredits() != null
                                ? tx.getCredits() : 0L;
                        if (tx.isEarn() || tx.isReceive()) {
                            earned += credits;
                        } else if (tx.isOutgoing()) {
                            spent += credits;
                        }
                    }
                    tvStatEarned.setText(String.valueOf(earned));
                    tvStatSpent.setText(String.valueOf(spent));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not load wallet stats",
                                Toast.LENGTH_SHORT
                        ).show());
    }

    private void renderAttendanceRate() {
        int rate = ActivityStats.attendanceRate(registeredCount, attendedCount);
        tvStatAttendanceRate.setText(rate + "%");
        tvAttendanceLabel.setText(
                ActivityStats.attendanceLabel(registeredCount, attendedCount));
        pbAttendanceRate.setProgress(rate);
    }

    // ------------------------------------------------------------------
    // Edit display name
    // ------------------------------------------------------------------
    private void showEditNameDialog() {

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Display name");
        input.setText(currentName);
        input.setSelection(input.getText().length());

        FrameLayout wrapper = new FrameLayout(this);
        float density = getResources().getDisplayMetrics().density;
        int padding = (int) (20 * density);
        wrapper.setPadding(padding, 0, padding, 0);
        wrapper.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Edit display name")
                .setView(wrapper)
                .setCancelable(true)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save",
                        (dialog, which) -> saveName(input))
                .show();
    }

    private void saveName(EditText input) {

        String name = input.getText().toString().trim();

        if (name.length() < 2) {
            Toast.makeText(this, "Name is too short",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(userId)
                .update("name", name)
                .addOnSuccessListener(unused -> {

                    currentName = name;

                    tvProfileName.setText(name);
                    tvProfileInitials.setText(initialsOf(name));

                    Toast.makeText(this, "Profile updated",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Update failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show());
    }
}

