package com.example.skytracker;

import com.example.skytracker.config.ModConfig;
import com.example.skytracker.gui.DashboardScreen;
import com.example.skytracker.storage.DataStorage;
import net.fabricmc.api.ClientModInitializer;
// NOTE: Fabric API itself renamed a batch of classes for 26.1's move to
// official mappings (see docs.fabricmc.net/develop/porting/26.1/fabric-api).
// ClientTickEvents and KeyBindingHelper were NOT in the rename list I could
// confirm, so these two import paths are likely still correct - but verify
// against that page (Ctrl+F the old name) before your first build, since I
// couldn't fetch the full list from this sandbox.
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only entrypoint. This mod never sends server-bound automation packets,
 * never simulates input, and never reads data the client wouldn't already have
 * available to it (open screens, held/received item stacks, chat).
 */
public final class SkyTrackerClient implements ClientModInitializer {

    public static final String MOD_ID = "skytracker";
    public static final Logger LOGGER = LoggerFactory.getLogger("SkyTracker");

    private static DataStorage storage;
    private static ModConfig config;

    private KeyBinding openDashboardKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing SkyBlock Tracker (Stage 1: dashboard + goals + storage)");

        config = ModConfig.loadOrCreateDefault();
        storage = new DataStorage(config);
        storage.loadAll();

        openDashboardKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.skytracker.open_dashboard",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.skytracker.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openDashboardKey.consumeClick()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.screen == null) {
                    minecraft.setScreen(new DashboardScreen(storage));
                }
            }
        });

        // Flush data to disk on shutdown so a crash mid-session doesn't lose goals/sessions.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                storage.saveAll();
            } catch (Exception e) {
                LOGGER.error("Failed to save SkyTracker data on shutdown", e);
            }
        }));
    }

    public static DataStorage getStorage() {
        return storage;
    }

    public static ModConfig getConfig() {
        return config;
    }
}
