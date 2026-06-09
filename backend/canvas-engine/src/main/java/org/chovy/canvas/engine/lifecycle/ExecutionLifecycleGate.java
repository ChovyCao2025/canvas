package org.chovy.canvas.engine.lifecycle;

import jakarta.annotation.PreDestroy;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 画布执行工作的统一准入闸门。
 */
@Component
public class ExecutionLifecycleGate implements SmartLifecycle {

    private final AtomicBoolean acceptingNewWork = new AtomicBoolean(true);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger inFlight = new AtomicInteger();

    /**
     * acquire 更新 engine.lifecycle 场景的业务状态。
     * @param source source 参数，用于 acquire 流程中的校验、计算或对象转换。
     * @return 返回 acquire 流程生成的业务结果。
     */
    public WorkPermit acquire(String source) {
        ensureAccepting(source);
        inFlight.incrementAndGet();
        if (!acceptingNewWork.get()) {
            release();
            throw new ExecutionLifecycleException(source);
        }
        return new WorkPermit();
    }

    /**
     * ensureAccepting 处理 engine.lifecycle 场景的业务逻辑。
     * @param source source 参数，用于 ensureAccepting 流程中的校验、计算或对象转换。
     */
    public void ensureAccepting(String source) {
        if (!acceptingNewWork.get()) {
            throw new ExecutionLifecycleException(source);
        }
    }

    /**
     * guard 处理 engine.lifecycle 场景的业务逻辑。
     * @param source source 参数，用于 guard 流程中的校验、计算或对象转换。
     * @param work work 参数，用于 guard 流程中的校验、计算或对象转换。
     * @return 返回 guard 流程生成的业务结果。
     */
    public <T> Mono<T> guard(String source, Mono<T> work) {
        return Mono.defer(() -> {
            WorkPermit permit = acquire(source);
            return work.doFinally(signalType -> permit.close());
        });
    }

    /**
     * trackAccepted 处理 engine.lifecycle 场景的业务逻辑。
     * @param source source 参数，用于 trackAccepted 流程中的校验、计算或对象转换。
     * @param work work 参数，用于 trackAccepted 流程中的校验、计算或对象转换。
     * @return 返回 trackAccepted 流程生成的业务结果。
     */
    public <T> Mono<T> trackAccepted(String source, Mono<T> work) {
        return Mono.defer(() -> {
            WorkPermit permit = acquireAccepted();
            return work.doFinally(signalType -> permit.close());
        });
    }

    /**
     * isAcceptingNewWork 校验或转换 engine.lifecycle 场景的数据。
     * @return 返回布尔判断结果。
     */
    public boolean isAcceptingNewWork() {
        return acceptingNewWork.get();
    }

    /**
     * inFlightCount 处理 engine.lifecycle 场景的业务逻辑。
     * @return 返回 in flight count 计算得到的数量、金额或指标值。
     */
    public int inFlightCount() {
        return inFlight.get();
    }

    /**
     * beginShutdown 处理 engine.lifecycle 场景的业务逻辑。
     */
    @PreDestroy
    public void beginShutdown() {
        acceptingNewWork.set(false);
        running.set(false);
    }

    /**
     * start 创建或触发 engine.lifecycle 场景的业务处理。
     */
    @Override
    public void start() {
        acceptingNewWork.set(true);
        running.set(true);
    }

    /**
     * stop 处理 engine.lifecycle 场景的业务逻辑。
     */
    @Override
    public void stop() {
        beginShutdown();
    }

    /**
     * stop 处理 engine.lifecycle 场景的业务逻辑。
     * @param callback callback 参数，用于 stop 流程中的校验、计算或对象转换。
     */
    @Override
    public void stop(Runnable callback) {
        beginShutdown();
        callback.run();
    }

    /**
     * isRunning 校验或转换 engine.lifecycle 场景的数据。
     * @return 返回布尔判断结果。
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * isAutoStartup 校验或转换 engine.lifecycle 场景的数据。
     * @return 返回布尔判断结果。
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * getPhase 查询 engine.lifecycle 场景的业务数据。
     * @return 返回 get phase 计算得到的数量、金额或指标值。
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    /**
     * 释放一个已登记的执行许可。
     */
    private void release() {
        inFlight.updateAndGet(current -> current > 0 ? current - 1 : 0);
    }

    /**
     * 在已确认停止准入后登记正在执行的工作。
     *
     * @return 新的执行许可
     */
    private WorkPermit acquireAccepted() {
        inFlight.incrementAndGet();
        return new WorkPermit();
    }

    /**
     * WorkPermit 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public final class WorkPermit implements AutoCloseable {
        private final AtomicBoolean closed = new AtomicBoolean(false);

        /**
         * close 删除或清理 engine.lifecycle 场景的业务数据。
         */
        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                release();
            }
        }
    }
}
