package com.example.campuspay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Canteen-style quick-add catalog for the marketplace listing form.
 * Tapping a preset pre-fills name, description and a suggested price —
 * the vendor can still edit everything before publishing.
 * Pure Java so the catalog invariants are unit-testable.
 */
public final class MarketCatalog {

    public static final class Template {
        public final String name;
        public final String description;
        public final long suggestedPrice;

        Template(String name, String description, long suggestedPrice) {
            this.name = name;
            this.description = description;
            this.suggestedPrice = suggestedPrice;
        }
    }

    private static final List<Template> ITEMS;

    static {
        List<Template> items = new ArrayList<>();
        items.add(new Template("Tea",
                "Hot Cutting chai from the campus canteen.", 10));
        items.add(new Template("Coffee",
                "Fresh filter coffee from the campus canteen.", 15));
        items.add(new Template("Samosa",
                "Crispy punjabi samosa (1 pc).", 15));
        items.add(new Template("Veg Patties",
                "Flaky baked veg patties (1 pc).", 20));
        items.add(new Template("Biscuits",
                "Assorted biscuit pack.", 15));
        items.add(new Template("Chips",
                "Potato chips packet.", 20));
        items.add(new Template("Cold Drink",
                "Chilled soft drink (250 ml).", 25));
        items.add(new Template("Maggi",
                "Masala Maggi bowl.", 30));
        items.add(new Template("Sandwich",
                "Veg grilled sandwich.", 40));
        items.add(new Template("Notebook",
                "200-page ruled notebook from the campus store.", 50));
        ITEMS = Collections.unmodifiableList(items);
    }

    private MarketCatalog() {
        // no instances
    }

    public static List<Template> defaults() {
        return ITEMS;
    }
}
