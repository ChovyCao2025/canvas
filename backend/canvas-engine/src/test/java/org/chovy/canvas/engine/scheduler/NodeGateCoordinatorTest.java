package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.context.NodeGate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NodeGateCoordinatorTest {

    private final NodeGateCoordinator coordinator = new NodeGateCoordinator();

    @Test
    void firstAcquireMarksGateExecuting() {
        NodeGate gate = new NodeGate();

        assertThat(coordinator.tryAcquireOrSignalRepeat(gate)).isTrue();

        assertThat(gate.executing).isTrue();
        assertThat(gate.repeatPending).isFalse();
    }

    @Test
    void competingAcquireSignalsRepeatWithoutTakingGate() {
        NodeGate gate = new NodeGate();
        coordinator.tryAcquireOrSignalRepeat(gate);

        assertThat(coordinator.tryAcquireOrSignalRepeat(gate)).isFalse();

        assertThat(gate.executing).isTrue();
        assertThat(gate.repeatPending).isTrue();
    }

    @Test
    void releaseConsumesRepeatSignalAndUnlocksGate() {
        NodeGate gate = new NodeGate();
        coordinator.tryAcquireOrSignalRepeat(gate);
        coordinator.tryAcquireOrSignalRepeat(gate);

        boolean repeat = coordinator.releaseAndConsumeRepeat(gate);

        assertThat(repeat).isTrue();
        assertThat(gate.executing).isFalse();
        assertThat(gate.repeatPending).isFalse();
    }

    @Test
    void repeatAcquireRetakesUnlockedGate() {
        NodeGate gate = new NodeGate();
        coordinator.tryAcquireOrSignalRepeat(gate);
        coordinator.release(gate);

        assertThat(coordinator.tryAcquireForRepeat(gate)).isTrue();

        assertThat(gate.executing).isTrue();
    }
}
