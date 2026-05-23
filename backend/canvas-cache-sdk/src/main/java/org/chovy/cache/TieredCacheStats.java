package org.chovy.cache;

public record TieredCacheStats(
        String name,
        long l1Size,
        long l1HitCount,
        long l1MissCount,
        long l2HitCount,
        long l2MissCount,
        long l3LoadCount,
        long penetrationRejectCount,
        long loaderFailureCount
) {
}
