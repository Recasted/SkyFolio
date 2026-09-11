package com.example.skytracker.data;

import java.util.UUID;

/**
 * A single tracked play session. Populated purely from client-visible values
 * (the player's own purse balance as displayed by the server) - never from
 * anything the server didn't already tell this client.
 */
public class ProfitSession {

    public final String id;
    public String methodLabel = "Other"; // e.g. "Lava Fishing", "Gemstone Mining", user-defined

    public long startEpochMillis;
    public Long endEpochMillis; // null while session is active

    public double startingPurse;
    public double currentPurse;

    public ProfitSession(long startEpochMillis, double startingPurse) {
        this.id = UUID.randomUUID().toString();
        this.startEpochMillis = startEpochMillis;
        this.startingPurse = startingPurse;
        this.currentPurse = startingPurse;
    }

    public double profit() {
        return currentPurse - startingPurse;
    }

    public long durationMillis(long nowEpochMillis) {
        long end = endEpochMillis != null ? endEpochMillis : nowEpochMillis;
        return Math.max(0, end - startEpochMillis);
    }

    /** Returns null (rather than a misleading 0) if duration is effectively zero. */
    public Double coinsPerHour(long nowEpochMillis) {
        long durationMs = durationMillis(nowEpochMillis);
        if (durationMs < 1000) return null;
        double hours = durationMs / 3_600_000.0;
        return profit() / hours;
    }
}
