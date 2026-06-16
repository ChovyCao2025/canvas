package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiRowPermissionView 视图。
 */
public record BiRowPermissionView(
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
         * ruleKey 对应的业务键。
         */
        String ruleKey,
        /**
         * subjectType 字段值。
         */
        String subjectType,
        /**
         * subjectId 对应的标识。
         */
        String subjectId,
        /**
         * filterJson 的 JSON 序列化内容。
         */
        String filterJson,
        /**
         * enabled 字段值。
         */
        boolean enabled,
        LocalDateTime createdAt) {
}
