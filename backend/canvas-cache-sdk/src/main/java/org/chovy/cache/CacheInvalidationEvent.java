package org.chovy.cache;

/**
 * Cross-node local cache invalidation event.
 */
public record CacheInvalidationEvent(String cacheName, String rawKey, long version) {
}
