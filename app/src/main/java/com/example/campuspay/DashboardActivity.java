package com.example.campuspay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
    private MaterialButton btnReceiveMoney;
    private boolean isOrganizer = false;
    private boolean isVendor = false;

    private com.google.firebase.firestore.ListenerRegistration userListener;
    private com.google.firebase.firestore.ListenerRegistration txListener;
    private com.google.firebase.firestore.ListenerRegistration eventListener;
    private com.google.firebase.firestore.ListenerRegistration itemListener;
    private String notifyUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvWelcome = findViewById(R.id.tvWelcome);
        tvBalance = findViewById(R.id.tvBalance);

        MaterialButton btnEvents = findViewById(R.id.btnEvents);

        MaterialButton btnMyEvents = findViewById(R.id.btnMyEvents);

        btnScanQR = findViewById(R.id.btnScanQR);
        // Hidden on the home screen for now (see layout); kept wired
        // so scan can return later without code changes.
        btnScanQR.setText("My QR");

        MaterialButton btnSendMoney = findViewById(R.id.btnSendMoney);

        btnReceiveMoney = findViewById(R.id.btnReceiveMoney);

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

        NotificationHelper.ensureChannels(this);
        requestNotificationPermissionIfNeeded();

        findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    NotificationsActivity.class
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

        // Staff scan: choose attendance or pickup. Students and vendors
        // never see this button (vendors get their own scan below).
        btnScanQR.setOnClickListener(v -> showScanChooser());

        findViewById(R.id.btnVendorScan).setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    ScanPickupActivity.class
            );
            startActivity(intent);
        });

        findViewById(R.id.btnVendorOrders).setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    VendorOrdersActivity.class
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

        findViewById(R.id.btnMarketplace).setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    MarketplaceActivity.class
            );
            startActivity(intent);
        });
    }

    /**
     * Staff scan: attendance or pickup. Students and vendors never
     * reach here (their buttons stay gone).
     */
    private void showScanChooser() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("What to scan?")
                .setItems(new String[]{"Scan attendance", "Scan pickup"},
                        (dialog, which) -> {
                            Intent intent = new Intent(
                                    DashboardActivity.this,
                                    which == 0 ? ScanQRActivity.class
                                            : ScanPickupActivity.class);
                            startActivity(intent);
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Register in onStart (not onCreate) so the balance refreshes again
    // every time the user returns to this screen after onStop().
    @Override
    protected void onStart() {
        super.onStart();
        startListeningToStudentData();
        startNotificationListeners();
        EventReminderScheduler.scheduleDaily(this);
        EventReminderScheduler.checkRemindersNow(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        if (txListener != null) {
            txListener.remove();
            txListener = null;
        }
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
        if (itemListener != null) {
            itemListener.remove();
            itemListener = null;
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT
                < android.os.Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    1001);
        }
    }

    /**
     * Live listeners that turn Firestore changes into system notifications:
     * payments received, credits earned, and newly added events.
     * First-run snapshots only seed the seen-sets (no notification storm).
     */
    private void startNotificationListeners() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }
        notifyUserId = mAuth.getCurrentUser().getUid();
        final String userId = notifyUserId;
        final boolean firstRun =
                NotificationTracker.isFirstRun(this, userId);

        txListener = db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }
                    for (com.google.firebase.firestore.DocumentChange change
                            : snapshots.getDocumentChanges()) {
                        if (change.getType()
                                != com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            continue;
                        }
                        String txId = change.getDocument().getId();
                        boolean isNew = NotificationTracker.markSeenTx(
                                DashboardActivity.this, userId, txId);
                        if (firstRun || !isNew) {
                            continue;
                        }
                        CreditTransaction tx = CreditTransaction.fromDocument(
                                change.getDocument());
                        if (tx.isReceive()) {
                            NotificationHelper.notifyPaymentReceived(
                                    DashboardActivity.this,
                                    tx.getCredits() != null ? tx.getCredits() : 0L,
                                    tx.getCounterpartyEmail());
                        } else if (tx.isEarn()) {
                            NotificationHelper.notifyCreditsEarned(
                                    DashboardActivity.this,
                                    tx.getCredits() != null ? tx.getCredits() : 0L,
                                    tx.getEventTitle());
                        }
                    }
                });

        eventListener = db.collection("events")
                .limit(20)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }
                    for (com.google.firebase.firestore.DocumentChange change
                            : snapshots.getDocumentChanges()) {
                        if (change.getType()
                                != com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            continue;
                        }
                        String eventId = change.getDocument().getId();
                        boolean isNew = NotificationTracker.markSeenEvent(
                                DashboardActivity.this, userId, eventId);
                        if (firstRun || !isNew) {
                            continue;
                        }
                        String title = change.getDocument().getString("title");
                        Long credits = change.getDocument().getLong("credits");
                        NotificationHelper.notifyNewEvent(
                                DashboardActivity.this,
                                title != null ? title : "Campus Event",
                                credits != null ? credits : 0L);
                    }
                    NotificationTracker.markInitialized(
                            DashboardActivity.this, userId);
                });

        itemListener = db.collection("market_items")
                .limit(20)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }
                    for (com.google.firebase.firestore.DocumentChange change
                            : snapshots.getDocumentChanges()) {
                        if (change.getType()
                                != com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            continue;
                        }
                        String itemId = change.getDocument().getId();
                        boolean isNew = NotificationTracker.markSeenItem(
                                DashboardActivity.this, userId, itemId);
                        if (firstRun || !isNew) {
                            continue;
                        }
                        String title = change.getDocument().getString("title");
                        Long price = change.getDocument().getLong("price");
                        NotificationHelper.notifyNewItem(
                                DashboardActivity.this,
                                title != null ? title : "Reward",
                                price != null ? price : 0L);
                    }
                });
    }

    /**
     * Three home screens in one: students get wallet + actions,
     * staff additionally get scan, vendors get only scan.
     */
    private void applyRoleUI() {
        boolean student = !isOrganizer && !isVendor;

        btnScanQR.setVisibility(isOrganizer ? View.VISIBLE : View.GONE);
        // Rebalance the row: 3 columns for staff, 2 for students, and
        // the trailing gap only exists when Scan follows Receive.
        ((android.widget.LinearLayout) findViewById(R.id.layoutQuickActions))
                .setWeightSum(isOrganizer ? 3 : 2);
        android.view.ViewGroup.MarginLayoutParams receiveParams =
                (android.view.ViewGroup.MarginLayoutParams)
                        btnReceiveMoney.getLayoutParams();
        receiveParams.setMarginEnd(isOrganizer
                ? (int) (8 * getResources().getDisplayMetrics().density)
                : 0);
        btnReceiveMoney.setLayoutParams(receiveParams);
        findViewById(R.id.btnVendorScan).setVisibility(
                isVendor ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnVendorOrders).setVisibility(
                isVendor ? View.VISIBLE : View.GONE);
        findViewById(R.id.layoutQuickActions).setVisibility(
                isVendor ? View.GONE : View.VISIBLE);
        findViewById(R.id.tvQuickActionsLabel).setVisibility(
                isVendor ? View.GONE : View.VISIBLE);
        findViewById(R.id.cardWallet).setVisibility(
                isVendor ? View.GONE : View.VISIBLE);
        findViewById(R.id.tvEventsLabel).setVisibility(
                isVendor ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnEvents).setVisibility(
                isVendor ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnMarketplace).setVisibility(
                isVendor ? View.GONE : View.VISIBLE);
        findViewById(R.id.cardMyEvents).setVisibility(
                student ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnNotifications).setVisibility(
                isVendor ? View.GONE : View.VISIBLE);
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
                        isVendor = UserRole.isVendor(
                                documentSnapshot.getString("role")
                        );
                        applyRoleUI();

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