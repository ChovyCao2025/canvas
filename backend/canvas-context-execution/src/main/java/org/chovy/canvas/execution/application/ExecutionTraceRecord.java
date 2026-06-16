package org.chovy.canvas.execution.application;

import java.time.Instant;

/**
 * 定义 ExecutionTraceRecord 的执行上下文数据结构或业务契约。
 * @param tenantId tenantId 对应的数据字段
 * @param executionId executionId 对应的数据字段
 * @param canvasId canvasId 对应的数据字段
 * @param versionId versionId 对应的数据字段
 * @param status status 对应的数据字段
 * @param startedAt startedAt 对应的数据字段
 * @param finishedAt finishedAt 对应的数据字段
 * @param failureReason failureReason 对应的数据字段
 */
public record ExecutionTraceRecord(
        Long tenantId,
        String executionId,
        Long canvasId,
        Long versionId,
        String status,
        Instant startedAt,
        Instant finishedAt,
        String failureReason) {

    public ExecutionTraceRecord {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("executionId is required");
        }
        if (canvasId == null || canvasId <= 0) {
            throw new IllegalArgumentException("canvasId is required");
        }
        if (versionId == null || versionId <= 0) {
            throw new IllegalArgumentException("versionId is required");
        }
        status = status == null || status.isBlank() ? "RUNNING" : status;
        startedAt = startedAt == null ? Instant.now() : startedAt;
        failureReason = failureReason == null ? "" : failureReason;
    }
}
