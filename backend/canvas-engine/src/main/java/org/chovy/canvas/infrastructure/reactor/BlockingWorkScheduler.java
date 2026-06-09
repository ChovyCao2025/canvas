package org.chovy.canvas.infrastructure.reactor;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Central adapter for blocking work used from the reactive application.
 */
@Component
public class BlockingWorkScheduler {

    /**
     * 执行 call 流程，围绕 call 完成校验、计算或结果组装。
     *
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param callable 待调度任务或操作名称，用于封装阻塞工作。
     * @return 返回 call 流程生成的业务结果。
     */
    public <T> Mono<T> call(String operation, Callable<T> callable) {
        Objects.requireNonNull(callable, "callable must not be null");
        return Mono.fromCallable(() -> {
                    assertNotEventLoop(operation);
                    return callable.call();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * run 创建或触发 infrastructure.reactor 场景的业务处理。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param runnable 待调度任务或操作名称，用于封装阻塞工作。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<Void> run(String operation, ThrowingRunnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        return call(operation, () -> {
            runnable.run();
            return null;
        }).then();
    }

    /**
     * await 处理 infrastructure.reactor 场景的业务逻辑。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param mono 待调度任务或操作名称，用于封装阻塞工作。
     * @return 返回 await 流程生成的业务结果。
     */
    public <T> T await(String operation, Mono<T> mono) {
        Objects.requireNonNull(mono, "mono must not be null");
        assertNotEventLoop(operation);
        return mono.block();
    }

    /**
     * get 查询 infrastructure.reactor 场景的业务数据。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param callable 待调度任务或操作名称，用于封装阻塞工作。
     * @return 返回 get 流程生成的业务结果。
     */
    public <T> T get(String operation, Callable<T> callable) {
        assertNotEventLoop(operation);
        return call(operation, callable).block();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     */
    private void assertNotEventLoop(String operation) {
        String threadName = Thread.currentThread().getName();
        String lower = threadName.toLowerCase(Locale.ROOT);
        if (lower.contains("reactor-http")
                || lower.contains("reactor-tcp")
                || lower.contains("nioeventloop")
                || lower.contains("eventloop")) {
            String name = operation == null || operation.isBlank() ? "blocking work" : operation;
            throw new IllegalStateException(name + " cannot run on event-loop thread: " + threadName);
        }
    }

    /**
     * ThrowingRunnable 接口契约。
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        /**
         * 执行核心业务处理流程。
         */
        void run() throws Exception;
    }
}
