package org.chovy.canvas.dto.project;

public record ProjectStatsResp(
        Long projectId,
        Long canvasCount,
        Long publishedCanvasCount,
        Long executionCount7d,
        Long failedExecutionCount7d,
        Long avgDurationMs7d
) {
}
