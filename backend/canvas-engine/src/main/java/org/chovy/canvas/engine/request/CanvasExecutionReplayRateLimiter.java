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

@Component
public class CanvasExecutionReplayRateLimiter {

    private final Clock clock;
    private final int singleReplayPerMinute;
    private final int batchReplayRequestsPerMinute;
    private final StringRedisTemplate redis;
    private final RedisKeyUtil keys;
    private final AtomicLong windowEpochMinute = new AtomicLong(Long.MIN_VALUE);
    private final AtomicInteger singleReplayCount = new AtomicInteger();
    private final AtomicInteger batchReplayRequestCount = new AtomicInteger();

    @Autowired
    public CanvasExecutionReplayRateLimiter(
            StringRedisTemplate redis,
            RedisKeyUtil keys,
            @Value("${canvas.execution-request.replay.single-per-minute:60}") int singleReplayPerMinute,
            @Value("${canvas.execution-request.replay.batch-requests-per-minute:1000}") int batchReplayRequestsPerMinute) {
        this(Clock.systemUTC(), singleReplayPerMinute, batchReplayRequestsPerMinute, redis, keys);
    }

    public CanvasExecutionReplayRateLimiter(int singleReplayPerMinute,
                                            int batchReplayRequestsPerMinute) {
        this(Clock.systemUTC(), singleReplayPerMinute, batchReplayRequestsPerMinute, null, null);
    }

    public CanvasExecutionReplayRateLimiter(Clock clock,
                                            int singleReplayPerMinute,
                                            int batchReplayRequestsPerMinute) {
        this(clock, singleReplayPerMinute, batchReplayRequestsPerMinute, null, null);
    }

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

    public boolean tryAcquireSingleReplay() {
        return tryAcquireSingleReplay("system");
    }

    public boolean tryAcquireSingleReplay(String operator) {
        return tryAcquire("single", operator, singleReplayCount, singleReplayPerMinute, 1);
    }

    public boolean tryAcquireBatchReplay(int requestCount) {
        return tryAcquireBatchReplay("system", requestCount);
    }

    public boolean tryAcquireBatchReplay(String operator, int requestCount) {
        return tryAcquire("batch", operator, batchReplayRequestCount, batchReplayRequestsPerMinute, Math.max(1, requestCount));
    }

    private synchronized boolean tryAcquire(String scope, String operator, AtomicInteger counter, int limit, int cost) {
        if (limit <= 0) {
            return true;
        }
        long nowMinute = clock.millis() / 60_000L;
        if (redis != null && keys != null) {
            return tryAcquireRedis(scope, operator, nowMinute, limit, cost);
        }
        if (windowEpochMinute.get() != nowMinute) {
            windowEpochMinute.set(nowMinute);
            singleReplayCount.set(0);
            batchReplayRequestCount.set(0);
        }
        if (counter.get() + cost > limit) {
            return false;
        }
        counter.addAndGet(cost);
        return true;
    }

    private boolean tryAcquireRedis(String scope, String operator, long nowMinute, int limit, int cost) {
        String key = keys.executionRequestReplayRateLimit(scope, operator, nowMinute);
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
