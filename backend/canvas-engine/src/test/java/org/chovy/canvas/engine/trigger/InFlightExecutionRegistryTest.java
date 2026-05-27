package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.Disposable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InFlightExecutionRegistryTest {

    @Test
    void tryAcquireRejectsWhenCanvasLimitReached() {
        InFlightExecutionRegistry registry = registryWithAcquireResults(1L, -1L);

        Optional<Disposable.Swap> first = registry.tryAcquire(10L, "exec-1", 1, 10);
        Optional<Disposable.Swap> second = registry.tryAcquire(10L, "exec-2", 1, 10);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(registry.activeCount(10L)).isEqualTo(1);
        assertThat(registry.totalActiveCount()).isEqualTo(1);
    }

    @Test
    void tryAcquireRejectsWhenGlobalLimitReached() {
        InFlightExecutionRegistry registry = registryWithAcquireResults(1L, -2L);

        Optional<Disposable.Swap> first = registry.tryAcquire(10L, "exec-1", 10, 1);
        Optional<Disposable.Swap> second = registry.tryAcquire(11L, "exec-2", 10, 1);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(registry.totalActiveCount()).isEqualTo(1);
    }

    @Test
    void cancelAllDisposesRegisteredSubscription() {
        InFlightExecutionRegistry registry = registryWithAcquireResults(1L);
        AtomicBoolean disposed = new AtomicBoolean(false);

        Disposable.Swap slot = registry.tryAcquire(10L, "exec-1", 10, 10).orElseThrow();
        slot.update(() -> disposed.set(true));

        int cancelled = registry.cancelAll(10L);

        assertThat(cancelled).isEqualTo(1);
        assertThat(disposed).isTrue();
        assertThat(registry.activeCount(10L)).isZero();
        assertThat(registry.totalActiveCount()).isZero();
    }

    private InFlightExecutionRegistry registryWithAcquireResults(Long... acquireResults) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisKeyUtil keys = mock(RedisKeyUtil.class);
        when(keys.inflightCanvas(any())).thenAnswer(invocation -> "canvas:" + invocation.getArgument(0));
        when(keys.inflightGlobal()).thenReturn("global");
        when(redis.execute(any(), anyList(), any(String[].class))).thenReturn(1L);
        when(redis.execute(any(), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(acquireResults[0], java.util.Arrays.copyOfRange(acquireResults, 1, acquireResults.length));
        return new InFlightExecutionRegistry(redis, keys);
    }
}
