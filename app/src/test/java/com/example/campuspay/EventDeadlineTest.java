package com.example.campuspay;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventDeadlineTest {

    @Test
    public void parsesDeadlineToEndOfDay() {
        long millis = EventDeadline.parseEndOfDayMillis("2026-10-20");
        assertTrue(millis > 0);
        assertEquals("2026-10-20", EventDeadline.formatDate(millis));
        // End of day: next calendar day at 00:00 minus 1ms.
        assertEquals("2026-10-20",
                EventDeadline.formatDate(millis - 1000));
    }

    @Test
    public void rejectsBadDeadlineInput() {
        assertEquals(-1, EventDeadline.parseEndOfDayMillis(null));
        assertEquals(-1, EventDeadline.parseEndOfDayMillis(""));
        assertEquals(-1, EventDeadline.parseEndOfDayMillis("20/10/2026"));
        assertEquals(-1, EventDeadline.parseEndOfDayMillis("2026-13-40"));
    }

    @Test
    public void openBeforeDeadlineClosedAfter() {
        long deadline = EventDeadline.parseEndOfDayMillis("2026-10-20");
        long noon = EventDeadline.parseEndOfDayMillis("2026-10-20")
                - 12L * 3600 * 1000;
        long nextDay = deadline + 1;
        assertTrue(EventDeadline.isRegistrationOpen(deadline, noon));
        assertTrue(EventDeadline.isRegistrationOpen(deadline, deadline));
        assertFalse(EventDeadline.isRegistrationOpen(deadline, nextDay));
    }

    @Test
    public void formatsDateTimeWithAmPm() {
        long millis = EventDeadline.combineDateTimeMillis(
                2026, 9, 20, 14, 30);
        assertEquals("20 Oct 2026, 02:30 PM",
                EventDeadline.formatDateTime(millis));
        long morning = EventDeadline.combineDateTimeMillis(
                2026, 9, 20, 9, 5);
        assertEquals("20 Oct 2026, 09:05 AM",
                EventDeadline.formatDateTime(morning));
    }

    @Test
    public void legacyEventsWithoutDeadlineStayOpen() {
        assertTrue(EventDeadline.isRegistrationOpen(-1,
                System.currentTimeMillis()));
    }

    @Test
    public void capacityGate() {
        assertTrue(EventDeadline.hasSeatsLeft(50, 12));
        assertFalse(EventDeadline.hasSeatsLeft(50, 50));
        assertFalse(EventDeadline.hasSeatsLeft(50, 60));
        // Negative capacity = unlimited (legacy events).
        assertTrue(EventDeadline.hasSeatsLeft(-1, 9999));
    }
}
