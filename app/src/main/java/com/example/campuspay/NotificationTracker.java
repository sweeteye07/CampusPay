package com.example.campuspay;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which transaction/event IDs already triggered a system
 * notification, so Firestore listeners don't re-notify on every
 * resume. Backed by SharedPreferences (per-user keys).
 */
public final class NotificationTracker {

    private NotificationTracker() {
        // no instances
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(
                "campuspay_notifications", Context.MODE_PRIVATE);
    }

    private static String key(String userId, String name) {
        return userId + "_" + name;
    }

    public static boolean isFirstRun(Context context, String userId) {
        return !prefs(context).contains(key(userId, "initialized"));
    }

    public static void markInitialized(Context context, String userId) {
        prefs(context).edit().putBoolean(key(userId, "initialized"), true).apply();
    }

    /** Returns true if this id was NOT seen before (and marks it seen). */
    public static boolean markSeenTx(Context context, String userId, String txId) {
        return markSeen(context, key(userId, "seen_tx"), txId);
    }

    /** Returns true if this id was NOT seen before (and marks it seen). */
    public static boolean markSeenEvent(Context context, String userId, String eventId) {
        return markSeen(context, key(userId, "seen_events"), eventId);
    }

    /** Returns true if this id was NOT seen before (and marks it seen). */
    public static boolean markSeenItem(Context context, String userId, String itemId) {
        return markSeen(context, key(userId, "seen_items"), itemId);
    }

    public static boolean alreadyReminded(
            Context context, String userId, String eventId) {
        return prefs(context).getStringSet(key(userId, "reminded"), new HashSet<>())
                .contains(eventId);
    }

    public static void markReminded(Context context, String userId, String eventId) {
        SharedPreferences p = prefs(context);
        Set<String> copy = new HashSet<>(
                p.getStringSet(key(userId, "reminded"), new HashSet<>()));
        copy.add(eventId);
        p.edit().putStringSet(key(userId, "reminded"), copy).apply();
    }

    private static boolean markSeen(Context context, String setKey, String id) {
        SharedPreferences p = context.getSharedPreferences(
                "campuspay_notifications", Context.MODE_PRIVATE);
        Set<String> copy = new HashSet<>(p.getStringSet(setKey, new HashSet<>()));
        // Cap growth: keep at most ~200 ids.
        if (copy.size() > 200) {
            copy.clear();
        }
        boolean isNew = !copy.contains(id);
        copy.add(id);
        p.edit().putStringSet(setKey, copy).apply();
        return isNew;
    }
}
