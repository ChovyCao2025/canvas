package org.chovy.canvas.domain.bi.query;

public record BiQueryCacheStats(
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
