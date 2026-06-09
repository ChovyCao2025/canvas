package org.chovy.canvas.engine.lane;

import reactor.core.Disposable;

/**
 * ExecutionLaneAdmissionResult 承载 engine.lane 场景中的不可变数据快照。
 * @param allowed allowed 字段。
 * @param reason reason 字段。
 * @param canvasActive canvasActive 字段。
 * @param laneActive laneActive 字段。
 * @param globalActive globalActive 字段。
 * @param slot slot 字段。
 */
public record ExecutionLaneAdmissionResult(
        boolean allowed,
        Reason reason,
        int canvasActive,
        int laneActive,
        int globalActive,
        Disposable.Swap slot
) {
    /**
     * 执行通道准入结果原因。
     */
    public enum Reason {
        NONE,
        CANVAS_LIMIT,
        LANE_LIMIT,
        GLOBAL_LIMIT,
        REGISTRY_UNAVAILABLE
    }

    /**
     * allowed 处理 engine.lane 场景的业务逻辑。
     * @param slot slot 参数，用于 allowed 流程中的校验、计算或对象转换。
     * @param canvasActive canvas active 参数，用于 allowed 流程中的校验、计算或对象转换。
     * @param laneActive lane active 参数，用于 allowed 流程中的校验、计算或对象转换。
     * @param globalActive global active 参数，用于 allowed 流程中的校验、计算或对象转换。
     * @return 返回 allowed 流程生成的业务结果。
     */
    public static ExecutionLaneAdmissionResult allowed(Disposable.Swap slot,
                                                       int canvasActive,
                                                       int laneActive,
                                                       int globalActive) {
        return new ExecutionLaneAdmissionResult(true, Reason.NONE, canvasActive, laneActive, globalActive, slot);
    }

    /**
     * rejected 更新 engine.lane 场景的业务状态。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param canvasActive canvas active 参数，用于 rejected 流程中的校验、计算或对象转换。
     * @param laneActive lane active 参数，用于 rejected 流程中的校验、计算或对象转换。
     * @param globalActive global active 参数，用于 rejected 流程中的校验、计算或对象转换。
     * @return 返回 rejected 流程生成的业务结果。
     */
    public static ExecutionLaneAdmissionResult rejected(Reason reason,
                                                        int canvasActive,
                                                        int laneActive,
                                                        int globalActive) {
        return new ExecutionLaneAdmissionResult(false, reason, canvasActive, laneActive, globalActive, null);
    }
}
