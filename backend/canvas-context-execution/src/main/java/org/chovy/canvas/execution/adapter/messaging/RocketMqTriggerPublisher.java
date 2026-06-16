package org.chovy.canvas.execution.adapter.messaging;

import org.apache.rocketmq.spring.core.RocketMQTemplate;

/**
 * 定义 RocketMqTriggerPublisher 的执行上下文数据结构或业务契约。
 */
public class RocketMqTriggerPublisher {

    /**
     * 保存 rocketMQTemplate 对应的状态或配置。
     */
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 执行 RocketMqTriggerPublisher 对应的业务处理。
     * @param rocketMQTemplate rocketMQTemplate 参数
     */
    public RocketMqTriggerPublisher(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 执行 publish 对应的业务处理。
     * @param topic topic 参数
     * @param tag tag 参数
     * @param message message 参数
     */
    public void publish(String topic, String tag, MqTriggerMessage message) {
        if (rocketMQTemplate != null) {
            rocketMQTemplate.convertAndSend(destination(topic, tag), message);
        }
    }

    /**
     * 执行 destination 对应的业务处理。
     * @param topic topic 参数
     * @param tag tag 参数
     * @return 处理后的结果
     */
    public String destination(String topic, String tag) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic is required");
        }
        return tag == null || tag.isBlank() ? topic : topic + ":" + tag;
    }
}
