package org.chovy.cache;

/**
 * Cross-node local cache invalidation event.
 */
public record CacheInvalidationEvent(
        /** 需要失效本地缓存的缓存名称。 */
        String cacheName,
        /** 需要失效的原始业务 key。 */
        String rawKey,
        /** 失效事件对应的缓存版本号。 */
        long version) {
}
