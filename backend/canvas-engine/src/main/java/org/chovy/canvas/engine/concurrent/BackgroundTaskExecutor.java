package org.chovy.canvas.engine.concurrent;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spring-owned virtual-thread executor for fire-and-forget and bounded background work.
 */
@Slf4j
@Component
public class BackgroundTaskExecutor {

    private static final Duration DEFAULT_DRAIN_TIMEOUT = Duration.ofSeconds(30);

    private final ExecutorService executor;
    private final Semaphore permits;
    private final Set<Future<?>> active = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Duration drainTimeout;

    public BackgroundTaskExecutor() {
        this(256, Executors.newVirtualThreadPerTaskExecutor(), DEFAULT_DRAIN_TIMEOUT);
    }

    @Autowired
    public BackgroundTaskExecutor(
            @Value("${canvas.background.max-tasks:256}") int maxTasks,
            @Value("${canvas.shutdown.background-task-drain-timeout-ms:30000}") long drainTimeoutMs) {
        this(maxTasks, Executors.newVirtualThreadPerTaskExecutor(),
                Duration.ofMillis(Math.max(0, drainTimeoutMs)));
    }

    BackgroundTaskExecutor(int maxTasks, ExecutorService executor) {
        this(maxTasks, executor, DEFAULT_DRAIN_TIMEOUT);
    }

    BackgroundTaskExecutor(int maxTasks, ExecutorService executor, Duration drainTimeout) {
        if (maxTasks < 1) {
            throw new IllegalArgumentException("maxTasks must be positive");
        }
        this.executor = executor;
        this.permits = new Semaphore(maxTasks);
        this.drainTimeout = drainTimeout == null ? Duration.ZERO : drainTimeout;
    }

    public Future<?> submit(String name, Runnable task) {
        return submit(name, Executors.callable(task, null));
    }

    public boolean submitBestEffort(String name, Runnable task) {
        try {
            submit(name, task);
            return true;
        } catch (RejectedExecutionException e) {
            log.warn("[BACKGROUND_TASK] rejected name={}: {}", name, e.getMessage());
            return false;
        }
    }

    public <T> Future<T> submit(String name, Callable<T> task) {
        if (closed.get()) {
            throw new RejectedExecutionException("BackgroundTaskExecutor is shut down");
        }
        if (!permits.tryAcquire()) {
            throw new RejectedExecutionException("Too many background tasks");
        }

        TrackedFutureTask<T> future = new TrackedFutureTask<>(name, () -> {
            try {
                return task.call();
            } catch (Throwable throwable) {
                log.warn("[BACKGROUND_TASK] failed name={}: {}", name, throwable.getMessage(), throwable);
                throw throwable;
            }
        });
        active.add(future);
        try {
            executor.execute(future);
        } catch (RuntimeException e) {
            active.remove(future);
            permits.release();
            throw e;
        }
        return future;
    }

    int activeCount() {
        return active.size();
    }

    @PreDestroy
    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        executor.shutdown();
        if (awaitTermination()) {
            return;
        }
        active.forEach(future -> future.cancel(true));
        executor.shutdownNow();
        active.removeIf(Future::isDone);
    }

    private boolean awaitTermination() {
        try {
            return executor.awaitTermination(Math.max(0, drainTimeout.toNanos()), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private class TrackedFutureTask<T> extends FutureTask<T> {

        private TrackedFutureTask(String name, Callable<T> callable) {
            super(callable);
        }

        @Override
        protected void done() {
            active.remove(this);
            permits.release();
        }
    }
}
