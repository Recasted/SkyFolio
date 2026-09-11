package com.example.skytracker.data;

import java.util.UUID;

/**
 * Section 13/14 of the spec. Deliberately all manually-edited fields - the
 * spec is explicit that exotic valuation is subjective and must not be
 * auto-computed ("Allow manual editing because exotic valuation can be
 * highly subjective").
 */
public class ExoticArmorEntry {
    public final String id;
    public String armorSet;
    public String piece;
    public String colorHex;
    public boolean isExotic;
    public boolean isCrystal;
    public Double purchasePrice;
    public Double estimatedCurrentValue;
    public Long acquiredEpochMillis;
    public boolean owned = true;

    public ExoticArmorEntry(String armorSet, String piece) {
        this.id = UUID.randomUUID().toString();
        this.armorSet = armorSet;
        this.piece = piece;
    }

    public Double profit() {
        if (purchasePrice == null || estimatedCurrentValue == null) return null;
        return estimatedCurrentValue - purchasePrice;
    }
}
