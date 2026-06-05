package org.chovy.canvas.infrastructure.concurrent;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.infrastructure.observability.MdcTaskDecorator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks off-path virtual-thread work so shutdown can reject new tasks and drain in-flight work.
 */
@Slf4j
@Component
public class ManagedVirtualThreadExecutor {

    private static final ManagedVirtualThreadExecutor DIRECT = new ManagedVirtualThreadExecutor();

    private final ExecutorService executor;
    private final int maxInFlight;
    private final Duration shutdownTimeout;
    private final boolean direct;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger inFlight = new AtomicInteger();

    public ManagedVirtualThreadExecutor(
            @Value("${canvas.background-tasks.max-in-flight:1000}") int maxInFlight,
            @Value("${canvas.background-tasks.shutdown-timeout-ms:5000}") long shutdownTimeoutMs) {
        this(Executors.newVirtualThreadPerTaskExecutor(), maxInFlight,
                Duration.ofMillis(shutdownTimeoutMs), false);
    }

    ManagedVirtualThreadExecutor(ExecutorService executor, int maxInFlight, Duration shutdownTimeout) {
        this(executor, maxInFlight, shutdownTimeout, false);
    }

    private ManagedVirtualThreadExecutor() {
        this(null, Integer.MAX_VALUE, Duration.ZERO, true);
    }

    private ManagedVirtualThreadExecutor(ExecutorService executor,
                                         int maxInFlight,
                                         Duration shutdownTimeout,
                                         boolean direct) {
        if (maxInFlight <= 0) {
            throw new IllegalArgumentException("maxInFlight must be greater than 0");
        }
        this.executor = executor;
        this.maxInFlight = maxInFlight;
        this.shutdownTimeout = shutdownTimeout == null ? Duration.ZERO : shutdownTimeout;
        this.direct = direct;
    }

    public static ManagedVirtualThreadExecutor direct() {
        return DIRECT;
    }

    public Future<?> submit(String taskName, Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        String resolvedTaskName = taskName == null || taskName.isBlank() ? "background-task" : taskName;
        reserve(resolvedTaskName);
        Runnable decoratedTask = MdcTaskDecorator.decorate(task);
        if (direct) {
            return runDirect(resolvedTaskName, decoratedTask);
        }
        try {
            return executor.submit(() -> runTracked(resolvedTaskName, decoratedTask));
        } catch (RuntimeException e) {
            inFlight.decrementAndGet();
            throw e;
        }
    }

    private void reserve(String taskName) {
        if (closed.get()) {
            throw new RejectedExecutionException("background executor is closed: " + taskName);
        }
        int count = inFlight.incrementAndGet();
        if (count > maxInFlight) {
            inFlight.decrementAndGet();
            throw new RejectedExecutionException("background executor is full: " + taskName);
        }
    }

    private Future<?> runDirect(String taskName, Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            task.run();
            future.complete(null);
        } catch (Throwable e) {
            log.warn("[BACKGROUND] task failed taskName={}: {}", taskName, e.getMessage(), e);
            future.completeExceptionally(e);
        } finally {
            inFlight.decrementAndGet();
        }
        return future;
    }

    private void runTracked(String taskName, Runnable task) {
        try {
            task.run();
        } catch (Throwable e) {
            log.warn("[BACKGROUND] task failed taskName={}: {}", taskName, e.getMessage(), e);
            throw e;
        } finally {
            inFlight.decrementAndGet();
        }
    }

    public int inFlightTaskCount() {
        return inFlight.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    @PreDestroy
    public void shutdown() {
        if (!closed.compareAndSet(false, true) || direct || executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
