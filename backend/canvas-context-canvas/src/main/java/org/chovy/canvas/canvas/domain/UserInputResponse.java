package org.chovy.canvas.canvas.domain;

import java.time.LocalDateTime;

public record UserInputResponse(
        Long id,
        Long tenantId,
        Long formId,
        Long canvasId,
        Long versionId,
        String executionId,
        String nodeId,
        String userId,
        String responseJson,
        UserInputStatus status,
        String idempotencyKey,
        String completedNodeId,
        String timeoutNodeId,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public UserInputResponse {
        UserInputForm.requirePositive(tenantId, "tenantId");
        UserInputForm.requirePositive(formId, "formId");
        UserInputForm.requirePositive(canvasId, "canvasId");
        UserInputForm.requirePositive(versionId, "versionId");
        UserInputForm.requireText(executionId, "executionId");
        UserInputForm.requireText(nodeId, "nodeId");
        UserInputForm.requireText(userId, "userId");
        UserInputForm.requireText(idempotencyKey, "idempotencyKey");
        status = status == null ? UserInputStatus.PENDING : status;
    }

    public static UserInputResponse pending(Long id,
                                            Long tenantId,
                                            Long formId,
                                            Long canvasId,
                                            Long versionId,
                                            String executionId,
                                            String nodeId,
                                            String userId,
                                            String idempotencyKey,
                                            String completedNodeId,
                                            String timeoutNodeId,
                                            LocalDateTime expiresAt) {
        return new UserInputResponse(id, tenantId, formId, canvasId, versionId, executionId, nodeId,
                userId, null, UserInputStatus.PENDING, idempotencyKey, UserInputForm.blankToNull(completedNodeId),
                UserInputForm.blankToNull(timeoutNodeId), expiresAt, null, null);
    }

    public UserInputResponse withId(Long newId) {
        return new UserInputResponse(newId, tenantId, formId, canvasId, versionId, executionId, nodeId,
                userId, responseJson, status, idempotencyKey, completedNodeId, timeoutNodeId,
                expiresAt, createdAt, updatedAt);
    }

    public UserInputResponse withFormId(Long newFormId) {
        return new UserInputResponse(id, tenantId, newFormId, canvasId, versionId, executionId, nodeId,
                userId, responseJson, status, idempotencyKey, completedNodeId, timeoutNodeId,
                expiresAt, createdAt, updatedAt);
    }

    public UserInputResponse withTimestamps(LocalDateTime created, LocalDateTime updated) {
        return new UserInputResponse(id, tenantId, formId, canvasId, versionId, executionId, nodeId,
                userId, responseJson, status, idempotencyKey, completedNodeId, timeoutNodeId,
                expiresAt, created, updated);
    }

    public UserInputResponse complete(String newResponseJson) {
        return new UserInputResponse(id, tenantId, formId, canvasId, versionId, executionId, nodeId,
                userId, newResponseJson, UserInputStatus.COMPLETED, idempotencyKey, completedNodeId,
                timeoutNodeId, expiresAt, createdAt, updatedAt);
    }

    public UserInputResponse complete(String newResponseJson, LocalDateTime updated) {
        return new UserInputResponse(id, tenantId, formId, canvasId, versionId, executionId, nodeId,
                userId, newResponseJson, UserInputStatus.COMPLETED, idempotencyKey, completedNodeId,
                timeoutNodeId, expiresAt, createdAt, updated);
    }
}
