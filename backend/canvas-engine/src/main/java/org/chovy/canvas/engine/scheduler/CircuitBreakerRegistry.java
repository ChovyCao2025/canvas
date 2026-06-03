package org.chovy.canvas.engine.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
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
                        defaultOpenDurationSec, defaultHalfOpenAttempts));
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

        /** 当前熔断器快照，所有状态转换通过 CAS 原子完成。 */
        private final AtomicReference<BreakerState> state =
                new AtomicReference<>(BreakerState.closed());

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
            while (true) {
                BreakerState current = state.get();
                if (current.state() == State.CLOSED) {
                    return;
                }

                if (current.state() == State.OPEN) {
                    // OPEN 冷却期已过才进入 HALF_OPEN 探测，冷却期内直接拒绝节点调用。
                    if (System.currentTimeMillis() - current.openedAt() < openDurationMs) {
                        throw new CircuitBreakerOpenException("熔断器打开: " + name);
                    }
                    if (state.compareAndSet(current, BreakerState.halfOpen(current.openedAt()))) {
                        log.info("[CIRCUIT] {} OPEN → HALF_OPEN", name);
                    }
                    continue;
                }

                if (current.halfTries() >= halfOpenAttempts) {
                    throw new CircuitBreakerOpenException("熔断器半开探测已满: " + name);
                }
                // HALF_OPEN 只放行有限探测请求，避免恢复期瞬间放大下游压力。
                BreakerState next = current.withHalfTry();
                if (state.compareAndSet(current, next)) {
                    return;
                }
            }
        }

        /** 记录成功：任何状态均重置失败计数，确保"连续失败"语义 */
        public void recordSuccess() {
            while (true) {
                BreakerState current = state.get();
                if (current.state() == State.CLOSED && current.failures() == 0) {
                    return;
                }

                BreakerState next = current.state() == State.OPEN
                        ? BreakerState.open(0, current.openedAt())
                        : BreakerState.closed();
                if (state.compareAndSet(current, next)) {
                    if (current.state() == State.HALF_OPEN) {
                        log.info("[CIRCUIT] {} HALF_OPEN → CLOSED（探测成功）", name);
                    }
                    return;
                }
            }
        }

        /** 记录失败 */
        public void recordFailure() {
            while (true) {
                BreakerState current = state.get();
                long now = System.currentTimeMillis();
                if (current.state() == State.HALF_OPEN) {
                    // 半开探测失败立即重开熔断，并刷新 openedAt 开始新一轮冷却。
                    if (state.compareAndSet(current, BreakerState.open(0, now))) {
                        log.warn("[CIRCUIT] {} HALF_OPEN → OPEN（探测失败）", name);
                        return;
                    }
                    continue;
                }
                if (current.state() == State.OPEN) {
                    return;
                }

                int count = current.failures() + 1;
                BreakerState next = count >= failureThreshold
                        ? BreakerState.open(count, now)
                        : BreakerState.closedWithFailures(count);
                if (state.compareAndSet(current, next)) {
                    if (next.state() == State.OPEN) {
                        // CLOSED 状态累计连续失败达到阈值才打开熔断器。
                        log.warn("[CIRCUIT] {} CLOSED → OPEN（失败次数={}）", name, count);
                    }
                    return;
                }
            }
        }

        private record BreakerState(State state, int failures, int halfTries, long openedAt) {
            static BreakerState closed() {
                return new BreakerState(State.CLOSED, 0, 0, 0);
            }

            static BreakerState closedWithFailures(int failures) {
                return new BreakerState(State.CLOSED, failures, 0, 0);
            }

            static BreakerState open(int failures, long openedAt) {
                return new BreakerState(State.OPEN, failures, 0, openedAt);
            }

            static BreakerState halfOpen(long openedAt) {
                return new BreakerState(State.HALF_OPEN, 0, 0, openedAt);
            }

            BreakerState withHalfTry() {
                return new BreakerState(State.HALF_OPEN, failures, halfTries + 1, openedAt);
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
