package org.chovy.canvas.bi.api;

public record BiQueryCacheStatsView(
        String provider,
        boolean enabled,
        int entryCount,
        int maxEntries,
        long ttlSeconds,
        long hitCount,
        long missCount,
        long putCount,
        long evictionCount) {
}
