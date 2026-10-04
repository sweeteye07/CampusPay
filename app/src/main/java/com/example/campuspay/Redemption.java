package com.example.campuspay;

import com.google.firebase.firestore.DocumentSnapshot;

public class Redemption {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_HANDED = "handed";

    private final String id;
    private final String userId;
    private final String itemId;
    private final String itemTitle;
    private final Long price;
    private final String status;
    private final com.google.firebase.Timestamp redeemedAt;

    public Redemption(
            String id,
            String userId,
            String itemId,
            String itemTitle,
            Long price,
            String status,
            com.google.firebase.Timestamp redeemedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.price = price;
        this.status = status;
        this.redeemedAt = redeemedAt;
    }

    public static Redemption fromDocument(DocumentSnapshot document) {
        return new Redemption(
                document.getId(),
                document.getString("userId"),
                document.getString("itemId"),
                document.getString("itemTitle"),
                document.getLong("price"),
                document.getString("status"),
                document.getTimestamp("redeemedAt")
        );
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public Long getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public com.google.firebase.Timestamp getRedeemedAt() {
        return redeemedAt;
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    /** QR payload the organizer scans at pickup. */
    public String pickupPayload() {
        return QrPayload.PICKUP_PREFIX + id;
    }
}
