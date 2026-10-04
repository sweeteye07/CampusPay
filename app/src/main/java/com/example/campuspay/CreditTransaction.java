package com.example.campuspay;

import com.google.firebase.firestore.DocumentSnapshot;

public class CreditTransaction {

    private final String id;
    private final String type;
    private final String description;
    private final String eventTitle;
    private final Long credits;
    private final com.google.firebase.Timestamp createdAt;

    public CreditTransaction(
            String id,
            String type,
            String description,
            String eventTitle,
            Long credits,
            com.google.firebase.Timestamp createdAt
    ) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.eventTitle = eventTitle;
        this.credits = credits;
        this.createdAt = createdAt;
    }

    public static CreditTransaction fromDocument(DocumentSnapshot document) {
        return new CreditTransaction(
                document.getId(),
                document.getString("type"),
                document.getString("description"),
                document.getString("eventTitle"),
                document.getLong("credits"),
                document.getTimestamp("createdAt")
        );
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public Long getCredits() {
        return credits;
    }

    public com.google.firebase.Timestamp getCreatedAt() {
        return createdAt;
    }

    public boolean isEarn() {
        return "earn".equals(type);
    }
}
