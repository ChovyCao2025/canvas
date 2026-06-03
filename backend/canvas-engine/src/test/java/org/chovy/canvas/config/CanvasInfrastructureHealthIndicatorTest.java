package org.chovy.canvas.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanvasInfrastructureHealthIndicatorTest {

    @Test
    void reportsUpWhenMysqlRedisAndRocketMqAreReachable() throws Exception {
        CanvasInfrastructureHealthIndicator indicator = new CanvasInfrastructureHealthIndicator(
                mysql(true),
                redis("PONG"),
                rocketMq(List.of(new MessageQueue("CANVAS_MQ_TRIGGER", "broker-a", 0))),
                "CANVAS_MQ_TRIGGER");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertComponentStatus(health, "mysql", "UP");
        assertComponentStatus(health, "redis", "UP");
        assertComponentStatus(health, "rocketmq", "UP");
    }

    @Test
    void reportsDownWhenRedisPingFails() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisCallback.class))).thenThrow(new IllegalStateException("redis down"));
        CanvasInfrastructureHealthIndicator indicator = new CanvasInfrastructureHealthIndicator(
                mysql(true),
                redis,
                rocketMq(List.of(new MessageQueue("CANVAS_MQ_TRIGGER", "broker-a", 0))),
                "CANVAS_MQ_TRIGGER");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertComponentStatus(health, "redis", "DOWN");
    }

    @Test
    void reportsDownWhenRocketMqHasNoPublishQueues() throws Exception {
        CanvasInfrastructureHealthIndicator indicator = new CanvasInfrastructureHealthIndicator(
                mysql(true),
                redis("PONG"),
                rocketMq(List.of()),
                "CANVAS_MQ_TRIGGER");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertComponentStatus(health, "rocketmq", "DOWN");
    }

    @SuppressWarnings("unchecked")
    private static void assertComponentStatus(Health health, String component, String status) {
        Map<String, Object> details = (Map<String, Object>) health.getDetails().get(component);
        assertThat(details).containsEntry("status", status);
    }

    private static DataSource mysql(boolean valid) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(valid);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");
        return dataSource;
    }

    @SuppressWarnings("unchecked")
    private static StringRedisTemplate redis(String ping) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(connection.ping()).thenReturn(ping);
        when(redis.execute(any(RedisCallback.class)))
                .thenAnswer(invocation -> ((RedisCallback<String>) invocation.getArgument(0))
                        .doInRedis(connection));
        return redis;
    }

    private static RocketMQTemplate rocketMq(List<MessageQueue> queues) throws Exception {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        DefaultMQProducer producer = mock(DefaultMQProducer.class);
        when(template.getProducer()).thenReturn(producer);
        when(producer.getProducerGroup()).thenReturn("PID_CANVAS_ENGINE");
        when(producer.fetchPublishMessageQueues("CANVAS_MQ_TRIGGER")).thenReturn(queues);
        return template;
    }
}
