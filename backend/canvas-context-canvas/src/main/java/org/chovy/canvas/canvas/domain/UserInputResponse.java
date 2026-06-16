package org.chovy.canvas.canvas.domain;

import java.time.LocalDateTime;

/**
 * 承载UserInputResponse的数据快照。
 */
public record UserInputResponse(
        /**
         * 记录标识。
         */
        Long id,
        /**
         * 记录租户标识。
         */
        Long tenantId,
        /**
         * 记录form标识。
         */
        Long formId,
        /**
         * 记录画布标识。
         */
        Long canvasId,
        /**
         * 记录版本标识。
         */
        Long versionId,
        /**
         * 记录execution标识。
         */
        String executionId,
        /**
         * 记录节点标识。
         */
        String nodeId,
        /**
         * 记录用户标识。
         */
        String userId,
        /**
         * 记录响应JSON 内容。
         */
        String responseJson,
        /**
         * 记录状态。
         */
        UserInputStatus status,
        /**
         * 记录idempotencyKey。
         */
        String idempotencyKey,
        /**
         * 记录completed node标识。
         */
        String completedNodeId,
        /**
         * 记录timeout node标识。
         */
        String timeoutNodeId,
        /**
         * 记录expires时间。
         */
        LocalDateTime expiresAt,
        /**
         * 记录创建时间。
         */
        LocalDateTime createdAt,
        /**
         * 记录更新时间。
         */
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

    /**
     * 返回替换id后的不可变副本。
     */
    public UserInputResponse withId(Long newId) {
        return new UserInputResponse(newId, tenantId, formId, canvasId, versionId, executionId, nodeId,
                userId, responseJson, status, idempotencyKey, completedNodeId, timeoutNodeId,
                expiresAt, createdAt, updatedAt);
    }

    /**
     * 返回替换form id后的不可变副本。
     */
    public UserInputResponse withFormId(Long newFormId) {
        return new UserInputResponse(id, tenantId, newFormId, canvasId, versionId, executionId, nodeId,
                userId, responseJson, status, idempotencyKey, completedNodeId, timeoutNodeId,
                expiresAt, createdAt, updatedAt);
    }

    /**
     * 返回替换timestamps后的不可变副本。
     */
    public UserInputResponse withTimestamps(LocalDateTime created, LocalDateTime updated) {
        return new UserInputResponse(id, tenantId, formId, canvasId, versionId, executionId, nodeId,
                userId, responseJson, status, idempotencyKey, completedNodeId, timeoutNodeId,
                expiresAt, created, updated);
    }

    /**
     * 返回完成后的用户输入响应副本。
     */
    public UserInputResponse complete(String newResponseJson) {
        return new UserInputResponse(id, tenantId, formId, canvasId, versionId, executionId, nodeId,
                userId, newResponseJson, UserInputStatus.COMPLETED, idempotencyKey, completedNodeId,
                timeoutNodeId, expiresAt, createdAt, updatedAt);
    }

    /**
     * 返回完成后的用户输入响应副本。
     */
    public UserInputResponse complete(String newResponseJson, LocalDateTime updated) {
        return new UserInputResponse(id, tenantId, formId, canvasId, versionId, executionId, nodeId,
                userId, newResponseJson, UserInputStatus.COMPLETED, idempotencyKey, completedNodeId,
                timeoutNodeId, expiresAt, createdAt, updated);
    }
}
