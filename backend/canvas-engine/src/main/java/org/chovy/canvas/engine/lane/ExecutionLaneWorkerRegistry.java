package org.chovy.canvas.engine.lane;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

@Component
public class ExecutionLaneWorkerRegistry {

    private static final Set<ExecutionLane> PROTECTED_LANES = Set.of(ExecutionLane.LIGHT, ExecutionLane.STANDARD);

    private final boolean enabled;
    private final Map<ExecutionLane, Integer> capacities;
    private final Map<ExecutionLane, Semaphore> semaphores;

    public ExecutionLaneWorkerRegistry(
            @Value("${canvas.execution-request.lane-isolation.enabled:false}") boolean enabled,
            @Value("${canvas.execution-request.lane-isolation.light-workers:800}") int lightWorkers,
            @Value("${canvas.execution-request.lane-isolation.standard-workers:2400}") int standardWorkers,
            @Value("${canvas.execution-request.lane-isolation.heavy-workers:400}") int heavyWorkers,
            @Value("${canvas.execution-request.lane-isolation.retry-workers:400}") int retryWorkers) {
        this(enabled, Map.of(
                ExecutionLane.LIGHT, lightWorkers,
                ExecutionLane.STANDARD, standardWorkers,
                ExecutionLane.HEAVY, heavyWorkers,
                ExecutionLane.RETRY, retryWorkers));
    }

    public ExecutionLaneWorkerRegistry(boolean enabled, Map<ExecutionLane, Integer> capacities) {
        this.enabled = enabled;
        this.capacities = new EnumMap<>(ExecutionLane.class);
        this.semaphores = new EnumMap<>(ExecutionLane.class);
        for (ExecutionLane lane : ExecutionLane.values()) {
            int capacity = Math.max(0, capacities.getOrDefault(lane, 0));
            this.capacities.put(lane, capacity);
            this.semaphores.put(lane, new Semaphore(capacity));
        }
    }

    public int capacity(ExecutionLane lane) {
        return capacities.getOrDefault(effectiveLane(lane), 0);
    }

    public int availablePermits(ExecutionLane lane) {
        Semaphore semaphore = semaphores.get(effectiveLane(lane));
        return semaphore == null ? 0 : semaphore.availablePermits();
    }

    public Set<ExecutionLane> protectedLanes() {
        return PROTECTED_LANES;
    }

    public Reservation tryReserve(ExecutionLane lane) {
        if (!enabled) {
            return Reservation.accepted(null);
        }
        ExecutionLane effectiveLane = effectiveLane(lane);
        Semaphore semaphore = semaphores.get(effectiveLane);
        if (semaphore == null || !semaphore.tryAcquire()) {
            return Reservation.rejected(effectiveLane);
        }
        return Reservation.accepted(semaphore);
    }

    public boolean dispatch(ExecutionLane lane, Runnable task) {
        try (Reservation reservation = tryReserve(lane)) {
            if (!reservation.accepted()) {
                return false;
            }
            task.run();
            return true;
        }
    }

    public <T> Mono<T> guard(ExecutionLane lane, Mono<T> source) {
        return Mono.defer(() -> {
            Reservation reservation = tryReserve(lane);
            if (!reservation.accepted()) {
                return Mono.error(new LaneCapacityExceededException(effectiveLane(lane)));
            }
            return source.doFinally(ignored -> reservation.close());
        });
    }

    private ExecutionLane effectiveLane(ExecutionLane lane) {
        return lane == null ? ExecutionLane.STANDARD : lane;
    }

    public static class LaneCapacityExceededException extends RuntimeException {
        private final ExecutionLane lane;

        LaneCapacityExceededException(ExecutionLane lane) {
            super("lane worker capacity exceeded: " + lane);
            this.lane = lane;
        }

        public ExecutionLane lane() {
            return lane;
        }
    }

    public record Reservation(boolean accepted, Semaphore semaphore) implements AutoCloseable {

        private static Reservation accepted(Semaphore semaphore) {
            return new Reservation(true, semaphore);
        }

        private static Reservation rejected(ExecutionLane lane) {
            return new Reservation(false, null);
        }

        @Override
        public void close() {
            if (accepted && semaphore != null) {
                semaphore.release();
            }
        }
    }
}
