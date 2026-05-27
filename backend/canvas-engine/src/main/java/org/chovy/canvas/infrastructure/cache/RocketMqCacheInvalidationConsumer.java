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
    /** 失效消息反序列化用 Jackson 组件。 */
    private final ObjectMapper objectMapper;
    /** 分层缓存管理器，用于接收跨节点失效事件并清理本机 L1。 */
    private final TieredCacheManager cacheManager;

    /**
     * 消费或监听 on Message 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     *
     * @param message message 方法执行所需的业务参数
     */
    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            CacheInvalidationEvent event = objectMapper.readValue(body, CacheInvalidationEvent.class);
            // BROADCASTING 模式下每个 JVM 都消费同一失效事件，用于同步清理本机 L1 缓存。
            cacheManager.receiveInvalidation(event);
            log.debug("[CACHE_INVALIDATION_MQ] received cache={} key={} version={}",
                    event.cacheName(), event.rawKey(), event.version());
        } catch (Exception e) {
            // 失效消息不可解析时抛出异常，交给 RocketMQ 重试，避免 L1 缓存长期不一致。
            log.error("[CACHE_INVALIDATION_MQ] invalid message msgId={} body={}: {}",
                    message.getMsgId(), body, e.getMessage(), e);
            throw new IllegalArgumentException("Invalid cache invalidation message: " + e.getMessage(), e);
        }
    }
}
