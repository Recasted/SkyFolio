package com.example.skytracker.valuation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * Section 27 of the spec: identify an item beyond just its display name,
 * since SkyBlock items share vanilla Minecraft item types and rely on NBT/
 * custom data for their real identity.
 *
 * HONESTY NOTE ON API CONFIDENCE: SkyBlock stores its item ID inside a
 * custom NBT compound under an "ExtraAttributes" key (this has been true
 * since SkyBlock's early days and long predates this mod). How to reach
 * that NBT from Java changed with Minecraft's 1.20.5+ "data components"
 * rework, and I can't verify against a live 26.1.2 jar from this sandbox
 * whether CustomData.getUnsafe()/CustomData.update() are still the right
 * calls, or whether they were renamed again in the 26.1 official-mappings
 * move. What's solid: the NBT structure itself (SkyBlock's own format) and
 * the overall approach (read custom NBT, don't parse display names). If
 * `getSkyblockId` doesn't compile, the fix is almost certainly just
 * swapping which DataComponents constant / CustomData method is called -
 * the rest of the valuation pipeline built on top of this doesn't need to
 * change.
 */
public final class ItemIdentifier {

    private ItemIdentifier() {}

    /** Returns the SkyBlock internal item ID (e.g. "HYPERION"), or empty if unidentifiable. */
    public static Optional<String> getSkyblockId(ItemStack stack) {
        CompoundTag extraAttributes = getExtraAttributes(stack);
        if (extraAttributes == null) return Optional.empty();
        if (!extraAttributes.contains("id")) return Optional.empty();
        return extraAttributes.getString("id").isEmpty()
                ? Optional.empty()
                : Optional.of(extraAttributes.getString("id"));
    }

    /** Reads an armor dye/leather-armor color as a hex string, if present (Section 13/14). */
    public static Optional<String> getArmorColorHex(ItemStack stack) {
        CompoundTag display = getSubTag(stack, "display");
        if (display != null && display.contains("color")) {
            int color = display.getInt("color");
            return Optional.of(String.format("#%06X", color & 0xFFFFFF));
        }
        return Optional.empty();
    }

    /** Best-effort read of a custom skin identifier some SkyBlock items store in ExtraAttributes. */
    public static Optional<String> getSkinId(ItemStack stack) {
        CompoundTag extraAttributes = getExtraAttributes(stack);
        if (extraAttributes == null) return Optional.empty();
        for (String key : new String[]{"skin", "item_skin", "new_years_cake_bag_data"}) {
            if (extraAttributes.contains(key)) {
                return Optional.of(extraAttributes.getString(key));
            }
        }
        return Optional.empty();
    }

    private static CompoundTag getExtraAttributes(ItemStack stack) {
        CompoundTag customData = getCustomDataTag(stack);
        if (customData == null) return null;
        return getSubTag(customData, "ExtraAttributes");
    }

    private static CompoundTag getCustomDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        // copyTag() is my best guess at the current (1.20.5+ data-components-era)
        // accessor for CustomData's backing CompoundTag - verify this name
        // against your exact Minecraft jar per the class-level note above;
        // an alternative you may see in some versions is getUnsafe().
        return customData.copyTag();
    }

    private static CompoundTag getSubTag(CompoundTag parent, String key) {
        if (parent == null || !parent.contains(key)) return null;
        Tag tag = parent.get(key);
        return tag instanceof CompoundTag compound ? compound : null;
    }

    private static CompoundTag getSubTag(ItemStack stack, String rootKey) {
        CompoundTag root = getCustomDataTag(stack);
        return getSubTag(root, rootKey);
    }
}
