package org.chovy.canvas.domain.bi.chart;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;

import java.util.Map;

public record BiChartResource(
        String chartKey,
        String name,
        String chartType,
        String datasetKey,
        BiQueryRequest query,
        Map<String, Object> style,
        Map<String, Object> interaction,
        String status,
        String source
) {
    public BiChartResource {
        style = style == null ? Map.of() : Map.copyOf(style);
        interaction = interaction == null ? Map.of() : Map.copyOf(interaction);
    }
}
