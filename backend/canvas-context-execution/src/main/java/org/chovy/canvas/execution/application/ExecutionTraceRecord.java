package org.chovy.canvas.execution.application;

import java.time.Instant;

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
