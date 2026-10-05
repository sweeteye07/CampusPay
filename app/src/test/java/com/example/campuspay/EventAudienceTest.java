package com.example.campuspay;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventAudienceTest {

    @Test
    public void acceptsKnownTags() {
        assertTrue(EventAudience.isValidTag(EventAudience.TAG_MUST_JOIN));
        assertTrue(EventAudience.isValidTag(EventAudience.TAG_HIGHLY_SUGGESTED));
        assertTrue(EventAudience.isValidTag(EventAudience.TAG_OPEN_FOR_ALL));
        assertFalse(EventAudience.isValidTag("vip_only"));
        assertFalse(EventAudience.isValidTag(null));
        assertFalse(EventAudience.isValidTag(""));
    }

    @Test
    public void labelsTags() {
        assertEquals("Must Join",
                EventAudience.labelFor(EventAudience.TAG_MUST_JOIN));
        assertEquals("Highly Suggested",
                EventAudience.labelFor(EventAudience.TAG_HIGHLY_SUGGESTED));
        assertEquals("Open for All",
                EventAudience.labelFor(EventAudience.TAG_OPEN_FOR_ALL));
        assertEquals("Open for All", EventAudience.labelFor("unknown"));
    }

    @Test
    public void departmentsIncludeOpenForAll() {
        assertTrue(EventAudience.departments()
                .contains(EventAudience.DEPARTMENT_ALL));
        assertTrue(EventAudience.isValidDepartment("CSE"));
        assertFalse(EventAudience.isValidDepartment("Hogwarts"));
        assertFalse(EventAudience.isValidDepartment(null));
    }
}
