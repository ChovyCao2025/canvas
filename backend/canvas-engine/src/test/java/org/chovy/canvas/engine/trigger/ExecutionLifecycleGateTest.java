package org.chovy.canvas.engine.trigger;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionLifecycleGateTest {

    @Test
    void shutdownRejectsNewEntries() {
        ExecutionLifecycleGate gate = new ExecutionLifecycleGate(10);

        gate.shutdown();

        assertThat(gate.isAccepting()).isFalse();
        assertThat(gate.tryEnter()).isFalse();
    }

    @Test
    void shutdownWaitsUntilInFlightExecutionExits() throws Exception {
        ExecutionLifecycleGate gate = new ExecutionLifecycleGate(5_000);
        assertThat(gate.tryEnter()).isTrue();

        CompletableFuture<Void> shutdown = CompletableFuture.runAsync(gate::shutdown);
        Thread.sleep(100);
        assertThat(shutdown).isNotDone();

        gate.exit();

        shutdown.get(5, TimeUnit.SECONDS);
        assertThat(gate.inFlightCount()).isZero();
        assertThat(gate.isAccepting()).isFalse();
    }
}
