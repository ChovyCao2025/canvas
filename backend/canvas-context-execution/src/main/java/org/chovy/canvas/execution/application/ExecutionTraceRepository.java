package org.chovy.canvas.execution.application;

import java.time.Instant;

import org.chovy.canvas.execution.api.trace.ExecutionTraceView;

public interface ExecutionTraceRepository {

    void saveStarted(ExecutionTraceRecord trace);

    void appendNode(ExecutionNodeTraceRecord nodeTrace);

    void markFinished(Long tenantId, String executionId, String status, String failureReason, Instant finishedAt);

    ExecutionTraceView get(Long tenantId, String executionId);
}
