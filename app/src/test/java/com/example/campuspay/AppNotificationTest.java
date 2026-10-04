package com.example.campuspay;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Notification helpers decide when reminders fire, so the date parsing
 * and reminder-window rules are covered directly here.
 */
public class AppNotificationTest {

    @Test
    public void parsesSupportedDateFormats() {
        assertTrue(AppNotification.parseEventDateMillis("2026-10-20") > 0);
        assertTrue(AppNotification.parseEventDateMillis("20/10/2026") > 0);
        assertTrue(AppNotification.parseEventDateMillis("20-10-2026") > 0);
        assertTrue(AppNotification.parseEventDateMillis("20 Oct 2026, 02:30 PM") > 0);
        assertTrue(AppNotification.parseEventDateMillis("20 Oct 2026") > 0);
    }

    @Test
    public void rejectsUnparseableDates() {
        assertEquals(-1, AppNotification.parseEventDateMillis(null));
        assertEquals(-1, AppNotification.parseEventDateMillis(""));
        assertEquals(-1, AppNotification.parseEventDateMillis("tomorrow-ish"));
        assertEquals(-1, AppNotification.parseEventDateMillis("2026-13-40"));
    }

    @Test
    public void flagsUpcomingEventsForReminder() {
        long now = System.currentTimeMillis();
        long tomorrow = now + 24L * 3600 * 1000;
        long lastWeek = now - 7L * 24 * 3600 * 1000;
        long nextMonth = now + 30L * 24 * 3600 * 1000;
        assertTrue(AppNotification.isUpcomingReminder(tomorrow, now));
        assertFalse(AppNotification.isUpcomingReminder(lastWeek, now));
        assertFalse(AppNotification.isUpcomingReminder(nextMonth, now));
        assertFalse(AppNotification.isUpcomingReminder(-1, now));
    }

    @Test
    public void formatsRelativeTime() {
        long now = System.currentTimeMillis();
        assertEquals("Just now", AppNotification.relativeTime(now, now));
        assertEquals("5m ago",
                AppNotification.relativeTime(now - 5 * 60000L, now));
        assertEquals("2h ago",
                AppNotification.relativeTime(now - 2 * 3600 * 1000L, now));
        assertEquals("3d ago",
                AppNotification.relativeTime(now - 3L * 24 * 3600 * 1000, now));
    }
}
