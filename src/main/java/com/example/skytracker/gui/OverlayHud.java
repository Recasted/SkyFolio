package com.example.skytracker.gui;

import com.example.skytracker.config.ModConfig;
import com.example.skytracker.data.ProfitSession;
import com.example.skytracker.storage.DataStorage;
import com.example.skytracker.tracker.ProfitTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Section 4 of the spec: small overlay while playing, showing the active
 * session's live stats. Stage 2 keeps position/scale fixed via config values
 * (no in-game drag handle yet - that's a Stage 6 polish item), but the
 * numbers themselves are fully live, refreshed by ProfitTracker.tick().
 *
 * NOTE ON API SURFACE: this is meant to be invoked from Fabric API's
 * HudRenderCallback, which I could not verify the exact current signature
 * for in this sandbox (recent Minecraft versions have swapped a raw
 * `float tickDelta` parameter for a `DeltaTracker`/`RenderTickCounter`
 * object in some render callbacks, and Fabric API renamed some of its own
 * classes as part of 26.1's official-mappings move). If
 * HudRenderCallback.register(...) in SkyTrackerClient doesn't match this
 * signature when you build, check docs.fabricmc.net/develop/rendering/hud
 * for the current one - the drawing logic here only needs a GuiGraphics, so
 * it doesn't depend on which delta-type the callback turns out to pass.
 */
public final class OverlayHud {

    private OverlayHud() {}

    public static void renderOverlay(GuiGraphics context, DataStorage storage,
                                      ProfitTracker tracker, ModConfig config) {
        if (!config.overlayEnabled || !tracker.hasActiveSession()) return;

        Font font = Minecraft.getInstance().font;
        ProfitSession session = tracker.activeSession();
        long now = System.currentTimeMillis();
        long durationMs = session.durationMillis(now);
        Double rate = session.coinsPerHour(now);

        int x = config.overlayX;
        int y = config.overlayY;
        int width = 150;
        int lineHeight = 11;
        int panelHeight = lineHeight * 3 + 8;

        context.fill(x, y, x + width, y + panelHeight, 0xB0141414);

        int textY = y + 4;
        context.drawString(font, Component.literal(session.methodLabel.toUpperCase()),
                x + 5, textY, 0xFF7C4DFF);
        textY += lineHeight;
        context.drawString(font, Component.literal("Profit: " + formatSigned(session.profit())),
                x + 5, textY, 0xFF55FF55);
        textY += lineHeight;
        context.drawString(font, Component.literal(
                "Rate: " + (rate != null ? format(rate) + "/h" : "...") + "  " + formatDuration(durationMs)),
                x + 5, textY, 0xFFDDDDDD);
    }

    private static String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        return h + "h" + m + "m";
    }

    private static String formatSigned(double value) {
        return (value >= 0 ? "+" : "") + format(value);
    }

    private static String format(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000) return String.format("%.2fB", value / 1_000_000_000);
        if (abs >= 1_000_000) return String.format("%.2fM", value / 1_000_000);
        if (abs >= 1_000) return String.format("%.2fK", value / 1_000);
        return String.format("%.0f", value);
    }
}
