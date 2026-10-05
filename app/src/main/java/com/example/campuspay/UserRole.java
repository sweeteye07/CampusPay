package com.example.campuspay;

/**
 * Shared role check used across the app so a user is treated the same
 * way everywhere.
 *
 * Four roles exist:
 * - student: send/receive credits, register for events, redeem rewards.
 * - staff ("staff" or legacy "organizer"): everything a student does,
 *   plus event/item management and QR scanning for their own listings.
 * - vendor ("vendor"): shop counter — scan pickup QRs only.
 * - admin ("admin"): all powers unlocked — every screen, every action,
 *   ownership checks bypassed.
 */
public final class UserRole {

    private UserRole() {
        // no instances
    }

    public static boolean isAdmin(String role) {
        return role != null && role.equalsIgnoreCase("admin");
    }

    public static boolean isStaff(String role) {
        return role != null
                && (role.equalsIgnoreCase("staff")
                        || role.equalsIgnoreCase("organizer"));
    }

    /** Anyone with organizer powers: staff plus admins. */
    public static boolean isOrganizer(String role) {
        return isAdmin(role) || isStaff(role);
    }

    public static boolean isVendor(String role) {
        return role != null && role.equalsIgnoreCase("vendor");
    }

    /** Students are everyone who is neither staff nor vendor. */
    public static boolean isStudent(String role) {
        return !isOrganizer(role) && !isVendor(role);
    }
}
