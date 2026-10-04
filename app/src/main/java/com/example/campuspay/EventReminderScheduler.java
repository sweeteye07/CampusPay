package com.example.campuspay;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Event reminders without any server component:
 * - a daily AlarmManager alarm fires EventReminderReceiver,
 * - plus an immediate check on Dashboard start,
 * - both scan the user's registrations and notify for events
 *   happening within ~48h (see AppNotification.isUpcomingReminder).
 */
public final class EventReminderScheduler {

    private static final int REQUEST_CODE = 9001;

    private EventReminderScheduler() {
        // no instances
    }

    public static void scheduleDaily(Context context) {
        AlarmManager manager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        Intent intent = new Intent(context, EventReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pendingIntent);
        long interval = AlarmManager.INTERVAL_DAY;
        long triggerAt = SystemClock.elapsedRealtime() + interval;
        try {
            manager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt, interval, pendingIntent);
        } catch (SecurityException ignored) {
            // Exact-alarm permission missing on some OEMs; daily check
            // still runs on Dashboard start.
        }
    }

    /** Immediate reminder scan (called from Dashboard.onStart). */
    public static void checkRemindersNow(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("registrations")
                .whereEqualTo("userId", userId)
                .limit(50)
                .get()
                .addOnSuccessListener(regSnap -> {
                    if (regSnap.isEmpty()) {
                        return;
                    }
                    Map<String, String> titles = new HashMap<>();
                    for (DocumentSnapshot reg : regSnap.getDocuments()) {
                        if (reg.getString("eventId") != null) {
                            titles.put(reg.getString("eventId"),
                                    reg.getString("eventTitle"));
                        }
                    }
                    db.collection("events")
                            .limit(50)
                            .get()
                            .addOnSuccessListener(eventSnap -> {
                                long now = System.currentTimeMillis();
                                for (DocumentSnapshot event : eventSnap.getDocuments()) {
                                    if (!titles.containsKey(event.getId())) {
                                        continue;
                                    }
                                    String date = event.getString("date");
                                    long eventMillis =
                                            AppNotification.parseEventDateMillis(date);
                                    if (!AppNotification.isUpcomingReminder(
                                            eventMillis, now)) {
                                        continue;
                                    }
                                    if (NotificationTracker.alreadyReminded(
                                            context, userId, event.getId())) {
                                        continue;
                                    }
                                    NotificationTracker.markReminded(
                                            context, userId, event.getId());
                                    String title = event.getString("title");
                                    if (title == null) {
                                        title = titles.get(event.getId());
                                    }
                                    NotificationHelper.notifyEventReminder(
                                            context,
                                            title != null ? title : "Campus Event",
                                            date != null ? date : "soon");
                                }
                            });
                });
    }
}
