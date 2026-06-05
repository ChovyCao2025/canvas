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
 * Redis Pub/Sub listener used to keep per-instance circuit breaker L1 cache aligned.
 */
@Slf4j
@Component
public class CircuitBreakerStateListener implements MessageListener {

    private final CircuitBreakerRegistry registry;
    private final RedisMessageListenerContainer listenerContainer;
    private final String eventsChannel;
    private final String redisKeyPrefix;

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

    @PostConstruct
    void subscribe() {
        listenerContainer.addMessageListener(this, new ChannelTopic(eventsChannel));
        log.info("[CIRCUIT] subscribed to Redis channel {}", eventsChannel);
    }

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
        } catch (IllegalArgumentException e) {
            registry.invalidateLocalState(serviceKey);
            log.warn("[CIRCUIT] invalid state event {}; invalidated local cache", body);
        }
    }
}
