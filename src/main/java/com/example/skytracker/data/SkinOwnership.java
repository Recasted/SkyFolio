package com.example.skytracker.data;

/** What the user owns for a given catalog SkinEntry.id (Section 8 of the spec). */
public class SkinOwnership {
    public String skinId;
    public int quantity = 1;
    public Long acquiredEpochMillis;
    public Double purchasePrice;
    public String notes = "";
}
