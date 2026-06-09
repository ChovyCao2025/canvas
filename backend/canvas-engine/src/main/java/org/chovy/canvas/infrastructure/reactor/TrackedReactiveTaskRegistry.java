package org.chovy.canvas.infrastructure.reactor;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * Tracks fire-and-forget reactive tasks so lifecycle shutdown can dispose them.
 */
@Slf4j
@Component
public class TrackedReactiveTaskRegistry {

    private static final TrackedReactiveTaskRegistry DIRECT =
            new TrackedReactiveTaskRegistry(Duration.ZERO, false);

    private final Set<TrackedTask> tasks = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Duration shutdownTimeout;
    private final boolean closeOnShutdown;

    /**
     * 创建 TrackedReactiveTaskRegistry 实例并注入 infrastructure.reactor 场景依赖。
     * @param shutdownTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
    @Autowired
    public TrackedReactiveTaskRegistry(
            @Value("${canvas.reactive-tasks.shutdown-timeout-ms:5000}") long shutdownTimeoutMs) {
        this(Duration.ofMillis(Math.max(0L, shutdownTimeoutMs)), true);
    }

    /**
     * 执行 TrackedReactiveTaskRegistry 流程，围绕 tracked reactive task registry 完成校验、计算或结果组装。
     *
     * @param shutdownTimeout 时间参数，用于计算窗口、过期或审计时间。
     * @param closeOnShutdown close on shutdown 参数，用于 TrackedReactiveTaskRegistry 流程中的校验、计算或对象转换。
     */
    TrackedReactiveTaskRegistry(Duration shutdownTimeout, boolean closeOnShutdown) {
        this.shutdownTimeout = shutdownTimeout == null ? Duration.ZERO : shutdownTimeout;
        this.closeOnShutdown = closeOnShutdown;
    }

    /**
     * direct 处理 infrastructure.reactor 场景的业务逻辑。
     * @return 返回 direct 流程生成的业务结果。
     */
    public static TrackedReactiveTaskRegistry direct() {
        return DIRECT;
    }

    /**
     * submit 创建或触发 infrastructure.reactor 场景的业务处理。
     * @param taskName 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 submit 流程中的校验、计算或对象转换。
     * @return 返回 submit 流程生成的业务结果。
     */
    public Disposable submit(String taskName, Mono<?> task) {
        return submit(taskName, task, null);
    }

    /**
     * submit 创建或触发 infrastructure.reactor 场景的业务处理。
     * @param taskName 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 submit 流程中的校验、计算或对象转换。
     * @param onError on error 参数，用于 submit 流程中的校验、计算或对象转换。
     * @return 返回 submit 流程生成的业务结果。
     */
    public Disposable submit(String taskName, Mono<?> task, Consumer<Throwable> onError) {
        return submitPublisher(taskName, task, onError);
    }

    /**
     * submit 创建或触发 infrastructure.reactor 场景的业务处理。
     * @param taskName 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 submit 流程中的校验、计算或对象转换。
     * @param onError on error 参数，用于 submit 流程中的校验、计算或对象转换。
     * @return 返回 submit 流程生成的业务结果。
     */
    public Disposable submit(String taskName, Publisher<?> task, Consumer<Throwable> onError) {
        return submitPublisher(taskName, task, onError);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param taskName 名称文本，用于展示或唯一性校验。
     * @param task task 参数，用于 submitPublisher 流程中的校验、计算或对象转换。
     * @param onError on error 参数，用于 submitPublisher 流程中的校验、计算或对象转换。
     * @return 返回 submitPublisher 流程生成的业务结果。
     */
    private Disposable submitPublisher(String taskName, Publisher<?> task, Consumer<Throwable> onError) {
        Objects.requireNonNull(task, "task must not be null");
        String resolvedTaskName = taskName == null || taskName.isBlank() ? "reactive-task" : taskName;
        if (closed.get()) {
            throw new RejectedExecutionException("reactive task registry is closed: " + resolvedTaskName);
        }

        Disposable.Swap delegate = Disposables.swap();
        TrackedTask tracked = new TrackedTask(resolvedTaskName, delegate);
        tasks.add(tracked);
        try {
            Disposable subscribed = Flux.from(task)
                    .doFinally(signalType -> tasks.remove(tracked))
                    .subscribe(
                            ignored -> {
                            },
                            e -> handleError(resolvedTaskName, e, onError));
            delegate.update(subscribed);
            return tracked;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            tasks.remove(tracked);
            delegate.dispose();
            throw e;
        }
    }

    /**
     * inFlightTaskCount 处理 infrastructure.reactor 场景的业务逻辑。
     * @return 返回 in flight task count 计算得到的数量、金额或指标值。
     */
    public int inFlightTaskCount() {
        return tasks.size();
    }

    /**
     * isClosed 校验或转换 infrastructure.reactor 场景的数据。
     * @return 返回布尔判断结果。
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * drain 处理 infrastructure.reactor 场景的业务逻辑。
     * @param timeout 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 drain 的布尔判断结果。
     */
    public boolean drain(Duration timeout) {
        long waitNanos = timeout == null ? 0L : Math.max(0L, timeout.toNanos());
        long deadline = System.nanoTime() + waitNanos;
        while (!tasks.isEmpty() && System.nanoTime() < deadline) {
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return tasks.isEmpty();
    }

    /**
     * shutdown 处理 infrastructure.reactor 场景的业务逻辑。
     */
    @PreDestroy
    public void shutdown() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (closeOnShutdown && !closed.compareAndSet(false, true)) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (!drain(shutdownTimeout)) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            tasks.forEach(TrackedTask::dispose);
            tasks.clear();
        }
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param taskName 名称文本，用于展示或唯一性校验。
     * @param error error 参数，用于 handleError 流程中的校验、计算或对象转换。
     * @param onError on error 参数，用于 handleError 流程中的校验、计算或对象转换。
     */
    private void handleError(String taskName, Throwable error, Consumer<Throwable> onError) {
        if (onError != null) {
            onError.accept(error);
            return;
        }
        log.warn("[REACTIVE_TASK] task failed taskName={}: {}", taskName, error.getMessage(), error);
    }

/**
 * dispose 处理 infrastructure.reactor 场景的业务逻辑。
 */
    private record TrackedTask(String name, Disposable.Swap delegate) implements Disposable {
        /**
         * 执行 dispose 流程，围绕 dispose 完成校验、计算或结果组装。
         */
        @Override
        public void dispose() {
            delegate.dispose();
        }

        /**
         * isDisposed 校验或转换 infrastructure.reactor 场景的数据。
         * @return 返回布尔判断结果。
         */
        @Override
        public boolean isDisposed() {
            return delegate.isDisposed();
        }
    }
}
