package com.example.campuspay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;



public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvWelcome;
    private TextView tvBalance;

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

        MaterialButton btnScanQR = findViewById(R.id.btnScanQR);

        btnScanQR.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    ScanQRActivity.class
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

        startListeningToStudentData();

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

    private void startListeningToStudentData() {

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

                    if (name != null) {
                        tvWelcome.setText("Welcome, " + name);
                    }

                    tvBalance.setText(
                            credits != null ? credits + " Credits" : "0 Credits"
                    );
                }
            });
}

            @Override
            protected void onStop() {
                super.onStop();
                if (userListener != null) {
                    userListener.remove();
                    userListener = null;
                }
            }
}