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
}