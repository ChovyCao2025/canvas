package org.chovy.canvas.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.cache.CacheInvalidationEvent;
import org.chovy.cache.CacheInvalidationPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RocketMQ transport for cross-node cache invalidation events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RocketMqCacheInvalidationPublisher implements CacheInvalidationPublisher {
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${canvas.cache.invalidation.topic:CANVAS_CACHE_INVALIDATE}")
    private String topic;

    @Override
    public void publish(CacheInvalidationEvent event) {
        rocketMQTemplate.syncSend(topic + ":" + event.cacheName(), event);
        log.debug("[CACHE_INVALIDATION_MQ] published cache={} key={} version={}",
                event.cacheName(), event.rawKey(), event.version());
    }
}
