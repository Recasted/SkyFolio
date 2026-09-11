package com.example.skytracker;

import com.example.skytracker.auction.AuctionHouseProvider;
import com.example.skytracker.auction.SkyCoflAuctionHouseProvider;
import com.example.skytracker.auction.WishlistWatcher;
import com.example.skytracker.config.ModConfig;
import com.example.skytracker.gui.DashboardScreen;
import com.example.skytracker.gui.OverlayHud;
import com.example.skytracker.pricing.ManualPriceProvider;
import com.example.skytracker.pricing.PriceProvider;
import com.example.skytracker.pricing.SkyCoflPriceProvider;
import com.example.skytracker.skins.SkinDatabase;
import com.example.skytracker.storage.DataStorage;
import com.example.skytracker.tracker.ProfitTracker;
import com.example.skytracker.valuation.InventoryValueOverlay;
import com.example.skytracker.valuation.ValuationEngine;
import net.fabricmc.api.ClientModInitializer;
// NOTE: Fabric API itself renamed a batch of classes for 26.1's move to
// official mappings (see docs.fabricmc.net/develop/porting/26.1/fabric-api).
// ClientTickEvents and KeyBindingHelper were NOT in the rename list I could
// confirm, so these two import paths are likely still correct - but verify
// against that page (Ctrl+F the old name) before your first build, since I
// couldn't fetch the full list from this sandbox. Same caveat applies to
// HudRenderCallback and ScreenEvents (used inside InventoryValueOverlay).
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only entrypoint. This mod never sends server-bound automation packets,
 * never simulates input, and never reads data the client wouldn't already have
 * available to it (open screens, held/received item stacks, chat, scoreboard).
 * All outbound network traffic (SkyCofl pricing/auction lookups) is
 * informational GET requests to a public price API - never anything that
 * touches the SkyBlock server connection itself.
 */
public final class SkyTrackerClient implements ClientModInitializer {

    public static final String MOD_ID = "skytracker";
    public static final Logger LOGGER = LoggerFactory.getLogger("SkyTracker");

    private static DataStorage storage;
    private static ModConfig config;
    private static ProfitTracker profitTracker;
    private static SkinDatabase skinDatabase;
    private static WishlistWatcher wishlistWatcher;

    private KeyMapping openDashboardKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing SkyBlock Tracker (all stages: goals, profit, skins, valuation, pricing)");

        config = ModConfig.loadOrCreateDefault();
        storage = new DataStorage(config);
        storage.loadAll();
        profitTracker = new ProfitTracker(storage);

        skinDatabase = new SkinDatabase();
        skinDatabase.load();

        PriceProvider priceProvider = config.priceSource.equals("SKYCOFL")
                ? new SkyCoflPriceProvider()
                : new ManualPriceProvider();
        ValuationEngine valuationEngine = new ValuationEngine(skinDatabase, priceProvider);

        AuctionHouseProvider auctionHouseProvider = new SkyCoflAuctionHouseProvider();
        wishlistWatcher = new WishlistWatcher(storage, auctionHouseProvider);

        if (config.containerValuationEnabled) {
            InventoryValueOverlay.register(valuationEngine);
        }

        openDashboardKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.skytracker.open_dashboard",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.skytracker.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            profitTracker.tick();
            if (config.auctionWatcherEnabled) {
                wishlistWatcher.tick();
            }

            while (openDashboardKey.consumeClick()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.screen == null) {
                    minecraft.setScreen(new DashboardScreen(storage, profitTracker, skinDatabase, config));
                }
            }
        });

        HudRenderCallback.register((context, tickDelta) ->
                OverlayHud.renderOverlay(context, storage, profitTracker, config));

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

    public static ProfitTracker getProfitTracker() {
        return profitTracker;
    }

    public static SkinDatabase getSkinDatabase() {
        return skinDatabase;
    }
}
