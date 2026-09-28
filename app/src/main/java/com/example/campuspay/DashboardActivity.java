package com.example.campuspay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvWelcome;
    private TextView tvBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvWelcome = findViewById(R.id.tvWelcome);
        tvBalance = findViewById(R.id.tvBalance);

        Button btnLogout = findViewById(R.id.btnLogout);

        Button btnEvents = findViewById(R.id.btnEvents);

        Button btnMyEvents = findViewById(R.id.btnMyEvents);

        Button btnScanQR = findViewById(R.id.btnScanQR);

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

        loadStudentData();

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

    private void loadStudentData() {

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        String name = documentSnapshot.getString("name");
                        Long credits = documentSnapshot.getLong("credits");

                        if (name != null) {
                            tvWelcome.setText("Welcome, " + name);
                        }

                        if (credits != null) {
                            tvBalance.setText(credits + " Credits");
                        }
                    }
                });
    }
}