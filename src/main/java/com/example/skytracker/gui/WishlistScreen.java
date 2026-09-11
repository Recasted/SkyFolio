package com.example.skytracker.gui;

import com.example.skytracker.data.WishlistEntry;
import com.example.skytracker.storage.DataStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Sections 9 and 34-43 of the spec: wishlist with AH watcher toggle per item. */
public class WishlistScreen extends Screen {

    private final DataStorage storage;
    private final Screen parent;
    private EditBox nameField;
    private EditBox maxPriceField;

    public WishlistScreen(DataStorage storage, Screen parent) {
        super(Component.literal("Wishlist"));
        this.storage = storage;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        nameField = new EditBox(this.font, centerX - 180, 30, 160, 20, Component.literal("Item name"));
        nameField.setMaxLength(64);
        this.addRenderableWidget(nameField);

        maxPriceField = new EditBox(this.font, centerX - 10, 30, 100, 20, Component.literal("Max price"));
        maxPriceField.setMaxLength(20);
        this.addRenderableWidget(maxPriceField);

        this.addRenderableWidget(Button.builder(Component.literal("Add"), btn -> addEntry())
                .bounds(centerX + 100, 30, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Back"), btn -> this.minecraft.setScreen(parent))
                .bounds(centerX - 60, this.height - 30, 120, 20).build());

        int y = 70;
        List<WishlistEntry> entries = storage.wishlist;
        for (WishlistEntry entry : entries) {
            Button watchToggle = Button.builder(
                    Component.literal(entry.watchAuctionHouse ? "Watching AH" : "Watch AH: OFF"),
                    btn -> {
                        entry.watchAuctionHouse = !entry.watchAuctionHouse;
                        storage.saveWishlist();
                        this.clearWidgets();
                        this.init();
                    }
            ).bounds(centerX + 60, y, 110, 16).build();
            this.addRenderableWidget(watchToggle);

            Button removeBtn = Button.builder(Component.literal("X"), btn -> {
                storage.removeWishlistEntry(entry.id);
                this.clearWidgets();
                this.init();
            }).bounds(centerX + 180, y, 16, 16).build();
            this.addRenderableWidget(removeBtn);

            y += 22;
        }
    }

    private void addEntry() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) return;
        WishlistEntry entry = new WishlistEntry(name);
        try {
            entry.maxPrice = Double.parseDouble(maxPriceField.getValue().trim());
            entry.targetPrice = entry.maxPrice;
        } catch (NumberFormatException ignored) {
            // No max price given - the entry is still valid, just unfiltered on price.
        }
        storage.addWishlistEntry(entry);
        nameField.setValue("");
        maxPriceField.setValue("");
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        context.drawCenteredString(this.font, this.title, centerX, 10, 0xFFFFFFFF);

        int y = 70;
        for (WishlistEntry entry : storage.wishlist) {
            String priority = switch (entry.priority) {
                case CRITICAL -> "\u00A7c\u25CF";
                case HIGH -> "\u00A76\u25CF";
                case MEDIUM -> "\u00A7e\u25CF";
                case LOW -> "\u00A7a\u25CF";
            };
            String priceText = entry.maxPrice != null ? " (max " + formatShort(entry.maxPrice) + ")" : "";
            context.drawString(this.font, Component.literal(priority + " " + entry.displayName + priceText),
                    centerX - 180, y + 4, 0xFFDDDDDD);
            y += 22;
        }

        if (storage.wishlist.isEmpty()) {
            context.drawCenteredString(this.font, Component.literal("Wishlist is empty."),
                    centerX, y + 10, 0xFF888888);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private static String formatShort(double value) {
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
