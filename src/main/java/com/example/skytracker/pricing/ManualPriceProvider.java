package com.example.skytracker.pricing;

import java.util.HashMap;
import java.util.Map;

/** Fallback/override source: prices the user typed in themselves. */
public class ManualPriceProvider implements PriceProvider {

    private final Map<String, Double> manualPrices = new HashMap<>();

    public void setPrice(String skyblockItemId, double value) {
        manualPrices.put(skyblockItemId, value);
    }

    public void clearPrice(String skyblockItemId) {
        manualPrices.remove(skyblockItemId);
    }

    @Override
    public Double getEstimatedValue(String skyblockItemId) {
        return manualPrices.get(skyblockItemId);
    }

    @Override
    public String sourceName() {
        return "Manual";
    }
}
