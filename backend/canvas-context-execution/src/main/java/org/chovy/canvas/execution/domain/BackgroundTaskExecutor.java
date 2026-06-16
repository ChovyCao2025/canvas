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

/**
 * 定义 BackgroundTaskExecutor 的执行上下文数据结构或业务契约。
 */
@Component
public class BackgroundTaskExecutor {

    /**
     * 保存 executor 对应的状态或配置。
     */
    private final ExecutorService executor;

    /**
     * 保存 permits 对应的状态或配置。
     */
    private final Semaphore permits;
    private final Set<Future<?>> active = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 保存 drainTimeout 对应的状态或配置。
     */
    private final Duration drainTimeout;

    /**
     * 执行 BackgroundTaskExecutor 对应的业务处理。
     */
    public BackgroundTaskExecutor() {
        this(256, Executors.newVirtualThreadPerTaskExecutor(), Duration.ofSeconds(30));
    }

    public BackgroundTaskExecutor(
            @Value("${canvas.background.max-tasks:256}") int maxTasks,
            @Value("${canvas.shutdown.background-task-drain-timeout-ms:30000}") long drainTimeoutMs) {
        this(maxTasks, Executors.newVirtualThreadPerTaskExecutor(), Duration.ofMillis(Math.max(0, drainTimeoutMs)));
    }

    /**
     * 执行 BackgroundTaskExecutor 对应的业务处理。
     * @param maxTasks maxTasks 参数
     * @param executor executor 参数
     * @param drainTimeout drainTimeout 参数
     */
    public BackgroundTaskExecutor(int maxTasks, ExecutorService executor, Duration drainTimeout) {
        if (maxTasks < 1) {
            throw new IllegalArgumentException("maxTasks must be positive");
        }
        this.executor = executor;
        this.permits = new Semaphore(maxTasks);
        this.drainTimeout = drainTimeout == null ? Duration.ZERO : drainTimeout;
    }

    /**
     * 执行 submit 对应的业务处理。
     * @param name name 参数
     * @param task task 参数
     * @return 处理后的结果
     */
    public Future<?> submit(String name, Runnable task) {
        return submit(name, Executors.callable(task, null));
    }

    /**
     * 执行 submit 对应的业务处理。
     * @param name name 参数
     * @param task task 参数
     * @return 处理后的结果
     */
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
            // 提交失败必须归还许可，否则后续任务会被错误地限流。
            active.remove(future);
            permits.release();
            throw e;
        }
        return future;
    }

    /**
     * 执行 submitBestEffort 对应的业务处理。
     * @param name name 参数
     * @param task task 参数
     */
    public boolean submitBestEffort(String name, Runnable task) {
        try {
            submit(name, task);
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    /**
     * 执行 shutdown 对应的业务处理。
     */
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

    /**
     * 定义 TrackedFutureTask 的执行上下文数据结构或业务契约。
     */
    private final class TrackedFutureTask<T> extends FutureTask<T> {
        /**
         * 执行 TrackedFutureTask 对应的业务处理。
         * @param callable callable 参数
         */
        private TrackedFutureTask(Callable<T> callable) {
            super(callable);
        }

        /**
         * 执行 done 对应的业务处理。
         */
        @Override
        protected void done() {
            active.remove(this);
            permits.release();
        }
    }
}
