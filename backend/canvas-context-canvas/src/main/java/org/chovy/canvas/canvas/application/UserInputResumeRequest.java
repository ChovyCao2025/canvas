package org.chovy.canvas.canvas.application;

import java.util.Map;

public record UserInputResumeRequest(
        Long tenantId,
        Long canvasId,
        Long versionId,
        String executionId,
        String nodeId,
        String userId,
        Long responseId,
        String resumeStatus,
        Map<String, Object> payload) {

    public UserInputResumeRequest {
        payload = Map.copyOf(payload == null ? Map.of() : payload);
    }
}
