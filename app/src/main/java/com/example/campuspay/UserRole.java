package com.example.campuspay;

/**
 * Shared role check used by the dashboard, the profile screen and the
 * attendance scanner, so a user is treated the same way everywhere.
 *
 * An organizer is a user whose Firestore role field is "admin" or
 * "organizer" (compared case-insensitively). Students have role
 * "student" and can only show their own QR.
 */
public final class UserRole {

    private UserRole() {
        // no instances
    }

    public static boolean isOrganizer(String role) {

        if (role == null) {
            return false;
        }

        return role.equalsIgnoreCase("admin")
                || role.equalsIgnoreCase("organizer");
    }
}
