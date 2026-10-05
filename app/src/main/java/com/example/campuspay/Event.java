package com.example.campuspay;

public class Event {

    private final String id;
    private final String title;
    private final String description;
    private final Long credits;
    private final String date;
    private final String createdBy;
    private final Long deadlineMillis;
    private final Long capacity;
    private final Long registeredCount;
    private final String department;
    private final String audienceTag;
    private final String eligibilityNote;

    private boolean registered;

    public Event(
            String id,
            String title,
            String description,
            Long credits,
            String date
    ) {
        this(id, title, description, credits, date, null);
    }

    public Event(
            String id,
            String title,
            String description,
            Long credits,
            String date,
            String createdBy
    ) {
        this(id, title, description, credits, date, createdBy,
                null, null, null);
    }

    public Event(
            String id,
            String title,
            String description,
            Long credits,
            String date,
            String createdBy,
            Long deadlineMillis,
            Long capacity,
            Long registeredCount
    ) {
        this(id, title, description, credits, date, createdBy,
                deadlineMillis, capacity, registeredCount,
                null, null, null);
    }

    public Event(
            String id,
            String title,
            String description,
            Long credits,
            String date,
            String createdBy,
            Long deadlineMillis,
            Long capacity,
            Long registeredCount,
            String department,
            String audienceTag,
            String eligibilityNote
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.credits = credits;
        this.date = date;
        this.createdBy = createdBy;
        this.deadlineMillis = deadlineMillis;
        this.capacity = capacity;
        this.registeredCount = registeredCount;
        this.department = department;
        this.audienceTag = audienceTag;
        this.eligibilityNote = eligibilityNote;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Long getCredits() {
        return credits;
    }

    public String getDate() {
        return date;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    /** Deadline as millis, or null for legacy events without one. */
    public Long getDeadlineMillis() {
        return deadlineMillis;
    }

    /** Max seats, or null for legacy events without a cap. */
    public Long getCapacity() {
        return capacity;
    }

    public long getRegisteredCount() {
        return registeredCount != null ? registeredCount : 0L;
    }

    public String getDepartment() {
        return department;
    }

    public String getAudienceTag() {
        return audienceTag;
    }

    public String getEligibilityNote() {
        return eligibilityNote;
    }

    public boolean isRegistrationOpen() {
        long deadline = deadlineMillis != null ? deadlineMillis : -1L;
        long cap = capacity != null ? capacity : -1L;
        return EventDeadline.isRegistrationOpen(deadline,
                        System.currentTimeMillis())
                && EventDeadline.hasSeatsLeft(cap, getRegisteredCount());
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
}
