package org.chovy.cache;

/**
 * 分层缓存运行指标快照。
 *
 * <p>记录 L1/L2 命中与未命中、L3 加载、穿透拒绝和加载失败等计数，便于观测缓存效果。
 * <p>作为不可变 record 返回，避免调用方修改统计值造成监控口径偏差。
 */
public record TieredCacheStats(
        /** 缓存实例名称。 */
        String name,
        /** 一级本地缓存当前条目数。 */
        long l1Size,
        /** 一级缓存命中次数。 */
        long l1HitCount,
        /** 一级缓存未命中次数。 */
        long l1MissCount,
        /** 二级缓存命中次数。 */
        long l2HitCount,
        /** 二级缓存未命中次数。 */
        long l2MissCount,
        /** 三级数据加载次数。 */
        long l3LoadCount,
        /** 穿透保护拒绝次数。 */
        long penetrationRejectCount,
        /** 数据加载失败次数。 */
        long loaderFailureCount
) {
}
