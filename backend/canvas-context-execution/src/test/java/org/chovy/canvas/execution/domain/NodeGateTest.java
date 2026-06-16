package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 定义 NodeGateTest 的执行上下文数据结构或业务契约。
 */
class NodeGateTest {

    /**
     * 执行 separatesExecutionLockFromRepeatSignal 对应的业务处理。
     */
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
