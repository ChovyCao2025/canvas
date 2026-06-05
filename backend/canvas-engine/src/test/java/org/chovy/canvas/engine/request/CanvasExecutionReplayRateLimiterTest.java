package org.chovy.canvas.engine.request;

import org.chovy.canvas.infrastructure.redis.DistributedRateLimiter;
import org.chovy.canvas.infrastructure.redis.RedisDistributedRateLimiter;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionReplayRateLimiterTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T00:01:00Z"), ZoneOffset.UTC);

    @Test
    void usesDistributedRateLimiterWhenConfigured() {
        DistributedRateLimiter distributedRateLimiter = mock(DistributedRateLimiter.class);
        when(distributedRateLimiter.tryAcquire("single", "ops", 1, 2, Duration.ofMinutes(1)))
                .thenReturn(true);
        CanvasExecutionReplayRateLimiter limiter =
                new CanvasExecutionReplayRateLimiter(FIXED_CLOCK, 2, 10, distributedRateLimiter);

        assertThat(limiter.tryAcquireSingleReplay("ops")).isTrue();

        verify(distributedRateLimiter).tryAcquire("single", "ops", 1, 2, Duration.ofMinutes(1));
    }

    @Test
    void fallsBackToLocalWindowWhenDistributedLimiterIsNotConfigured() {
        CanvasExecutionReplayRateLimiter limiter =
                new CanvasExecutionReplayRateLimiter(FIXED_CLOCK, 1, 10);

        assertThat(limiter.tryAcquireSingleReplay("ops")).isTrue();
        assertThat(limiter.tryAcquireSingleReplay("ops")).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisDistributedRateLimiterIncrementsNamespacedReplayKeyAndSetsTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        RedisKeyUtil keys = new RedisKeyUtil();
        RedisDistributedRateLimiter limiter = new RedisDistributedRateLimiter(redis, keys, FIXED_CLOCK);
        long epochMinute = FIXED_CLOCK.millis() / 60_000L;
        String expectedKey = keys.executionRequestReplayRateLimit("single", "ops", epochMinute);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(expectedKey, 1)).thenReturn(1L);

        assertThat(limiter.tryAcquire("single", "ops", 1, 2, Duration.ofMinutes(1))).isTrue();

        verify(ops).increment(expectedKey, 1);
        verify(redis).expire(expectedKey, Duration.ofSeconds(61));
    }
}
