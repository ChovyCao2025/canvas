package org.chovy.cache;

/**
 * Cross-node local cache invalidation event.
 * @param cacheName 需要失效本地缓存的缓存名称.
 * @param rawKey 需要失效的原始业务 key.
 * @param version 失效事件对应的缓存版本号.
 */
public record CacheInvalidationEvent(
        String cacheName,
        String rawKey,
        long version) {
}
