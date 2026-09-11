package com.example.skytracker.tracker;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads coin amounts from the sidebar scoreboard SkyBlock already renders on
 * the player's own screen. This is client-visible display text, not packet
 * data extraction or server-authoritative state - equivalent to a human
 * reading their own screen and typing the number in themselves, just automated
 * for convenience. No network traffic, no automation of gameplay actions.
 *
 * NOTE ON API SURFACE: the Scoreboard/Objective/DisplaySlot classes here use
 * long-standing Mojang (official) names and should be stable, but the exact
 * method used to fetch a line's rendered text has shifted between versions
 * (PlayerTeam formatting, ScoreHolder vs. String keys, etc.). Verify this
 * compiles against your exact 26.1.2 Minecraft jar - if `getPlayerScores` or
 * `ScoreHolder` don't match, check the current Minecraft source for the
 * Scoreboard class's line-rendering path and adjust accordingly.
 */
public final class ScoreboardReader {

    // Matches lines like "Purse: 1,234,567" or "Purse: 1.2M" etc.
    private static final Pattern PURSE_PATTERN =
            Pattern.compile("Purse:\\s*([0-9.,]+)([kKmMbB]?)");
    private static final Pattern BANK_PATTERN =
            Pattern.compile("Bank:\\s*([0-9.,]+)([kKmMbB]?)");

    private ScoreboardReader() {}

    public static Double readPurse() {
        return readAmountMatching(PURSE_PATTERN);
    }

    public static Double readBank() {
        return readAmountMatching(BANK_PATTERN);
    }

    private static Double readAmountMatching(Pattern pattern) {
        for (String line : currentSidebarLines()) {
            String plain = stripColorCodes(line);
            Matcher m = pattern.matcher(plain);
            if (m.find()) {
                return parseShorthandNumber(m.group(1), m.group(2));
            }
        }
        return null;
    }

    private static Iterable<String> currentSidebarLines() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return java.util.Collections.emptyList();

        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebarObjective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebarObjective == null) return java.util.Collections.emptyList();

        java.util.List<String> lines = new java.util.ArrayList<>();
        Collection<ScoreHolder> holders = scoreboard.getTrackedPlayers();
        for (ScoreHolder holder : holders) {
            // The sidebar's visible text for a given "player" entry is
            // typically driven through a team prefix/suffix or the holder's
            // own name, depending on how the server built the scoreboard.
            // SkyBlock encodes each line's text this way rather than through
            // the numeric score value itself.
            Component displayName = holder.getFeedbackDisplayName();
            if (displayName != null) {
                lines.add(displayName.getString());
            }
        }
        return lines;
    }

    private static String stripColorCodes(String s) {
        return s.replaceAll("(?i)\u00A7[0-9A-FK-OR]", "");
    }

    /**
     * Parses "1,234,567" or "12.5" with a "K"/"M"/"B" suffix into a raw double.
     * Returns null (never a fabricated guess) if the text doesn't parse cleanly.
     */
    private static Double parseShorthandNumber(String numberPart, String suffix) {
        try {
            double value = Double.parseDouble(numberPart.replace(",", ""));
            if (suffix == null || suffix.isEmpty()) return value;
            return switch (Character.toUpperCase(suffix.charAt(0))) {
                case 'K' -> value * 1_000;
                case 'M' -> value * 1_000_000;
                case 'B' -> value * 1_000_000_000;
                default -> value;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
