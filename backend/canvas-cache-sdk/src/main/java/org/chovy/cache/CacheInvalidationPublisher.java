package org.chovy.cache;

/**
 * Publishes cache invalidation events to an external transport such as MQ.
 */
@FunctionalInterface
public interface CacheInvalidationPublisher {
    void publish(CacheInvalidationEvent event);
}
