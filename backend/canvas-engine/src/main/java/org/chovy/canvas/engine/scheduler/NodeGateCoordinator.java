package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.context.NodeGate;
import org.springframework.stereotype.Component;

/**
 * Coordinates per-node execution gates and repeat signals.
 */
@Component
public class NodeGateCoordinator {

    boolean tryAcquireOrSignalRepeat(NodeGate nodeGate) {
        if (nodeGate.executing.compareAndSet(false, true)) {
            return true;
        }
        nodeGate.repeatPending.set(true);
        return false;
    }

    boolean releaseAndConsumeRepeat(NodeGate nodeGate) {
        boolean needsRepeat = nodeGate.repeatPending.getAndSet(false);
        nodeGate.executing.set(false);
        needsRepeat |= nodeGate.repeatPending.getAndSet(false);
        return needsRepeat;
    }

    boolean tryAcquireForRepeat(NodeGate nodeGate) {
        return nodeGate.executing.compareAndSet(false, true);
    }

    void release(NodeGate nodeGate) {
        nodeGate.executing.set(false);
    }
}
