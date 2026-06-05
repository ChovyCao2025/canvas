package org.chovy.canvas.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.cache.CacheInvalidationEvent;
import org.chovy.cache.CacheInvalidationPublisher;
import org.chovy.canvas.infrastructure.mq.CanvasMessageBus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RocketMQ transport for cross-node cache invalidation events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RocketMqCacheInvalidationPublisher implements CacheInvalidationPublisher {
    /** 画布消息总线，用于广播缓存失效事件。 */
    private final CanvasMessageBus messageBus;

    /** 缓存失效事件发布 topic。 */
    @Value("${canvas.cache.invalidation.topic:CANVAS_CACHE_INVALIDATE}")
    private String topic;

    /**
     * 发布或发送 publish 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param event event 方法执行所需的业务参数
     */
    @Override
    public void publish(CacheInvalidationEvent event) {
        // 使用 cacheName 作为 RocketMQ tag，订阅端仍广播接收，便于按缓存域观察和排查失效事件。
        messageBus.publishCacheInvalidation(topic, event);
        log.debug("[CACHE_INVALIDATION_MQ] published cache={} key={} version={}",
                event.cacheName(), event.rawKey(), event.version());
    }
}
