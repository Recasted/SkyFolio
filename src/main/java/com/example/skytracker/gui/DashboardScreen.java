package com.example.skytracker.gui;

import com.example.skytracker.data.Goal;
import com.example.skytracker.config.ModConfig;
import com.example.skytracker.skins.SkinDatabase;
import com.example.skytracker.storage.DataStorage;
import com.example.skytracker.tracker.ProfitTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

// Ported to official (Mojang) mapping names for MC 26.1.2:
//   DrawContext -> GuiGraphics, Text -> Component, ButtonWidget -> Button,
//   package screen -> screens, .dimensions(...) -> .bounds(...),
//   textRenderer field -> font field.

/**
 * Stage 3+ dashboard: goal list with progress bars, top nav to every other
 * screen (Section 21's tab list), and a per-goal Edit button.
 */
public class DashboardScreen extends Screen {

    private static final int PANEL_COLOR = 0xE0141414;
    private static final int ACCENT_COLOR = 0xFF7C4DFF;
    private static final int TEXT_MUTED = 0xFFAAAAAA;
    private static final int ROW_HEIGHT = 26;

    private final DataStorage storage;
    private final ProfitTracker tracker;
    private final SkinDatabase skinDatabase;
    private final ModConfig config;
    private final List<Button> goalEditButtons = new ArrayList<>();

    public DashboardScreen(DataStorage storage, ProfitTracker tracker, SkinDatabase skinDatabase, ModConfig config) {
        super(Component.literal("SkyBlock Tracker"));
        this.storage = storage;
        this.tracker = tracker;
        this.skinDatabase = skinDatabase;
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;

        // --- Top nav (Section 21's tab list) ---
        String[] labels = {"Profit", "Net Worth", "Skins", "Wishlist", "Settings"};
        int navX = 20;
        for (String label : labels) {
            int width = 90;
            Screen target = switch (label) {
                case "Profit" -> new ProfitScreen(storage, tracker, this);
                case "Net Worth" -> new NetWorthScreen(storage, this);
                case "Skins" -> new SkinsScreen(storage, skinDatabase, this);
                case "Wishlist" -> new WishlistScreen(storage, this);
                default -> new SettingsScreen(config, this);
            };
            this.addRenderableWidget(Button.builder(Component.literal(label),
                    btn -> this.minecraft.setScreen(target))
                    .bounds(navX, 20, width, 20).build());
            navX += width + 4;
        }

        // --- Bottom controls ---
        this.addRenderableWidget(Button.builder(Component.literal("Add Goal"),
                btn -> this.minecraft.setScreen(new GoalEditScreen(storage, this, null)))
                .bounds(centerX - 100, this.height - 40, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(centerX + 110, this.height - 40, 60, 20).build());

        // --- Per-goal edit buttons, laid out alongside each rendered row ---
        goalEditButtons.clear();
        int panelX = 20;
        int panelY = 46; // pushed down to make room for the nav row above
        int panelW = this.width - 40;
        int y = panelY + 34;
        for (Goal goal : storage.goals) {
            Button editBtn = Button.builder(Component.literal("Edit"),
                    btn -> this.minecraft.setScreen(new GoalEditScreen(storage, this, goal)))
                    .bounds(panelX + panelW - 12 - 40, y - 4, 40, 16).build();
            this.addRenderableWidget(editBtn);
            goalEditButtons.add(editBtn);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelX = 20;
        int panelY = 46;
        int panelW = this.width - 40;
        int panelH = this.height - 46 - 60;
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
                renderGoalRow(context, goal, panelX + 12, y, panelW - 24 - 48);
                y += ROW_HEIGHT;
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
        String status = goal.completed ? " \u00A7a[DONE]" : "";
        context.drawString(this.font,
                Component.literal(priorityMark + " \u00A7f" + goal.name + status), x, y, 0xFFFFFFFF);

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
