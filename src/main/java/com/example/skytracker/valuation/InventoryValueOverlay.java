package com.example.skytracker.valuation;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Section 11 of the spec: when a chest/backpack/ender chest/storage screen
 * opens, show an estimated total value panel. This only reads the item
 * stacks the client already has for slots it's already rendering - no
 * automated clicking, no server interaction beyond the normal "open a
 * container" the player themselves triggered.
 *
 * NOTE ON API SURFACE: registers via Fabric API's ScreenEvents (client.screen.v1)
 * - I'm fairly confident in AbstractContainerScreen/Slot/getItem() since
 * they're long-standing Mojmap names unlikely to have shifted in 26.1's
 * unobfuscation move, but ScreenEvents itself is a Fabric API class and
 * could theoretically have been touched by Fabric API's own 26.1 renames -
 * same caveat as HudRenderCallback elsewhere in this project.
 */
public final class InventoryValueOverlay {

    private InventoryValueOverlay() {}

    public static void register(ValuationEngine engine) {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

            ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, delta) ->
                    renderValuationPanel(context, containerScreen, engine));
        });
    }

    private static void renderValuationPanel(GuiGraphics context, AbstractContainerScreen<?> screen,
                                              ValuationEngine engine) {
        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) items.add(stack);
        }
        if (items.isEmpty()) return;

        ValuationEngine.Result result = engine.valuate(items);
        if (result.totalValue() <= 0 && result.confidence() == ValuationEngine.Confidence.UNKNOWN) return;

        Font font = Minecraft.getInstance().font;
        int x = 10;
        int y = 10;
        int width = 160;
        int height = 40;

        context.fill(x, y, x + width, y + height, 0xB0141414);
        context.drawString(font, Component.literal("CONTAINER VALUE"), x + 5, y + 4, 0xFF7C4DFF);
        context.drawString(font, Component.literal("Total: " + formatShort(result.totalValue())),
                x + 5, y + 15, 0xFF55FF55);
        context.drawString(font, Component.literal("Confidence: " + result.confidence()),
                x + 5, y + 26, 0xFFAAAAAA);
    }

    private static String formatShort(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000) return String.format("%.2fB", value / 1_000_000_000);
        if (abs >= 1_000_000) return String.format("%.2fM", value / 1_000_000);
        if (abs >= 1_000) return String.format("%.2fK", value / 1_000);
        return String.format("%.0f", value);
    }
}
