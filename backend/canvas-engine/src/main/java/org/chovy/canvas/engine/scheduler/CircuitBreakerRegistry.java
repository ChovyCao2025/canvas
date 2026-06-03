package org.chovy.canvas.engine.scheduler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final String SCRIPT_LOCATION = "scripts/circuit_breaker_transition.lua";
    private static final String ACTION_CHECK = "CHECK";
    private static final String ACTION_FAILURE = "FAILURE";
    private static final String ACTION_SUCCESS = "SUCCESS";
    private static final String ACTION_READ = "READ";

    /** 默认连续失败阈值。 */
    private final int defaultFailureThreshold;

    /** 默认熔断打开后的冷却秒数。 */
    private final long defaultOpenDurationSec;

    /** 默认半开状态允许的探测次数。 */
    private final int defaultHalfOpenAttempts;

    /** Redis 阻塞模板；不存在时保留 JVM 本地兼容模式。 */
    private final StringRedisTemplate redisTemplate;

    /** Redis Hash key 前缀，例如 cb:<serviceKey>。 */
    private final String redisKeyPrefix;

    /** Redis Pub/Sub 频道名。 */
    private final String eventsChannel;

    /** Redis Lua 脚本，保证状态读写与事件发布原子执行。 */
    private final DefaultRedisScript<String> transitionScript;

    /** 读取状态的本地短 TTL 缓存，事件监听器会更新该缓存。 */
    private final Cache<String, CircuitBreaker.State> localStateCache;

    private final Clock clock;

    /** 按节点类型缓存的熔断器实例。 */
    private final Map<String, CircuitBreaker> registry = new ConcurrentHashMap<>();

    @Autowired
    public CircuitBreakerRegistry(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            @Value("${canvas.circuit-breaker.default.failure-threshold:5}") int defaultFailureThreshold,
            @Value("${canvas.circuit-breaker.default.open-duration-sec:30}") long defaultOpenDurationSec,
            @Value("${canvas.circuit-breaker.default.half-open-attempts:3}") int defaultHalfOpenAttempts,
            @Value("${canvas.circuit-breaker.redis.key-prefix:cb:}") String redisKeyPrefix,
            @Value("${canvas.circuit-breaker.redis.pub-sub-channel:circuit-breaker-events}") String eventsChannel,
            @Value("${canvas.circuit-breaker.redis.local-cache-ttl-sec:5}") long localCacheTtlSec) {
        this(redisTemplateProvider.getIfAvailable(), defaultFailureThreshold, defaultOpenDurationSec,
                defaultHalfOpenAttempts, redisKeyPrefix, eventsChannel, localCacheTtlSec);
    }

    CircuitBreakerRegistry(StringRedisTemplate redisTemplate,
                           int defaultFailureThreshold,
                           long defaultOpenDurationSec,
                           int defaultHalfOpenAttempts,
                           String redisKeyPrefix,
                           String eventsChannel,
                           long localCacheTtlSec) {
        this(redisTemplate, defaultFailureThreshold, defaultOpenDurationSec, defaultHalfOpenAttempts,
                redisKeyPrefix, eventsChannel, localCacheTtlSec, Clock.systemUTC());
    }

    CircuitBreakerRegistry(StringRedisTemplate redisTemplate,
                           int defaultFailureThreshold,
                           long defaultOpenDurationSec,
                           int defaultHalfOpenAttempts,
                           String redisKeyPrefix,
                           String eventsChannel,
                           long localCacheTtlSec,
                           Clock clock) {
        this.redisTemplate = redisTemplate;
        this.defaultFailureThreshold = defaultFailureThreshold;
        this.defaultOpenDurationSec = defaultOpenDurationSec;
        this.defaultHalfOpenAttempts = defaultHalfOpenAttempts;
        this.redisKeyPrefix = redisKeyPrefix == null || redisKeyPrefix.isBlank() ? "cb:" : redisKeyPrefix;
        this.eventsChannel = eventsChannel == null || eventsChannel.isBlank()
                ? "circuit-breaker-events" : eventsChannel;
        this.transitionScript = new DefaultRedisScript<>();
        this.transitionScript.setLocation(new ClassPathResource(SCRIPT_LOCATION));
        this.transitionScript.setResultType(String.class);
        this.localStateCache = Caffeine.newBuilder()
                .expireAfterWrite(Math.max(1, localCacheTtlSec), TimeUnit.SECONDS)
                .build();
        this.clock = clock;
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
        return registry.computeIfAbsent(nodeType, k ->
                new CircuitBreaker(nodeType, defaultFailureThreshold,
                        defaultOpenDurationSec, defaultHalfOpenAttempts, redisTemplate == null ? null : this));
    }

    CircuitBreaker.State getState(String nodeType) {
        CircuitBreaker.State cached = localStateCache.getIfPresent(nodeType);
        if (cached != null) {
            return cached;
        }

        CircuitBreaker breaker = get(nodeType);
        if (redisTemplate == null) {
            return breaker.localState();
        }
        return runTransition(nodeType, ACTION_READ, breaker.failureThreshold,
                breaker.openDurationMs, breaker.halfOpenAttempts).state();
    }

    void updateLocalState(String serviceKey, CircuitBreaker.State state) {
        localStateCache.put(serviceKey, state);
    }

    void invalidateLocalState(String serviceKey) {
        localStateCache.invalidate(serviceKey);
    }

    private Transition runTransition(String serviceKey, String action, int failureThreshold,
                                     long openDurationMs, int halfOpenAttempts) {
        String redisKey = redisKey(serviceKey);
        String result = redisTemplate.execute(
                transitionScript,
                List.of(redisKey),
                action,
                String.valueOf(failureThreshold),
                String.valueOf(openDurationMs),
                String.valueOf(halfOpenAttempts),
                String.valueOf(clock.millis()),
                eventsChannel
        );
        Transition transition = Transition.parse(result);
        localStateCache.put(serviceKey, transition.state());
        return transition;
    }

    private String redisKey(String serviceKey) {
        return redisKeyPrefix + serviceKey;
    }

    private record Transition(CircuitBreaker.State state, boolean allowed, boolean changed) {
        private static Transition parse(String result) {
            if (result == null || result.isBlank()) {
                throw new IllegalStateException("Redis circuit breaker script returned no state");
            }
            String[] parts = result.split("\\|");
            CircuitBreaker.State state = CircuitBreaker.State.valueOf(parts[0]);
            boolean allowed = parts.length < 2 || "1".equals(parts[1]) || "true".equalsIgnoreCase(parts[1]);
            boolean changed = parts.length >= 3 && ("1".equals(parts[2]) || "true".equalsIgnoreCase(parts[2]));
            return new Transition(state, allowed, changed);
        }
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
        /** Redis 后端；为空时使用原 JVM 本地实现。 */
        private final CircuitBreakerRegistry redisRegistry;

        /** 当前熔断状态。 */
        private volatile State      state     = State.CLOSED;
        /** CLOSED 状态下的连续失败计数。 */
        private final AtomicInteger failures  = new AtomicInteger(0);
        /** HALF_OPEN 状态下已放行的探测次数。 */
        private final AtomicInteger halfTries = new AtomicInteger(0);
        /** 熔断最近一次打开的时间戳。 */
        private volatile long       openedAt  = 0;

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

        private CircuitBreaker(String name, int failureThreshold,
                               long openDurationSec, int halfOpenAttempts,
                               CircuitBreakerRegistry redisRegistry) {
            this.name             = name;
            this.failureThreshold = failureThreshold;
            this.openDurationMs   = openDurationSec * 1000L;
            this.halfOpenAttempts = halfOpenAttempts;
            this.redisRegistry    = redisRegistry;
        }

        /**
         * 调用前检查：OPEN 状态直接抛出，HALF_OPEN 按 attempt 数控制。
         * @throws CircuitBreakerOpenException 熔断打开时
         */
        public void checkState() {
            if (redisRegistry != null) {
                Transition transition = redisRegistry.runTransition(name, ACTION_CHECK,
                        failureThreshold, openDurationMs, halfOpenAttempts);
                if (transition.changed()) {
                    log.info("[CIRCUIT] {} Redis transition -> {}", name, transition.state());
                }
                if (!transition.allowed()) {
                    if (transition.state() == State.HALF_OPEN) {
                        throw new CircuitBreakerOpenException("熔断器半开探测已满: " + name);
                    }
                    throw new CircuitBreakerOpenException("熔断器打开: " + name);
                }
                return;
            }

            if (state == State.CLOSED) return;

            if (state == State.OPEN) {
                // OPEN 冷却期已过才进入 HALF_OPEN 探测，冷却期内直接拒绝节点调用。
                if (System.currentTimeMillis() - openedAt >= openDurationMs) {
                    state = State.HALF_OPEN;
                    halfTries.set(0);
                    log.info("[CIRCUIT] {} OPEN → HALF_OPEN", name);
                } else {
                    throw new CircuitBreakerOpenException("熔断器打开: " + name);
                }
            }

            if (state == State.HALF_OPEN) {
                // HALF_OPEN 只放行有限探测请求，避免恢复期瞬间放大下游压力。
                if (halfTries.incrementAndGet() > halfOpenAttempts) {
                    throw new CircuitBreakerOpenException("熔断器半开探测已满: " + name);
                }
            }
        }

        /** 记录成功：任何状态均重置失败计数，确保"连续失败"语义 */
        public void recordSuccess() {
            if (redisRegistry != null) {
                Transition transition = redisRegistry.runTransition(name, ACTION_SUCCESS,
                        failureThreshold, openDurationMs, halfOpenAttempts);
                if (transition.changed()) {
                    log.info("[CIRCUIT] {} Redis -> CLOSED（成功恢复）", name);
                }
                return;
            }

            if (state == State.HALF_OPEN) {
                state = State.CLOSED;
                log.info("[CIRCUIT] {} HALF_OPEN → CLOSED（探测成功）", name);
            }
            failures.set(0); // CLOSED 状态也重置，防止"偶发失败"积累到阈值
        }

        /** 记录失败 */
        public void recordFailure() {
            if (redisRegistry != null) {
                Transition transition = redisRegistry.runTransition(name, ACTION_FAILURE,
                        failureThreshold, openDurationMs, halfOpenAttempts);
                if (transition.changed() && transition.state() == State.OPEN) {
                    log.warn("[CIRCUIT] {} Redis -> OPEN", name);
                }
                return;
            }

            if (state == State.HALF_OPEN) {
                // 半开探测失败立即重开熔断，并刷新 openedAt 开始新一轮冷却。
                state = State.OPEN;
                openedAt = System.currentTimeMillis();
                log.warn("[CIRCUIT] {} HALF_OPEN → OPEN（探测失败）", name);
                return;
            }

            int count = failures.incrementAndGet();
            if (count >= failureThreshold) {
                // CLOSED 状态累计连续失败达到阈值才打开熔断器。
                state    = State.OPEN;
                openedAt = System.currentTimeMillis();
                log.warn("[CIRCUIT] {} CLOSED → OPEN（失败次数={}）", name, count);
            }
        }

        State localState() {
            return state;
        }

        public enum State {
            /** 熔断关闭，正常放行并统计连续失败。 */
            CLOSED,
            /** 熔断打开，冷却期内直接拒绝调用。 */
            OPEN,
            /** 半开探测，限制少量请求验证下游恢复情况。 */
            HALF_OPEN
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
