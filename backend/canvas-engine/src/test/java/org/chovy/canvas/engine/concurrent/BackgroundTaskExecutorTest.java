package org.chovy.canvas.engine.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackgroundTaskExecutorTest {

    @Test
    void submitTracksActiveTaskUntilCompletion() throws Exception {
        BackgroundTaskExecutor executor = new BackgroundTaskExecutor(1, Executors.newSingleThreadExecutor());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try {
            Future<?> future = executor.submit("blocked", () -> {
                entered.countDown();
                await(release);
            });

            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(executor.activeCount()).isEqualTo(1);

            release.countDown();
            future.get(1, TimeUnit.SECONDS);

            assertThat(executor.activeCount()).isZero();
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void submitRejectsWhenActiveTaskLimitIsReached() throws Exception {
        BackgroundTaskExecutor executor = new BackgroundTaskExecutor(1, Executors.newSingleThreadExecutor());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try {
            Future<?> future = executor.submit("blocked", () -> {
                entered.countDown();
                await(release);
            });
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> executor.submit("second", () -> {
            })).isInstanceOf(RejectedExecutionException.class);

            release.countDown();
            future.get(1, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void submitBestEffortReturnsFalseWhenActiveTaskLimitIsReached() throws Exception {
        BackgroundTaskExecutor executor = new BackgroundTaskExecutor(1, Executors.newSingleThreadExecutor());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try {
            Future<?> future = executor.submit("blocked", () -> {
                entered.countDown();
                await(release);
            });
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();

            assertThat(executor.submitBestEffort("second", () -> {
            })).isFalse();

            release.countDown();
            future.get(1, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void shutdownCancelsActiveTasksAndRejectsNewSubmissions() throws Exception {
        BackgroundTaskExecutor executor = new BackgroundTaskExecutor(1, Executors.newSingleThreadExecutor());
        CountDownLatch entered = new CountDownLatch(1);

        Future<?> future = executor.submit("blocked", () -> {
            entered.countDown();
            sleep();
        });
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();

        executor.shutdown();

        assertThat(future.isCancelled()).isTrue();
        assertThat(executor.activeCount()).isZero();
        assertThatThrownBy(() -> executor.submit("after-shutdown", () -> {
        })).isInstanceOf(RejectedExecutionException.class);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
