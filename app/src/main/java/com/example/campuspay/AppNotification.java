package com.example.campuspay;

/**
 * Pure-Java notification model for the in-app Notification Center.
 * Free of Android imports so it can be unit-tested on the JVM.
 */
public class AppNotification {

    public static final String TYPE_PAYMENT_RECEIVED = "payment_received";
    public static final String TYPE_CREDITS_EARNED = "credits_earned";
    public static final String TYPE_NEW_EVENT = "new_event";
    public static final String TYPE_EVENT_REMINDER = "event_reminder";
    public static final String TYPE_REWARD_REDEEMED = "reward_redeemed";
    public static final String TYPE_NEW_ITEM = "new_item";

    private final String id;
    private final String type;
    private final String title;
    private final String message;
    private final long timestampMillis;

    public AppNotification(
            String id,
            String type,
            String title,
            String message,
            long timestampMillis
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.timestampMillis = timestampMillis;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    /** Human-readable relative time, e.g. "2h ago". Testable on JVM. */
    public static String relativeTime(long timestampMillis, long nowMillis) {
        long diff = Math.max(0, nowMillis - timestampMillis);
        long minutes = diff / 60000L;
        if (minutes < 1) {
            return "Just now";
        }
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h ago";
        }
        long days = hours / 24;
        if (days < 7) {
            return days + "d ago";
        }
        return (days / 7) + "w ago";
    }

    /**
     * Best-effort event date parsing. Accepts yyyy-MM-dd, dd/MM/yyyy,
     * dd-MM-yyyy, the new picker format "20 Oct 2026, 02:30 PM" and
     * plain "20 Oct 2026", falling back to -1 when unparseable.
     */
    public static long parseEventDateMillis(String date) {
        if (date == null) {
            return -1;
        }
        String trimmed = date.trim();
        if (trimmed.isEmpty()) {
            return -1;
        }
        String[] patterns = {"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy",
                "dd MMM yyyy, hh:mm a", "dd MMM yyyy"};
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat(pattern, java.util.Locale.US);
                sdf.setLenient(false);
                java.util.Date parsed = sdf.parse(trimmed);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (java.text.ParseException ignored) {
                // try next pattern
            }
        }
        return -1;
    }

    /** True when the event starts within the next 48h (reminder window). */
    public static boolean isUpcomingReminder(long eventMillis, long nowMillis) {
        if (eventMillis < 0) {
            return false;
        }
        long diff = eventMillis - nowMillis;
        return diff >= -24L * 3600 * 1000 && diff <= 48L * 3600 * 1000;
    }
}
