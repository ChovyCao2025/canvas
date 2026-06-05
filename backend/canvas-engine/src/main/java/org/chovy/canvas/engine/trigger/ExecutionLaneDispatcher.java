package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneAdmissionResult;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Owns final execution lane slot acquisition and overflow response shaping.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionLaneDispatcher {

    private final InFlightExecutionRegistry executionRegistry;
    private final ContextPersistenceService ctxStore;

    SlotAcquisitionResult tryAcquireSlot(Long canvasId,
                                         ExecutionContext ctx,
                                         ExecutionLane executionLane,
                                         int admissionLimit,
                                         int laneLimit,
                                         int globalMaxConcurrency,
                                         boolean dryRun,
                                         boolean isResume,
                                         String acquiredDedupKey) {
        if (dryRun) return SlotAcquisitionResult.skipped();
        ExecutionLaneAdmissionResult acquired = executionRegistry.tryAcquire(
                canvasId, ctx.getExecutionId(), executionLane,
                admissionLimit, laneLimit, globalMaxConcurrency);
        if (acquired == null || !acquired.allowed()) {
            ExecutionLaneAdmissionResult result = acquired != null
                    ? acquired
                    : ExecutionLaneAdmissionResult.rejected(
                    ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE, 0, 0, 0);
            log.warn("[ENGINE] 执行并发上限已达 canvasId={} lane={} reason={} canvas={}/{} lane={}/{} global={}/{}",
                    canvasId, executionLane, result.reason(),
                    result.canvasActive(), admissionLimit,
                    result.laneActive(), laneLimit,
                    result.globalActive(), globalMaxConcurrency);
            if (isResume) {
                ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId(), ctx.getResumeLockToken());
            }
            if (acquiredDedupKey != null) {
                ctxStore.releaseDedup(acquiredDedupKey);
            }
            return SlotAcquisitionResult.overflow(
                    Mono.just(Map.of(MapFieldKeys.OVERFLOW, "concurrency_limit_reached",
                            MapFieldKeys.EXECUTION_LANE, executionLane.name(),
                            MapFieldKeys.ADMISSION_REASON, result.reason().name(),
                            MapFieldKeys.ACTIVE, result.canvasActive(),
                            MapFieldKeys.LANE_ACTIVE, result.laneActive(),
                            MapFieldKeys.GLOBAL_ACTIVE, result.globalActive(),
                            MapFieldKeys.LIMIT, admissionLimit)));
        }
        return SlotAcquisitionResult.acquired(acquired.slot());
    }

    record SlotAcquisitionResult(
            Disposable.Swap slot,
            Mono<Map<String, Object>> overflowMono) {

        static SlotAcquisitionResult acquired(Disposable.Swap slot) {
            return new SlotAcquisitionResult(slot, null);
        }

        static SlotAcquisitionResult overflow(Mono<Map<String, Object>> mono) {
            return new SlotAcquisitionResult(null, mono);
        }

        static SlotAcquisitionResult skipped() {
            return new SlotAcquisitionResult(null, null);
        }

        boolean isOverflow() {
            return overflowMono != null;
        }
    }
}
