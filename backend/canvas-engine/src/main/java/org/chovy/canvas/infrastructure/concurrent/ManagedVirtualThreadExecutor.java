package org.chovy.canvas.infrastructure.concurrent;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.infrastructure.observability.MdcTaskDecorator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
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

    /**
     * 创建 ManagedVirtualThreadExecutor 实例并注入 infrastructure.concurrent 场景依赖。
     * @param maxInFlight max in flight 参数，用于 ManagedVirtualThreadExecutor 流程中的校验、计算或对象转换。
     * @param shutdownTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
    public ManagedVirtualThreadExecutor(
            @Value("${canvas.background-tasks.max-in-flight:1000}") int maxInFlight,
            @Value("${canvas.background-tasks.shutdown-timeout-ms:5000}") long shutdownTimeoutMs) {
        this(Executors.newVirtualThreadPerTaskExecutor(), maxInFlight,
                Duration.ofMillis(shutdownTimeoutMs), false);
    }

    /**
     * 执行 ManagedVirtualThreadExecutor 流程，围绕 managed virtual thread executor 完成校验、计算或结果组装。
     *
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param maxInFlight max in flight 参数，用于 ManagedVirtualThreadExecutor 流程中的校验、计算或对象转换。
     * @param shutdownTimeout 时间参数，用于计算窗口、过期或审计时间。
     */
    ManagedVirtualThreadExecutor(ExecutorService executor, int maxInFlight, Duration shutdownTimeout) {
        this(executor, maxInFlight, shutdownTimeout, false);
    }

    /**
     * 执行 ManagedVirtualThreadExecutor 流程，围绕 managed virtual thread executor 完成校验、计算或结果组装。
     */
    private ManagedVirtualThreadExecutor() {
        this(null, Integer.MAX_VALUE, Duration.ZERO, true);
    }

    /**
     * 执行 ManagedVirtualThreadExecutor 流程，围绕 managed virtual thread executor 完成校验、计算或结果组装。
     *
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param maxInFlight max in flight 参数，用于 ManagedVirtualThreadExecutor 流程中的校验、计算或对象转换。
     * @param shutdownTimeout 时间参数，用于计算窗口、过期或审计时间。
     * @param direct direct 参数，用于 ManagedVirtualThreadExecutor 流程中的校验、计算或对象转换。
     */
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

    /**
     * direct 处理 infrastructure.concurrent 场景的业务逻辑。
     * @return 返回 direct 流程生成的业务结果。
     */
    public static ManagedVirtualThreadExecutor direct() {
        return DIRECT;
    }

    /**
     * submit 创建或触发 infrastructure.concurrent 场景的业务处理。
     * @param taskName 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 submit 流程中的校验、计算或对象转换。
     * @return 返回 submit 流程生成的业务结果。
     */
    public Future<?> submit(String taskName, Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        return submit(taskName, Executors.callable(task, null));
    }

    /**
     * submit 创建或触发 infrastructure.concurrent 场景的业务处理。
     * @param taskName 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 submit 流程中的校验、计算或对象转换。
     * @return 返回 submit 流程生成的业务结果。
     */
    public <T> Future<T> submit(String taskName, Callable<T> task) {
        Objects.requireNonNull(task, "task must not be null");
        String resolvedTaskName = taskName == null || taskName.isBlank() ? "background-task" : taskName;
        // Reserve before submitting so callers see backpressure even when the executor queue accepts immediately.
        reserve(resolvedTaskName);
        Callable<T> decoratedTask = MdcTaskDecorator.decorate(task);
        if (direct) {
            return runDirect(resolvedTaskName, decoratedTask);
        }
        try {
            return executor.submit(() -> runTracked(resolvedTaskName, decoratedTask));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            inFlight.decrementAndGet();
            throw e;
        }
    }

    /**
     * 执行 reserve 流程，围绕 reserve 完成校验、计算或结果组装。
     *
     * @param taskName 名称文本，用于展示或唯一性校验。
     */
    private void reserve(String taskName) {
        if (closed.get()) {
            throw new RejectedExecutionException("background executor is closed: " + taskName);
        }
        int count = inFlight.incrementAndGet();
        if (count > maxInFlight) {
            // Roll back the reservation on rejection; otherwise the executor would permanently leak capacity.
            inFlight.decrementAndGet();
            throw new RejectedExecutionException("background executor is full: " + taskName);
        }
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param taskName 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 runDirect 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private <T> Future<T> runDirect(String taskName, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            future.complete(task.call());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Throwable e) {
            log.warn("[BACKGROUND] task failed taskName={}: {}", taskName, e.getMessage(), e);
            future.completeExceptionally(e);
        } finally {
            inFlight.decrementAndGet();
        }
        return future;
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param taskName 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 runTracked 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private <T> T runTracked(String taskName, Callable<T> task) throws Exception {
        try {
            return task.call();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Throwable e) {
            log.warn("[BACKGROUND] task failed taskName={}: {}", taskName, e.getMessage(), e);
            if (e instanceof Exception exception) {
                throw exception;
            }
            if (e instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(e);
        } finally {
            inFlight.decrementAndGet();
        }
    }

    /**
     * inFlightTaskCount 处理 infrastructure.concurrent 场景的业务逻辑。
     * @return 返回 in flight task count 计算得到的数量、金额或指标值。
     */
    public int inFlightTaskCount() {
        return inFlight.get();
    }

    /**
     * isClosed 校验或转换 infrastructure.concurrent 场景的数据。
     * @return 返回布尔判断结果。
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * shutdown 处理 infrastructure.concurrent 场景的业务逻辑。
     */
    @PreDestroy
    public void shutdown() {
        if (!closed.compareAndSet(false, true) || direct || executor == null) {
            return;
        }
        // Stop accepting work first, then give in-flight virtual-thread tasks a bounded drain window.
        executor.shutdown();
        try {
            if (!executor.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
