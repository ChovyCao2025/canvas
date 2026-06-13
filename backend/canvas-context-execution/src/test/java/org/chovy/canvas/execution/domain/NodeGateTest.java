package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NodeGateTest {

    @Test
    void separatesExecutionLockFromRepeatSignal() {
        NodeGate gate = new NodeGate();

        assertThat(gate.tryEnter()).isTrue();
        assertThat(gate.tryEnter()).isFalse();
        gate.markRepeat();

        assertThat(gate.release()).isTrue();
        assertThat(gate.tryEnter()).isTrue();
        assertThat(gate.release()).isFalse();
    }
}
