package org.chovy.canvas.engine.trigger;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class InFlightExecutionRegistryTest {

    @Test
    void tryAcquireRejectsWhenCanvasLimitReached() {
        InFlightExecutionRegistry registry = new InFlightExecutionRegistry();

        Optional<Disposable.Swap> first = registry.tryAcquire(10L, "exec-1", 1, 10);
        Optional<Disposable.Swap> second = registry.tryAcquire(10L, "exec-2", 1, 10);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(registry.activeCount(10L)).isEqualTo(1);
        assertThat(registry.totalActiveCount()).isEqualTo(1);
    }

    @Test
    void tryAcquireRejectsWhenGlobalLimitReached() {
        InFlightExecutionRegistry registry = new InFlightExecutionRegistry();

        Optional<Disposable.Swap> first = registry.tryAcquire(10L, "exec-1", 10, 1);
        Optional<Disposable.Swap> second = registry.tryAcquire(11L, "exec-2", 10, 1);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(registry.totalActiveCount()).isEqualTo(1);
    }

    @Test
    void cancelAllDisposesRegisteredSubscription() {
        InFlightExecutionRegistry registry = new InFlightExecutionRegistry();
        AtomicBoolean disposed = new AtomicBoolean(false);

        Disposable.Swap slot = registry.tryAcquire(10L, "exec-1", 10, 10).orElseThrow();
        slot.update(() -> disposed.set(true));

        int cancelled = registry.cancelAll(10L);

        assertThat(cancelled).isEqualTo(1);
        assertThat(disposed).isTrue();
        assertThat(registry.activeCount(10L)).isZero();
        assertThat(registry.totalActiveCount()).isZero();
    }
}
