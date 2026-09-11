package com.example.skytracker.data;

import java.util.UUID;

/** Section 9/34-43 of the spec: a wishlist item, optionally watched on the AH. */
public class WishlistEntry {
    public final String id;
    public String skinId;          // links to a SkinEntry.id when known
    public String displayName;     // fallback label if skinId is unset

    public Double targetPrice;
    public Goal.Priority priority = Goal.Priority.MEDIUM;
    public String notes = "";

    public boolean watchAuctionHouse = false;
    public boolean notifyChat = true;
    public boolean binOnly = true;
    public Double minPrice;
    public Double maxPrice;

    /** Auction UUIDs already notified about, so we never repeat a notification (Section 39). */
    public java.util.Set<String> seenAuctionIds = new java.util.HashSet<>();

    public WishlistEntry(String displayName) {
        this.id = UUID.randomUUID().toString();
        this.displayName = displayName;
    }
}
