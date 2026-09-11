package com.example.skytracker.tracker;

import com.example.skytracker.data.ProfitSession;
import com.example.skytracker.storage.DataStorage;

/**
 * Owns start/stop/tick for the active profit session. Purely reads client-
 * visible scoreboard text (see ScoreboardReader) on an interval - never
 * touches the network layer and never runs on every single tick (see
 * POLL_INTERVAL_MS) to avoid unnecessary CPU/allocation churn.
 */
public class ProfitTracker {

    private static final long POLL_INTERVAL_MS = 1000; // once a second is plenty

    private final DataStorage storage;
    private long lastPollAt = 0;

    public ProfitTracker(DataStorage storage) {
        this.storage = storage;
    }

    public boolean hasActiveSession() {
        return storage.activeSession != null;
    }

    public ProfitSession activeSession() {
        return storage.activeSession;
    }

    /**
     * Starts a new session. If the scoreboard doesn't currently show a
     * readable purse value, starts at 0 rather than guessing - the user can
     * see this reflected honestly in the "starting purse" display.
     */
    public ProfitSession startSession(String methodLabel) {
        Double purse = ScoreboardReader.readPurse();
        ProfitSession session = new ProfitSession(System.currentTimeMillis(), purse != null ? purse : 0.0);
        session.methodLabel = methodLabel;
        storage.activeSession = session;
        return session;
    }

    public void stopSession() {
        ProfitSession session = storage.activeSession;
        if (session == null) return;
        storage.recordSessionEnd(session, System.currentTimeMillis());
        storage.activeSession = null;
    }

    /** Call once per client tick; internally throttles actual scoreboard reads. */
    public void tick() {
        ProfitSession session = storage.activeSession;
        if (session == null) return;

        long now = System.currentTimeMillis();
        if (now - lastPollAt < POLL_INTERVAL_MS) return;
        lastPollAt = now;

        Double purse = ScoreboardReader.readPurse();
        if (purse != null) {
            session.currentPurse = purse;
        }
        // If unreadable this tick (e.g. player not in SkyBlock, scoreboard
        // hidden), simply skip the update rather than writing a wrong value.
    }
}
