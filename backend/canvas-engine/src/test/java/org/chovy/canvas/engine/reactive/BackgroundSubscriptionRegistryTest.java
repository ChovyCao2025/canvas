package org.chovy.canvas.engine.reactive;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicReference;

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
        BackgroundSubscriptionRegistry registry = new BackgroundSubscriptionRegistry();
        Sinks.Empty<Void> pending = Sinks.empty();

        Disposable disposable = registry.track("pending", pending.asMono(), e -> {});

        assertThat(registry.activeCount()).isEqualTo(1);
        registry.shutdown();

        assertThat(disposable.isDisposed()).isTrue();
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
