package org.chovy.canvas.execution.domain;

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BackgroundTaskExecutor {

    private final ExecutorService executor;
    private final Semaphore permits;
    private final Set<Future<?>> active = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Duration drainTimeout;

    public BackgroundTaskExecutor() {
        this(256, Executors.newVirtualThreadPerTaskExecutor(), Duration.ofSeconds(30));
    }

    public BackgroundTaskExecutor(
            @Value("${canvas.background.max-tasks:256}") int maxTasks,
            @Value("${canvas.shutdown.background-task-drain-timeout-ms:30000}") long drainTimeoutMs) {
        this(maxTasks, Executors.newVirtualThreadPerTaskExecutor(), Duration.ofMillis(Math.max(0, drainTimeoutMs)));
    }

    public BackgroundTaskExecutor(int maxTasks, ExecutorService executor, Duration drainTimeout) {
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

    public <T> Future<T> submit(String name, Callable<T> task) {
        if (closed.get()) {
            throw new RejectedExecutionException("BackgroundTaskExecutor is shut down");
        }
        if (!permits.tryAcquire()) {
            throw new RejectedExecutionException("Too many background tasks");
        }
        TrackedFutureTask<T> future = new TrackedFutureTask<>(task);
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

    public boolean submitBestEffort(String name, Runnable task) {
        try {
            submit(name, task);
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(drainTimeout.toNanos(), TimeUnit.NANOSECONDS)) {
                active.forEach(future -> future.cancel(true));
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            active.forEach(future -> future.cancel(true));
            executor.shutdownNow();
        }
    }

    private final class TrackedFutureTask<T> extends FutureTask<T> {
        private TrackedFutureTask(Callable<T> callable) {
            super(callable);
        }

        @Override
        protected void done() {
            active.remove(this);
            permits.release();
        }
    }
}
