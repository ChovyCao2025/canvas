package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiDatasourceOnboardingView 视图。
 */
public record BiDatasourceOnboardingView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * sourceKey 对应的业务键。
         */
        String sourceKey,
        /**
         * connectorType 字段值。
         */
        String connectorType,
        /**
         * 展示名称。
         */
        String name,
        /**
         * maskedUrl 字段值。
         */
        String maskedUrl,
        /**
         * maskedUsername 字段值。
         */
        String maskedUsername,
        /**
         * 说明文本。
         */
        String description,
        /**
         * enabled 字段值。
         */
        boolean enabled,
        /**
         * 状态值。
         */
        String status,
        /**
         * connectorConfig 字段值。
         */
        Map<String, Object> connectorConfig,
        /**
         * 创建人。
         */
        String createdBy,
        String updatedBy) {

    public BiDatasourceOnboardingView {
        connectorConfig = connectorConfig == null ? Map.of() : Map.copyOf(connectorConfig);
    }
}
