# Circuit Breaker Redis Externalization Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Move circuit breaker state from JVM-local ConcurrentHashMap to Redis with Lua script atomicity and cross-instance synchronization via Redis Pub/Sub.

**Architecture:** Redis Hash per circuit breaker key (cb:<serviceKey>) stores state/failures/opened_at. State transitions via Lua scripts for atomicity. Redis Pub/Sub (using RedisMessageListenerContainer, not @RedisListener which doesn't exist) for cross-instance notification. Local Caffeine cache as read-through optimization.

**Tech Stack:** Redis, Lua scripts, Spring Data Redis (RedisMessageListenerContainer), Caffeine

---

### Task 1: Implement Redis-Backed CircuitBreakerRegistry

**Files:**
- Create: `backend/canvas-engine/src/main/resources/scripts/circuit_breaker_transition.lua`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/RedisCircuitBreakerRegistry.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/RedisCircuitBreakerRegistryTest.java`

- [ ] **Step 1: Write failing test**

```java
package org.chovy.canvas.engine.policy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisCircuitBreakerRegistryTest {

    @Autowired
    private RedisCircuitBreakerRegistry registry;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void recordFailure_belowThreshold_staysClosed() {
        registry.recordFailure("test-service-1");
        registry.recordFailure("test-service-1");
        assertThat(registry.getState("test-service-1")).isEqualTo(CircuitState.CLOSED);
    }

    @Test
    void recordFailure_reachesThreshold_transitionsToOpen() {
        // Default threshold is 3
        registry.recordFailure("test-service-2");
        registry.recordFailure("test-service-2");
        registry.recordFailure("test-service-2");
        assertThat(registry.getState("test-service-2")).isEqualTo(CircuitState.OPEN);
    }

    @Test
    void recordSuccess_resetsToClosed() {
        registry.recordFailure("test-service-3");
        registry.recordFailure("test-service-3");
        registry.recordFailure("test-service-3"); // OPEN
        registry.recordSuccess("test-service-3");
        assertThat(registry.getState("test-service-3")).isEqualTo(CircuitState.CLOSED);
    }

    @Test
    void stateSharedAcrossInstances() {
        // Simulate two instances by creating second registry pointing to same Redis
        RedisCircuitBreakerRegistry instance2 = new RedisCircuitBreakerRegistry(
            redisTemplate, 3, 60000
        );

        registry.recordFailure("shared-service");
        registry.recordFailure("shared-service");
        registry.recordFailure("shared-service"); // opens on instance1

        assertThat(instance2.getState("shared-service")).isEqualTo(CircuitState.OPEN);
    }

    @Test
    void concurrentFailureRecording_noRaceCondition() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            pool.submit(() -> registry.recordFailure("concurrent-service"));
        }
        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        // State should be consistently OPEN (10 failures > threshold of 3)
        assertThat(registry.getState("concurrent-service")).isEqualTo(CircuitState.OPEN);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=RedisCircuitBreakerRegistryTest -v`
Expected: FAIL - RedisCircuitBreakerRegistry class not found

- [ ] **Step 3: Create Lua script for atomic state transition**

```lua
-- backend/canvas-engine/src/main/resources/scripts/circuit_breaker_transition.lua
-- KEYS[1] = circuit breaker key (cb:<serviceKey>)
-- ARGV[1] = failure_threshold (e.g. 3)
-- ARGV[2] = open_duration_ms (e.g. 60000)
-- ARGV[3] = current timestamp in ms

local key = KEYS[1]
local failure_threshold = tonumber(ARGV[1])
local open_duration_ms = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local state = redis.call('HGET', key, 'state')
if not state then state = 'CLOSED' end

-- If OPEN, check if half-open window has elapsed
if state == 'OPEN' then
    local opened_at = tonumber(redis.call('HGET', key, 'opened_at') or '0')
    if now - opened_at > open_duration_ms then
        redis.call('HSET', key, 'state', 'HALF_OPEN')
        redis.call('PUBLISH', 'circuit-breaker-events', key .. ':HALF_OPEN')
        return 'HALF_OPEN'
    end
    return 'OPEN'
end

-- Increment failure count atomically
local failures = tonumber(redis.call('HINCRBY', key, 'failures', 1))

-- Transition to OPEN if threshold reached
if failures >= failure_threshold then
    redis.call('HSET', key, 'state', 'OPEN')
    redis.call('HSET', key, 'opened_at', now)
    redis.call('PUBLISH', 'circuit-breaker-events', key .. ':OPEN')
    return 'OPEN'
end

return 'CLOSED'
```

- [ ] **Step 4: Implement RedisCircuitBreakerRegistry**

```java
package org.chovy.canvas.engine.policy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
public class RedisCircuitBreakerRegistry {

    private static final String KEY_PREFIX = "cb:";
    private static final String PUB_SUB_CHANNEL = "circuit-breaker-events";

    @Value("${canvas.circuit-breaker.failure-threshold:3}")
    private int failureThreshold;

    @Value("${canvas.circuit-breaker.open-duration-ms:60000}")
    private long openDurationMs;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<String> transitionScript;

    // Local Caffeine cache for read-through optimization
    private final com.github.benmanes.caffeine.cache.Cache<String, CircuitState> localCache =
        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .expireAfterWrite(java.util.concurrent.TimeUnit.SECONDS, 10)
            .build();

    /**
     * Test-friendly constructor for creating registry with explicit parameters.
     * Used by tests to create cross-instance registries sharing the same Redis.
     */
    RedisCircuitBreakerRegistry(StringRedisTemplate redisTemplate, int failureThreshold, long openDurationMs) {
        this.redisTemplate = redisTemplate;
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
    }

    @PostConstruct
    void init() {
        transitionScript = new DefaultRedisScript<>();
        transitionScript.setLocation(
            new org.springframework.core.io.ClassPathResource("scripts/circuit_breaker_transition.lua")
        );
        transitionScript.setResultType(String.class);
    }

    public CircuitState recordFailure(String serviceKey) {
        String redisKey = KEY_PREFIX + serviceKey;
        String result = redisTemplate.execute(
            transitionScript,
            List.of(redisKey),
            String.valueOf(failureThreshold),
            String.valueOf(openDurationMs),
            String.valueOf(System.currentTimeMillis())
        );
        CircuitState state = CircuitState.valueOf(result);
        localCache.put(serviceKey, state);
        log.debug("Circuit breaker {}: failure recorded, state={}", serviceKey, state);
        return state;
    }

    public void recordSuccess(String serviceKey) {
        String redisKey = KEY_PREFIX + serviceKey;
        redisTemplate.opsForHash().put(redisKey, "failures", "0");
        redisTemplate.opsForHash().put(redisKey, "state", "CLOSED");
        redisTemplate.convertAndSend(PUB_SUB_CHANNEL, redisKey + ":CLOSED");
        localCache.put(serviceKey, CircuitState.CLOSED);
        log.debug("Circuit breaker {}: success recorded, state=CLOSED", serviceKey);
    }

    public CircuitState getState(String serviceKey) {
        CircuitState cached = localCache.getIfPresent(serviceKey);
        if (cached != null) return cached;

        String redisKey = KEY_PREFIX + serviceKey;
        String stateStr = (String) redisTemplate.opsForHash().get(redisKey, "state");
        CircuitState state = stateStr != null ? CircuitState.valueOf(stateStr) : CircuitState.CLOSED;
        localCache.put(serviceKey, state);
        return state;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=RedisCircuitBreakerRegistryTest -v`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/resources/scripts/circuit_breaker_transition.lua
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/RedisCircuitBreakerRegistry.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/RedisCircuitBreakerRegistryTest.java
git commit -m "feat: implement Redis-backed CircuitBreakerRegistry with Lua atomicity"
```

---

### Task 2: Add Redis Pub/Sub Listener for Cross-Instance Sync

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/CircuitBreakerStateListener.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/CircuitBreakerStateListenerTest.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

- [ ] **Step 1: Write failing test**

```java
package org.chovy.canvas.engine.policy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CircuitBreakerStateListenerTest {

    @Autowired
    private RedisCircuitBreakerRegistry registry;

    @Autowired
    private CircuitBreakerStateListener listener;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void onStateChangeMessage_updatesLocalCache() {
        // Publish a state change message
        redisTemplate.convertAndSend("circuit-breaker-events", "cb:listener-test:OPEN");

        // Wait for listener to process
        Thread.sleep(100);

        // Local cache should reflect the new state
        assertThat(registry.getState("listener-test")).isEqualTo(CircuitState.OPEN);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=CircuitBreakerStateListenerTest -v`
Expected: FAIL - CircuitBreakerStateListener class not found

- [ ] **Step 3: Implement CircuitBreakerStateListener using RedisMessageListenerContainer**

```java
package org.chovy.canvas.engine.policy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Slf4j
@Component
public class CircuitBreakerStateListener implements MessageListener {

    private static final String PUB_SUB_CHANNEL = "circuit-breaker-events";
    private static final String KEY_PREFIX = "cb:";

    @Autowired
    private RedisCircuitBreakerRegistry registry;

    @Autowired
    private RedisMessageListenerContainer listenerContainer;

    @PostConstruct
    void subscribe() {
        listenerContainer.addMessageListener(this, new ChannelTopic(PUB_SUB_CHANNEL));
        log.info("Subscribed to circuit breaker state changes on channel: {}", PUB_SUB_CHANNEL);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        String channel = new String(message.getChannel());

        log.info("Received circuit breaker state change: {}", body);

        // Parse message format: "cb:<serviceKey>:<newState>"
        if (body.startsWith(KEY_PREFIX)) {
            String[] parts = body.substring(KEY_PREFIX.length()).split(":");
            if (parts.length == 2) {
                String serviceKey = parts[0];
                CircuitState newState = CircuitState.valueOf(parts[1]);
                registry.updateLocalCache(serviceKey, newState);
                log.info("Updated local cache: {} -> {}", serviceKey, newState);
            }
        }
    }
}
```

- [ ] **Step 4: Add updateLocalCache method to RedisCircuitBreakerRegistry**

Add this 3-line method to `RedisCircuitBreakerRegistry.java`, after the `getState()` method (around line 223):

```java
// Add to RedisCircuitBreakerRegistry.java — this is a 3-line method addition.
// Insert after the getState method (approximately after line 223 in the class shown above):

public void updateLocalCache(String serviceKey, CircuitState state) {
    localCache.put(serviceKey, state);
}
```

- [ ] **Step 5: Add Redis Pub/Sub configuration to application.yml**

Add these config entries to the existing `canvas:` section in `application.yml`:

```yaml
# Add to application.yml:
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

canvas:
  circuit-breaker:
    failure-threshold: 3
    open-duration-ms: 60000
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=CircuitBreakerStateListenerTest -v`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/CircuitBreakerStateListener.java
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/RedisCircuitBreakerRegistry.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/CircuitBreakerStateListenerTest.java
git add backend/canvas-engine/src/main/resources/application.yml
git commit -m "feat: add Redis Pub/Sub listener for cross-instance circuit breaker state sync"
```