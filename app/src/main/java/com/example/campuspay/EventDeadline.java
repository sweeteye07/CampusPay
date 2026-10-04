package com.example.campuspay;

/**
 * Pure-Java helpers for event registration deadlines and capacity.
 * Deadlines are entered as "yyyy-MM-dd" (same as the event date) and
 * mean end-of-day in device local time. Free of Android/Firebase
 * imports so the rules can be covered by plain JVM unit tests.
 */
public final class EventDeadline {

    private EventDeadline() {
        // no instances
    }

    /**
     * Parses a "yyyy-MM-dd" deadline to end-of-day millis (23:59:59.999
     * local). Returns -1 when unparseable.
     */
    public static long parseEndOfDayMillis(String date) {
        if (date == null) {
            return -1;
        }
        String trimmed = date.trim();
        if (!trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return -1;
        }
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd",
                            java.util.Locale.US);
            sdf.setLenient(false);
            java.util.Date parsed = sdf.parse(trimmed);
            if (parsed == null) {
                return -1;
            }
            return parsed.getTime() + 24L * 3600 * 1000 - 1;
        } catch (java.text.ParseException e) {
            return -1;
        }
    }

    /** Formats deadline millis back to "yyyy-MM-dd" for display/editing. */
    public static String formatDate(long millis) {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd",
                        java.util.Locale.US);
        return sdf.format(new java.util.Date(millis));
    }

    /**
     * Formats millis to "20 Oct 2026, 02:30 PM" (device local time) for
     * event dates and deadlines shown in lists and picker fields.
     */
    public static String formatDateTime(long millis) {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a",
                        java.util.Locale.US);
        return sdf.format(new java.util.Date(millis));
    }

    /**
     * Combines picker values into millis. Month is 0-based (as returned
     * by DatePicker), hourOfDay is 0-23 (as returned by TimePicker).
     */
    public static long combineDateTimeMillis(int year, int month, int day,
            int hourOfDay, int minute) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.YEAR, year);
        cal.set(java.util.Calendar.MONTH, month);
        cal.set(java.util.Calendar.DAY_OF_MONTH, day);
        cal.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(java.util.Calendar.MINUTE, minute);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * True when registration is still open. Events without a deadline
     * (legacy docs, negative millis) stay open.
     */
    public static boolean isRegistrationOpen(long deadlineMillis, long nowMillis) {
        if (deadlineMillis < 0) {
            return true;
        }
        return nowMillis <= deadlineMillis;
    }

    /**
     * True when seats remain. Events without a capacity (negative)
     * have unlimited seats.
     */
    public static boolean hasSeatsLeft(long capacity, long registeredCount) {
        if (capacity < 0) {
            return true;
        }
        return registeredCount < capacity;
    }
}
