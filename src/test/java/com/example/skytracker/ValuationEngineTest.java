package com.example.skytracker;

import com.example.skytracker.pricing.ManualPriceProvider;
import com.example.skytracker.skins.SkinDatabase;
import com.example.skytracker.valuation.ValuationEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deliberately doesn't construct any ItemStack here - that requires
 * Minecraft's registries to be bootstrapped, which isn't available in a
 * plain JUnit run outside the game. The empty-input case below still
 * exercises the confidence-rating logic without needing that bootstrap.
 */
class ValuationEngineTest {

    @Test
    void valuate_emptyInventory_isUnknownConfidenceAndZeroValue() {
        ValuationEngine engine = new ValuationEngine(new SkinDatabase(), new ManualPriceProvider());
        ValuationEngine.Result result = engine.valuate(List.of());

        assertEquals(0.0, result.totalValue(), 0.0001);
        assertEquals(ValuationEngine.Confidence.UNKNOWN, result.confidence());
    }
}
