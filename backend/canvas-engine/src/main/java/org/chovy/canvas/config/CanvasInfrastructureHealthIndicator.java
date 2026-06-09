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

/**
 * CanvasInfrastructureHealthIndicator 提供 config 场景的 Spring 配置或启动校验。
 */
@Component
public class CanvasInfrastructureHealthIndicator implements HealthIndicator {

    private static final String STATUS = "status";
    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    private final DataSource dataSource;
    private final StringRedisTemplate redis;
    private final RocketMQTemplate rocketMQTemplate;
    private final String rocketMqHealthTopic;

    /**
     * 创建 CanvasInfrastructureHealthIndicator 实例并注入 config 场景依赖。
     * @param dataSource data source 参数，用于 CanvasInfrastructureHealthIndicator 流程中的校验、计算或对象转换。
     * @param redis redis 参数，用于 CanvasInfrastructureHealthIndicator 流程中的校验、计算或对象转换。
     * @param rocketMQTemplate rocket mqtemplate 参数，用于 CanvasInfrastructureHealthIndicator 流程中的校验、计算或对象转换。
     * @param rocketMqHealthTopic rocket mq health topic 参数，用于 CanvasInfrastructureHealthIndicator 流程中的校验、计算或对象转换。
     */
    public CanvasInfrastructureHealthIndicator(
            DataSource dataSource,
            StringRedisTemplate redis,
            RocketMQTemplate rocketMQTemplate,
            @Value("${canvas.health.rocketmq-topic:${canvas.mq.topic:CANVAS_MQ_TRIGGER}}")
            String rocketMqHealthTopic) {
        // 准备本次处理所需的上下文和中间变量。
        this.dataSource = dataSource;
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        this.redis = redis;
        this.rocketMQTemplate = rocketMQTemplate;
        this.rocketMqHealthTopic = rocketMqHealthTopic;
    }

    /**
     * health 查询 config 场景的业务数据。
     * @return 返回 health 流程生成的业务结果。
     */
    @Override
    public Health health() {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> mysql = checkMysql();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        Map<String, Object> redisHealth = checkRedis();
        Map<String, Object> rocketMq = checkRocketMq();

        boolean up = isUp(mysql) && isUp(redisHealth) && isUp(rocketMq);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return (up ? Health.up() : Health.down())
                .withDetail("mysql", mysql)
                .withDetail("redis", redisHealth)
                .withDetail("rocketmq", rocketMq)
                .build();
    }

    /**
     * 检查 MySQL 连接是否可用，并返回数据库产品信息。
     *
     * @return MySQL 健康详情
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return down(e);
        }
    }

    /**
     * 通过 PING 检查 Redis 连接是否可用。
     *
     * @return Redis 健康详情
     */
    private Map<String, Object> checkRedis() {
        try {
            String pong = redis.execute((RedisCallback<String>) connection -> connection.ping());
            if (!"PONG".equalsIgnoreCase(pong)) {
                return down("unexpected ping response: " + pong);
            }
            Map<String, Object> details = up();
            details.put("ping", pong);
            return details;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return down(e);
        }
    }

    /**
     * 检查 RocketMQ 生产者和健康检查主题队列是否可用。
     *
     * @return RocketMQ 健康详情
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return down(e);
        }
    }

    /**
     * 判断单项健康详情是否为 UP。
     *
     * @param details 健康详情 Map
     * @return true 表示该项健康
     */
    private static boolean isUp(Map<String, Object> details) {
        return UP.equals(details.get(STATUS));
    }

    /**
     * 构造 UP 健康详情。
     *
     * @return 状态为 UP 的详情 Map
     */
    private static Map<String, Object> up() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(STATUS, UP);
        return details;
    }

    /**
     * 根据异常构造 DOWN 健康详情。
     *
     * @param e 健康检查异常
     * @return 状态为 DOWN 的详情 Map
     */
    private static Map<String, Object> down(Exception e) {
        return down(e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    /**
     * 根据原因构造 DOWN 健康详情。
     *
     * @param reason 失败原因
     * @return 状态为 DOWN 的详情 Map
     */
    private static Map<String, Object> down(String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(STATUS, DOWN);
        details.put("reason", reason);
        return details;
    }
}
