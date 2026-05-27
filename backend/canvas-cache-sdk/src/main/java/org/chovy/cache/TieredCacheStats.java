package org.chovy.cache;

/**
 * 分层缓存运行指标快照。
 *
 * <p>记录 L1/L2 命中与未命中、L3 加载、穿透拒绝和加载失败等计数，便于观测缓存效果。
 * <p>作为不可变 record 返回，避免调用方修改统计值造成监控口径偏差。
 */
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
