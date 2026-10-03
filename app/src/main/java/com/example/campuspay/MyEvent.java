package com.example.campuspay;

public class MyEvent {

    private final String eventId;
    private final String title;
    private final String date;
    private final Long credits;
    private final boolean attended;

    public MyEvent(
            String eventId,
            String title,
            String date,
            Long credits,
            boolean attended
    ) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.credits = credits;
        this.attended = attended;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public Long getCredits() {
        return credits;
    }

    public boolean isAttended() {
        return attended;
    }
}
