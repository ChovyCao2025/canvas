package org.chovy.canvas.infrastructure.mq;

import org.chovy.cache.CacheInvalidationEvent;

public interface CanvasMessageBus {

    void publishOrderly(String topic, String tag, Object payload, String shardingKey);

    void publishCacheInvalidation(String topic, CacheInvalidationEvent event);
}
