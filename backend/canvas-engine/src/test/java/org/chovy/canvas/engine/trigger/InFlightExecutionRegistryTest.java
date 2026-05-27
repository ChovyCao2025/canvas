package org.chovy.canvas.engine.trigger;

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
import reactor.core.Disposable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * In Flight Execution Registry 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class InFlightExecutionRegistryTest {

    @Mock StringRedisTemplate redis;
    @Mock RedisKeyUtil keys;
    @Mock ZSetOperations<String, String> zSetOps;

    @BeforeEach
    void setUp() {
        when(keys.inflightCanvas(anyLong())).thenAnswer(invocation -> "canvas:" + invocation.getArgument(0));
        when(keys.inflightGlobal()).thenReturn("global");
    }

    @Test
    void tryAcquireRejectsWhenCanvasLimitReached() {
        InFlightExecutionRegistry registry = registry();
        when(redis.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1L, -1L);
        when(redis.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(anyString())).thenReturn(1L);

        Optional<Disposable.Swap> first = registry.tryAcquire(10L, "exec-1", 1, 10);
        Optional<Disposable.Swap> second = registry.tryAcquire(10L, "exec-2", 1, 10);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(registry.activeCount(10L)).isEqualTo(1);
        assertThat(registry.totalActiveCount()).isEqualTo(1);
    }

    @Test
    void tryAcquireRejectsWhenGlobalLimitReached() {
        InFlightExecutionRegistry registry = registry();
        when(redis.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1L, -2L);
        when(redis.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(anyString())).thenReturn(1L);

        Optional<Disposable.Swap> first = registry.tryAcquire(10L, "exec-1", 10, 1);
        Optional<Disposable.Swap> second = registry.tryAcquire(11L, "exec-2", 10, 1);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(registry.totalActiveCount()).isEqualTo(1);
    }

    @Test
    void cancelAllDisposesRegisteredSubscription() {
        InFlightExecutionRegistry registry = registry();
        when(redis.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1L);
        when(redis.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(anyString())).thenReturn(0L);
        AtomicBoolean disposed = new AtomicBoolean(false);

        Disposable.Swap slot = registry.tryAcquire(10L, "exec-1", 10, 10).orElseThrow();
        slot.update(() -> disposed.set(true));

        int cancelled = registry.cancelAll(10L);

        assertThat(cancelled).isEqualTo(1);
        assertThat(disposed).isTrue();
        assertThat(registry.activeCount(10L)).isZero();
        assertThat(registry.totalActiveCount()).isZero();
    }

    private InFlightExecutionRegistry registry() {
        InFlightExecutionRegistry registry = new InFlightExecutionRegistry(redis, keys);
        ReflectionTestUtils.setField(registry, "globalTimeoutSec", 600L);
        return registry;
    }
}
