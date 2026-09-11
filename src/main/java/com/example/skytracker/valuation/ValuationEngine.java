package com.example.skytracker.valuation;

import com.example.skytracker.data.SkinEntry;
import com.example.skytracker.pricing.PriceProvider;
import com.example.skytracker.skins.SkinDatabase;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Section 28 of the spec: accepts a set of items (a container's contents)
 * and returns a base/skin/exotic/crystal breakdown with a confidence rating.
 * Deliberately synchronous and cheap per call - it's invoked when a
 * container screen opens, not every tick (Section 24: performance).
 */
public class ValuationEngine {

    public enum Confidence { HIGH, MEDIUM, LOW, UNKNOWN }

    public record Result(double baseValue, double skinPremium, double exoticPremium,
                          double crystalPremium, double totalValue, Confidence confidence) {}

    private final SkinDatabase skinDatabase;
    private final PriceProvider priceProvider;

    public ValuationEngine(SkinDatabase skinDatabase, PriceProvider priceProvider) {
        this.skinDatabase = skinDatabase;
        this.priceProvider = priceProvider;
    }

    public Result valuate(List<ItemStack> items) {
        double baseValue = 0;
        double skinPremium = 0;
        double exoticPremium = 0;
        double crystalPremium = 0;
        int identified = 0;
        int total = 0;

        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            total++;

            var skyblockId = ItemIdentifier.getSkyblockId(stack);
            if (skyblockId.isPresent()) {
                identified++;
                Double base = priceProvider.getEstimatedValue(skyblockId.get());
                if (base != null) {
                    baseValue += base * stack.getCount();
                }
            }

            var skinId = ItemIdentifier.getSkinId(stack);
            if (skinId.isPresent()) {
                SkinEntry skin = skinDatabase.byId(skinId.get());
                if (skin != null && skin.estimatedValue != null) {
                    skinPremium += skin.estimatedValue * stack.getCount();
                    identified++;
                }
            }

            // Exotic/crystal premiums (Sections 13/14): Stage 4 detects an armor
            // color and flags it as "has a custom color", but assigning an exotic
            // premium requires a value the spec explicitly says is subjective and
            // must stay manually editable (Section 13: "Allow manual editing
            // because exotic valuation can be highly subjective") - so this engine
            // deliberately does NOT invent an exotic/crystal dollar value here.
            // ExoticArmorEntry (manually entered/edited by the user) is where that
            // number comes from; wire it in once you have real exotic armor data
            // to test against.
        }

        double totalValue = baseValue + skinPremium + exoticPremium + crystalPremium;
        Confidence confidence = confidenceFor(identified, total);

        return new Result(baseValue, skinPremium, exoticPremium, crystalPremium, totalValue, confidence);
    }

    private static Confidence confidenceFor(int identified, int total) {
        if (total == 0) return Confidence.UNKNOWN;
        double ratio = (double) identified / total;
        if (ratio >= 0.8) return Confidence.HIGH;
        if (ratio >= 0.4) return Confidence.MEDIUM;
        if (ratio > 0) return Confidence.LOW;
        return Confidence.UNKNOWN;
    }
}
