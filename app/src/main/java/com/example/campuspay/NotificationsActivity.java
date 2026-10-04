package com.example.campuspay;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-app Notification Center. Merges six sources into one timeline:
 * payments received, credits earned, rewards redeemed, new events,
 * new marketplace rewards, and event reminders.
 * Derived from existing collections — no new Firestore rules needed.
 */
public class NotificationsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private NotificationsAdapter adapter;
    private final List<AppNotification> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.toolbarNotifications);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.notificationsRecyclerView);
        tvEmpty = findViewById(R.id.tvNotificationsEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationsAdapter(items,
                n -> startActivity(NotificationsAdapter.targetIntent(this, n)));
        recyclerView.setAdapter(adapter);

        loadAll();
    }

    private void loadAll() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        // 1) Wallet activity: payments received, credits earned, rewards redeemed.
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(txSnap -> {
                    List<AppNotification> built = new ArrayList<>();
                    for (DocumentSnapshot doc : txSnap.getDocuments()) {
                        CreditTransaction tx = CreditTransaction.fromDocument(doc);
                        Timestamp ts = tx.getCreatedAt();
                        long millis = ts != null ? ts.toDate().getTime()
                                : System.currentTimeMillis();
                        if (tx.isReceive()) {
                            built.add(new AppNotification(
                                    "tx_" + doc.getId(),
                                    AppNotification.TYPE_PAYMENT_RECEIVED,
                                    "Payment received",
                                    "Received " + tx.getCredits() + " credits"
                                            + (tx.getCounterpartyEmail() != null
                                            ? " from " + tx.getCounterpartyEmail() : "")
                                            + ".",
                                    millis));
                        } else if (tx.isEarn()) {
                            built.add(new AppNotification(
                                    "tx_" + doc.getId(),
                                    AppNotification.TYPE_CREDITS_EARNED,
                                    "Credits earned",
                                    "Earned " + tx.getCredits() + " credits"
                                            + (tx.getEventTitle() != null
                                            ? " for " + tx.getEventTitle() : "")
                                            + ".",
                                    millis));
                        } else if (tx.isSpend()) {
                            String reward = tx.getItemTitle() != null
                                    ? tx.getItemTitle() : "a reward";
                            built.add(new AppNotification(
                                    "tx_" + doc.getId(),
                                    AppNotification.TYPE_REWARD_REDEEMED,
                                    "Reward redeemed",
                                    "Redeemed " + reward
                                            + " for " + tx.getCredits() + " credits.",
                                    millis));
                        }
                    }
                    loadEvents(userId, built);
                })
                .addOnFailureListener(e -> loadEvents(userId, new ArrayList<>()));
    }

    private void loadEvents(String userId, List<AppNotification> acc) {
        db.collection("events")
                .limit(20)
                .get()
                .addOnSuccessListener(eventSnap -> {
                    Map<String, DocumentSnapshot> byId = new HashMap<>();
                    for (DocumentSnapshot doc : eventSnap.getDocuments()) {
                        byId.put(doc.getId(), doc);
                        Timestamp createdAt = doc.getTimestamp("createdAt");
                        long millis = createdAt != null ? createdAt.toDate().getTime()
                                : System.currentTimeMillis();
                        String title = doc.getString("title");
                        Long credits = doc.getLong("credits");
                        acc.add(new AppNotification(
                                "event_" + doc.getId(),
                                AppNotification.TYPE_NEW_EVENT,
                                "New event: " + (title != null ? title : "Campus Event"),
                                "+" + (credits != null ? credits : 0)
                                        + " pts — open Campus Events to register.",
                                millis));
                    }
                    loadItems(userId, acc, byId);
                })
                .addOnFailureListener(e -> loadItems(userId, acc, new HashMap<>()));
    }

    private void loadItems(String userId, List<AppNotification> acc,
            Map<String, DocumentSnapshot> eventsById) {
        db.collection("market_items")
                .limit(10)
                .get()
                .addOnSuccessListener(itemSnap -> {
                    for (DocumentSnapshot doc : itemSnap.getDocuments()) {
                        Boolean active = doc.getBoolean("active");
                        if (active != null && !active) {
                            continue;
                        }
                        Timestamp createdAt = doc.getTimestamp("createdAt");
                        long millis = createdAt != null ? createdAt.toDate().getTime()
                                : System.currentTimeMillis();
                        String title = doc.getString("title");
                        Long price = doc.getLong("price");
                        acc.add(new AppNotification(
                                "item_" + doc.getId(),
                                AppNotification.TYPE_NEW_ITEM,
                                "New reward: " + (title != null ? title : "Reward"),
                                (price != null ? price : 0)
                                        + " pts — open Campus Marketplace to redeem.",
                                millis));
                    }
                    loadReminders(userId, acc, eventsById);
                })
                .addOnFailureListener(e -> loadReminders(userId, acc, eventsById));
    }

    private void loadReminders(
            String userId, List<AppNotification> acc,
            Map<String, DocumentSnapshot> eventsById) {
        db.collection("registrations")
                .whereEqualTo("userId", userId)
                .limit(50)
                .get()
                .addOnSuccessListener(regSnap -> {
                    long now = System.currentTimeMillis();
                    for (DocumentSnapshot reg : regSnap.getDocuments()) {
                        String eventId = reg.getString("eventId");
                        String title = reg.getString("eventTitle");
                        if (eventId == null) {
                            continue;
                        }
                        DocumentSnapshot event = eventsById.get(eventId);
                        String date = event != null ? event.getString("date") : null;
                        // Fall back to registration title when event doc is missing.
                        if (title == null) {
                            title = event != null ? event.getString("title") : "Campus Event";
                        }
                        if (date == null) {
                            continue;
                        }
                        long eventMillis = AppNotification.parseEventDateMillis(date);
                        if (AppNotification.isUpcomingReminder(eventMillis, now)) {
                            acc.add(new AppNotification(
                                    "reminder_" + eventId,
                                    AppNotification.TYPE_EVENT_REMINDER,
                                    "Reminder: " + title,
                                    title + " is on " + date + ". Don't miss it!",
                                    eventMillis));
                        }
                    }
                    render(acc);
                })
                .addOnFailureListener(e -> render(acc));
    }

    private void render(List<AppNotification> acc) {
        items.clear();
        items.addAll(acc);
        Collections.sort(items,
                (a, b) -> Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));
        adapter.notifyDataSetChanged();
        boolean empty = items.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
