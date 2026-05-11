package com.photon.canvas.engine.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    @Value("${canvas.circuit-breaker.default.failure-threshold:5}")
    private int defaultFailureThreshold;

    @Value("${canvas.circuit-breaker.default.open-duration-sec:30}")
    private long defaultOpenDurationSec;

    @Value("${canvas.circuit-breaker.default.half-open-attempts:3}")
    private int defaultHalfOpenAttempts;

    private final Map<String, CircuitBreaker> registry = new ConcurrentHashMap<>();

    public CircuitBreaker get(String nodeType) {
        return registry.computeIfAbsent(nodeType, k ->
                new CircuitBreaker(nodeType, defaultFailureThreshold,
                        defaultOpenDurationSec, defaultHalfOpenAttempts));
    }

    // ── 内部熔断器实现 ────────────────────────────────────────────

    public static class CircuitBreaker {

        private final String   name;
        private final int      failureThreshold;
        private final long     openDurationMs;
        private final int      halfOpenAttempts;

        private volatile State      state     = State.CLOSED;
        private final AtomicInteger failures  = new AtomicInteger(0);
        private final AtomicInteger halfTries = new AtomicInteger(0);
        private volatile long       openedAt  = 0;

        public CircuitBreaker(String name, int failureThreshold,
                               long openDurationSec, int halfOpenAttempts) {
            this.name             = name;
            this.failureThreshold = failureThreshold;
            this.openDurationMs   = openDurationSec * 1000L;
            this.halfOpenAttempts = halfOpenAttempts;
        }

        /**
         * 调用前检查：OPEN 状态直接抛出，HALF_OPEN 按 attempt 数控制。
         * @throws CircuitBreakerOpenException 熔断打开时
         */
        public void checkState() {
            if (state == State.CLOSED) return;

            if (state == State.OPEN) {
                if (System.currentTimeMillis() - openedAt >= openDurationMs) {
                    state = State.HALF_OPEN;
                    halfTries.set(0);
                    log.info("[CIRCUIT] {} OPEN → HALF_OPEN", name);
                } else {
                    throw new CircuitBreakerOpenException("熔断器打开: " + name);
                }
            }

            if (state == State.HALF_OPEN) {
                if (halfTries.incrementAndGet() > halfOpenAttempts) {
                    throw new CircuitBreakerOpenException("熔断器半开探测已满: " + name);
                }
            }
        }

        /** 记录成功 */
        public void recordSuccess() {
            if (state == State.HALF_OPEN) {
                state = State.CLOSED;
                failures.set(0);
                log.info("[CIRCUIT] {} HALF_OPEN → CLOSED（探测成功）", name);
            }
        }

        /** 记录失败 */
        public void recordFailure() {
            if (state == State.HALF_OPEN) {
                state = State.OPEN;
                openedAt = System.currentTimeMillis();
                log.warn("[CIRCUIT] {} HALF_OPEN → OPEN（探测失败）", name);
                return;
            }

            int count = failures.incrementAndGet();
            if (count >= failureThreshold) {
                state    = State.OPEN;
                openedAt = System.currentTimeMillis();
                log.warn("[CIRCUIT] {} CLOSED → OPEN（失败次数={}）", name, count);
            }
        }

        enum State { CLOSED, OPEN, HALF_OPEN }
    }

    /** 熔断器打开时抛出（不可重试） */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String msg) { super(msg); }
    }
}
