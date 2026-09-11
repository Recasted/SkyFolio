package com.example.skytracker.gui;

import com.example.skytracker.data.NetWorthSnapshot;
import com.example.skytracker.storage.DataStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Section 16 of the spec (net worth dashboard), Stage 2 version: every
 * category is manually entered here. Stage 4 replaces inventoryValue/
 * storageValue/skinsValue/armorValue with automatic scan results without
 * needing to touch this screen's layout - only where the numbers come from
 * changes.
 */
public class NetWorthScreen extends Screen {

    private final DataStorage storage;
    private final Screen parent;

    private EditBox purseField, bankField, inventoryField, storageField,
            skinsField, armorField, petsField;

    public NetWorthScreen(DataStorage storage, Screen parent) {
        super(Component.literal("Net Worth"));
        this.storage = storage;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int labelX = centerX - 150;
        int fieldX = centerX + 10;
        int y = 40;
        int rowHeight = 22;

        NetWorthSnapshot nw = storage.netWorth;

        purseField = makeField(fieldX, y, String.valueOf((long) nw.purse)); y += rowHeight;
        bankField = makeField(fieldX, y, String.valueOf((long) nw.bank)); y += rowHeight;
        inventoryField = makeField(fieldX, y, String.valueOf((long) nw.inventoryValue)); y += rowHeight;
        storageField = makeField(fieldX, y, String.valueOf((long) nw.storageValue)); y += rowHeight;
        skinsField = makeField(fieldX, y, String.valueOf((long) nw.skinsValue)); y += rowHeight;
        armorField = makeField(fieldX, y, String.valueOf((long) nw.armorValue)); y += rowHeight;
        petsField = makeField(fieldX, y, String.valueOf((long) nw.petsValue)); y += rowHeight + 16;

        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> saveAndClose())
                .bounds(centerX - 130, y, 120, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> this.minecraft.setScreen(parent))
                .bounds(centerX + 10, y, 120, 20).build());
    }

    private EditBox makeField(int x, int y, String initial) {
        EditBox box = new EditBox(this.font, x, y, 140, 18, Component.literal(""));
        box.setMaxLength(20);
        box.setValue(initial);
        this.addRenderableWidget(box);
        return box;
    }

    private void saveAndClose() {
        NetWorthSnapshot nw = storage.netWorth;
        nw.purse = parseOrKeep(purseField, nw.purse);
        nw.bank = parseOrKeep(bankField, nw.bank);
        nw.inventoryValue = parseOrKeep(inventoryField, nw.inventoryValue);
        nw.storageValue = parseOrKeep(storageField, nw.storageValue);
        nw.skinsValue = parseOrKeep(skinsField, nw.skinsValue);
        nw.armorValue = parseOrKeep(armorField, nw.armorValue);
        nw.petsValue = parseOrKeep(petsField, nw.petsValue);
        storage.saveNetWorth();
        this.minecraft.setScreen(parent);
    }

    private static double parseOrKeep(EditBox box, double fallback) {
        try {
            return Double.parseDouble(box.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int labelX = centerX - 150;

        context.drawCenteredString(this.font, this.title, centerX, 20, 0xFFFFFFFF);

        String[] labels = {"Purse", "Bank", "Inventory", "Storage", "Skins", "Armor", "Pets"};
        int y = 44;
        for (String label : labels) {
            context.drawString(this.font, Component.literal(label), labelX, y, 0xFFDDDDDD);
            y += 22;
        }

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredString(this.font, Component.literal(
                "TOTAL: " + formatShort(storage.netWorth.total())), centerX, y + 6, 0xFF55FF55);
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
