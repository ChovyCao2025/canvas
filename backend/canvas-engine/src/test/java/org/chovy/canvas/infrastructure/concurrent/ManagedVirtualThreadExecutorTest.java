package org.chovy.canvas.infrastructure.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagedVirtualThreadExecutorTest {

    @Test
    void tracksInFlightTasksAndDrainsOnCompletion() throws Exception {
        ManagedVirtualThreadExecutor executor = new ManagedVirtualThreadExecutor(
                Executors.newVirtualThreadPerTaskExecutor(), 2, Duration.ofSeconds(1));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        executor.submit("slow", () -> {
            started.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(executor.inFlightTaskCount()).isEqualTo(1);
        release.countDown();
        Thread.sleep(100);
        assertThat(executor.inFlightTaskCount()).isZero();
        executor.shutdown();
    }

    @Test
    void rejectsWhenCapacityIsFullOrClosed() throws Exception {
        ManagedVirtualThreadExecutor executor = new ManagedVirtualThreadExecutor(
                Executors.newVirtualThreadPerTaskExecutor(), 1, Duration.ofMillis(100));
        CountDownLatch release = new CountDownLatch(1);
        executor.submit("held", () -> {
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertThatThrownBy(() -> executor.submit("overflow", () -> {}))
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessageContaining("full");

        release.countDown();
        Thread.sleep(100);
        executor.shutdown();

        assertThat(executor.isClosed()).isTrue();
        assertThatThrownBy(() -> executor.submit("after-close", () -> {}))
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessageContaining("closed");
    }
}
