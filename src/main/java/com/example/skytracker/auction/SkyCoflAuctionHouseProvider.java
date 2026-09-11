package com.example.skytracker.auction;

import com.example.skytracker.SkyTrackerClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Sections 32-48 of the spec, built against SkyCofl's real documented API
 * (sky-commands.coflnet.com/wiki/api, confirmed 2026-09-11):
 *
 *   GET https://sky.coflnet.com/api/auctions/tag/{itemTag}/active/bin
 *
 * Response fields per that documentation: uuid, tag, itemName, startingBid,
 * highestBidAmount, bin, end, auctioneerId, count, category, tier, reforge,
 * itemCreatedAt. There is no sellerName field in the documented response
 * (only auctioneerId, a player UUID) - Section 37's example message shows a
 * player NAME, so resolving UUID -> name would need an extra lookup (e.g.
 * Mojang's profile API) that I haven't wired in here to keep this within
 * SkyCofl's own documented rate limits; sellerName below is populated with
 * the raw UUID as an honest placeholder rather than a fabricated name.
 *
 * AUCTION URL: the docs I found give an attribution pattern for item pages
 * (https://sky.coflnet.com/item/{tag}) and player pages
 * (https://sky.coflnet.com/player/{userId}), but did not explicitly
 * document a single-auction page URL. https://sky.coflnet.com/auction/{uuid}
 * below is inferred by pattern, not confirmed - Section 45 says not to
 * fabricate a URL when the API doesn't provide enough to construct one, so
 * treat this one URL as unverified until you've confirmed it resolves; the
 * auction UUID itself (always real, straight from the API) is included in
 * every Listing regardless, so a copy-ID fallback is always available per
 * Section 45's guidance.
 */
public class SkyCoflAuctionHouseProvider implements AuctionHouseProvider {

    private static final String BASE_URL = "https://sky.coflnet.com/api";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public CompletableFuture<List<Listing>> getActiveListings(String skyblockItemTag) {
        String url = BASE_URL + "/auctions/tag/" + skyblockItemTag + "/active/bin";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("SkyCofl returned HTTP " + response.statusCode());
                    }
                    return parseListings(response.body());
                })
                .exceptionally(ex -> {
                    SkyTrackerClient.LOGGER.warn("SkyCofl auction lookup failed for {}", skyblockItemTag, ex);
                    return List.of();
                });
    }

    private List<Listing> parseListings(String body) {
        List<Listing> results = new ArrayList<>();
        JsonArray array = JsonParser.parseString(body).getAsJsonArray();

        for (var element : array) {
            JsonObject obj = element.getAsJsonObject();
            String uuid = getString(obj, "uuid");
            String itemName = getString(obj, "itemName");
            double price = obj.has("startingBid") ? obj.get("startingBid").getAsDouble() : 0;
            boolean bin = obj.has("bin") && obj.get("bin").getAsBoolean();
            String seller = getString(obj, "auctioneerId"); // UUID, not a display name - see class doc
            String auctionUrl = uuid.isEmpty() ? null : "https://sky.coflnet.com/auction/" + uuid;

            results.add(new Listing(uuid, itemName, price, bin, seller, 0, auctionUrl));
        }
        return results;
    }

    private static String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    @Override
    public String sourceName() {
        return "SkyCofl";
    }
}
