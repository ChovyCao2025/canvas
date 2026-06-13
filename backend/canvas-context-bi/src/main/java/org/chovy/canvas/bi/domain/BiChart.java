package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record BiChart(
        Long id,
        Long tenantId,
        Long workspaceId,
        BiResourceKey chartKey,
        String name,
        String chartType,
        Long datasetId,
        BiResourceKey datasetKey,
        Map<String, Object> query,
        Map<String, Object> style,
        Map<String, Object> interaction,
        BiResourceStatus status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BiChart {
        tenantId = tenantId == null ? 0L : tenantId;
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        if (chartKey == null) {
            throw new IllegalArgumentException("chartKey is required");
        }
        if (datasetId == null || datasetId <= 0 || datasetKey == null) {
            throw new IllegalArgumentException("chart dataset is required");
        }
        name = name == null || name.isBlank() ? chartKey.value() : name.trim();
        chartType = chartType == null || chartType.isBlank()
                ? "TABLE"
                : chartType.trim().toUpperCase(java.util.Locale.ROOT);
        query = query == null ? Map.of() : Map.copyOf(query);
        style = style == null ? Map.of() : Map.copyOf(style);
        interaction = interaction == null ? Map.of() : Map.copyOf(interaction);
        status = status == null ? BiResourceStatus.DRAFT : status;
    }

    public BiChart withId(Long newId) {
        return new BiChart(newId, tenantId, workspaceId, chartKey, name, chartType, datasetId, datasetKey,
                query, style, interaction, status, createdBy, createdAt, updatedAt);
    }
}
