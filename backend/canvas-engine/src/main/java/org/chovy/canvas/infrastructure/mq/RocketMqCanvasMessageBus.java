package org.chovy.canvas.infrastructure.mq;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.cache.CacheInvalidationEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RocketMqCanvasMessageBus implements CanvasMessageBus {

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void publishOrderly(String topic, String tag, Object payload, String shardingKey) {
        SendResult result = rocketMQTemplate.syncSendOrderly(destination(topic, tag), payload, shardingKey);
        assertSendOk(result);
    }

    @Override
    public void publishCacheInvalidation(String topic, CacheInvalidationEvent event) {
        SendResult result = rocketMQTemplate.syncSend(destination(topic, event.cacheName()), event);
        assertSendOk(result);
    }

    private void assertSendOk(SendResult result) {
        if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
            SendStatus status = result == null ? null : result.getSendStatus();
            throw new IllegalStateException("RocketMQ send status=" + status);
        }
    }

    private String destination(String topic, String tag) {
        String normalizedTopic = topic == null ? "" : topic.trim();
        String normalizedTag = tag == null ? "" : tag.trim();
        return normalizedTag.isBlank() ? normalizedTopic : normalizedTopic + ":" + normalizedTag;
    }
}
