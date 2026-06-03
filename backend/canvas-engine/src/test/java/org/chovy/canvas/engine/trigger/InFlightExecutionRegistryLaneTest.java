package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneAdmissionResult;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InFlightExecutionRegistryLaneTest {

    @Mock StringRedisTemplate redis;
    @Mock RedisKeyUtil keys;
    @Mock ZSetOperations<String, String> zSetOps;

    @BeforeEach
    void setUp() {
        lenient().when(keys.inflightCanvas(anyLong())).thenAnswer(invocation -> "canvas:" + invocation.getArgument(0));
        when(keys.inflightLane(any())).thenAnswer(invocation -> {
            ExecutionLane lane = invocation.getArgument(0);
            return "lane:" + lane.key();
        });
        lenient().when(keys.inflightGlobal()).thenReturn("global");
    }

    @Test
    void tryAcquireReturnsTypedRejectionWhenLaneLimitReached() {
        InFlightExecutionRegistry registry = registry();
        when(redis.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(-2L);
        when(redis.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard("canvas:10")).thenReturn(3L);
        when(zSetOps.zCard("lane:standard")).thenReturn(1800L);
        when(zSetOps.zCard("global")).thenReturn(1800L);

        ExecutionLaneAdmissionResult result = registry.tryAcquire(
                10L, "exec-1", ExecutionLane.STANDARD, 3000, 1800, 3000);

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo(ExecutionLaneAdmissionResult.Reason.LANE_LIMIT);
        assertThat(result.canvasActive()).isEqualTo(3);
        assertThat(result.laneActive()).isEqualTo(1800);
        assertThat(result.globalActive()).isEqualTo(1800);
    }

    @Test
    void laneActiveCountReadsLaneZset() {
        InFlightExecutionRegistry registry = registry();
        when(redis.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard("lane:heavy")).thenReturn(300L);

        assertThat(registry.laneActiveCount(ExecutionLane.HEAVY)).isEqualTo(300);
    }

    @Test
    void tryAcquireRejectsWhenLocalRegistryLimitIsReached() {
        InFlightExecutionRegistry registry = registry();
        ReflectionTestUtils.setField(registry, "localRegistryMaxEntries", 1);
        when(redis.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        ExecutionLaneAdmissionResult first = registry.tryAcquire(
                10L, "exec-1", ExecutionLane.STANDARD, 3000, 1800, 3000);
        ExecutionLaneAdmissionResult second = registry.tryAcquire(
                10L, "exec-2", ExecutionLane.STANDARD, 3000, 1800, 3000);

        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isFalse();
        assertThat(second.reason()).isEqualTo(ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE);
    }

    @Test
    void fallbackCountsPruneExpiredLocalEntries() {
        InFlightExecutionRegistry registry = registry();
        ReflectionTestUtils.setField(registry, "globalTimeoutSec", 0L);
        when(redis.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1L);
        registry.tryAcquire(10L, "exec-expired", ExecutionLane.STANDARD, 3000, 1800, 3000);
        when(redis.opsForZSet()).thenThrow(new IllegalStateException("redis down"));

        assertThat(registry.activeCount(10L)).isZero();
        assertThat(registry.totalActiveCount()).isZero();
        assertThat(registry.laneActiveCount(ExecutionLane.STANDARD)).isZero();
    }

    private InFlightExecutionRegistry registry() {
        InFlightExecutionRegistry registry = new InFlightExecutionRegistry(redis, keys);
        ReflectionTestUtils.setField(registry, "globalTimeoutSec", 600L);
        ReflectionTestUtils.setField(registry, "localRegistryMaxEntries", 10000);
        return registry;
    }
}
