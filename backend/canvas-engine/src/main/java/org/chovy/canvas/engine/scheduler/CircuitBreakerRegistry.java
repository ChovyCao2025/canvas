package org.chovy.canvas.engine.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 每个集成节点类型独立的熔断器（设计文档第 12.5 节）。
 *
 * 状态机：CLOSED → OPEN → HALF_OPEN → CLOSED
 *   - CLOSED  ：正常，统计失败次数
 *   - OPEN    ：熔断，直接抛 CircuitBreakerOpenException
 *   - HALF_OPEN：冷却后探测，成功则恢复 CLOSED，失败则重开 OPEN
 */
@Slf4j
@Component
public class CircuitBreakerRegistry {

    /** 默认连续失败阈值。 */
    @Value("${canvas.circuit-breaker.default.failure-threshold:5}")
    private int defaultFailureThreshold;

    /** 默认熔断打开后的冷却秒数。 */
    @Value("${canvas.circuit-breaker.default.open-duration-sec:30}")
    private long defaultOpenDurationSec;

    /** 默认半开状态允许的探测次数。 */
    @Value("${canvas.circuit-breaker.default.half-open-attempts:3}")
    private int defaultHalfOpenAttempts;

    /** 按节点类型缓存的熔断器实例。 */
    private final Map<String, CircuitBreaker> registry = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final String redisKeyPrefix;
    private final String redisPubSubChannel;
    private final RedisScript<String> transitionScript;

    public CircuitBreakerRegistry() {
        this.redisTemplate = null;
        this.redisKeyPrefix = "cb:";
        this.redisPubSubChannel = "circuit-breaker-events";
        this.transitionScript = null;
    }

