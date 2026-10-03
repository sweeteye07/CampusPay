package com.example.campuspay;

public class Event {

    private final String id;
    private final String title;
    private final String description;
    private final Long credits;
    private final String date;

    private boolean registered;

    public Event(
            String id,
            String title,
            String description,
            Long credits,
            String date
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.credits = credits;
        this.date = date;
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

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
}
