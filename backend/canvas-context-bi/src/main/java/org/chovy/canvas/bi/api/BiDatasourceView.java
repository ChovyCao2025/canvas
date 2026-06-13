package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;

public record BiDatasourceView(
        Long id,
        Long tenantId,
        String sourceKey,
        String connectorType,
        String syncStatus,
        boolean available,
        String healthMessage,
        Map<String, Object> schema,
        LocalDateTime lastCheckedAt
) {
    public BiDatasourceView {
        schema = schema == null ? Map.of() : Map.copyOf(schema);
    }
}
