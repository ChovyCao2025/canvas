package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircuitBreakerRegistryRedisTest {

    private static final String CHANNEL = "circuit-breaker-events";

    @Test
    void recordFailure_belowThreshold_staysClosed() {
        FakeRedisTemplate redis = new FakeRedisTemplate();
        CircuitBreakerRegistry registry = redisRegistry(redis, 3);
        CircuitBreakerRegistry.CircuitBreaker breaker = registry.get("below-threshold");

        breaker.recordFailure();
        breaker.recordFailure();

        assertThat(registry.getState("below-threshold"))
                .isEqualTo(CircuitBreakerRegistry.CircuitBreaker.State.CLOSED);
        assertThatCode(breaker::checkState).doesNotThrowAnyException();
    }

    @Test
    void recordFailure_atThreshold_opensAndPublishesStateChange() {
        FakeRedisTemplate redis = new FakeRedisTemplate();
        CircuitBreakerRegistry registry = redisRegistry(redis, 3);
        CircuitBreakerRegistry.CircuitBreaker breaker = registry.get("at-threshold");

        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();

        assertThat(registry.getState("at-threshold"))
                .isEqualTo(CircuitBreakerRegistry.CircuitBreaker.State.OPEN);
        assertThatThrownBy(breaker::checkState)
                .isInstanceOf(CircuitBreakerRegistry.CircuitBreakerOpenException.class);
        assertThat(redis.publishedMessages()).contains("cb:at-threshold:OPEN");
    }

    @Test
    void recordSuccess_resetsOpenBreakerToClosedAndPublishesRecovery() {
        FakeRedisTemplate redis = new FakeRedisTemplate();
        CircuitBreakerRegistry registry = redisRegistry(redis, 2);
        CircuitBreakerRegistry.CircuitBreaker breaker = registry.get("recovering");
        breaker.recordFailure();
        breaker.recordFailure();

        breaker.recordSuccess();

        assertThat(registry.getState("recovering"))
                .isEqualTo(CircuitBreakerRegistry.CircuitBreaker.State.CLOSED);
        assertThatCode(breaker::checkState).doesNotThrowAnyException();
        assertThat(redis.publishedMessages()).contains("cb:recovering:OPEN", "cb:recovering:CLOSED");
    }

    @Test
    void recordFailure_whileOpenDoesNotExtendOpenWindow() {
        FakeRedisTemplate redis = new FakeRedisTemplate();
        CircuitBreakerRegistry registry = redisRegistry(redis, 1);
        CircuitBreakerRegistry.CircuitBreaker breaker = registry.get("already-open");

        breaker.recordFailure();
        long openedAt = redis.openedAt("cb:already-open");
        int failures = redis.failures("cb:already-open");

        breaker.recordFailure();

        assertThat(redis.openedAt("cb:already-open")).isEqualTo(openedAt);
        assertThat(redis.failures("cb:already-open")).isEqualTo(failures);
        assertThat(redis.publishedMessages()).containsExactly("cb:already-open:OPEN");
    }

    @Test
    void stateIsSharedAcrossTwoRegistryInstances() {
        FakeRedisTemplate redis = new FakeRedisTemplate();
        CircuitBreakerRegistry instanceA = redisRegistry(redis, 2);
        CircuitBreakerRegistry instanceB = redisRegistry(redis, 2);

        instanceA.get("shared-service").recordFailure();
        instanceA.get("shared-service").recordFailure();

        assertThat(instanceB.getState("shared-service"))
                .isEqualTo(CircuitBreakerRegistry.CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> instanceB.get("shared-service").checkState())
                .isInstanceOf(CircuitBreakerRegistry.CircuitBreakerOpenException.class);
    }

    @Test
    void concurrentFailureRecording_hasNoLostUpdates() throws Exception {
        FakeRedisTemplate redis = new FakeRedisTemplate();
        int threshold = 20;
        CircuitBreakerRegistry registry = redisRegistry(redis, threshold);
        CircuitBreakerRegistry.CircuitBreaker breaker = registry.get("concurrent-service");
        int workers = 32;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        List<Exception> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < workers; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    breaker.recordFailure();
                } catch (Exception e) {
                    failures.add(e);
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(failures).isEmpty();
        assertThat(redis.failures("cb:concurrent-service")).isEqualTo(threshold);
        assertThat(registry.getState("concurrent-service"))
                .isEqualTo(CircuitBreakerRegistry.CircuitBreaker.State.OPEN);
    }

    @Test
    void registryConstructor_rejectsNullRedisTemplate() {
        assertThatThrownBy(() -> new CircuitBreakerRegistry(null, 3, 30, 3,
                "cb:", CHANNEL, 10))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("redisTemplate");
    }

    @Test
    void luaScript_containsAtomicHashTransitionsAndPublishesEvents() throws IOException {
        ClassPathResource script = new ClassPathResource("scripts/circuit_breaker_transition.lua");

        assertThat(script.exists()).isTrue();
        String lua = script.getContentAsString(StandardCharsets.UTF_8);
        assertThat(lua)
                .contains("CHECK")
                .contains("FAILURE")
                .contains("SUCCESS")
                .contains("READ")
                .contains("redis.call('HINCRBY'")
                .contains("redis.call('HSET'")
                .contains("redis.call('PUBLISH'")
                .contains("'failures'")
                .contains("'opened_at'")
                .contains("'half_tries'");
    }

    private CircuitBreakerRegistry redisRegistry(FakeRedisTemplate redis, int failureThreshold) {
        return new CircuitBreakerRegistry(redis, failureThreshold, 30, 3, "cb:", CHANNEL, 10);
    }

    private static final class FakeRedisTemplate extends StringRedisTemplate {
        private final Map<String, BreakerRecord> records = new ConcurrentHashMap<>();
        private final List<String> publishedMessages = new CopyOnWriteArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public synchronized <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            assertThat(script).as("state transitions must run through the Lua script").isNotNull();
            String action = String.valueOf(args[0]);
            int threshold = Integer.parseInt(String.valueOf(args[1]));
            long openDurationMs = Long.parseLong(String.valueOf(args[2]));
            int halfOpenAttempts = Integer.parseInt(String.valueOf(args[3]));
            long now = Long.parseLong(String.valueOf(args[4]));
            String channel = String.valueOf(args[5]);

            BreakerRecord record = records.computeIfAbsent(keys.get(0), ignored -> new BreakerRecord());
            return (T) switch (action) {
                case "CHECK" -> check(keys.get(0), record, openDurationMs, halfOpenAttempts, now, channel).wire();
                case "FAILURE" -> failure(keys.get(0), record, threshold, now, channel).wire();
                case "SUCCESS" -> success(keys.get(0), record, channel).wire();
                case "READ" -> new TransitionResult(record.state, true, false).wire();
                default -> throw new IllegalArgumentException("unsupported action: " + action);
            };
        }

        List<String> publishedMessages() {
            return new ArrayList<>(publishedMessages);
        }

        int failures(String redisKey) {
            return records.get(redisKey).failures;
        }

        long openedAt(String redisKey) {
            return records.get(redisKey).openedAt;
        }

        private TransitionResult check(String key, BreakerRecord record, long openDurationMs,
                                       int halfOpenAttempts, long now, String channel) {
            boolean changed = false;
            if (record.state == CircuitBreakerRegistry.CircuitBreaker.State.OPEN
                    && now - record.openedAt >= openDurationMs) {
                record.state = CircuitBreakerRegistry.CircuitBreaker.State.HALF_OPEN;
                record.halfTries = 0;
                changed = true;
                publish(key, record.state, channel);
            }
            if (record.state == CircuitBreakerRegistry.CircuitBreaker.State.OPEN) {
                return new TransitionResult(record.state, false, changed);
            }
            if (record.state == CircuitBreakerRegistry.CircuitBreaker.State.HALF_OPEN) {
                record.halfTries++;
                return new TransitionResult(record.state, record.halfTries <= halfOpenAttempts, changed);
            }
            return new TransitionResult(record.state, true, changed);
        }

        private TransitionResult failure(String key, BreakerRecord record, int threshold,
                                         long now, String channel) {
            if (record.state == CircuitBreakerRegistry.CircuitBreaker.State.OPEN) {
                return new TransitionResult(record.state, true, false);
            }
            if (record.state == CircuitBreakerRegistry.CircuitBreaker.State.HALF_OPEN) {
                record.state = CircuitBreakerRegistry.CircuitBreaker.State.OPEN;
                record.failures = threshold;
                record.openedAt = now;
                record.halfTries = 0;
                publish(key, record.state, channel);
                return new TransitionResult(record.state, true, true);
            }

            record.failures++;
            if (record.failures >= threshold) {
                boolean changed = record.state != CircuitBreakerRegistry.CircuitBreaker.State.OPEN;
                record.state = CircuitBreakerRegistry.CircuitBreaker.State.OPEN;
                record.openedAt = now;
                record.halfTries = 0;
                if (changed) {
                    publish(key, record.state, channel);
                }
                return new TransitionResult(record.state, true, changed);
            }
            return new TransitionResult(record.state, true, false);
        }

        private TransitionResult success(String key, BreakerRecord record, String channel) {
            boolean changed = record.state != CircuitBreakerRegistry.CircuitBreaker.State.CLOSED
                    || record.failures != 0
                    || record.halfTries != 0
                    || record.openedAt != 0;
            record.state = CircuitBreakerRegistry.CircuitBreaker.State.CLOSED;
            record.failures = 0;
            record.halfTries = 0;
            record.openedAt = 0;
            if (changed) {
                publish(key, record.state, channel);
            }
            return new TransitionResult(record.state, true, changed);
        }

        private void publish(String key, CircuitBreakerRegistry.CircuitBreaker.State state, String channel) {
            assertThat(channel).isEqualTo(CHANNEL);
            publishedMessages.add(key + ":" + state.name());
        }
    }

    private static final class BreakerRecord {
        CircuitBreakerRegistry.CircuitBreaker.State state = CircuitBreakerRegistry.CircuitBreaker.State.CLOSED;
        int failures;
        int halfTries;
        long openedAt;
    }

    private record TransitionResult(CircuitBreakerRegistry.CircuitBreaker.State state,
                                    boolean allowed,
                                    boolean changed) {
        String wire() {
            return state.name() + "|" + (allowed ? "1" : "0") + "|" + (changed ? "1" : "0");
        }
    }
}
