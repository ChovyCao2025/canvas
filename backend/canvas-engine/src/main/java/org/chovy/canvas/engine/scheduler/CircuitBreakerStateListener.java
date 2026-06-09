package org.chovy.canvas.engine.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 熔断状态监听器。
 *
 * <p>用于同步各实例的一层本地熔断状态缓存，避免 Redis 后端状态变化后本实例继续使用旧状态。
 */
@Slf4j
@Component
public class CircuitBreakerStateListener implements MessageListener {

    private final CircuitBreakerRegistry registry;
    private final RedisMessageListenerContainer listenerContainer;
    private final String eventsChannel;
    private final String redisKeyPrefix;

    /**
     * 创建 CircuitBreakerStateListener 实例并注入 engine.scheduler 场景依赖。
     * @param registry registry 参数，用于 CircuitBreakerStateListener 流程中的校验、计算或对象转换。
     * @param listenerContainer listener container 参数，用于 CircuitBreakerStateListener 流程中的校验、计算或对象转换。
     * @param eventsChannel events channel 参数，用于 CircuitBreakerStateListener 流程中的校验、计算或对象转换。
     * @param redisKeyPrefix redis key prefix 参数，用于 CircuitBreakerStateListener 流程中的校验、计算或对象转换。
     */
    public CircuitBreakerStateListener(
            CircuitBreakerRegistry registry,
            RedisMessageListenerContainer listenerContainer,
            @Value("${canvas.circuit-breaker.redis.pub-sub-channel:circuit-breaker-events}") String eventsChannel,
            @Value("${canvas.circuit-breaker.redis.key-prefix:cb:}") String redisKeyPrefix) {
        this.registry = registry;
        this.listenerContainer = listenerContainer;
        this.eventsChannel = eventsChannel;
        this.redisKeyPrefix = redisKeyPrefix == null || redisKeyPrefix.isBlank() ? "cb:" : redisKeyPrefix;
    }

    /**
     * 注册 Redis 频道监听。
     */
    @PostConstruct
    void subscribe() {
        listenerContainer.addMessageListener(this, new ChannelTopic(eventsChannel));
        log.info("[CIRCUIT] subscribed to Redis channel {}", eventsChannel);
    }

    /**
     * onMessage 处理 engine.scheduler 场景的业务逻辑。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param pattern pattern 参数，用于 onMessage 流程中的校验、计算或对象转换。
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        if (!body.startsWith(redisKeyPrefix)) {
            return;
        }

        int stateSeparator = body.lastIndexOf(':');
        if (stateSeparator <= redisKeyPrefix.length()) {
            log.warn("[CIRCUIT] ignored malformed state event: {}", body);
            return;
        }

        String serviceKey = body.substring(redisKeyPrefix.length(), stateSeparator);
        String stateValue = body.substring(stateSeparator + 1);
        try {
            CircuitBreakerRegistry.CircuitBreaker.State state =
                    CircuitBreakerRegistry.CircuitBreaker.State.valueOf(stateValue);
            registry.updateLocalState(serviceKey, state);
            log.debug("[CIRCUIT] local state updated from Pub/Sub: {} -> {}", serviceKey, state);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException e) {
            registry.invalidateLocalState(serviceKey);
            log.warn("[CIRCUIT] invalid state event {}; invalidated local cache", body);
        }
    }
}
