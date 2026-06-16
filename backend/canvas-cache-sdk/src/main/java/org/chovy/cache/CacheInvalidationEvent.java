package org.chovy.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 跨节点本地缓存失效事件。
 *
 * <p>事件携带缓存名称、原始业务 key 和失效版本，用于通知其他节点清理本地 L1 缓存。
 */
public final class CacheInvalidationEvent {
    /**
     * 需要失效本地缓存的缓存名称。
     */
    @JsonProperty("cacheName")
    private final String cacheName;

    /**
     * 需要失效的原始业务 key。
     */
    @JsonProperty("rawKey")
    private final String rawKey;

    /**
     * 失效事件对应的缓存版本号。
     */
    @JsonProperty("version")
    private final long version;

    /**
     * 创建跨节点本地缓存失效事件。
     *
     * @param cacheName 需要失效本地缓存的缓存名称
     * @param rawKey 需要失效的原始业务 key
     * @param version 失效事件对应的缓存版本号
     */
    @JsonCreator
    public CacheInvalidationEvent(@JsonProperty("cacheName") String cacheName,
                                  @JsonProperty("rawKey") String rawKey,
                                  @JsonProperty("version") long version) {
        this.cacheName = cacheName;
        this.rawKey = rawKey;
        this.version = version;
    }

    /**
     * 返回需要失效本地缓存的缓存名称。
     *
     * @return 缓存名称
     */
    public String cacheName() {
        return cacheName;
    }

    /**
     * 返回需要失效的原始业务 key。
     *
     * @return 原始业务 key
     */
    public String rawKey() {
        return rawKey;
    }

    /**
     * 返回失效事件对应的缓存版本号。
     *
     * @return 缓存版本号
     */
    public long version() {
        return version;
    }

    /**
     * 按缓存名称、原始 key 和版本比较事件。
     *
     * @param o 待比较对象
     * @return 三个事件字段都相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CacheInvalidationEvent that)) {
            return false;
        }
        return version == that.version
                && Objects.equals(cacheName, that.cacheName)
                && Objects.equals(rawKey, that.rawKey);
    }

    /**
     * 生成与事件字段匹配的哈希值。
     *
     * @return 事件哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(cacheName, rawKey, version);
    }

    /**
     * 返回与 record 语义一致的事件字符串。
     *
     * @return 事件字符串
     */
    @Override
    public String toString() {
        return "CacheInvalidationEvent[cacheName=" + cacheName
                + ", rawKey=" + rawKey
                + ", version=" + version
                + ']';
    }
}
