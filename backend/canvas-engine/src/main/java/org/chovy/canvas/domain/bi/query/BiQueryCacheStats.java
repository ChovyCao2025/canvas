package org.chovy.canvas.domain.bi.query;

/**
 * BiQueryCacheStats 承载 domain.bi.query 场景中的不可变数据快照。
 * @param provider provider 字段。
 * @param enabled enabled 字段。
 * @param entryCount entryCount 字段。
 * @param maxEntries maxEntries 字段。
 * @param ttlSeconds ttlSeconds 字段。
 * @param hitCount hitCount 字段。
 * @param missCount missCount 字段。
 * @param putCount putCount 字段。
 * @param evictionCount evictionCount 字段。
 */
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
