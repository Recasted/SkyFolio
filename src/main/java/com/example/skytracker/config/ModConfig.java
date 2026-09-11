package com.example.skytracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Top-level user-configurable settings. Stage 1 covers only what's needed for
 * the dashboard + storage; overlay/price-source/notification settings are
 * added in their respective stages so this class doesn't balloon prematurely.
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Path CONFIG_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve("skytracker");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");

    // --- Stage 1 settings ---
    public String currencyFormat = "SHORT"; // SHORT (1.7B) or FULL (1,700,000,000)
    public int decimalPrecision = 2;
    public boolean darkTheme = true;

    public static ModConfig loadOrCreateDefault() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
                ModConfig loaded = GSON.fromJson(json, ModConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            }
        } catch (IOException e) {
            // Fall through to defaults; we never want a config read failure to crash startup.
        }
        ModConfig fresh = new ModConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(CONFIG_FILE, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SkyTrackerConfigError.log(e);
        }
    }

    private static final class SkyTrackerConfigError {
        static void log(IOException e) {
            com.example.skytracker.SkyTrackerClient.LOGGER.error("Failed to save config", e);
        }
    }
}
