package com.example.campuspay;

/**
 * Pure-Java helpers for the profile "Your Activity" section.
 * Free of Android/Firebase imports so the math can be covered
 * by plain JVM unit tests.
 */
public final class ActivityStats {

    private ActivityStats() {
        // no instances
    }

    /** Attendance rate as 0-100, rounded. 0 when nothing registered. */
    public static int attendanceRate(int registered, int attended) {
        if (registered <= 0 || attended <= 0) {
            return 0;
        }
        int clamped = Math.min(attended, registered);
        return Math.round((clamped * 100f) / registered);
    }

    public static String attendanceLabel(int registered, int attended) {
        if (registered <= 0) {
            return "No events yet";
        }
        return attendanceRate(registered, attended) + "% attendance";
    }

    /** Net credits from ledger totals. */
    public static long netCredits(long earned, long spent) {
        return earned - spent;
    }
}
