package com.example.skytracker.skins;

import com.example.skytracker.SkyTrackerClient;
import com.example.skytracker.config.ModConfig;
import com.example.skytracker.data.SkinEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Section 26 of the spec: data-driven skin database, not a giant hardcoded
 * Java class. Lives at config/skytracker/skins.json - an EXTERNAL, editable
 * file, not bundled read-only inside the jar - so the community (or you) can
 * update/replace it without recompiling anything. On first run it's seeded
 * from a small starter set bundled as a mod resource.
 *
 * HONESTY NOTE: the bundled starter set has 2 example entries, not the
 * spec's full ~1,284-skin catalog. I have no reliable, verifiable source for
 * a complete, accurate, currently-priced SkyBlock skin list to hardcode here
 * - and inventing one would mean fabricating rarity/value data, which the
 * spec itself explicitly prohibits (Section 10: "Do NOT invent historical
 * values"). This importer is the real deliverable: point it at a real,
 * current community-maintained skins.json (NEU's repo/Skytils' data files
 * are reasonable sources to adapt) and it works immediately, with no code
 * changes.
 */
public class SkinDatabase {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SKIN_LIST_TYPE = new TypeToken<ArrayList<SkinEntry>>() {}.getType();
    private static final String BUNDLED_SEED_PATH = "data/skytracker/skins.json";

    private final Path databaseFile;
    private List<SkinEntry> entries = new ArrayList<>();
    private Map<String, SkinEntry> byId = Map.of();

    public SkinDatabase() {
        this.databaseFile = ModConfig.CONFIG_DIR.resolve("skins.json");
    }

    public void load() {
        try {
            if (!Files.exists(databaseFile)) {
                seedFromBundledDefault();
            }
            String json = Files.readString(databaseFile, StandardCharsets.UTF_8);
            List<SkinEntry> loaded = GSON.fromJson(json, SKIN_LIST_TYPE);
            entries = loaded != null ? loaded : new ArrayList<>();
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            SkyTrackerClient.LOGGER.error("Failed to load skin database from {} - starting empty. " +
                    "The file was left untouched so you don't lose any edits.", databaseFile, e);
            entries = new ArrayList<>();
        }
        byId = entries.stream()
                .filter(s -> s.id != null)
                .collect(Collectors.toMap(s -> s.id, s -> s, (a, b) -> a));
    }

    /** Re-import a replacement database file (Section 26: "importer" requirement). */
    public void importFrom(Path externalFile) throws IOException {
        String json = Files.readString(externalFile, StandardCharsets.UTF_8);
        // Validate it parses before overwriting the live file.
        List<SkinEntry> parsed = GSON.fromJson(json, SKIN_LIST_TYPE);
        if (parsed == null) throw new IOException("File did not contain a valid skin array");
        Files.writeString(databaseFile, GSON.toJson(parsed), StandardCharsets.UTF_8);
        load();
    }

    /**
     * Bulk-imports pet skins and helmet skins from Altpapier/Skyblock-Item-Emojis
     * (github.com/Altpapier/Skyblock-Item-Emojis), a public, actively-maintained
     * dataset covering every current SkyBlock item's texture identity. This
     * runs at RUNTIME, from the actual player's machine - not from the sandbox
     * this project was built in, which has no general internet access and
     * couldn't fetch or verify this file's exact contents (GitHub blocks
     * automated fetching of it from outside a normal browser/game client).
     *
     * HONESTY NOTE ON SCHEMA CONFIDENCE: the exact JSON structure of
     * itemHash.json below is inferred from that repo's own README text
     * (which I could read via search), not from directly inspecting the
     * file - I'm reasonably confident in the shape described (a flat map of
     * custom-id -> hash string) and in the {@code PET_SKIN_<NAME>} id
     * convention it documents, but this is worth confirming against the
     * real file's contents the first time you run an import in-game (a
     * failed/empty import logs a warning rather than corrupting your
     * existing skins.json - see the try/catch below).
     *
     * ATTRIBUTION: that repo's README asks for credit if you use its data
     * ("it would be nice if you could give some credit as this took a lot
     * of time making"). Worth a mention in your own README/credits if you
     * ship a build that uses this.
     */
    public CompletableFuture<Integer> importPetAndHelmetSkinsFromSkyblockItemEmojis() {
        String url = "https://raw.githubusercontent.com/Altpapier/Skyblock-Item-Emojis/main/v3/itemHash.json";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        SkyTrackerClient.LOGGER.warn("Skin import failed: HTTP {}", response.statusCode());
                        return 0;
                    }
                    try {
                        com.google.gson.JsonObject idToHash = com.google.gson.JsonParser
                                .parseString(response.body()).getAsJsonObject();

                        List<SkinEntry> imported = new ArrayList<>();
                        for (var entry : idToHash.entrySet()) {
                            String id = entry.getKey();
                            boolean isPetSkin = id.startsWith("PET_SKIN_");
                            boolean isHelmet = id.contains("HELMET");
                            if (!isPetSkin && !isHelmet) continue;

                            SkinEntry skin = new SkinEntry();
                            skin.id = id.toLowerCase();
                            skin.name = humanizeId(id);
                            skin.appliesToItem = isPetSkin ? "PET_SKIN" : "HELMET";
                            skin.skinType = isPetSkin ? "PET" : "ARMOR";
                            skin.category = isPetSkin ? "Pet skins" : "Armor skins";
                            skin.rarity = null;   // not provided by this source - left honest
                            skin.estimatedValue = null;
                            skin.source = "Imported from Altpapier/Skyblock-Item-Emojis (schema inferred from README - verify)";
                            skin.aliases = new String[0];
                            skin.tags = new String[0];
                            skin.textureHash = entry.getValue().getAsString();
                            imported.add(skin);
                        }

                        if (imported.isEmpty()) {
                            SkyTrackerClient.LOGGER.warn("Skin import found 0 matching entries - " +
                                    "the file's real structure may not match what was inferred from its README. " +
                                    "Existing skins.json was left untouched.");
                            return 0;
                        }

                        // Merge: keep existing entries whose id isn't in the import, add/replace the rest.
                        Map<String, SkinEntry> merged = new java.util.LinkedHashMap<>(byId);
                        for (SkinEntry s : imported) merged.put(s.id, s);
                        List<SkinEntry> mergedList = new ArrayList<>(merged.values());

                        Files.writeString(databaseFile, GSON.toJson(mergedList), StandardCharsets.UTF_8);
                        load();
                        return imported.size();
                    } catch (Exception e) {
                        SkyTrackerClient.LOGGER.error("Failed to parse skin import - " +
                                "existing skins.json was left untouched.", e);
                        return 0;
                    }
                })
                .exceptionally(ex -> {
                    SkyTrackerClient.LOGGER.warn("Skin import request failed", ex);
                    return 0;
                });
    }

    private static String humanizeId(String id) {
        String withoutPrefix = id.replace("PET_SKIN_", "").replace("_HELMET", " Helmet");
        String[] parts = withoutPrefix.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    public List<SkinEntry> all() {
        return entries;
    }

    public SkinEntry byId(String id) {
        return byId.get(id);
    }

    private void seedFromBundledDefault() throws IOException {
        Files.createDirectories(databaseFile.getParent());
        try (InputStream in = SkinDatabase.class.getClassLoader().getResourceAsStream(BUNDLED_SEED_PATH)) {
            if (in == null) {
                Files.writeString(databaseFile, "[]", StandardCharsets.UTF_8);
                return;
            }
            Files.write(databaseFile, in.readAllBytes());
        }
    }
}
