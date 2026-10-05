package com.example.campuspay;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The role check decides which organizer tools are offered, so it is
 * covered directly here.
 */
public class UserRoleTest {

    @Test
    public void adminIsOrganizer() {
        assertTrue(UserRole.isOrganizer("admin"));
    }

    @Test
    public void organizerIsOrganizer() {
        assertTrue(UserRole.isOrganizer("organizer"));
    }

    @Test
    public void roleMatchIgnoresCase() {
        assertTrue(UserRole.isOrganizer("Admin"));
        assertTrue(UserRole.isOrganizer("ORGANIZER"));
        assertTrue(UserRole.isOrganizer("oRgAnIzEr"));
    }

    @Test
    public void studentIsNotOrganizer() {
        assertFalse(UserRole.isOrganizer("student"));
        assertFalse(UserRole.isOrganizer("Student"));
    }

    @Test
    public void missingOrUnknownRoleIsNotOrganizer() {
        assertFalse(UserRole.isOrganizer(null));
        assertFalse(UserRole.isOrganizer(""));
        assertFalse(UserRole.isOrganizer("   "));
    }

    @Test
    public void similarWordsAreNotTreatedAsOrganizer() {
        assertFalse(UserRole.isOrganizer("administrator"));
        assertFalse(UserRole.isOrganizer("admin2"));
        assertFalse(UserRole.isOrganizer("superadmin"));
    }

    @Test
    public void adminHasAllPowers() {
        assertTrue(UserRole.isAdmin("admin"));
        assertTrue(UserRole.isAdmin("Admin"));
        assertTrue(UserRole.isOrganizer("admin"));
        assertFalse(UserRole.isAdmin("staff"));
        assertFalse(UserRole.isAdmin("organizer"));
        assertFalse(UserRole.isAdmin("vendor"));
        assertFalse(UserRole.isAdmin(null));
    }

    @Test
    public void staffStringsAreStaffAndOrganizers() {
        assertTrue(UserRole.isStaff("staff"));
        assertTrue(UserRole.isStaff("organizer"));
        assertTrue(UserRole.isOrganizer("staff"));
        assertTrue(UserRole.isOrganizer("organizer"));
        assertFalse(UserRole.isStaff("admin"));
        assertFalse(UserRole.isStaff("vendor"));
        assertFalse(UserRole.isStaff(null));
    }

    @Test
    public void vendorIsOnlyVendor() {
        assertTrue(UserRole.isVendor("vendor"));
        assertTrue(UserRole.isVendor("Vendor"));
        assertFalse(UserRole.isVendor("student"));
        assertFalse(UserRole.isVendor("admin"));
        assertFalse(UserRole.isVendor(null));
        assertFalse(UserRole.isVendor(""));
    }

    @Test
    public void vendorsAreNotOrganizers() {
        assertFalse(UserRole.isOrganizer("vendor"));
    }

    @Test
    public void onlyStudentsAreStudents() {
        assertTrue(UserRole.isStudent("student"));
        assertTrue(UserRole.isStudent(null));
        assertFalse(UserRole.isStudent("admin"));
        assertFalse(UserRole.isStudent("organizer"));
        assertFalse(UserRole.isStudent("vendor"));
    }
}