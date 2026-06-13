package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record BiDatasourceHealth(
        Long id,
        Long tenantId,
        BiResourceKey sourceKey,
        String connectorType,
        String syncStatus,
        boolean available,
        String healthMessage,
        Map<String, Object> schema,
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
