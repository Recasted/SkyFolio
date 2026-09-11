package com.example.skytracker.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Goal {

    public enum Category { MONEY, COMBAT, MINING, FISHING, FARMING, COLLECTIONS, SKINS, GEAR, PETS, MISC }
    public enum Priority { CRITICAL, HIGH, MEDIUM, LOW }

    public final String id;
    public String name;
    public String description;

    public double targetAmount;
    public double startingAmount;
    public double currentAmount;

    public Long deadlineEpochMillis; // nullable
    public Priority priority = Priority.MEDIUM;
    public Category category = Category.MISC;
    public boolean completed = false;

    /** IDs of other goals that must be completed first. */
    public List<String> dependencyGoalIds = new ArrayList<>();

    public String notes = "";

    public Goal(String name, Category category, double targetAmount) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.category = category;
        this.targetAmount = targetAmount;
        this.startingAmount = 0;
        this.currentAmount = 0;
    }

    public double progressPercent() {
        if (targetAmount <= 0) return completed ? 100.0 : 0.0;
        double p = 100.0 * (currentAmount - startingAmount) / (targetAmount - startingAmount);
        return Math.max(0.0, Math.min(100.0, p));
    }

    public double remaining() {
        return Math.max(0.0, targetAmount - currentAmount);
    }

    /**
     * Hours remaining at a given coins/hour rate. Returns null if the rate can't
     * make progress (<=0) so callers must show "unknown" rather than fabricating a number.
     */
    public Double estimatedHoursRemaining(double coinsPerHour) {
        if (coinsPerHour <= 0) return null;
        return remaining() / coinsPerHour;
    }
}
