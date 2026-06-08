package org.chovy.canvas.domain.bi.chart;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;

import java.util.Map;

/**
 * BiChartResource 承载 domain.bi.chart 场景中的不可变数据快照。
 * @param chartKey chartKey 字段。
 * @param name name 字段。
 * @param chartType chartType 字段。
 * @param datasetKey datasetKey 字段。
 * @param query query 字段。
 * @param style style 字段。
 * @param interaction interaction 字段。
 * @param status status 字段。
 * @param source source 字段。
 */
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
