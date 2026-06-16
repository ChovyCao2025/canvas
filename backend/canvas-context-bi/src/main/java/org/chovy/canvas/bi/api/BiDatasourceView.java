package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiDatasourceView 视图。
 */
public record BiDatasourceView(
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
    public BiDatasourceView {
        schema = schema == null ? Map.of() : Map.copyOf(schema);
    }
}