    CircuitBreakerRegistry(StringRedisTemplate redisTemplate,
                           int defaultFailureThreshold,
                           long defaultOpenDurationSec,
                           int defaultHalfOpenAttempts,
                           String redisKeyPrefix,
                           String redisPubSubChannel,
                           int ignoredLocalCacheSize) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
        this.defaultFailureThreshold = defaultFailureThreshold;
        this.defaultOpenDurationSec = defaultOpenDurationSec;
        this.defaultHalfOpenAttempts = defaultHalfOpenAttempts;
        this.redisKeyPrefix = redisKeyPrefix == null || redisKeyPrefix.isBlank() ? "cb:" : redisKeyPrefix;
        this.redisPubSubChannel = redisPubSubChannel == null || redisPubSubChannel.isBlank()
                ? "circuit-breaker-events"
                : redisPubSubChannel;
        this.transitionScript = RedisScript.of(
                new ClassPathResource("scripts/circuit_breaker_transition.lua"),
                String.class);
    }

    /**
     * 查询或读取 get 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nodeType nodeType 节点相关对象、标识或配置
     * @return 方法执行后的业务结果
     */
    public CircuitBreaker get(String nodeType) {
        return registry.computeIfAbsent(nodeType, k -> {
            if (redisTemplate == null) {
                return new CircuitBreaker(nodeType, defaultFailureThreshold,
                        defaultOpenDurationSec, defaultHalfOpenAttempts);
            }
            return new CircuitBreaker(nodeType, defaultFailureThreshold,
                    defaultOpenDurationSec, defaultHalfOpenAttempts,
                    (action, threshold, openDurationMs, halfOpenAttempts) ->
                            transitionRedis(nodeType, action, threshold, openDurationMs, halfOpenAttempts));
        });
    }

    CircuitBreaker.State getState(String nodeType) {
        return get(nodeType).currentState();
    }

    void updateLocalState(String nodeType, CircuitBreaker.State state) {
        get(nodeType).forceState(state);
    }

    void invalidateLocalState(String nodeType) {
        registry.remove(nodeType);
    }

    private TransitionResult transitionRedis(String nodeType,
                                             String action,
                                             int threshold,
                                             long openDurationMs,
                                             int halfOpenAttempts) {
        String wire = redisTemplate.execute(transitionScript,
                List.of(redisKeyPrefix + nodeType),
                action,
                String.valueOf(threshold),
                String.valueOf(openDurationMs),
                String.valueOf(halfOpenAttempts),
                String.valueOf(System.currentTimeMillis()),
                redisPubSubChannel);
        return TransitionResult.parse(wire);
    }

    // ── 内部熔断器实现 ────────────────────────────────────────────

    public static class CircuitBreaker {

        /** 熔断器名称，通常为节点类型。 */
        private final String   name;
        /** 连续失败打开熔断的阈值。 */
        private final int      failureThreshold;
        /** 熔断打开后的冷却毫秒数。 */
        private final long     openDurationMs;
        /** 半开状态允许放行的探测次数。 */
        private final int      halfOpenAttempts;
        private final TransitionBackend backend;

        /** 当前熔断状态快照，状态、失败计数、半开探测计数和打开时间必须一起原子变更。 */
        private final AtomicReference<BreakerState> stateRef =
                new AtomicReference<>(BreakerState.closed());
        private final AtomicReference<State> localOverride = new AtomicReference<>();

        /**
         * 构造 CircuitBreaker 实例，并根据入参初始化依赖、配置或内部状态。
         *
         * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
         *
         * @param name name 方法执行所需的业务参数
         * @param failureThreshold failureThreshold 方法执行所需的业务参数
         * @param openDurationSec openDurationSec 时间、过期时间或持续时长参数
         * @param halfOpenAttempts halfOpenAttempts 方法执行所需的业务参数
         */
        public CircuitBreaker(String name, int failureThreshold,
                               long openDurationSec, int halfOpenAttempts) {
            this(name, failureThreshold, openDurationSec, halfOpenAttempts, null);
        }

        CircuitBreaker(String name, int failureThreshold,
                       long openDurationSec, int halfOpenAttempts,
                       TransitionBackend backend) {
            if (failureThreshold <= 0) {
                throw new IllegalArgumentException("failureThreshold must be greater than 0");
            }
            if (openDurationSec < 0) {
                throw new IllegalArgumentException("openDurationSec must not be negative");
            }
            if (halfOpenAttempts <= 0) {
                throw new IllegalArgumentException("halfOpenAttempts must be greater than 0");
            }
            this.name             = name;
            this.failureThreshold = failureThreshold;
            this.openDurationMs   = openDurationSec * 1000L;
            this.halfOpenAttempts = halfOpenAttempts;
            this.backend = backend;
        }

        /**
         * 调用前检查：OPEN 状态直接抛出，HALF_OPEN 按 attempt 数控制。
         * @throws CircuitBreakerOpenException 熔断打开时
         */
        public void checkState() {
            if (backend != null) {
                localOverride.set(null);
                TransitionResult result = backend.transition("CHECK", failureThreshold, openDurationMs, halfOpenAttempts);
                if (!result.allowed()) {
                    throw new CircuitBreakerOpenException("熔断器打开: " + name);
                }
                return;
            }
            while (true) {
                BreakerState current = stateRef.get();
                if (current.state == State.CLOSED) {
                    return;
                }

                if (current.state == State.OPEN) {
                    if (System.currentTimeMillis() - current.openedAt < openDurationMs) {
                        throw new CircuitBreakerOpenException("熔断器打开: " + name);
                    }
                    BreakerState next = new BreakerState(State.HALF_OPEN, 0, 1, current.openedAt);
                    if (stateRef.compareAndSet(current, next)) {
                        log.info("[CIRCUIT] {} OPEN → HALF_OPEN", name);
                        return;
                    }
                    continue;
                }

                if (current.halfTries >= halfOpenAttempts) {
                    throw new CircuitBreakerOpenException("熔断器半开探测已满: " + name);
                }
                BreakerState next = current.withHalfTries(current.halfTries + 1);
                if (stateRef.compareAndSet(current, next)) {
                    return;
                }
            }
        }

        /** 记录成功：任何状态均重置失败计数，确保"连续失败"语义 */
        public void recordSuccess() {
            if (backend != null) {
                localOverride.set(null);
                backend.transition("SUCCESS", failureThreshold, openDurationMs, halfOpenAttempts);
                return;
            }
            while (true) {
                BreakerState current = stateRef.get();
                BreakerState next = switch (current.state) {
                    case CLOSED -> current.failures == 0 && current.halfTries == 0
                            ? current
                            : BreakerState.closed();
                    case OPEN -> current.failures == 0
                            ? current
                            : new BreakerState(State.OPEN, 0, current.halfTries, current.openedAt);
                    case HALF_OPEN -> BreakerState.closed();
                };
                if (next == current || stateRef.compareAndSet(current, next)) {
                    if (current.state == State.HALF_OPEN && next.state == State.CLOSED) {
                        log.info("[CIRCUIT] {} HALF_OPEN → CLOSED（探测成功）", name);
                    }
                    return;
                }
            }
        }

        /** 记录失败 */
        public void recordFailure() {
            if (backend != null) {
                localOverride.set(null);
                backend.transition("FAILURE", failureThreshold, openDurationMs, halfOpenAttempts);
                return;
            }
            while (true) {
                BreakerState current = stateRef.get();
                if (current.state == State.OPEN) {
                    return;
                }

                long now = System.currentTimeMillis();
                BreakerState next;
                if (current.state == State.HALF_OPEN) {
                    next = new BreakerState(State.OPEN, 0, 0, now);
                } else {
                    int count = current.failures + 1;
                    next = count >= failureThreshold
                            ? new BreakerState(State.OPEN, count, 0, now)
                            : new BreakerState(State.CLOSED, count, 0, 0);
                }

                if (stateRef.compareAndSet(current, next)) {
                    if (current.state == State.HALF_OPEN) {
                        log.warn("[CIRCUIT] {} HALF_OPEN → OPEN（探测失败）", name);
                    } else if (next.state == State.OPEN) {
                        log.warn("[CIRCUIT] {} CLOSED → OPEN（失败次数={}）", name, next.failures);
                    }
                    return;
                }
            }
        }

        State currentState() {
            State override = localOverride.get();
            if (override != null) {
                return override;
            }
            if (backend != null) {
                return backend.transition("READ", failureThreshold, openDurationMs, halfOpenAttempts).state();
            }
            return stateRef.get().state;
        }

        int failureCount() {
            return stateRef.get().failures;
        }

        int halfOpenTryCount() {
            return stateRef.get().halfTries;
        }

        void forceState(State state) {
            if (backend != null) {
                localOverride.set(state);
                return;
            }
            long openedAt = state == State.OPEN ? System.currentTimeMillis() : 0L;
            stateRef.set(new BreakerState(state, 0, 0, openedAt));
        }

        private record BreakerState(State state, int failures, int halfTries, long openedAt) {
            static BreakerState closed() {
                return new BreakerState(State.CLOSED, 0, 0, 0);
            }

            BreakerState withHalfTries(int halfTries) {
                return new BreakerState(state, failures, halfTries, openedAt);
            }
        }

        enum State {
            /** 熔断关闭，正常放行并统计连续失败。 */
            CLOSED,
            /** 熔断打开，冷却期内直接拒绝调用。 */
            OPEN,
            /** 半开探测，限制少量请求验证下游恢复情况。 */
            HALF_OPEN
        }
    }

    @FunctionalInterface
    interface TransitionBackend {
        TransitionResult transition(String action, int threshold, long openDurationMs, int halfOpenAttempts);
    }

    record TransitionResult(CircuitBreaker.State state, boolean allowed, boolean changed) {
        static TransitionResult parse(String wire) {
            if (wire == null || wire.isBlank()) {
                return new TransitionResult(CircuitBreaker.State.CLOSED, true, false);
            }
            String[] parts = wire.split("\\|");
            CircuitBreaker.State state = CircuitBreaker.State.valueOf(parts[0]);
            boolean allowed = parts.length < 2 || "1".equals(parts[1]);
            boolean changed = parts.length >= 3 && "1".equals(parts[2]);
            return new TransitionResult(state, allowed, changed);
        }
    }

    /** 熔断器打开时抛出（不可重试） */
    public static class CircuitBreakerOpenException extends RuntimeException {
        /**
         * 构造 CircuitBreakerOpenException 实例，并根据入参初始化依赖、配置或内部状态。
         *
         * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
         *
         * @param msg msg 方法执行所需的业务参数
         */
        public CircuitBreakerOpenException(String msg) { super(msg); }
    }
}
