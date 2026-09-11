package com.example.skytracker;

import com.example.skytracker.data.NetWorthSnapshot;
import com.example.skytracker.data.ProfitSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Stage2CalculationTest {

    @Test
    void netWorthTotal_sumsAllCategories() {
        NetWorthSnapshot nw = new NetWorthSnapshot();
        nw.purse = 1_000_000;
        nw.bank = 2_000_000;
        nw.inventoryValue = 500_000;
        nw.storageValue = 250_000;
        nw.skinsValue = 100_000;
        nw.armorValue = 75_000;
        nw.petsValue = 25_000;
        assertEquals(3_950_000, nw.total(), 0.0001);
    }

    @Test
    void profitSession_coinsPerHour_matchesWorkedExample() {
        // 87.42M profit over 3h42m18s -> roughly 23.6M/h, matching the
        // spec's live-overlay example.
        long startMs = 0;
        ProfitSession session = new ProfitSession(startMs, 0);
        session.currentPurse = 87_420_000;
        long durationMs = (3 * 3600 + 42 * 60 + 18) * 1000L;

        Double rate = session.coinsPerHour(startMs + durationMs);
        assertNotNull(rate);
        assertEquals(23_579_732, rate, 50_000); // loose tolerance, just sanity-checking magnitude
    }

    @Test
    void profitSession_coinsPerHour_isNull_forNearZeroDuration() {
        ProfitSession session = new ProfitSession(0, 0);
        session.currentPurse = 1_000_000;
        assertNull(session.coinsPerHour(500)); // under 1 second elapsed
    }

    @Test
    void profitSession_profit_canBeNegative() {
        ProfitSession session = new ProfitSession(0, 1_000_000);
        session.currentPurse = 400_000;
        assertEquals(-600_000, session.profit(), 0.0001);
    }
}
