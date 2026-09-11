package com.example.skytracker.data;

/**
 * Stage 2: manual entry per category, since automatic inventory/storage
 * scanning (Section 11/16 of the spec) is Stage 4's job. This class is the
 * shape Stage 4's valuation engine will populate automatically later -
 * building the UI/storage against this shape now means Stage 4 only has to
 * change *how* these numbers get filled in, not the model itself.
 */
public class NetWorthSnapshot {

    public double purse;
    public double bank;
    public double inventoryValue;
    public double storageValue;
    public double skinsValue;
    public double armorValue;
    public double petsValue;

    public long lastUpdatedEpochMillis;

    public double total() {
        return purse + bank + inventoryValue + storageValue + skinsValue + armorValue + petsValue;
    }
}
