package com.example.campuspay;

/**
 * Helpers for the student QR payload, formatted as
 * CAMPUSPAY_STUDENT:<userId>.
 *
 * This class is deliberately free of Android and Firebase imports so the
 * parsing rules can be covered by plain JVM unit tests.
 */
public final class QrPayload {

    public static final String STUDENT_PREFIX = "CAMPUSPAY_STUDENT:";

    private QrPayload() {
        // no instances
    }

    /** True when the scanned text is a CampusPay student QR. */
    public static boolean isStudentPayload(String data) {
        return data != null && data.startsWith(STUDENT_PREFIX);
    }

    /** Extracts the user id from a student QR payload. */
    public static String userIdFrom(String data) {
        return data.substring(STUDENT_PREFIX.length());
    }

    /** Firebase UIDs are alphanumeric and at most 128 characters. */
    public static boolean isValidUserId(String userId) {
        return userId != null && userId.matches("[A-Za-z0-9]{1,128}");
    }
}