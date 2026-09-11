package com.example.skytracker.auction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Section 32 of the spec: pluggable AH data source, so SkyCofl isn't hardwired everywhere. */
public interface AuctionHouseProvider {

    record Listing(String auctionId, String itemName, double price, boolean bin,
                    String sellerName, long listingAgeMillis, String auctionUrl) {}

    /** Async - never blocks the render thread (Section 24). */
    CompletableFuture<List<Listing>> getActiveListings(String skyblockItemTag);

    String sourceName();
}
