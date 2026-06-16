package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiDatasourceHealth 不可变数据载体。
 */
public record BiDatasourceHealth(
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
        BiResourceKey sourceKey,
        /**
         * connectorType 字段值。
         */
        String connectorType,
        /**
         * syncStatus 对应的数据集合。
         */
        String syncStatus,
        /**
         * available 字段值。
         */
        boolean available,
        /**
         * healthMessage 字段值。
         */
        String healthMessage,
        /**
         * schema 字段值。
         */
        Map<String, Object> schema,
        /**
         * 最近检测时间。
         */
        LocalDateTime lastCheckedAt
) {
    public BiDatasourceHealth {
        tenantId = tenantId == null ? 0L : tenantId;
        if (sourceKey == null) {
            throw new IllegalArgumentException("sourceKey is required");
        }
        connectorType = connectorType == null || connectorType.isBlank()
                ? "UNKNOWN"
                : connectorType.trim().toUpperCase(java.util.Locale.ROOT);
        syncStatus = syncStatus == null || syncStatus.isBlank()
                ? "UNKNOWN"
                : syncStatus.trim().toUpperCase(java.util.Locale.ROOT);
        schema = schema == null ? Map.of() : Map.copyOf(schema);
    }
}
