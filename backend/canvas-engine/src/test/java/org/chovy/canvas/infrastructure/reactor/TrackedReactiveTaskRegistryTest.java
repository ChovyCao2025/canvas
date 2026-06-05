package org.chovy.canvas.infrastructure.reactor;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackedReactiveTaskRegistryTest {

    @Test
    void submitTracksAndReleasesSuccessfulTask() {
        TrackedReactiveTaskRegistry registry =
                new TrackedReactiveTaskRegistry(Duration.ofMillis(100), true);
        AtomicBoolean ran = new AtomicBoolean(false);

        registry.submit("success", Mono.fromRunnable(() -> ran.set(true)));

        assertThat(registry.drain(Duration.ofSeconds(1))).isTrue();
        assertThat(ran).isTrue();
        assertThat(registry.inFlightTaskCount()).isZero();
    }

    @Test
    void submitTracksFailureAndInvokesErrorHandler() {
        TrackedReactiveTaskRegistry registry =
                new TrackedReactiveTaskRegistry(Duration.ofMillis(100), true);
        AtomicReference<Throwable> error = new AtomicReference<>();

        registry.submit("failure", Mono.error(new IllegalStateException("boom")), error::set);

        assertThat(registry.drain(Duration.ofSeconds(1))).isTrue();
        assertThat(error.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        assertThat(registry.inFlightTaskCount()).isZero();
    }

    @Test
    void disposableCancelsAndReleasesNeverTask() {
        TrackedReactiveTaskRegistry registry =
                new TrackedReactiveTaskRegistry(Duration.ofMillis(100), true);

        Disposable disposable = registry.submit("never", Mono.never());
        assertThat(registry.inFlightTaskCount()).isEqualTo(1);

        disposable.dispose();

        assertThat(registry.drain(Duration.ofSeconds(1))).isTrue();
        assertThat(disposable.isDisposed()).isTrue();
        assertThat(registry.inFlightTaskCount()).isZero();
    }

    @Test
    void shutdownDisposesInFlightTasksAndRejectsNewTasks() {
        TrackedReactiveTaskRegistry registry =
                new TrackedReactiveTaskRegistry(Duration.ZERO, true);
        registry.submit("never", Mono.never());

        registry.shutdown();

        assertThat(registry.isClosed()).isTrue();
        assertThat(registry.inFlightTaskCount()).isZero();
        assertThatThrownBy(() -> registry.submit("late", Mono.empty()))
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessageContaining("late");
    }
}
