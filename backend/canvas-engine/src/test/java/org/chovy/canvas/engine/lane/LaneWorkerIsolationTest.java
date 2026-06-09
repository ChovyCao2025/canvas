package org.chovy.canvas.engine.lane;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LaneWorkerIsolationTest {

    @Test
    void createsReadinessWorkersForLightStandardHeavyAndRetry() {
        ExecutionLaneWorkerRegistry registry = readinessRegistry();

        assertThat(registry.capacity(ExecutionLane.LIGHT)).isEqualTo(2);
        assertThat(registry.capacity(ExecutionLane.STANDARD)).isEqualTo(3);
        assertThat(registry.capacity(ExecutionLane.HEAVY)).isEqualTo(1);
        assertThat(registry.capacity(ExecutionLane.RETRY)).isEqualTo(1);
    }

    @Test
    void protectsLightAndStandardCapacity() {
        ExecutionLaneWorkerRegistry registry = readinessRegistry();

        assertThat(registry.protectedLanes()).containsExactlyInAnyOrder(ExecutionLane.LIGHT, ExecutionLane.STANDARD);
    }

    @Test
    void heavySaturationDoesNotConsumeLightWorkers() {
        ExecutionLaneWorkerRegistry registry = readinessRegistry();

        ExecutionLaneWorkerRegistry.Reservation heavy = registry.tryReserve(ExecutionLane.HEAVY);
        ExecutionLaneWorkerRegistry.Reservation rejectedHeavy = registry.tryReserve(ExecutionLane.HEAVY);
        ExecutionLaneWorkerRegistry.Reservation light = registry.tryReserve(ExecutionLane.LIGHT);

        assertThat(heavy.accepted()).isTrue();
        assertThat(rejectedHeavy.accepted()).isFalse();
        assertThat(light.accepted()).isTrue();

        heavy.close();
        light.close();
    }

    @Test
    void retrySaturationDoesNotConsumeStandardWorkers() {
        ExecutionLaneWorkerRegistry registry = readinessRegistry();

        ExecutionLaneWorkerRegistry.Reservation retry = registry.tryReserve(ExecutionLane.RETRY);
        ExecutionLaneWorkerRegistry.Reservation rejectedRetry = registry.tryReserve(ExecutionLane.RETRY);
        ExecutionLaneWorkerRegistry.Reservation standard = registry.tryReserve(ExecutionLane.STANDARD);

        assertThat(retry.accepted()).isTrue();
        assertThat(rejectedRetry.accepted()).isFalse();
        assertThat(standard.accepted()).isTrue();

        retry.close();
        standard.close();
    }

    @Test
    void guardedExecutionHoldsLanePermitUntilCompletion() {
        ExecutionLaneWorkerRegistry registry = new ExecutionLaneWorkerRegistry(true, Map.of(ExecutionLane.HEAVY, 1));
        Sinks.Empty<Void> completion = Sinks.empty();

        registry.guard(ExecutionLane.HEAVY, completion.asMono()).subscribe();

        assertThat(registry.availablePermits(ExecutionLane.HEAVY)).isZero();

        completion.tryEmitEmpty();

        assertThat(registry.availablePermits(ExecutionLane.HEAVY)).isEqualTo(1);
    }

    @Test
    void disabledIsolationUsesExistingDispatcherPath() {
        ExecutionLaneWorkerRegistry registry = new ExecutionLaneWorkerRegistry(false, Map.of(ExecutionLane.HEAVY, 0));

        ExecutionLaneWorkerRegistry.Reservation first = registry.tryReserve(ExecutionLane.HEAVY);
        ExecutionLaneWorkerRegistry.Reservation second = registry.tryReserve(ExecutionLane.HEAVY);

        assertThat(first.accepted()).isTrue();
        assertThat(second.accepted()).isTrue();
        assertThat(registry.dispatch(ExecutionLane.HEAVY, () -> { })).isTrue();
    }

    private ExecutionLaneWorkerRegistry readinessRegistry() {
        return new ExecutionLaneWorkerRegistry(true, Map.of(
                ExecutionLane.LIGHT, 2,
                ExecutionLane.STANDARD, 3,
                ExecutionLane.HEAVY, 1,
                ExecutionLane.RETRY, 1));
    }
}
