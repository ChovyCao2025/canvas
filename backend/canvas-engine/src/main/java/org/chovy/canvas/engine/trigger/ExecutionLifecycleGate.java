package org.chovy.canvas.engine.trigger;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Process-local shutdown gate for canvas execution entrypoints.
 */
@Slf4j
@Component("triggerExecutionLifecycleGate")
public class ExecutionLifecycleGate {

    private final Duration drainTimeout;
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final AtomicInteger inFlight = new AtomicInteger();
    private final Object monitor = new Object();

    /**
     * 初始化 ExecutionLifecycleGate 实例。
     *
     * @param drainTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
    public ExecutionLifecycleGate(
            @Value("${canvas.execution.shutdown-drain-timeout-ms:10000}") long drainTimeoutMs) {
        this.drainTimeout = Duration.ofMillis(Math.max(0, drainTimeoutMs));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param mono 待调度任务或操作名称，用于封装阻塞工作。
     * @return 返回 guard 流程生成的业务结果。
     */
    public <T> Mono<T> guard(Mono<T> mono) {
        return Mono.defer(() -> {
            if (!tryEnter()) {
                return Mono.error(new RejectedExecutionException("canvas execution is shutting down"));
            }
            return mono.doFinally(signal -> exit());
        });
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 try enter 的布尔判断结果。
     */
    boolean tryEnter() {
        if (!accepting.get()) {
            return false;
        }
        inFlight.incrementAndGet();
        if (!accepting.get()) {
            exit();
            return false;
        }
        return true;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     */
    void exit() {
        int remaining = inFlight.updateAndGet(current -> Math.max(0, current - 1));
        if (remaining == 0) {
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @return 返回布尔判断结果。
     */
    public boolean isAccepting() {
        return accepting.get();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 in flight count 计算得到的数量、金额或指标值。
     */
    public int inFlightCount() {
        return inFlight.get();
    }

    @PreDestroy
    /**
     * 根据方法职责完成对应的业务处理流程。
     */
    public void shutdown() {
        if (!accepting.getAndSet(false)) {
            return;
        }
        awaitDrain();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     */
    private void awaitDrain() {
        long deadline = System.nanoTime() + drainTimeout.toNanos();
        synchronized (monitor) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            while (inFlight.get() > 0) {
                long remainingNanos = deadline - System.nanoTime();
                // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                if (remainingNanos <= 0) {
                    log.warn("[LIFECYCLE] shutdown drain timed out, inFlight={}", inFlight.get());
                    return;
                }
                try {
                    long waitMillis = Math.max(1, Math.min(100, remainingNanos / 1_000_000));
                    monitor.wait(waitMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // 汇总前面计算出的状态和明细，返回给调用方。
                    return;
                }
            }
        }
    }
}
