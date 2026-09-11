package com.example.skytracker.data;

/**
 * One entry in the skin catalog (Section 6 of the spec). Loaded from
 * skins.json (data-driven, per Section 26 - never hardcoded into a giant
 * Java class). Ownership fields are populated separately by SkinOwnership
 * records, not stored here, so the catalog itself can be replaced/updated
 * without touching what the user owns.
 */
public class SkinEntry {
    public String id;              // stable catalog id, e.g. "dragon_hunter_skin"
    public String name;
    public String appliesToItem;   // base item this skin applies to
    public String skinType;        // e.g. ARMOR, WEAPON, PET, TOOL, COSMETIC
    public String category;        // Section 6's category list
    public String rarity;
    public Double estimatedValue;  // null = unknown, never fabricated
    public String source;          // where the estimate came from
    public String[] aliases;
    public String[] tags;

    /**
     * Mojang texture-server hash (the part after "/texture/" in a URL like
     * https://textures.minecraft.net/texture/<hash>) for skull-based skins.
     * Many SkyBlock cosmetics (pets, hats, various cosmetic heads) are
     * actually player-head items using this exact mechanism - it's how
     * Minecraft renders every player's skin anywhere in the game, not
     * something specific to this mod. Left null here since I don't have a
     * verified source of real hash values to populate (see SkinDatabase's
     * class doc on the same honesty point for values/rarity).
     * Not applicable to skins that are retextured item MODELS rather than
     * player heads (see SkullTextureFactory's class doc for that distinction).
     */
    public String textureHash;
}
