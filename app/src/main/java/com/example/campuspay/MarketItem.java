package com.example.campuspay;

import com.google.firebase.firestore.DocumentSnapshot;

public class MarketItem {

    private final String id;
    private final String title;
    private final String description;
    private final Long price;
    private final Long stock;
    private final Boolean active;
    private final String createdBy;

    public MarketItem(
            String id,
            String title,
            String description,
            Long price,
            Long stock,
            Boolean active,
            String createdBy
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.active = active;
        this.createdBy = createdBy;
    }

    public static MarketItem fromDocument(DocumentSnapshot document) {
        return new MarketItem(
                document.getId(),
                document.getString("title"),
                document.getString("description"),
                document.getLong("price"),
                document.getLong("stock"),
                document.getBoolean("active"),
                document.getString("createdBy")
        );
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

    public Long getPrice() {
        return price;
    }

    public Long getStock() {
        return stock;
    }

    public Boolean getActive() {
        return active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public boolean isActive() {
        return active == null || active;
    }

    public boolean isInStock() {
        return isActive() && stock != null && stock > 0;
    }
}
