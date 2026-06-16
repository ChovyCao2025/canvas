package org.chovy.canvas.bi.api;
/**
 * BiQueryCacheStatsView 视图。
 */
public record BiQueryCacheStatsView(
        /**
         * provider 字段值。
         */
        String provider,
        /**
         * enabled 字段值。
         */
        boolean enabled,
        /**
         * entryCount 对应的统计数量。
         */
        int entryCount,
        /**
         * maxEntries 对应的数据集合。
         */
        int maxEntries,
        /**
         * ttlSeconds 对应的数据集合。
         */
        long ttlSeconds,
        /**
         * hitCount 对应的统计数量。
         */
        long hitCount,
        /**
         * missCount 对应的统计数量。
         */
        long missCount,
        /**
         * putCount 对应的统计数量。
         */
        long putCount,
        long evictionCount) {
}
