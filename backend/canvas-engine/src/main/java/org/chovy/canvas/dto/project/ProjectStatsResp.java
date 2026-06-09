package org.chovy.canvas.dto.project;

/**
 * ProjectStatsResp 承载 dto.project 场景中的不可变数据快照。
 * @param projectId projectId 字段。
 * @param canvasCount canvasCount 字段。
 * @param publishedCanvasCount publishedCanvasCount 字段。
 * @param executionCount7d executionCount7d 字段。
 * @param failedExecutionCount7d failedExecutionCount7d 字段。
 * @param avgDurationMs7d avgDurationMs7d 字段。
 */
public record ProjectStatsResp(
        Long projectId,
        Long canvasCount,
        Long publishedCanvasCount,
        Long executionCount7d,
        Long failedExecutionCount7d,
        Long avgDurationMs7d
) {
}
