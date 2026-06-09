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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param ctx ctx 参数，用于 tryAcquireSlot 流程中的校验、计算或对象转换。
     * @param executionLane execution lane 参数，用于 tryAcquireSlot 流程中的校验、计算或对象转换。
     * @param admissionLimit admission limit 参数，用于 tryAcquireSlot 流程中的校验、计算或对象转换。
     * @param laneLimit lane limit 参数，用于 tryAcquireSlot 流程中的校验、计算或对象转换。
     * @param globalMaxConcurrency global max concurrency 参数，用于 tryAcquireSlot 流程中的校验、计算或对象转换。
     * @param dryRun dry run 参数，用于 tryAcquireSlot 流程中的校验、计算或对象转换。
     * @param isResume is resume 参数，用于 tryAcquireSlot 流程中的校验、计算或对象转换。
     * @param acquiredDedupKey 业务键，用于在同一租户下定位资源。
     * @return 返回 tryAcquireSlot 流程生成的业务结果。
     */
    SlotAcquisitionResult tryAcquireSlot(Long canvasId,
                                         ExecutionContext ctx,
                                         ExecutionLane executionLane,
                                         int admissionLimit,
                                         int laneLimit,
                                         int globalMaxConcurrency,
                                         boolean dryRun,
                                         boolean isResume,
                                         String acquiredDedupKey) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return SlotAcquisitionResult.acquired(acquired.slot());
    }

    /**
     * SlotAcquisitionResult 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    record SlotAcquisitionResult(
            Disposable.Swap slot,
            Mono<Map<String, Object>> overflowMono) {

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param slot slot 参数，用于 acquired 流程中的校验、计算或对象转换。
         * @return 返回 acquired 流程生成的业务结果。
         */
        static SlotAcquisitionResult acquired(Disposable.Swap slot) {
            return new SlotAcquisitionResult(slot, null);
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param MonoMapString mono map string 参数，用于 overflow 流程中的校验、计算或对象转换。
         * @param mono 待调度任务或操作名称，用于封装阻塞工作。
         * @return 返回 overflow 流程生成的业务结果。
         */
        static SlotAcquisitionResult overflow(Mono<Map<String, Object>> mono) {
            return new SlotAcquisitionResult(null, mono);
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 skipped 流程生成的业务结果。
         */
        static SlotAcquisitionResult skipped() {
            return new SlotAcquisitionResult(null, null);
        }

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @return 返回布尔判断结果。
         */
        boolean isOverflow() {
            return overflowMono != null;
        }
    }
}
