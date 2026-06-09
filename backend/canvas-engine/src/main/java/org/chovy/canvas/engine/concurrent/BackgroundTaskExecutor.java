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
 * Spring 托管的虚拟线程后台任务执行器。
 *
 * <p>用于承接即发即忘和有并发上限的后台工作，并在应用关闭时等待任务排空。
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

    /**
     * 创建 BackgroundTaskExecutor 实例并注入 engine.concurrent 场景依赖。
     */
    public BackgroundTaskExecutor() {
        this(256, Executors.newVirtualThreadPerTaskExecutor(), DEFAULT_DRAIN_TIMEOUT);
    }

    /**
     * 创建 BackgroundTaskExecutor 实例并注入 engine.concurrent 场景依赖。
     * @param maxTasks max tasks 参数，用于 BackgroundTaskExecutor 流程中的校验、计算或对象转换。
     * @param drainTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
    @Autowired
    public BackgroundTaskExecutor(
            @Value("${canvas.background.max-tasks:256}") int maxTasks,
            @Value("${canvas.shutdown.background-task-drain-timeout-ms:30000}") long drainTimeoutMs) {
        this(maxTasks, Executors.newVirtualThreadPerTaskExecutor(),
                Duration.ofMillis(Math.max(0, drainTimeoutMs)));
    }

    /**
     * 使用自定义线程池创建后台任务执行器，使用默认排空超时时间。
     *
     * @param maxTasks 最大并发后台任务数
     * @param executor 实际执行任务的线程池
     */
    BackgroundTaskExecutor(int maxTasks, ExecutorService executor) {
        this(maxTasks, executor, DEFAULT_DRAIN_TIMEOUT);
    }

    /**
     * 使用自定义线程池和排空超时时间创建后台任务执行器。
     *
     * @param maxTasks 最大并发后台任务数
     * @param executor 实际执行任务的线程池
     * @param drainTimeout 关闭时等待任务排空的最长时间
     */
    BackgroundTaskExecutor(int maxTasks, ExecutorService executor, Duration drainTimeout) {
        if (maxTasks < 1) {
            throw new IllegalArgumentException("maxTasks must be positive");
        }
        this.executor = executor;
        this.permits = new Semaphore(maxTasks);
        this.drainTimeout = drainTimeout == null ? Duration.ZERO : drainTimeout;
    }

    /**
     * submit 创建或触发 engine.concurrent 场景的业务处理。
     * @param name 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 submit 流程中的校验、计算或对象转换。
     * @return 返回 submit 流程生成的业务结果。
     */
    public Future<?> submit(String name, Runnable task) {
        return submit(name, Executors.callable(task, null));
    }

    /**
     * submitBestEffort 创建或触发 engine.concurrent 场景的业务处理。
     * @param name 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 submitBestEffort 流程中的校验、计算或对象转换。
     * @return 返回 submit best effort 的布尔判断结果。
     */
    public boolean submitBestEffort(String name, Runnable task) {
        try {
            submit(name, task);
            return true;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RejectedExecutionException e) {
            log.warn("[BACKGROUND_TASK] rejected name={}: {}", name, e.getMessage());
            return false;
        }
    }

    /**
     * submit 创建或触发 engine.concurrent 场景的业务处理。
     * @param name 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 submit 流程中的校验、计算或对象转换。
     * @return 返回 submit 流程生成的业务结果。
     */
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
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (Throwable throwable) {
                log.warn("[BACKGROUND_TASK] failed name={}: {}", name, throwable.getMessage(), throwable);
                throw throwable;
            }
        });
        active.add(future);
        try {
            executor.execute(future);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            active.remove(future);
            permits.release();
            throw e;
        }
        return future;
    }

    /**
     * 查询当前仍在跟踪的后台任务数量。
     *
     * @return 活跃任务数
     */
    int activeCount() {
        return active.size();
    }

    /**
     * shutdown 处理 engine.concurrent 场景的业务逻辑。
     */
    @PreDestroy
    public void shutdown() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        executor.shutdown();
        if (awaitTermination()) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        active.forEach(future -> future.cancel(true));
        executor.shutdownNow();
        active.removeIf(Future::isDone);
    }

    /**
     * 等待线程池在配置时间内自然终止。
     *
     * @return true 表示线程池已终止，false 表示超时或当前线程被中断
     */
    private boolean awaitTermination() {
        try {
            return executor.awaitTermination(Math.max(0, drainTimeout.toNanos()), TimeUnit.NANOSECONDS);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * TrackedFutureTask 业务组件。
     */
    private class TrackedFutureTask<T> extends FutureTask<T> {

        /**
         * 创建会在结束时释放并发许可的 FutureTask。
         *
         * @param name 任务名称，用于日志定位
         * @param callable 实际任务逻辑
         */
        private TrackedFutureTask(String name, Callable<T> callable) {
            super(callable);
        }

        /**
         * done 处理 engine.concurrent 场景的业务逻辑。
         */
        @Override
        protected void done() {
            active.remove(this);
            permits.release();
        }
    }
}
