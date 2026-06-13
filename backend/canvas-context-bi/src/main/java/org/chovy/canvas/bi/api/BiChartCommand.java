package org.chovy.canvas.bi.api;

import java.util.Map;

public record BiChartCommand(
        Long workspaceId,
        String chartKey,
        String name,
        String chartType,
        String datasetKey,
        Map<String, Object> query,
        Map<String, Object> style,
        Map<String, Object> interaction,
        String status
) {
    public BiChartCommand {
        query = query == null ? Map.of() : Map.copyOf(query);
        style = style == null ? Map.of() : Map.copyOf(style);
        interaction = interaction == null ? Map.of() : Map.copyOf(interaction);
    }
}
