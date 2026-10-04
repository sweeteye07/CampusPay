package com.example.campuspay;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * QR parsing decides which user id the organizer awards credits to, so the
 * prefix and user id format rules are covered directly here.
 */
public class QrPayloadTest {

    private static String repeat(char c, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(c);
        }
        return builder.toString();
    }

    @Test
    public void recognisesStudentPayload() {
        assertTrue(QrPayload.isStudentPayload(
                QrPayload.STUDENT_PREFIX + "abc123"));
    }

    @Test
    public void rejectsForeignPayload() {
        assertFalse(QrPayload.isStudentPayload("https://example.com"));
        assertFalse(QrPayload.isStudentPayload("CAMPUSPAY_EVENT:event1"));
        assertFalse(QrPayload.isStudentPayload("campuspay_student:abc123"));
        assertFalse(QrPayload.isStudentPayload(""));
        assertFalse(QrPayload.isStudentPayload(null));
    }

    @Test
    public void extractsUserIdFromPayload() {
        assertEquals("abc123", QrPayload.userIdFrom(
                QrPayload.STUDENT_PREFIX + "abc123"));
    }

    @Test
    public void acceptsFirebaseUserIdFormat() {
        assertTrue(QrPayload.isValidUserId("aBc123"));
        assertTrue(QrPayload.isValidUserId(repeat('a', 128)));
    }

    @Test
    public void rejectsInvalidUserIds() {
        assertFalse(QrPayload.isValidUserId(null));
        assertFalse(QrPayload.isValidUserId(""));
        assertFalse(QrPayload.isValidUserId("has space"));
        assertFalse(QrPayload.isValidUserId("../admin"));
        assertFalse(QrPayload.isValidUserId("emoji😀"));
        assertFalse(QrPayload.isValidUserId(repeat('a', 129)));
    }

    @Test
    public void recognisesPickupPayload() {
        assertTrue(QrPayload.isPickupPayload(
                QrPayload.PICKUP_PREFIX + "AbC123xYz9876543210"));
        assertFalse(QrPayload.isPickupPayload("https://example.com"));
        assertFalse(QrPayload.isPickupPayload(
                QrPayload.STUDENT_PREFIX + "abc123"));
        assertFalse(QrPayload.isPickupPayload(""));
        assertFalse(QrPayload.isPickupPayload(null));
    }

    @Test
    public void extractsRedemptionIdFromPayload() {
        assertEquals("AbC123xYz9876543210", QrPayload.redemptionIdFrom(
                QrPayload.PICKUP_PREFIX + "AbC123xYz9876543210"));
    }

    @Test
    public void validatesRedemptionIdFormat() {
        assertTrue(QrPayload.isValidRedemptionId("AbC123xYz9876543210"));
        assertFalse(QrPayload.isValidRedemptionId(null));
        assertFalse(QrPayload.isValidRedemptionId(""));
        assertFalse(QrPayload.isValidRedemptionId("has space"));
        assertFalse(QrPayload.isValidRedemptionId("../admin"));
        assertFalse(QrPayload.isValidRedemptionId(repeat('a', 65)));
    }
}