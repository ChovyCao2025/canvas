package org.chovy.canvas.canvas.domain;

import java.time.LocalDateTime;

public record UserInputForm(
        Long id,
        Long tenantId,
        Long canvasId,
        Long versionId,
        String executionId,
        String nodeId,
        String userId,
        String schemaJson,
        String completedNodeId,
        String timeoutNodeId,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public UserInputForm {
        requirePositive(tenantId, "tenantId");
        requirePositive(canvasId, "canvasId");
        requirePositive(versionId, "versionId");
        requireText(executionId, "executionId");
        requireText(nodeId, "nodeId");
        requireText(userId, "userId");
        requireText(schemaJson, "schemaJson");
    }

    public static UserInputForm pending(Long id,
                                        Long tenantId,
                                        Long canvasId,
                                        Long versionId,
                                        String executionId,
                                        String nodeId,
                                        String userId,
                                        String schemaJson,
                                        String completedNodeId,
                                        String timeoutNodeId,
                                        LocalDateTime expiresAt,
                                        LocalDateTime now) {
        return new UserInputForm(id, tenantId, canvasId, versionId, executionId, nodeId, userId,
                schemaJson, blankToNull(completedNodeId), blankToNull(timeoutNodeId), expiresAt, now, now);
    }

    public UserInputForm withId(Long newId) {
        return new UserInputForm(newId, tenantId, canvasId, versionId, executionId, nodeId, userId,
                schemaJson, completedNodeId, timeoutNodeId, expiresAt, createdAt, updatedAt);
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
