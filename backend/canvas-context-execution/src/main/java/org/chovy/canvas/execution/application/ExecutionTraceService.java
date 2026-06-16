package org.chovy.canvas.execution.application;

import java.time.Instant;
import java.util.Map;

import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.springframework.stereotype.Service;

@Service
public class ExecutionTraceService {

    private final ExecutionTraceRepository repository;

    public ExecutionTraceService(ExecutionTraceRepository repository) {
        this.repository = repository;
    }

    public void start(Long tenantId, String executionId, Long canvasId, Long versionId) {
        repository.saveStarted(new ExecutionTraceRecord(
                tenantId,
                executionId,
                canvasId,
                versionId,
                "RUNNING",
                Instant.now(),
                null,
                ""));
    }

    public void recordNode(
            Long tenantId,
            String executionId,
            String nodeId,
            String nodeType,
            String status,
            String error,
            Map<String, Object> outputData) {
        repository.appendNode(new ExecutionNodeTraceRecord(
                tenantId,
                executionId,
                nodeId,
                nodeType,
                status,
                error,
                outputData,
                Instant.now()));
    }

    public void finish(Long tenantId, String executionId, String status, String failureReason) {
        repository.markFinished(tenantId, executionId, status, failureReason, Instant.now());
    }

    public void recordResume(
            Long tenantId,
            Long canvasId,
            Long versionId,
            String executionId,
            String nodeId,
            String status,
            Map<String, Object> outputData) {
        recordNode(tenantId, executionId, nodeId, "USER_INPUT", "SUCCESS", "", outputData);
        finish(tenantId, executionId, status, "");
    }

    public ExecutionTraceView trace(Long tenantId, String executionId) {
        return repository.get(tenantId, executionId);
    }
}
