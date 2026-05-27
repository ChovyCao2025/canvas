package org.chovy.canvas.engine.lane;

import reactor.core.Disposable;

public record ExecutionLaneAdmissionResult(
        boolean allowed,
        Reason reason,
        int canvasActive,
        int laneActive,
        int globalActive,
        Disposable.Swap slot
) {
    public enum Reason {
        NONE,
        CANVAS_LIMIT,
        LANE_LIMIT,
        GLOBAL_LIMIT,
        REGISTRY_UNAVAILABLE
    }

    public static ExecutionLaneAdmissionResult allowed(Disposable.Swap slot,
                                                       int canvasActive,
                                                       int laneActive,
                                                       int globalActive) {
        return new ExecutionLaneAdmissionResult(true, Reason.NONE, canvasActive, laneActive, globalActive, slot);
    }

    public static ExecutionLaneAdmissionResult rejected(Reason reason,
                                                        int canvasActive,
                                                        int laneActive,
                                                        int globalActive) {
        return new ExecutionLaneAdmissionResult(false, reason, canvasActive, laneActive, globalActive, null);
    }
}
