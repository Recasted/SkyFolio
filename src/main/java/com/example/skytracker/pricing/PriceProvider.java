package com.example.skytracker.pricing;

/**
 * Section 15 of the spec: pluggable price source. Never returns a fabricated
 * guess - null means "unknown value" and callers must display that honestly
 * rather than substituting a made-up number.
 */
public interface PriceProvider {
    /** Estimated value for a SkyBlock item ID (e.g. "HYPERION"), or null if unknown. */
    Double getEstimatedValue(String skyblockItemId);

    /** Human-readable name of this source, for "Price source: X" UI labels. */
    String sourceName();
}
