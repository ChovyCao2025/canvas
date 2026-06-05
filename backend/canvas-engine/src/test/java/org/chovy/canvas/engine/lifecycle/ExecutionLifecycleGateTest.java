package org.chovy.canvas.engine.lifecycle;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionLifecycleGateTest {

    @Test
    void beginShutdownRejectsNewWorkButLetsAcceptedWorkDrain() {
        ExecutionLifecycleGate gate = new ExecutionLifecycleGate();
        ExecutionLifecycleGate.WorkPermit permit = gate.acquire("direct-call");

        gate.beginShutdown();

        assertThat(gate.isAcceptingNewWork()).isFalse();
        assertThat(gate.inFlightCount()).isEqualTo(1);
        assertThatThrownBy(() -> gate.acquire("mq-trigger"))
                .isInstanceOf(ExecutionLifecycleException.class)
                .hasMessageContaining("mq-trigger");

        permit.close();

        assertThat(gate.inFlightCount()).isZero();
    }

    @Test
    void guardTracksReactiveWorkUntilTermination() {
        ExecutionLifecycleGate gate = new ExecutionLifecycleGate();

        Disposable subscription = gate.guard("disruptor", Mono.never()).subscribe();

        assertThat(gate.inFlightCount()).isEqualTo(1);
        subscription.dispose();
        assertThat(gate.inFlightCount()).isZero();
    }

    @Test
    void trackAcceptedWorkAllowsAlreadyAdmittedWorkToDrainAfterShutdownBegins() {
        ExecutionLifecycleGate gate = new ExecutionLifecycleGate();
        gate.beginShutdown();

        Disposable subscription = gate.trackAccepted("disruptor", Mono.never()).subscribe();

        assertThat(gate.inFlightCount()).isEqualTo(1);
        subscription.dispose();
        assertThat(gate.inFlightCount()).isZero();
    }
}
