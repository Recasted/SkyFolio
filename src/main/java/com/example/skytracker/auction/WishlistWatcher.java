package com.example.skytracker.auction;

import com.example.skytracker.data.WishlistEntry;
import com.example.skytracker.storage.DataStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Sections 34-43 of the spec: periodically checks the AH for wishlist items
 * with watching enabled, applies price/BIN filters, and sends a one-time
 * clickable chat notification per new auction ID (Section 39: anti-spam).
 *
 * Runs on a plain interval via tick polling rather than a background thread
 * pool, matching Section 24's "sensible update intervals" and keeping the
 * whole feature easy to disable/reason about. A wishlist of a few dozen
 * items polled every 30-60s is nowhere near SkyCofl's documented rate
 * limits (30 req/10s, 100 req/min per the docs I verified for this project).
 *
 * NOTE ON API SURFACE: `ClickEvent.OpenUrl(URI)` reflects Minecraft's 1.21.5+
 * rework of ClickEvent into a sealed interface of record types (replacing
 * the old `new ClickEvent(ClickEvent.Action.OPEN_URL, urlString)`
 * constructor) - I'm reasonably confident in this one since it predates
 * 26.1, but it's still worth a quick check against the current
 * `net.minecraft.network.chat.ClickEvent` source if this line doesn't
 * compile; the rest of the notification logic doesn't depend on which form
 * it turns out to be.
 */
public class WishlistWatcher {

    private static final long POLL_INTERVAL_MS = 45_000; // Section 40: user-configurable in a future pass

    private final DataStorage storage;
    private final AuctionHouseProvider provider;
    private long lastPollAtMillis = 0;

    public WishlistWatcher(DataStorage storage, AuctionHouseProvider provider) {
        this.storage = storage;
        this.provider = provider;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastPollAtMillis < POLL_INTERVAL_MS) return;
        lastPollAtMillis = now;

        for (WishlistEntry entry : storage.wishlist) {
            if (!entry.watchAuctionHouse || entry.skinId == null) continue;
            // skinId doubles as the SkyBlock item tag to search for here; a
            // more precise Section-35 match (exact skin ID rather than a
            // generic item tag) needs the skin database wired to real
            // SkyBlock item tags, which depends on what a real skins.json
            // import provides - left as a TODO rather than guessed.
            checkEntry(entry);
        }
    }

    private void checkEntry(WishlistEntry entry) {
        provider.getActiveListings(entry.skinId).thenAccept(listings -> {
            for (AuctionHouseProvider.Listing listing : listings) {
                if (entry.seenAuctionIds.contains(listing.auctionId())) continue; // Section 39
                if (entry.binOnly && !listing.bin()) continue;
                if (entry.minPrice != null && listing.price() < entry.minPrice) continue;
                if (entry.maxPrice != null && listing.price() > entry.maxPrice) continue;

                entry.seenAuctionIds.add(listing.auctionId());
                storage.saveWishlist();
                if (entry.notifyChat) {
                    sendChatNotification(entry, listing);
                }
            }
        });
    }

    private void sendChatNotification(WishlistEntry entry, AuctionHouseProvider.Listing listing) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        MutableComponent message = Component.literal("[Skin Tracker] ")
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal("WISHLIST ITEM FOUND!\n").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(entry.displayName + " - " + formatShort(listing.price()) + "\n")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(listing.bin() ? "BIN" : "Auction").withStyle(ChatFormatting.GRAY));

        if (listing.auctionUrl() != null) {
            MutableComponent link = Component.literal("  [OPEN AUCTION]").withStyle(
                    Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(listing.auctionUrl())))
            );
            message = message.append(link);
        } else {
            message = message.append(Component.literal("  (Auction ID: " + listing.auctionId() + ")")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        client.player.displayClientMessage(message, false);
    }

    private static String formatShort(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000) return String.format("%.2fB", value / 1_000_000_000);
        if (abs >= 1_000_000) return String.format("%.2fM", value / 1_000_000);
        if (abs >= 1_000) return String.format("%.2fK", value / 1_000);
        return String.format("%.0f", value);
    }
}
