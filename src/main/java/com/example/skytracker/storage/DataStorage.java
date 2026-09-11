package com.example.skytracker.storage;

import com.example.skytracker.SkyTrackerClient;
import com.example.skytracker.config.ModConfig;
import com.example.skytracker.data.Goal;
import com.example.skytracker.data.NetWorthSnapshot;
import com.example.skytracker.data.ProfitSession;
import com.example.skytracker.data.SkinOwnership;
import com.example.skytracker.data.WishlistEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns all persistent state as separate JSON files under the config directory:
 *   goals.json, sessions.json, networth.json, skins-ownership.json, wishlist.json
 * (the skin catalog itself, skins.json, is owned by SkinDatabase - kept
 * separate since it's a replaceable reference dataset, not per-user state).
 *
 * Every write goes to a temp file then atomically replaces the real file, so a
 * crash mid-write can't corrupt existing data.
 */
public class DataStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type GOAL_LIST_TYPE = new TypeToken<ArrayList<Goal>>() {}.getType();
    private static final Type SESSION_LIST_TYPE = new TypeToken<ArrayList<ProfitSession>>() {}.getType();
    private static final Type SKIN_OWNERSHIP_LIST_TYPE = new TypeToken<ArrayList<SkinOwnership>>() {}.getType();
    private static final Type WISHLIST_LIST_TYPE = new TypeToken<ArrayList<WishlistEntry>>() {}.getType();

    private final Path goalsFile;
    private final Path sessionsFile;
    private final Path netWorthFile;
    private final Path skinOwnershipFile;
    private final Path wishlistFile;

    public List<Goal> goals = new ArrayList<>();
    public List<ProfitSession> sessions = new ArrayList<>();
    public ProfitSession activeSession = null;
    public NetWorthSnapshot netWorth = new NetWorthSnapshot();
    public List<SkinOwnership> skinOwnership = new ArrayList<>();
    public List<WishlistEntry> wishlist = new ArrayList<>();

    public DataStorage(ModConfig config) {
        this.goalsFile = ModConfig.CONFIG_DIR.resolve("goals.json");
        this.sessionsFile = ModConfig.CONFIG_DIR.resolve("sessions.json");
        this.netWorthFile = ModConfig.CONFIG_DIR.resolve("networth.json");
        this.skinOwnershipFile = ModConfig.CONFIG_DIR.resolve("skins-ownership.json");
        this.wishlistFile = ModConfig.CONFIG_DIR.resolve("wishlist.json");
    }

    public void loadAll() {
        goals = readList(goalsFile, GOAL_LIST_TYPE);
        sessions = readList(sessionsFile, SESSION_LIST_TYPE);
        NetWorthSnapshot loaded = readSingle(netWorthFile, NetWorthSnapshot.class);
        netWorth = loaded != null ? loaded : new NetWorthSnapshot();
        skinOwnership = readList(skinOwnershipFile, SKIN_OWNERSHIP_LIST_TYPE);
        wishlist = readList(wishlistFile, WISHLIST_LIST_TYPE);
    }

    public void saveAll() {
        writeList(goalsFile, goals);
        writeList(sessionsFile, sessions);
        writeSingle(netWorthFile, netWorth);
        writeList(skinOwnershipFile, skinOwnership);
        writeList(wishlistFile, wishlist);
    }

    public void addGoal(Goal goal) {
        goals.add(goal);
        writeList(goalsFile, goals);
    }

    public void removeGoal(String goalId) {
        goals.removeIf(g -> g.id.equals(goalId));
        writeList(goalsFile, goals);
    }

    /** Call after mutating a Goal object in place (e.g. from the edit screen). */
    public void saveGoals() {
        writeList(goalsFile, goals);
    }

    public void saveNetWorth() {
        netWorth.lastUpdatedEpochMillis = System.currentTimeMillis();
        writeSingle(netWorthFile, netWorth);
    }

    public void markSkinOwned(String skinId, int quantity) {
        skinOwnership.removeIf(o -> o.skinId.equals(skinId));
        SkinOwnership record = new SkinOwnership();
        record.skinId = skinId;
        record.quantity = quantity;
        record.acquiredEpochMillis = System.currentTimeMillis();
        skinOwnership.add(record);
        writeList(skinOwnershipFile, skinOwnership);
    }

    public void markSkinNotOwned(String skinId) {
        skinOwnership.removeIf(o -> o.skinId.equals(skinId));
        writeList(skinOwnershipFile, skinOwnership);
    }

    public boolean ownsSkin(String skinId) {
        return skinOwnership.stream().anyMatch(o -> o.skinId.equals(skinId));
    }

    public void addWishlistEntry(WishlistEntry entry) {
        wishlist.add(entry);
        writeList(wishlistFile, wishlist);
    }

    public void removeWishlistEntry(String id) {
        wishlist.removeIf(w -> w.id.equals(id));
        writeList(wishlistFile, wishlist);
    }

    public void saveWishlist() {
        writeList(wishlistFile, wishlist);
    }

    public void recordSessionEnd(ProfitSession session, long endEpochMillis) {
        session.endEpochMillis = endEpochMillis;
        if (!sessions.contains(session)) {
            sessions.add(session);
        }
        writeList(sessionsFile, sessions);
    }

    /** Average coins/hour across the most recent N completed sessions (Section 19 of the spec). */
    public double recentAverageCoinsPerHour(int lastN) {
        List<ProfitSession> completed = sessions.stream()
                .filter(s -> s.endEpochMillis != null)
                .toList();
        int from = Math.max(0, completed.size() - lastN);
        List<ProfitSession> recent = completed.subList(from, completed.size());
        if (recent.isEmpty()) return 0.0;

        double totalProfit = 0;
        double totalHours = 0;
        for (ProfitSession s : recent) {
            totalProfit += s.profit();
            totalHours += s.durationMillis(s.endEpochMillis) / 3_600_000.0;
        }
        return totalHours > 0 ? totalProfit / totalHours : 0.0;
    }

    private <T> List<T> readList(Path path, Type type) {
        try {
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }
            String json = Files.readString(path, StandardCharsets.UTF_8);
            List<T> result = GSON.fromJson(json, type);
            return result != null ? result : new ArrayList<>();
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            SkyTrackerClient.LOGGER.error("Failed to read {} - starting with an empty list. " +
                    "The original file was left untouched.", path, e);
            return new ArrayList<>();
        }
    }

    private <T> T readSingle(Path path, Class<T> type) {
        try {
            if (!Files.exists(path)) return null;
            String json = Files.readString(path, StandardCharsets.UTF_8);
            return GSON.fromJson(json, type);
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            SkyTrackerClient.LOGGER.error("Failed to read {} - the original file was left untouched.", path, e);
            return null;
        }
    }

    private <T> void writeList(Path path, List<T> data) {
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(data), StandardCharsets.UTF_8);
            Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            SkyTrackerClient.LOGGER.error("Failed to write {}", path, e);
        }
    }

    private void writeSingle(Path path, Object data) {
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(data), StandardCharsets.UTF_8);
            Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            SkyTrackerClient.LOGGER.error("Failed to write {}", path, e);
        }
    }
}
