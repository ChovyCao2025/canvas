package org.chovy.canvas.execution.adapter.messaging;

import org.apache.rocketmq.spring.core.RocketMQTemplate;

public class RocketMqTriggerPublisher {

    private final RocketMQTemplate rocketMQTemplate;

    public RocketMqTriggerPublisher(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void publish(String topic, String tag, MqTriggerMessage message) {
        if (rocketMQTemplate != null) {
            rocketMQTemplate.convertAndSend(destination(topic, tag), message);
        }
    }

    public String destination(String topic, String tag) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic is required");
        }
        return tag == null || tag.isBlank() ? topic : topic + ":" + tag;
    }
}
