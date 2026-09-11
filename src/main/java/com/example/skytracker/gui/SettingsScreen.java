package com.example.skytracker.gui;

import com.example.skytracker.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Section 22/47 of the spec: one place for the toggles that matter day to day. */
public class SettingsScreen extends Screen {

    private final ModConfig config;
    private final Screen parent;

    public SettingsScreen(ModConfig config, Screen parent) {
        super(Component.literal("Settings"));
        this.config = config;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 40;
        int rowHeight = 26;

        this.addRenderableWidget(toggleButton("Live overlay", () -> config.overlayEnabled,
                v -> config.overlayEnabled = v, centerX, y)); y += rowHeight;

        this.addRenderableWidget(toggleButton("Container valuation", () -> config.containerValuationEnabled,
                v -> config.containerValuationEnabled = v, centerX, y)); y += rowHeight;

        this.addRenderableWidget(Button.builder(
                Component.literal("Price source: " + config.priceSource), btn -> {
            config.priceSource = config.priceSource.equals("MANUAL") ? "SKYCOFL" : "MANUAL";
            btn.setMessage(Component.literal("Price source: " + config.priceSource));
            config.save();
        }).bounds(centerX - 100, y, 200, 20).build()); y += rowHeight;

        this.addRenderableWidget(toggleButton("Auction watcher", () -> config.auctionWatcherEnabled,
                v -> config.auctionWatcherEnabled = v, centerX, y)); y += rowHeight;

        this.addRenderableWidget(toggleButton("Chat notifications", () -> config.chatNotificationsEnabled,
                v -> config.chatNotificationsEnabled = v, centerX, y)); y += rowHeight;

        y += 10;
        this.addRenderableWidget(Button.builder(Component.literal("Back"), btn -> this.minecraft.setScreen(parent))
                .bounds(centerX - 60, y, 120, 20).build());
    }

    private Button toggleButton(String label, java.util.function.BooleanSupplier getter,
                                 java.util.function.Consumer<Boolean> setter, int centerX, int y) {
        return Button.builder(Component.literal(label + ": " + (getter.getAsBoolean() ? "ON" : "OFF")), btn -> {
            boolean newValue = !getter.getAsBoolean();
            setter.accept(newValue);
            btn.setMessage(Component.literal(label + ": " + (newValue ? "ON" : "OFF")));
            config.save();
        }).bounds(centerX - 100, y, 200, 20).build();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
