package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CircuitBreakerStateListenerTest {

    @Test
    void subscribe_registersRedisPubSubChannel() {
        CircuitBreakerRegistry registry = registry(new FakeRedisTemplate());
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
        CircuitBreakerStateListener listener = new CircuitBreakerStateListener(
                registry, container, "circuit-breaker-events", "cb:");

        listener.subscribe();

        ArgumentCaptor<ChannelTopic> topicCaptor = ArgumentCaptor.forClass(ChannelTopic.class);
        verify(container).addMessageListener(eq(listener), topicCaptor.capture());
        assertThat(topicCaptor.getValue().getTopic()).isEqualTo("circuit-breaker-events");
    }

    @Test
    void onStateChangeMessage_invalidatesStaleLocalState() {
        FakeRedisTemplate redis = new FakeRedisTemplate();
        CircuitBreakerRegistry registry = registry(redis);
        CircuitBreakerStateListener listener = new CircuitBreakerStateListener(
                registry, mock(RedisMessageListenerContainer.class), "circuit-breaker-events", "cb:");

        redis.forceState("cb:listener-service", CircuitBreakerRegistry.CircuitBreaker.State.OPEN);
        assertThat(registry.getState("listener-service"))
                .isEqualTo(CircuitBreakerRegistry.CircuitBreaker.State.OPEN);

        redis.forceState("cb:listener-service", CircuitBreakerRegistry.CircuitBreaker.State.CLOSED);
        listener.onMessage(message("circuit-breaker-events", "cb:listener-service:CLOSED"), null);

        assertThat(registry.getState("listener-service"))
                .isEqualTo(CircuitBreakerRegistry.CircuitBreaker.State.CLOSED);
    }

    @Test
    void onStateChangeMessage_updatesLocalStateFromEventPayload() {
        CircuitBreakerRegistry registry = registry(new FakeRedisTemplate());
        CircuitBreakerStateListener listener = new CircuitBreakerStateListener(
                registry, mock(RedisMessageListenerContainer.class), "circuit-breaker-events", "cb:");

        listener.onMessage(message("circuit-breaker-events", "cb:event-only-service:OPEN"), null);

        assertThat(registry.getState("event-only-service"))
                .isEqualTo(CircuitBreakerRegistry.CircuitBreaker.State.OPEN);
    }

    private static CircuitBreakerRegistry registry(FakeRedisTemplate redis) {
        return new CircuitBreakerRegistry(redis, 3, 30, 3, "cb:", "circuit-breaker-events", 10);
    }

    private static Message message(String channel, String body) {
        return new Message() {
            @Override
            public byte[] getBody() {
                return body.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public byte[] getChannel() {
                return channel.getBytes(StandardCharsets.UTF_8);
            }
        };
    }

    private static final class FakeRedisTemplate extends StringRedisTemplate {
        private final Map<String, CircuitBreakerRegistry.CircuitBreaker.State> states = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public synchronized <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            String action = String.valueOf(args[0]);
            CircuitBreakerRegistry.CircuitBreaker.State state =
                    states.getOrDefault(keys.get(0), CircuitBreakerRegistry.CircuitBreaker.State.CLOSED);
            if ("READ".equals(action)) {
                return (T) (state.name() + "|1|0");
            }
            throw new IllegalArgumentException("unsupported action for listener fake: " + action);
        }

        void forceState(String redisKey, CircuitBreakerRegistry.CircuitBreaker.State state) {
            states.put(redisKey, state);
        }
    }
}
