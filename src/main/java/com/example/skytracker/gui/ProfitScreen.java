package com.example.skytracker.gui;

import com.example.skytracker.data.ProfitSession;
import com.example.skytracker.storage.DataStorage;
import com.example.skytracker.tracker.ProfitTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ProfitScreen extends Screen {

    private final DataStorage storage;
    private final ProfitTracker tracker;
    private final Screen parent;

    private EditBox methodLabelField;
    private Button startStopButton;

    public ProfitScreen(DataStorage storage, ProfitTracker tracker, Screen parent) {
        super(Component.literal("Profit Tracker"));
        this.storage = storage;
        this.tracker = tracker;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        methodLabelField = new EditBox(this.font, centerX - 130, 40, 260, 20,
                Component.literal("Method"));
        methodLabelField.setMaxLength(32);
        methodLabelField.setValue(tracker.hasActiveSession()
                ? tracker.activeSession().methodLabel : "Lava Fishing");
        methodLabelField.setEditable(!tracker.hasActiveSession());
        this.addRenderableWidget(methodLabelField);

        startStopButton = Button.builder(
                Component.literal(tracker.hasActiveSession() ? "Stop Session" : "Start Session"),
                btn -> toggleSession()
        ).bounds(centerX - 60, 66, 120, 20).build();
        this.addRenderableWidget(startStopButton);

        this.addRenderableWidget(Button.builder(Component.literal("Back"), btn -> this.minecraft.setScreen(parent))
                .bounds(centerX - 60, this.height - 30, 120, 20).build());
    }

    private void toggleSession() {
        if (tracker.hasActiveSession()) {
            tracker.stopSession();
        } else {
            String label = methodLabelField.getValue().trim();
            tracker.startSession(label.isEmpty() ? "Other" : label);
        }
        // Rebuild widgets to reflect the new state (button label, field editability).
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        int y = 100;
        if (tracker.hasActiveSession()) {
            ProfitSession session = tracker.activeSession();
            long now = System.currentTimeMillis();
            long durationMs = session.durationMillis(now);
            Double rate = session.coinsPerHour(now);

            context.drawCenteredString(this.font, Component.literal(
                    "Session profit: " + formatSigned(session.profit())), this.width / 2, y, 0xFF55FF55);
            y += 14;
            context.drawCenteredString(this.font, Component.literal(
                    "Time: " + formatDuration(durationMs)), this.width / 2, y, 0xFFAAAAAA);
            y += 14;
            context.drawCenteredString(this.font, Component.literal(
                    "Rate: " + (rate != null ? format(rate) + "/h" : "calculating...")),
                    this.width / 2, y, 0xFFAAAAAA);
            y += 24;
        } else {
            context.drawCenteredString(this.font, Component.literal(
                    "No active session."), this.width / 2, y, 0xFFAAAAAA);
            y += 24;
        }

        context.drawCenteredString(this.font, Component.literal("RECENT SESSIONS"), this.width / 2, y, 0xFF7C4DFF);
        y += 14;

        List<ProfitSession> recent = storage.sessions.reversed();
        int shown = 0;
        for (ProfitSession s : recent) {
            if (shown >= 6) break;
            long dur = s.durationMillis(s.endEpochMillis != null ? s.endEpochMillis : System.currentTimeMillis());
            context.drawCenteredString(this.font, Component.literal(
                    s.methodLabel + ": " + formatSigned(s.profit()) + " over " + formatDuration(dur)),
                    this.width / 2, y, 0xFFDDDDDD);
            y += 12;
            shown++;
        }
        if (shown == 0) {
            context.drawCenteredString(this.font, Component.literal("No completed sessions yet."),
                    this.width / 2, y, 0xFF888888);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private static String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        return h + "h " + m + "m";
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
