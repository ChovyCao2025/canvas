package org.chovy.canvas.engine.reactive;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class BackgroundSubscriptionRegistryTest {

    @Test
    void trackRemovesSubscriptionAfterCompletion() {
        BackgroundSubscriptionRegistry registry = new BackgroundSubscriptionRegistry();

        registry.track("complete", Mono.empty(), e -> {});

        assertThat(registry.activeCount()).isZero();
    }

    @Test
    void shutdownDisposesActiveSubscriptions() {
        BackgroundSubscriptionRegistry registry = new BackgroundSubscriptionRegistry(Duration.ZERO);
        Sinks.Empty<Void> pending = Sinks.empty();

        Disposable disposable = registry.track("pending", pending.asMono(), e -> {});

        assertThat(registry.activeCount()).isEqualTo(1);
        registry.shutdown();

        assertThat(disposable.isDisposed()).isTrue();
        assertThat(registry.activeCount()).isZero();
    }

    @Test
    void shutdownWaitsForActiveSubscriptionToCompleteBeforeDisposing() throws Exception {
        BackgroundSubscriptionRegistry registry = new BackgroundSubscriptionRegistry(Duration.ofMillis(500));
        Sinks.Empty<Void> pending = Sinks.empty();
        AtomicBoolean cancelled = new AtomicBoolean(false);
        var executor = Executors.newSingleThreadExecutor();

        try {
            registry.track("pending", pending.asMono().doOnCancel(() -> cancelled.set(true)), e -> {});

            Future<?> shutdown = executor.submit(registry::shutdown);
            Thread.sleep(50);
            assertThat(shutdown.isDone()).isFalse();

            pending.tryEmitEmpty();
            shutdown.get(1, TimeUnit.SECONDS);

            assertThat(cancelled).isFalse();
            assertThat(registry.activeCount()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shutdownDisposesActiveSubscriptionsAfterDrainTimeout() {
        BackgroundSubscriptionRegistry registry = new BackgroundSubscriptionRegistry(Duration.ofMillis(10));
        AtomicBoolean cancelled = new AtomicBoolean(false);

        registry.track("pending", Mono.never().doOnCancel(() -> cancelled.set(true)), e -> {});

        registry.shutdown();

        assertThat(cancelled).isTrue();
        assertThat(registry.activeCount()).isZero();
    }

    @Test
    void trackConsumesErrorsAfterCallingErrorHandler() {
        BackgroundSubscriptionRegistry registry = new BackgroundSubscriptionRegistry();
        AtomicReference<Throwable> handled = new AtomicReference<>();
        AtomicReference<Throwable> dropped = new AtomicReference<>();
        Hooks.onErrorDropped(dropped::set);

        try {
            registry.track("fail", Mono.error(new IllegalStateException("boom")), handled::set);

            assertThat(handled.get()).isInstanceOf(IllegalStateException.class);
            assertThat(dropped.get()).isNull();
            assertThat(registry.activeCount()).isZero();
        } finally {
            Hooks.resetOnErrorDropped();
        }
    }
}
