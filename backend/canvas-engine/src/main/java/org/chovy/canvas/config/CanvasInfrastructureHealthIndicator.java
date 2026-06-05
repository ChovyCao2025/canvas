package org.chovy.canvas.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CanvasInfrastructureHealthIndicator implements HealthIndicator {

    private static final String STATUS = "status";
    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    private final DataSource dataSource;
    private final StringRedisTemplate redis;
    private final RocketMQTemplate rocketMQTemplate;
    private final String rocketMqHealthTopic;

    public CanvasInfrastructureHealthIndicator(
            DataSource dataSource,
            StringRedisTemplate redis,
            RocketMQTemplate rocketMQTemplate,
            @Value("${canvas.health.rocketmq-topic:${canvas.mq.topic:CANVAS_MQ_TRIGGER}}")
            String rocketMqHealthTopic) {
        this.dataSource = dataSource;
        this.redis = redis;
        this.rocketMQTemplate = rocketMQTemplate;
        this.rocketMqHealthTopic = rocketMqHealthTopic;
    }

    @Override
    public Health health() {
        Map<String, Object> mysql = checkMysql();
        Map<String, Object> redisHealth = checkRedis();
        Map<String, Object> rocketMq = checkRocketMq();

        boolean up = isUp(mysql) && isUp(redisHealth) && isUp(rocketMq);
        return (up ? Health.up() : Health.down())
                .withDetail("mysql", mysql)
                .withDetail("redis", redisHealth)
                .withDetail("rocketmq", rocketMq)
                .build();
    }

    private Map<String, Object> checkMysql() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(1);
            if (!valid) {
                return down("connection validation failed");
            }
            Map<String, Object> details = up();
            DatabaseMetaData metaData = connection.getMetaData();
            if (metaData != null) {
                details.put("database", metaData.getDatabaseProductName());
            }
            return details;
        } catch (Exception e) {
            return down(e);
        }
    }

    private Map<String, Object> checkRedis() {
        try {
            String pong = redis.execute((RedisCallback<String>) connection -> connection.ping());
            if (!"PONG".equalsIgnoreCase(pong)) {
                return down("unexpected ping response: " + pong);
            }
            Map<String, Object> details = up();
            details.put("ping", pong);
            return details;
        } catch (Exception e) {
            return down(e);
        }
    }

    private Map<String, Object> checkRocketMq() {
        try {
            DefaultMQProducer producer = rocketMQTemplate.getProducer();
            if (producer == null) {
                return down("producer is not configured");
            }
            List<MessageQueue> queues = producer.fetchPublishMessageQueues(rocketMqHealthTopic);
            if (queues == null || queues.isEmpty()) {
                return down("no publish queues for topic " + rocketMqHealthTopic);
            }
            Map<String, Object> details = up();
            details.put("topic", rocketMqHealthTopic);
            details.put("queueCount", queues.size());
            details.put("producerGroup", producer.getProducerGroup());
            return details;
        } catch (Exception e) {
            return down(e);
        }
    }

    private static boolean isUp(Map<String, Object> details) {
        return UP.equals(details.get(STATUS));
    }

    private static Map<String, Object> up() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(STATUS, UP);
        return details;
    }

    private static Map<String, Object> down(Exception e) {
        return down(e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    private static Map<String, Object> down(String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(STATUS, DOWN);
        details.put("reason", reason);
        return details;
    }
}
