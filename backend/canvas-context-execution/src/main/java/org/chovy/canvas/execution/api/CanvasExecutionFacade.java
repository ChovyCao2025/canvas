package org.chovy.canvas.execution.api;

import java.util.Map;

import org.chovy.canvas.execution.api.trace.ExecutionTraceView;

public interface CanvasExecutionFacade {

    ExecutionResultView trigger(ExecutionRequestCommand command);

    ExecutionTraceView trace(Long tenantId, String executionId);

    record ExecutionRequestCommand(
            Long tenantId,
            Long canvasId,
            Long versionId,
            String triggerType,
            String userId,
            Map<String, Object> payload,
            boolean dryRun) {

        public ExecutionRequestCommand {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("tenantId is required");
            }
            if (canvasId == null || canvasId <= 0) {
                throw new IllegalArgumentException("canvasId is required");
            }
            triggerType = triggerType == null || triggerType.isBlank() ? "MANUAL" : triggerType;
            userId = userId == null ? "" : userId;
            payload = Map.copyOf(payload == null ? Map.of() : payload);
        }
    }

    record ExecutionResultView(String executionId, String status) {
        public ExecutionResultView {
            if (executionId == null || executionId.isBlank()) {
                throw new IllegalArgumentException("executionId is required");
            }
            status = status == null || status.isBlank() ? "STARTED" : status;
        }
    }
}
