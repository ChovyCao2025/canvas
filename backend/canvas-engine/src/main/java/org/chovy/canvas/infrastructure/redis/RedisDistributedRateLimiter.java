package org.chovy.canvas.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
public class RedisDistributedRateLimiter implements DistributedRateLimiter {

    private final StringRedisTemplate redis;
    private final RedisKeyUtil keys;
    private final Clock clock;

    public RedisDistributedRateLimiter(StringRedisTemplate redis, RedisKeyUtil keys) {
        this(redis, keys, Clock.systemUTC());
    }

    public RedisDistributedRateLimiter(StringRedisTemplate redis, RedisKeyUtil keys, Clock clock) {
        this.redis = redis;
        this.keys = keys;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public boolean tryAcquire(String scope, String operator, int cost, int limit, Duration window) {
        if (limit <= 0) {
            return true;
        }
        int normalizedCost = Math.max(1, cost);
        Duration normalizedWindow = window == null || window.isNegative() || window.isZero()
                ? Duration.ofMinutes(1)
                : window;
        long windowIndex = clock.millis() / Math.max(1L, normalizedWindow.toMillis());
        String key = keys.executionRequestReplayRateLimit(scope, operator, windowIndex);
        Long count = redis.opsForValue().increment(key, normalizedCost);
        if (count == null) {
            return false;
        }
        if (count <= normalizedCost) {
            redis.expire(key, normalizedWindow.plusSeconds(1));
        }
        return count <= limit;
    }
}
