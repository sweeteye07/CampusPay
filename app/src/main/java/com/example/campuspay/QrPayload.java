package com.example.campuspay;

/**
 * Helpers for CampusPay QR payloads:
 * student identity (CAMPUSPAY_STUDENT:&lt;userId&gt;) and marketplace
 * pickup codes (CAMPUSPAY_PICKUP:&lt;redemptionId&gt;).
 *
 * This class is deliberately free of Android and Firebase imports so the
 * parsing rules can be covered by plain JVM unit tests.
 */
public final class QrPayload {

    public static final String STUDENT_PREFIX = "CAMPUSPAY_STUDENT:";
    public static final String PICKUP_PREFIX = "CAMPUSPAY_PICKUP:";

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

    /** True when the scanned text is a marketplace pickup QR. */
    public static boolean isPickupPayload(String data) {
        return data != null && data.startsWith(PICKUP_PREFIX);
    }

    /** Extracts the redemption id from a pickup QR payload. */
    public static String redemptionIdFrom(String data) {
        return data.substring(PICKUP_PREFIX.length());
    }

    /** Firestore auto-IDs are 20 chars of [A-Za-z0-9]; allow a margin. */
    public static boolean isValidRedemptionId(String redemptionId) {
        return redemptionId != null
                && redemptionId.matches("[A-Za-z0-9_-]{1,64}");
    }
}