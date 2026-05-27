package org.chovy.canvas.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.chovy.cache.CacheInvalidationEvent;
import org.chovy.cache.TieredCacheManager;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Receives RocketMQ cache invalidation broadcasts and evicts this JVM's L1 caches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${canvas.cache.invalidation.topic:CANVAS_CACHE_INVALIDATE}",
        consumerGroup = "${canvas.cache.invalidation.consumer-group:GID_CANVAS_CACHE_INVALIDATE}",
        selectorType = SelectorType.TAG,
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.BROADCASTING,
        consumeThreadNumber = 2
)
public class RocketMqCacheInvalidationConsumer implements RocketMQListener<MessageExt> {
    private final ObjectMapper objectMapper;
    private final TieredCacheManager cacheManager;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            CacheInvalidationEvent event = objectMapper.readValue(body, CacheInvalidationEvent.class);
            cacheManager.receiveInvalidation(event);
            log.debug("[CACHE_INVALIDATION_MQ] received cache={} key={} version={}",
                    event.cacheName(), event.rawKey(), event.version());
        } catch (Exception e) {
            log.error("[CACHE_INVALIDATION_MQ] invalid message msgId={} body={}: {}",
                    message.getMsgId(), body, e.getMessage(), e);
            throw new IllegalArgumentException("Invalid cache invalidation message: " + e.getMessage(), e);
        }
    }
}
