package org.chovy.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 分层缓存运行指标快照。
 *
 * <p>记录 L1/L2 命中与未命中、L3 加载、穿透拒绝和加载失败等计数，便于观测缓存效果。
 * <p>作为不可变对象返回，避免调用方修改统计值造成监控口径偏差。
 */
public final class TieredCacheStats {
    /**
     * 缓存实例名称。
     */
    @JsonProperty("name")
    private final String name;

    /**
     * 一级本地缓存当前条目数。
     */
    @JsonProperty("l1Size")
    private final long l1Size;

    /**
     * 一级缓存命中次数。
     */
    @JsonProperty("l1HitCount")
    private final long l1HitCount;

    /**
     * 一级缓存未命中次数。
     */
    @JsonProperty("l1MissCount")
    private final long l1MissCount;

    /**
     * 二级缓存命中次数。
     */
    @JsonProperty("l2HitCount")
    private final long l2HitCount;

    /**
     * 二级缓存未命中次数。
     */
    @JsonProperty("l2MissCount")
    private final long l2MissCount;

    /**
     * 三级数据加载次数。
     */
    @JsonProperty("l3LoadCount")
    private final long l3LoadCount;

    /**
     * 穿透保护拒绝次数。
     */
    @JsonProperty("penetrationRejectCount")
    private final long penetrationRejectCount;

    /**
     * 数据加载失败次数。
     */
    @JsonProperty("loaderFailureCount")
    private final long loaderFailureCount;

    /**
     * 创建分层缓存运行指标快照。
     *
     * @param name 缓存实例名称
     * @param l1Size 一级本地缓存当前条目数
     * @param l1HitCount 一级缓存命中次数
     * @param l1MissCount 一级缓存未命中次数
     * @param l2HitCount 二级缓存命中次数
     * @param l2MissCount 二级缓存未命中次数
     * @param l3LoadCount 三级数据加载次数
     * @param penetrationRejectCount 穿透保护拒绝次数
     * @param loaderFailureCount 数据加载失败次数
     */
    @JsonCreator
    public TieredCacheStats(@JsonProperty("name") String name,
                            @JsonProperty("l1Size") long l1Size,
                            @JsonProperty("l1HitCount") long l1HitCount,
                            @JsonProperty("l1MissCount") long l1MissCount,
                            @JsonProperty("l2HitCount") long l2HitCount,
                            @JsonProperty("l2MissCount") long l2MissCount,
                            @JsonProperty("l3LoadCount") long l3LoadCount,
                            @JsonProperty("penetrationRejectCount") long penetrationRejectCount,
                            @JsonProperty("loaderFailureCount") long loaderFailureCount) {
        this.name = name;
        this.l1Size = l1Size;
        this.l1HitCount = l1HitCount;
        this.l1MissCount = l1MissCount;
        this.l2HitCount = l2HitCount;
        this.l2MissCount = l2MissCount;
        this.l3LoadCount = l3LoadCount;
        this.penetrationRejectCount = penetrationRejectCount;
        this.loaderFailureCount = loaderFailureCount;
    }

    /**
     * 返回缓存实例名称。
     *
     * @return 缓存实例名称
     */
    public String name() {
        return name;
    }

    /**
     * 返回一级本地缓存当前条目数。
     *
     * @return L1 当前条目数
     */
    public long l1Size() {
        return l1Size;
    }

    /**
     * 返回一级缓存命中次数。
     *
     * @return L1 命中次数
     */
    public long l1HitCount() {
        return l1HitCount;
    }

    /**
     * 返回一级缓存未命中次数。
     *
     * @return L1 未命中次数
     */
    public long l1MissCount() {
        return l1MissCount;
    }

    /**
     * 返回二级缓存命中次数。
     *
     * @return L2 命中次数
     */
    public long l2HitCount() {
        return l2HitCount;
    }

    /**
     * 返回二级缓存未命中次数。
     *
     * @return L2 未命中次数
     */
    public long l2MissCount() {
        return l2MissCount;
    }

    /**
     * 返回三级数据加载次数。
     *
     * @return L3 加载次数
     */
    public long l3LoadCount() {
        return l3LoadCount;
    }

    /**
     * 返回穿透保护拒绝次数。
     *
     * @return 穿透保护拒绝次数
     */
    public long penetrationRejectCount() {
        return penetrationRejectCount;
    }

    /**
     * 返回数据加载失败次数。
     *
     * @return 数据加载失败次数
     */
    public long loaderFailureCount() {
        return loaderFailureCount;
    }

    /**
     * 按所有指标字段比较两个快照。
     *
     * @param o 待比较对象
     * @return 所有字段相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TieredCacheStats that)) {
            return false;
        }
        return l1Size == that.l1Size
                && l1HitCount == that.l1HitCount
                && l1MissCount == that.l1MissCount
                && l2HitCount == that.l2HitCount
                && l2MissCount == that.l2MissCount
                && l3LoadCount == that.l3LoadCount
                && penetrationRejectCount == that.penetrationRejectCount
                && loaderFailureCount == that.loaderFailureCount
                && Objects.equals(name, that.name);
    }

    /**
     * 生成与所有指标字段匹配的哈希值。
     *
     * @return 指标快照哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, l1Size, l1HitCount, l1MissCount, l2HitCount, l2MissCount,
                l3LoadCount, penetrationRejectCount, loaderFailureCount);
    }

    /**
     * 返回与 record 语义一致的指标快照字符串。
     *
     * @return 指标快照字符串
     */
    @Override
    public String toString() {
        return "TieredCacheStats["
                + "name=" + name
                + ", l1Size=" + l1Size
                + ", l1HitCount=" + l1HitCount
                + ", l1MissCount=" + l1MissCount
                + ", l2HitCount=" + l2HitCount
                + ", l2MissCount=" + l2MissCount
                + ", l3LoadCount=" + l3LoadCount
                + ", penetrationRejectCount=" + penetrationRejectCount
                + ", loaderFailureCount=" + loaderFailureCount
                + ']';
    }
}
