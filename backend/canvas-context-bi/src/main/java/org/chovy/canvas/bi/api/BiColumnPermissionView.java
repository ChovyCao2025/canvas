package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiColumnPermissionView 视图。
 */
public record BiColumnPermissionView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * 数据集标识。
         */
        Long datasetId,
        /**
         * fieldKey 对应的业务键。
         */
        String fieldKey,
        /**
         * subjectType 字段值。
         */
        String subjectType,
        /**
         * subjectId 对应的标识。
         */
        String subjectId,
        /**
         * policy 字段值。
         */
        String policy,
        /**
         * maskJson 的 JSON 序列化内容。
         */
        String maskJson,
        /**
         * enabled 字段值。
         */
        boolean enabled,
        LocalDateTime createdAt) {
}
