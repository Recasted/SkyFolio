package com.example.skytracker;

import com.example.skytracker.data.Goal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoalCalculationTest {

    @Test
    void progressPercent_isZero_atStart() {
        Goal g = new Goal("2B coins", Goal.Category.MONEY, 2_000_000_000);
        g.startingAmount = 0;
        g.currentAmount = 0;
        assertEquals(0.0, g.progressPercent(), 0.0001);
    }

    @Test
    void progressPercent_isHundred_whenTargetReached() {
        Goal g = new Goal("2B coins", Goal.Category.MONEY, 2_000_000_000);
        g.startingAmount = 300_000_000;
        g.currentAmount = 2_000_000_000;
        assertEquals(100.0, g.progressPercent(), 0.0001);
    }

    @Test
    void progressPercent_isClampedToHundred_ifOvershot() {
        Goal g = new Goal("2B coins", Goal.Category.MONEY, 2_000_000_000);
        g.startingAmount = 0;
        g.currentAmount = 3_000_000_000.0;
        assertEquals(100.0, g.progressPercent(), 0.0001);
    }

    @Test
    void remaining_neverNegative() {
        Goal g = new Goal("2B coins", Goal.Category.MONEY, 2_000_000_000);
        g.currentAmount = 5_000_000_000.0;
        assertEquals(0.0, g.remaining(), 0.0001);
    }

    @Test
    void estimatedHoursRemaining_matchesExample_fromSpec() {
        // Current: 300M, Target: 2B, Rate: 25M/h -> Remaining 1.7B, Required 68h
        Goal g = new Goal("2B coins", Goal.Category.MONEY, 2_000_000_000);
        g.currentAmount = 300_000_000;
        Double hours = g.estimatedHoursRemaining(25_000_000);
        assertNotNull(hours);
        assertEquals(68.0, hours, 0.01);
    }

    @Test
    void estimatedHoursRemaining_isNull_whenRateIsZeroOrNegative() {
        Goal g = new Goal("2B coins", Goal.Category.MONEY, 2_000_000_000);
        g.currentAmount = 300_000_000;
        assertNull(g.estimatedHoursRemaining(0));
        assertNull(g.estimatedHoursRemaining(-5));
    }
}
