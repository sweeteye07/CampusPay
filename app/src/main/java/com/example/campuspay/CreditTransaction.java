package com.example.campuspay;

import com.google.firebase.firestore.DocumentSnapshot;

public class CreditTransaction {

    private final String id;
    private final String type;
    private final String description;
    private final String eventTitle;
    private final Long credits;
    private final String counterpartyEmail;
    private final String direction;
    private final com.google.firebase.Timestamp createdAt;

    public CreditTransaction(
            String id,
            String type,
            String description,
            String eventTitle,
            Long credits,
            String counterpartyEmail,
            String direction,
            com.google.firebase.Timestamp createdAt
    ) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.eventTitle = eventTitle;
        this.credits = credits;
        this.counterpartyEmail = counterpartyEmail;
        this.direction = direction;
        this.createdAt = createdAt;
    }

    public static CreditTransaction fromDocument(DocumentSnapshot document) {
        return new CreditTransaction(
                document.getId(),
                document.getString("type"),
                document.getString("description"),
                document.getString("eventTitle"),
                document.getLong("credits"),
                document.getString("counterpartyEmail"),
                document.getString("direction"),
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

    public String getCounterpartyEmail() {
        return counterpartyEmail;
    }

    public String getDirection() {
        return direction;
    }

    public com.google.firebase.Timestamp getCreatedAt() {
        return createdAt;
    }

    public boolean isEarn() {
        return "earn".equals(type);
    }

    public boolean isSend() {
        return "transfer".equals(type) && "out".equals(direction);
    }

    public boolean isReceive() {
        return "transfer".equals(type) && "in".equals(direction);
    }

    public boolean isOutgoing() {
        return isSend() || ("spend".equals(type));
    }
}
