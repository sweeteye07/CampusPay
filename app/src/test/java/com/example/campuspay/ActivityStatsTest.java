package com.example.campuspay;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ActivityStatsTest {

    @Test
    public void fullAttendanceIs100() {
        assertEquals(100, ActivityStats.attendanceRate(4, 4));
    }

    @Test
    public void partialAttendanceRounds() {
        assertEquals(67, ActivityStats.attendanceRate(3, 2));
        assertEquals(50, ActivityStats.attendanceRate(2, 1));
    }

    @Test
    public void noRegistrationsIsZero() {
        assertEquals(0, ActivityStats.attendanceRate(0, 0));
        assertEquals("No events yet", ActivityStats.attendanceLabel(0, 0));
    }

    @Test
    public void clampsAttendedAboveRegistered() {
        assertEquals(100, ActivityStats.attendanceRate(2, 5));
    }

    @Test
    public void netCreditsSubtractsSpent() {
        assertEquals(150L, ActivityStats.netCredits(200L, 50L));
    }
}
