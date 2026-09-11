package com.example.skytracker.pricing;

import com.example.skytracker.SkyTrackerClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sections 15/32-48 of the spec. Built against SkyCofl's real, currently
 * published REST API (confirmed via sky-commands.coflnet.com/wiki/api on
 * 2026-09-11, not assumed or guessed - see Section 48's explicit
 * requirement to check current docs before implementing):
 *
 *   GET https://sky.coflnet.com/api/auctions/tag/{itemTag}/active/bin
 *     -> lowest current BIN listings for an item tag
 *
 * Rate limits per that same documentation: 30 requests/10s and 100
 * requests/minute per IP, enforced in parallel. This provider caches every
 * result and enforces its own minimum interval well under those limits
 * (Section 33/40: "avoid excessive API requests", "respect API rate
 * limits"). All requests run on HttpClient's async executor, never the
 * render thread (Section 24).
 *
 * ATTRIBUTION: SkyCofl's docs require attributing them wherever their data
 * is shown - a link to https://sky.coflnet.com/item/{tag} with visible text
 * like "Prices provided by SkyCofl". Wire that into whatever UI displays a
 * SkyCofl-sourced price (Sections 15's "Price updated Xm ago" line is a
 * natural place for it) - I haven't added UI text for it here since it
 * belongs at the display layer, not this provider.
 */
public class SkyCoflPriceProvider implements PriceProvider {

    private static final String BASE_URL = "https://sky.coflnet.com/api";
    private static final long MIN_REQUEST_INTERVAL_MS = 1500; // well under the 30/10s burst limit
    private static final long CACHE_TTL_MS = 5 * 60_000; // Section 33: "update only when necessary"

    private record CachedPrice(double value, long fetchedAtMillis) {}

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final Map<String, CachedPrice> cache = new ConcurrentHashMap<>();
    private volatile long lastRequestAtMillis = 0;
    private volatile boolean lastRequestFailed = false;

    @Override
    public Double getEstimatedValue(String skyblockItemId) {
        CachedPrice cached = cache.get(skyblockItemId);
        long now = System.currentTimeMillis();

        if (cached != null && now - cached.fetchedAtMillis < CACHE_TTL_MS) {
            return cached.value();
        }

        // Return the stale cached value immediately (Section 46: keep cached
        // prices, don't block) while a fresh fetch happens in the background.
        requestRefreshAsync(skyblockItemId);
        return cached != null ? cached.value() : null;
    }

    public boolean isOnline() {
        return !lastRequestFailed;
    }

    private void requestRefreshAsync(String skyblockItemId) {
        long now = System.currentTimeMillis();
        if (now - lastRequestAtMillis < MIN_REQUEST_INTERVAL_MS) return; // Section 40: interval floor
        lastRequestAtMillis = now;

        String url = BASE_URL + "/auctions/tag/" + skyblockItemId + "/active/bin";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        lastRequestFailed = true;
                        return;
                    }
                    try {
                        JsonArray listings = JsonParser.parseString(response.body()).getAsJsonArray();
                        double lowest = Double.MAX_VALUE;
                        for (var element : listings) {
                            var obj = element.getAsJsonObject();
                            // Field names per the documented response shape:
                            // startingBid is the BIN price for a fixed-price listing.
                            if (obj.has("startingBid")) {
                                lowest = Math.min(lowest, obj.get("startingBid").getAsDouble());
                            }
                        }
                        if (lowest < Double.MAX_VALUE) {
                            cache.put(skyblockItemId, new CachedPrice(lowest, System.currentTimeMillis()));
                        }
                        lastRequestFailed = false;
                    } catch (Exception e) {
                        SkyTrackerClient.LOGGER.warn("Failed to parse SkyCofl price response for {}", skyblockItemId, e);
                        lastRequestFailed = true;
                    }
                })
                .exceptionally(ex -> {
                    lastRequestFailed = true;
                    SkyTrackerClient.LOGGER.warn("SkyCofl price request failed for {}", skyblockItemId, ex);
                    return null;
                });
    }

    @Override
    public String sourceName() {
        return "SkyCofl";
    }
}
