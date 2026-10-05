package com.example.campuspay;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Department targeting and audience signalling for campus events.
 * Pure Java so labels and validation can be covered by JVM unit tests.
 * Eligibility is advisory (shown to students), not enforced — user
 * profiles carry no department field.
 */
public final class EventAudience {

    public static final String TAG_MUST_JOIN = "must_join";
    public static final String TAG_HIGHLY_SUGGESTED = "highly_suggested";
    public static final String TAG_OPEN_FOR_ALL = "open_for_all";

    public static final String DEPARTMENT_ALL = "All Departments";

    private static final List<String> DEPARTMENTS;

    static {
        DEPARTMENTS = Collections.unmodifiableList(Arrays.asList(
                DEPARTMENT_ALL,
                "CSE",
                "IT",
                "ECE",
                "EEE",
                "ME",
                "CE",
                "BCA",
                "MCA",
                "BBA",
                "MBA"
        ));
    }

    private EventAudience() {
        // no instances
    }

    public static List<String> departments() {
        return DEPARTMENTS;
    }

    public static boolean isValidTag(String tag) {
        return TAG_MUST_JOIN.equals(tag)
                || TAG_HIGHLY_SUGGESTED.equals(tag)
                || TAG_OPEN_FOR_ALL.equals(tag);
    }

    public static boolean isValidDepartment(String department) {
        return department != null && DEPARTMENTS.contains(department);
    }

    public static String labelFor(String tag) {
        if (TAG_MUST_JOIN.equals(tag)) {
            return "Must Join";
        }
        if (TAG_HIGHLY_SUGGESTED.equals(tag)) {
            return "Highly Suggested";
        }
        return "Open for All";
    }
}
