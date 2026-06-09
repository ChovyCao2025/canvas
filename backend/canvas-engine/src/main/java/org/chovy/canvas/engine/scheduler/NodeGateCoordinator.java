package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.context.NodeGate;
import org.springframework.stereotype.Component;

/**
 * Coordinates per-node execution gates and repeat signals.
 */
@Component
public class NodeGateCoordinator {

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param nodeGate node gate 参数，用于 tryAcquireOrSignalRepeat 流程中的校验、计算或对象转换。
     * @return 返回 try acquire or signal repeat 的布尔判断结果。
     */
    boolean tryAcquireOrSignalRepeat(NodeGate nodeGate) {
        if (nodeGate.executing.compareAndSet(false, true)) {
            return true;
        }
        nodeGate.repeatPending.set(true);
        return false;
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param nodeGate node gate 参数，用于 releaseAndConsumeRepeat 流程中的校验、计算或对象转换。
     * @return 返回 release and consume repeat 的布尔判断结果。
     */
    boolean releaseAndConsumeRepeat(NodeGate nodeGate) {
        boolean needsRepeat = nodeGate.repeatPending.getAndSet(false);
        nodeGate.executing.set(false);
        needsRepeat |= nodeGate.repeatPending.getAndSet(false);
        return needsRepeat;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param nodeGate node gate 参数，用于 tryAcquireForRepeat 流程中的校验、计算或对象转换。
     * @return 返回 try acquire for repeat 的布尔判断结果。
     */
    boolean tryAcquireForRepeat(NodeGate nodeGate) {
        return nodeGate.executing.compareAndSet(false, true);
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param nodeGate node gate 参数，用于 release 流程中的校验、计算或对象转换。
     */
    void release(NodeGate nodeGate) {
        nodeGate.executing.set(false);
    }
}
