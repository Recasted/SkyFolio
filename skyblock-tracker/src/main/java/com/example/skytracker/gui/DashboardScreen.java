package com.example.skytracker.gui;

import com.example.skytracker.data.Goal;
import com.example.skytracker.storage.DataStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

// Ported to official (Mojang) mapping names for MC 26.1.2:
//   DrawContext -> GuiGraphics, Text -> Component, ButtonWidget -> Button,
//   package screen -> screens, .dimensions(...) -> .bounds(...),
//   textRenderer field -> font field.
// I'm confident in these (they're long-standing Mojmap names from before the
// unobfuscation), but the exact GuiGraphics.drawString overload for
// with/without shadow is worth a quick check against the current Javadoc -
// I used the most common 5-arg form below.

/**
 * Stage 1 dashboard: shows the goal list with progress bars. Profit/Skins/
 * Wishlist/Inventory/Net-worth/Settings tabs are added in later stages -
 * this establishes the shared tab-bar pattern they'll all reuse.
 */
public class DashboardScreen extends Screen {

    private static final int PANEL_COLOR = 0xE0141414;
    private static final int ACCENT_COLOR = 0xFF7C4DFF;
    private static final int TEXT_MUTED = 0xFFAAAAAA;

    private final DataStorage storage;

    public DashboardScreen(DataStorage storage) {
        super(Component.literal("SkyBlock Tracker"));
        this.storage = storage;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Add Goal"), btn -> {
            // Stage 2 will replace this with a proper Goal-creation screen.
            Goal placeholder = new Goal("New Goal", Goal.Category.MONEY, 1_000_000_000);
            storage.addGoal(placeholder);
        }).bounds(centerX - 100, this.height - 40, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(centerX + 110, this.height - 40, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelX = 20;
        int panelY = 20;
        int panelW = this.width - 40;
        int panelH = this.height - 80;
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_COLOR);

        context.drawString(this.font, Component.literal("PRIORITY GOALS"),
                panelX + 12, panelY + 12, ACCENT_COLOR);

        List<Goal> goals = storage.goals;
        if (goals.isEmpty()) {
            context.drawString(this.font,
                    Component.literal("No goals yet - click Add Goal to create one."),
                    panelX + 12, panelY + 34, TEXT_MUTED);
        } else {
            int y = panelY + 34;
            for (Goal goal : goals) {
                renderGoalRow(context, goal, panelX + 12, y, panelW - 24);
                y += 26;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderGoalRow(GuiGraphics context, Goal goal, int x, int y, int width) {
        String priorityMark = switch (goal.priority) {
            case CRITICAL -> "\u00A7c\u25CF"; // red dot
            case HIGH -> "\u00A76\u25CF";     // orange dot
            case MEDIUM -> "\u00A7e\u25CF";   // yellow dot
            case LOW -> "\u00A7a\u25CF";      // green dot
        };
        context.drawString(this.font,
                Component.literal(priorityMark + " \u00A7f" + goal.name), x, y, 0xFFFFFFFF);

        int barX = x;
        int barY = y + 12;
        int barW = width;
        int barH = 4;
        context.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        int filled = (int) (barW * (goal.progressPercent() / 100.0));
        context.fill(barX, barY, barX + filled, barY + barH, ACCENT_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
