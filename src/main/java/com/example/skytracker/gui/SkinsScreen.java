package com.example.skytracker.gui;

import com.example.skytracker.data.SkinEntry;
import com.example.skytracker.skins.SkinDatabase;
import com.example.skytracker.skins.SkullTextureFactory;
import com.example.skytracker.storage.DataStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Section 7 of the spec: skin collection UI. Renders a real item icon per
 * row via SkullTextureFactory where a skin has a known texture hash (see
 * that class's doc for exactly what this does and doesn't cover); falls
 * back to a plain icon + text-only info otherwise, since I don't have
 * verified hash data to populate the bundled starter skins.json with.
 */
public class SkinsScreen extends Screen {

    private static final int ICON_SIZE = 16;

    private enum FilterMode { ALL, OWNED, NOT_OWNED }

    private final DataStorage storage;
    private final SkinDatabase database;
    private final Screen parent;

    private EditBox searchField;
    private FilterMode filterMode = FilterMode.ALL;
    private Button filterButton;
    private int scrollOffset = 0;

    public SkinsScreen(DataStorage storage, SkinDatabase database, Screen parent) {
        super(Component.literal("Skin Collection"));
        this.storage = storage;
        this.database = database;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        searchField = new EditBox(this.font, centerX - 150, 40, 220, 20, Component.literal("Search"));
        searchField.setMaxLength(64);
        this.addRenderableWidget(searchField);

        filterButton = Button.builder(filterLabel(), btn -> {
            filterMode = switch (filterMode) {
                case ALL -> FilterMode.OWNED;
                case OWNED -> FilterMode.NOT_OWNED;
                case NOT_OWNED -> FilterMode.ALL;
            };
            btn.setMessage(filterLabel());
            this.clearWidgets();
            this.init();
        }).bounds(centerX + 80, 40, 100, 20).build();
        this.addRenderableWidget(filterButton);

        this.addRenderableWidget(Button.builder(Component.literal("Back"), btn -> this.minecraft.setScreen(parent))
                .bounds(centerX - 60, this.height - 30, 120, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Import pet + helmet skins"), btn -> {
            btn.active = false;
            btn.setMessage(Component.literal("Importing..."));
            database.importPetAndHelmetSkinsFromSkyblockItemEmojis().thenAccept(count -> {
                // Callback runs on the HTTP client's own thread, not the render
                // thread - only touch Minecraft's screen state back on the main
                // thread via execute(), matching how Minecraft itself expects
                // cross-thread UI updates to happen.
                this.minecraft.execute(() -> {
                    btn.active = true;
                    btn.setMessage(Component.literal(count > 0
                            ? "Imported " + count + " - reopen to refresh"
                            : "Import failed - see log"));
                });
            });
        }).bounds(centerX - 340, 40, 180, 20).build());

        List<SkinEntry> filtered = filteredSkins();
        int rowY = 70;
        int rowHeight = 20;
        int visibleRows = (this.height - 100) / rowHeight;
        int end = Math.min(filtered.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < end; i++) {
            SkinEntry skin = filtered.get(i);
            boolean owned = storage.ownsSkin(skin.id);
            Button toggle = Button.builder(Component.literal(owned ? "Owned \u2713" : "Mark owned"), btn -> {
                if (owned) {
                    storage.markSkinNotOwned(skin.id);
                } else {
                    storage.markSkinOwned(skin.id, 1);
                }
                this.clearWidgets();
                this.init();
            }).bounds(centerX + 100, rowY, 100, 16).build();
            this.addRenderableWidget(toggle);
            rowY += rowHeight;
        }
    }

    private Component filterLabel() {
        return Component.literal("Filter: " + filterMode.name());
    }

    private List<SkinEntry> filteredSkins() {
        String query = searchField != null ? searchField.getValue().toLowerCase() : "";
        return database.all().stream()
                .filter(s -> query.isEmpty() || (s.name != null && s.name.toLowerCase().contains(query)))
                .filter(s -> switch (filterMode) {
                    case ALL -> true;
                    case OWNED -> storage.ownsSkin(s.id);
                    case NOT_OWNED -> !storage.ownsSkin(s.id);
                })
                .sorted(Comparator.comparing(s -> s.name == null ? "" : s.name))
                .collect(Collectors.toList());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        // NOTE ON API SURFACE: GuiGraphics.renderItem(ItemStack, int, int) is a
        // long-standing Mojmap name for drawing an item icon (predates 26.1),
        // so this should be stable - flagging alongside the project's other
        // API-surface notes for completeness rather than out of specific doubt.

        long ownedCount = database.all().stream().filter(s -> storage.ownsSkin(s.id)).count();
        int total = database.all().size();
        double pct = total > 0 ? 100.0 * ownedCount / total : 0.0;

        context.drawCenteredString(this.font, Component.literal(
                "Owned: " + ownedCount + " / " + total + "  (" + String.format("%.1f", pct) + "%)"),
                centerX, 20, 0xFFFFFFFF);

        List<SkinEntry> filtered = filteredSkins();
        int rowY = 70;
        int rowHeight = 20;
        int visible = Math.min(filtered.size(), scrollOffset + (this.height - 100) / rowHeight);
        for (SkinEntry skin : filtered.subList(Math.min(scrollOffset, filtered.size()), visible)) {
            String valueText = skin.estimatedValue != null ? formatShort(skin.estimatedValue) : "Unknown value";

            ItemStack displayStack = SkullTextureFactory.getDisplayStack(skin.textureHash);
            context.renderItem(displayStack, centerX - 150, rowY - 2);

            context.drawString(this.font, Component.literal(
                    skin.name + "  \u00A77[" + skin.rarity + "]  " + valueText),
                    centerX - 150 + ICON_SIZE + 6, rowY + 4, 0xFFDDDDDD);
            rowY += rowHeight;
        }

        if (filtered.isEmpty()) {
            context.drawCenteredString(this.font, Component.literal(
                    "No skins match. The bundled skin database is a small starter set - see README for how to import a full one."),
                    centerX, 90, 0xFF888888);
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
