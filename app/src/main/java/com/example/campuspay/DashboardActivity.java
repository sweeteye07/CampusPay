package com.example.campuspay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;



public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvWelcome;
    private TextView tvBalance;
    private MaterialButton btnScanQR;
    private boolean isOrganizer = false;

    private com.google.firebase.firestore.ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvWelcome = findViewById(R.id.tvWelcome);
        tvBalance = findViewById(R.id.tvBalance);

        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        MaterialButton btnEvents = findViewById(R.id.btnEvents);

        MaterialButton btnMyEvents = findViewById(R.id.btnMyEvents);

        btnScanQR = findViewById(R.id.btnScanQR);
        btnScanQR.setText("My QR");

        MaterialButton btnSendMoney = findViewById(R.id.btnSendMoney);

        MaterialButton btnReceiveMoney = findViewById(R.id.btnReceiveMoney);

        btnSendMoney.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    SendMoneyActivity.class
            );
            startActivity(intent);
        });

        btnReceiveMoney.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    ReceiveMoneyActivity.class
            );
            startActivity(intent);
        });

        MaterialButton btnProfile = findViewById(R.id.btnProfile);

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    ProfileActivity.class
            );
            startActivity(intent);
        });

        // Event create/manage now lives inside Campus Events
        // (role-gated there), so no separate dashboard entry is needed.

        // Wallet card opens the credit history ledger
        findViewById(R.id.cardWallet).setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    HistoryActivity.class
            );
            startActivity(intent);
        });

        // Students show their QR; organizers (role = admin/organizer) scan students
        btnScanQR.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    isOrganizer ? ScanQRActivity.class : MyQRActivity.class
            );
            startActivity(intent);
        });

        btnMyEvents.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    MyEventsActivity.class
            );
            startActivity(intent);
        });

        btnEvents.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    EventsActivity.class
            );
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();

            Intent intent = new Intent(
                    DashboardActivity.this,
                    MainActivity.class
            );

            startActivity(intent);
            finish();
        });
    }

    // Register in onStart (not onCreate) so the balance refreshes again
    // every time the user returns to this screen after onStop().
    @Override
    protected void onStart() {
        super.onStart();
        startListeningToStudentData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

    private void startListeningToStudentData() {

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        userListener = db.collection("users")
                .document(userId)
                .addSnapshotListener((documentSnapshot, error) -> {

                    if (error != null) {
                        // Listener failed (e.g. lost permission) — fail quietly
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {

                        String name = documentSnapshot.getString("name");
                        Long credits = documentSnapshot.getLong("credits");

                        isOrganizer = UserRole.isOrganizer(
                                documentSnapshot.getString("role")
                        );
                        btnScanQR.setText(isOrganizer ? "Scan" : "My QR");

                        if (name != null) {
                            tvWelcome.setText("Welcome, " + name);
                        }

                        tvBalance.setText(
                                credits != null ? credits + " Credits" : "0 Credits"
                        );
                    }
                });
    }
}