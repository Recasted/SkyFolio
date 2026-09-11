package com.example.skytracker.skins;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Base64;
import java.util.UUID;

/**
 * Builds a player-head ItemStack carrying a custom skin texture, purely for
 * DISPLAY in this mod's own screens (not-owned items in the wishlist/skin
 * list have nothing else to render). This uses Minecraft's own built-in
 * mechanism for showing a custom skin on a head - the same one every server
 * uses for decorative player heads, and the same one NEU/Skytils/Firmament
 * use for SkyBlock cosmetic heads - NOT a bundled image file. The texture
 * itself is fetched by Minecraft/Mojang's own client code from
 * textures.minecraft.net at render time; this mod never stores, copies, or
 * ships any image bytes.
 *
 * IMPORTANT SCOPE LIMIT: this only works for skins that are actually player-
 * head items with a custom skin (a real mechanism, common for SkyBlock pet
 * skins, hats, and various cosmetics). It does NOT work for skins that are
 * retextured item MODELS on non-head items (many weapon/armor "skins" work
 * this way instead) - those need the Hypixel resource pack itself to be
 * installed and active, which is entirely up to the player's own client
 * setup and can't be forced or bundled by this mod (see Firmament's own
 * texture-pack-format docs, which describe this as a resource-pack-level
 * concern, not something a mod injects). SkinEntry.textureHash is null for
 * those; getDisplayStack falls back to a plain barrier/paper icon so the UI
 * still shows *something* rather than silently rendering nothing.
 *
 * NOTE ON API SURFACE: ResolvableProfile + DataComponents.PROFILE reflects
 * Minecraft's 1.20.5+ data-components rework of how skulls carry a
 * GameProfile (replacing the older NBT "SkullOwner" tag approach). I'm
 * reasonably confident in this shape since it predates 26.1, but the exact
 * constructor signature has had minor changes across 1.20.5-1.21.x point
 * releases - if this doesn't compile, check ResolvableProfile's current
 * constructors; the base64 texture-property format itself (Mojang's own,
 * not mine) hasn't changed in years and is not the likely failure point.
 */
public final class SkullTextureFactory {

    private SkullTextureFactory() {}

    public static ItemStack getDisplayStack(String textureHash) {
        if (textureHash == null || textureHash.isBlank()) {
            return new ItemStack(Items.PAPER); // honest "no visual available" fallback
        }

        String textureUrl = "https://textures.minecraft.net/texture/" + textureHash;
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
        String base64Value = Base64.getEncoder().encodeToString(json.getBytes());

        GameProfile profile = new GameProfile(UUID.randomUUID(), "SkyTrackerDisplay");
        profile.getProperties().put("textures", new Property("textures", base64Value));

        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, new ResolvableProfile(profile));
        return stack;
    }
}
