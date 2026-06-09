package org.chovy.canvas.infrastructure.mq;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.cache.CacheInvalidationEvent;
import org.springframework.stereotype.Component;

/**
 * RocketMqCanvasMessageBus 封装 infrastructure.mq 场景的基础设施集成。
 */
@Component
@RequiredArgsConstructor
public class RocketMqCanvasMessageBus implements CanvasMessageBus {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * publishOrderly 创建或触发 infrastructure.mq 场景的业务处理。
     * @param topic 待处理业务值，用于规则计算、转换或外部调用。
     * @param tag 待处理业务值，用于规则计算、转换或外部调用。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param shardingKey 业务键，用于在同一租户下定位资源。
     */
    @Override
    public void publishOrderly(String topic, String tag, Object payload, String shardingKey) {
        SendResult result = rocketMQTemplate.syncSendOrderly(destination(topic, tag), payload, shardingKey);
        assertSendOk(result);
    }

    /**
     * publishCacheInvalidation 创建或触发 infrastructure.mq 场景的业务处理。
     * @param topic 待处理业务值，用于规则计算、转换或外部调用。
     * @param event event 参数，用于 publishCacheInvalidation 流程中的校验、计算或对象转换。
     */
    @Override
    public void publishCacheInvalidation(String topic, CacheInvalidationEvent event) {
        SendResult result = rocketMQTemplate.syncSend(destination(topic, event.cacheName()), event);
        assertSendOk(result);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param result result 参数，用于 assertSendOk 流程中的校验、计算或对象转换。
     */
    private void assertSendOk(SendResult result) {
        if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
            SendStatus status = result == null ? null : result.getSendStatus();
            throw new IllegalStateException("RocketMQ send status=" + status);
        }
    }

    /**
     * 执行 destination 流程，围绕 destination 完成校验、计算或结果组装。
     *
     * @param topic 待处理业务值，用于规则计算、转换或外部调用。
     * @param tag 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 destination 生成的文本或业务键。
     */
    private String destination(String topic, String tag) {
        String normalizedTopic = topic == null ? "" : topic.trim();
        String normalizedTag = tag == null ? "" : tag.trim();
        return normalizedTag.isBlank() ? normalizedTopic : normalizedTopic + ":" + normalizedTag;
    }
}
