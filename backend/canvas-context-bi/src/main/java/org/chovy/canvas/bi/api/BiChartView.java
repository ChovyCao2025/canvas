package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;

public record BiChartView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String chartKey,
        String name,
        String chartType,
        Long datasetId,
        String datasetKey,
        Map<String, Object> query,
        Map<String, Object> style,
        Map<String, Object> interaction,
        String status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BiChartView {
        query = query == null ? Map.of() : Map.copyOf(query);
        style = style == null ? Map.of() : Map.copyOf(style);
        interaction = interaction == null ? Map.of() : Map.copyOf(interaction);
    }
}
