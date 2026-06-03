package org.chovy.canvas.engine.concurrent;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spring-owned virtual-thread executor for fire-and-forget and bounded background work.
 */
@Slf4j
@Component
public class BackgroundTaskExecutor {

    private final ExecutorService executor;
    private final Semaphore permits;
    private final Set<Future<?>> active = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public BackgroundTaskExecutor() {
        this(256, Executors.newVirtualThreadPerTaskExecutor());
    }

    @Autowired
    public BackgroundTaskExecutor(@Value("${canvas.background.max-tasks:256}") int maxTasks) {
        this(maxTasks, Executors.newVirtualThreadPerTaskExecutor());
    }

    BackgroundTaskExecutor(int maxTasks, ExecutorService executor) {
        if (maxTasks < 1) {
            throw new IllegalArgumentException("maxTasks must be positive");
        }
        this.executor = executor;
        this.permits = new Semaphore(maxTasks);
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
        active.forEach(future -> future.cancel(true));
        active.clear();
        executor.shutdownNow();
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
