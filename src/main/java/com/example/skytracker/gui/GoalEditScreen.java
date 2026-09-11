package com.example.skytracker.gui;

import com.example.skytracker.data.Goal;
import com.example.skytracker.storage.DataStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Create/edit form for a Goal - name, target amount, category, priority,
 * deadline, notes. Category/priority use simple "click to cycle" buttons
 * rather than a dropdown widget, since vanilla Minecraft has no built-in
 * dropdown component.
 */
public class GoalEditScreen extends Screen {

    private final DataStorage storage;
    private final Goal editingGoal; // null when creating a new goal
    private final Screen parent;

    private EditBox nameField;
    private EditBox descriptionField;
    private EditBox targetAmountField;
    private EditBox notesField;

    private Goal.Category selectedCategory;
    private Goal.Priority selectedPriority;
    private Button categoryButton;
    private Button priorityButton;

    public GoalEditScreen(DataStorage storage, Screen parent, Goal editingGoal) {
        super(Component.literal(editingGoal == null ? "New Goal" : "Edit Goal"));
        this.storage = storage;
        this.parent = parent;
        this.editingGoal = editingGoal;
        this.selectedCategory = editingGoal != null ? editingGoal.category : Goal.Category.MONEY;
        this.selectedPriority = editingGoal != null ? editingGoal.priority : Goal.Priority.MEDIUM;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int fieldWidth = 260;
        int y = 40;
        int rowHeight = 26;

        nameField = new EditBox(this.font, centerX - fieldWidth / 2, y, fieldWidth, 20, Component.literal("Name"));
        nameField.setMaxLength(64);
        if (editingGoal != null) nameField.setValue(editingGoal.name);
        this.addRenderableWidget(nameField);
        y += rowHeight;

        descriptionField = new EditBox(this.font, centerX - fieldWidth / 2, y, fieldWidth, 20, Component.literal("Description"));
        descriptionField.setMaxLength(256);
        if (editingGoal != null) descriptionField.setValue(editingGoal.description == null ? "" : editingGoal.description);
        this.addRenderableWidget(descriptionField);
        y += rowHeight;

        targetAmountField = new EditBox(this.font, centerX - fieldWidth / 2, y, fieldWidth, 20, Component.literal("Target amount"));
        targetAmountField.setMaxLength(20);
        if (editingGoal != null) targetAmountField.setValue(String.valueOf((long) editingGoal.targetAmount));
        this.addRenderableWidget(targetAmountField);
        y += rowHeight;

        categoryButton = Button.builder(categoryLabel(), btn -> {
            selectedCategory = nextCategory(selectedCategory);
            btn.setMessage(categoryLabel());
        }).bounds(centerX - fieldWidth / 2, y, 125, 20).build();
        this.addRenderableWidget(categoryButton);

        priorityButton = Button.builder(priorityLabel(), btn -> {
            selectedPriority = nextPriority(selectedPriority);
            btn.setMessage(priorityLabel());
        }).bounds(centerX + fieldWidth / 2 - 125, y, 125, 20).build();
        this.addRenderableWidget(priorityButton);
        y += rowHeight;

        notesField = new EditBox(this.font, centerX - fieldWidth / 2, y, fieldWidth, 20, Component.literal("Notes"));
        notesField.setMaxLength(256);
        if (editingGoal != null) notesField.setValue(editingGoal.notes == null ? "" : editingGoal.notes);
        this.addRenderableWidget(notesField);
        y += rowHeight + 10;

        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> saveAndClose())
                .bounds(centerX - 130, y, 120, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> this.minecraft.setScreen(parent))
                .bounds(centerX + 10, y, 120, 20).build());

        if (editingGoal != null) {
            y += rowHeight;
            this.addRenderableWidget(Button.builder(Component.literal("Delete goal"), btn -> {
                storage.removeGoal(editingGoal.id);
                this.minecraft.setScreen(parent);
            }).bounds(centerX - 60, y, 120, 20).build());
        }
    }

    private void saveAndClose() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) name = "Unnamed goal";

        double target;
        try {
            target = Double.parseDouble(targetAmountField.getValue().trim());
        } catch (NumberFormatException e) {
            target = editingGoal != null ? editingGoal.targetAmount : 0;
        }

        if (editingGoal == null) {
            Goal goal = new Goal(name, selectedCategory, target);
            goal.description = descriptionField.getValue();
            goal.priority = selectedPriority;
            goal.notes = notesField.getValue();
            storage.addGoal(goal);
        } else {
            editingGoal.name = name;
            editingGoal.description = descriptionField.getValue();
            editingGoal.targetAmount = target;
            editingGoal.category = selectedCategory;
            editingGoal.priority = selectedPriority;
            editingGoal.notes = notesField.getValue();
            storage.saveGoals();
        }
        this.minecraft.setScreen(parent);
    }

    private Component categoryLabel() {
        return Component.literal("Category: " + selectedCategory.name());
    }

    private Component priorityLabel() {
        return Component.literal("Priority: " + selectedPriority.name());
    }

    private static Goal.Category nextCategory(Goal.Category current) {
        Goal.Category[] values = Goal.Category.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    private static Goal.Priority nextPriority(Goal.Priority current) {
        Goal.Priority[] values = Goal.Priority.values();
        return values[(current.ordinal() + 1) % values.length];
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
