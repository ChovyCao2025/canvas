package org.chovy.canvas.engine.request;

import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 画布执行重放 Rate Limiter 执行请求治理组件。
 *
 * <p>负责画布执行请求的排队、派发、限流、重放或积压度量，削峰高并发触发流量。
 * <p>该层位于触发入口和 DAG 执行之间，核心目标是保护执行引擎稳定性。
 */
@Component
public class CanvasExecutionReplayRateLimiter {

    /** 时钟源，用于划分本地分钟限流窗口。 */
    private final Clock clock;
    /** 单条重放每分钟允许次数。 */
    private final int singleReplayPerMinute;
    /** 批量重放每分钟允许的请求数。 */
    private final int batchReplayRequestsPerMinute;
    /** Redis 模板，启用分布式重放限流时使用。 */
    private final StringRedisTemplate redis;
    /** 重放限流 Redis key 生成工具。 */
    private final RedisKeyUtil keys;
    /** 当前本地限流窗口所属分钟。 */
    private final AtomicLong windowEpochMinute = new AtomicLong(Long.MIN_VALUE);
    /** 单条重放的本地窗口计数器。 */
    private final AtomicInteger singleReplayCount = new AtomicInteger();
    /** 批量重放请求的本地窗口计数器。 */
    private final AtomicInteger batchReplayRequestCount = new AtomicInteger();

    @Autowired
    public CanvasExecutionReplayRateLimiter(
            StringRedisTemplate redis,
            RedisKeyUtil keys,
            @Value("${canvas.execution-request.replay.single-per-minute:60}") int singleReplayPerMinute,
            @Value("${canvas.execution-request.replay.batch-requests-per-minute:1000}") int batchReplayRequestsPerMinute) {
        this(Clock.systemUTC(), singleReplayPerMinute, batchReplayRequestsPerMinute, redis, keys);
    }

    /**
     * 构造 CanvasExecutionReplayRateLimiter 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param singleReplayPerMinute singleReplayPerMinute 方法执行所需的业务参数
     * @param batchReplayRequestsPerMinute batchReplayRequestsPerMinute 方法执行所需的业务参数
     */
    public CanvasExecutionReplayRateLimiter(int singleReplayPerMinute,
                                            int batchReplayRequestsPerMinute) {
        this(Clock.systemUTC(), singleReplayPerMinute, batchReplayRequestsPerMinute, null, null);
    }

    /**
     * 构造 CanvasExecutionReplayRateLimiter 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param clock clock 方法执行所需的业务参数
     * @param singleReplayPerMinute singleReplayPerMinute 方法执行所需的业务参数
     * @param batchReplayRequestsPerMinute batchReplayRequestsPerMinute 方法执行所需的业务参数
     */
    public CanvasExecutionReplayRateLimiter(Clock clock,
                                            int singleReplayPerMinute,
                                            int batchReplayRequestsPerMinute) {
        this(clock, singleReplayPerMinute, batchReplayRequestsPerMinute, null, null);
    }

    /**
     * 构造 CanvasExecutionReplayRateLimiter 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param clock clock 方法执行所需的业务参数
     * @param singleReplayPerMinute singleReplayPerMinute 方法执行所需的业务参数
     * @param batchReplayRequestsPerMinute batchReplayRequestsPerMinute 方法执行所需的业务参数
     * @param redis redis 方法执行所需的业务参数
     * @param keys keys 对应的缓存键、配置键或业务键
     */
    public CanvasExecutionReplayRateLimiter(Clock clock,
                                            int singleReplayPerMinute,
                                            int batchReplayRequestsPerMinute,
                                            StringRedisTemplate redis,
                                            RedisKeyUtil keys) {
        this.clock = clock;
        this.singleReplayPerMinute = singleReplayPerMinute;
        this.batchReplayRequestsPerMinute = batchReplayRequestsPerMinute;
        this.redis = redis;
        this.keys = keys;
    }

    /**
     * 执行 try Acquire Single Replay 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean tryAcquireSingleReplay() {
        return tryAcquireSingleReplay("system");
    }

    /**
     * 执行 try Acquire Single Replay 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param operator operator 操作人标识
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean tryAcquireSingleReplay(String operator) {
        return tryAcquire("single", operator, singleReplayCount, singleReplayPerMinute, 1);
    }

    /**
     * 执行 try Acquire Batch Replay 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param requestCount requestCount 数量、阈值或分页参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean tryAcquireBatchReplay(int requestCount) {
        return tryAcquireBatchReplay("system", requestCount);
    }

    /**
     * 执行 try Acquire Batch Replay 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param operator operator 操作人标识
     * @param requestCount requestCount 数量、阈值或分页参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean tryAcquireBatchReplay(String operator, int requestCount) {
        return tryAcquire("batch", operator, batchReplayRequestCount, batchReplayRequestsPerMinute, Math.max(1, requestCount));
    }

    /**
     * 执行 try Acquire 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param scope scope 方法执行所需的业务参数
     * @param operator operator 操作人标识
     * @param counter counter 数量、阈值或分页参数
     * @param limit limit 数量、阈值或分页参数
     * @param cost cost 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private synchronized boolean tryAcquire(String scope, String operator, AtomicInteger counter, int limit, int cost) {
        if (limit <= 0) {
            return true;
        }
        long nowMinute = clock.millis() / 60_000L;
        if (redis != null && keys != null) {
            // 有 Redis 时走分布式配额，保证多实例之间的重放限流一致。
            return tryAcquireRedis(scope, operator, nowMinute, limit, cost);
        }
        if (windowEpochMinute.get() != nowMinute) {
            // 本地窗口切换时清零，避免跨分钟累积误伤新的重放批次。
            windowEpochMinute.set(nowMinute);
            singleReplayCount.set(0);
            batchReplayRequestCount.set(0);
        }
        if (counter.get() + cost > limit) {
            // 超过当分钟额度就直接拒绝，给上游回压。
            return false;
        }
        counter.addAndGet(cost);
        return true;
    }

    /**
     * 执行 try Acquire Redis 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param scope scope 方法执行所需的业务参数
     * @param operator operator 操作人标识
     * @param nowMinute nowMinute 方法执行所需的业务参数
     * @param limit limit 数量、阈值或分页参数
     * @param cost cost 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean tryAcquireRedis(String scope, String operator, long nowMinute, int limit, int cost) {
        String key = keys.executionRequestReplayRateLimit(scope, operator, nowMinute);
        // Redis 计数器用原子自增做闸门，适合多实例下的并发重放控制。
        Long count = redis.opsForValue().increment(key, cost);
        if (count == null) {
            return false;
        }
        if (count <= cost) {
            redis.expire(key, Duration.ofSeconds(61));
        }
        return count <= limit;
    }
}
