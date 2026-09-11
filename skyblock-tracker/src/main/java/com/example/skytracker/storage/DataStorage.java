package com.example.skytracker.storage;

import com.example.skytracker.SkyTrackerClient;
import com.example.skytracker.config.ModConfig;
import com.example.skytracker.data.Goal;
import com.example.skytracker.data.ProfitSession;
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
 *   goals.json, sessions.json
 * Later stages add skins-ownership.json, wishlist.json, inventory-snapshots.json
 * without touching these files, per the "no monolithic store" requirement.
 *
 * Every write goes to a temp file then atomically replaces the real file, so a
 * crash mid-write can't corrupt existing data.
 */
public class DataStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type GOAL_LIST_TYPE = new TypeToken<ArrayList<Goal>>() {}.getType();
    private static final Type SESSION_LIST_TYPE = new TypeToken<ArrayList<ProfitSession>>() {}.getType();

    private final Path goalsFile;
    private final Path sessionsFile;

    public List<Goal> goals = new ArrayList<>();
    public List<ProfitSession> sessions = new ArrayList<>();
    public ProfitSession activeSession = null;

    public DataStorage(ModConfig config) {
        this.goalsFile = ModConfig.CONFIG_DIR.resolve("goals.json");
        this.sessionsFile = ModConfig.CONFIG_DIR.resolve("sessions.json");
    }

    public void loadAll() {
        goals = readList(goalsFile, GOAL_LIST_TYPE);
        sessions = readList(sessionsFile, SESSION_LIST_TYPE);
    }

    public void saveAll() {
        writeList(goalsFile, goals);
        writeList(sessionsFile, sessions);
    }

    public void addGoal(Goal goal) {
        goals.add(goal);
        writeList(goalsFile, goals);
    }

    public void removeGoal(String goalId) {
        goals.removeIf(g -> g.id.equals(goalId));
        writeList(goalsFile, goals);
    }

    public void recordSessionEnd(ProfitSession session, long endEpochMillis) {
        session.endEpochMillis = endEpochMillis;
        if (!sessions.contains(session)) {
            sessions.add(session);
        }
        writeList(sessionsFile, sessions);
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
}
