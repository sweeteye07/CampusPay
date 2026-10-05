package com.example.campuspay;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MarketCatalogTest {

    @Test
    public void catalogHasValidPresets() {
        assertFalse(MarketCatalog.defaults().isEmpty());
        Set<String> names = new HashSet<>();
        for (MarketCatalog.Template item : MarketCatalog.defaults()) {
            assertTrue(names.add(item.name));
            assertFalse(item.description.trim().isEmpty());
            assertTrue(item.suggestedPrice >= 1
                    && item.suggestedPrice <= 10000);
        }
    }
}
