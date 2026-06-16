package org.chovy.canvas.canvas.domain;

import java.time.LocalDateTime;

/**
 * 承载UserInputForm的数据快照。
 */
public record UserInputForm(
        /**
         * 记录标识。
         */
        Long id,
        /**
         * 记录租户标识。
         */
        Long tenantId,
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
         * 记录schemaJSON 内容。
         */
        String schemaJson,
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

    /**
     * 返回替换id后的不可变副本。
     */
    public UserInputForm withId(Long newId) {
        return new UserInputForm(newId, tenantId, canvasId, versionId, executionId, nodeId, userId,
                schemaJson, completedNodeId, timeoutNodeId, expiresAt, createdAt, updatedAt);
    }

    /**
     * 将空白文本规范化为空值。
     */
    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 校验数值必须为正数。
     */
    static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    /**
     * 校验文本不能为空。
     */
    static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
