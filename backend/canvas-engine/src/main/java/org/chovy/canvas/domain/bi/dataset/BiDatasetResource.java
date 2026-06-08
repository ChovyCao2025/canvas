package org.chovy.canvas.domain.bi.dataset;

import java.util.List;
import java.util.Map;

/**
 * BiDatasetResource 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param name name 字段。
 * @param datasetType datasetType 字段。
 * @param tableExpression tableExpression 字段。
 * @param tenantColumn tenantColumn 字段。
 * @param model model 字段。
 * @param fields fields 字段。
 * @param metrics metrics 字段。
 * @param status status 字段。
 * @param source source 字段。
 */
public record BiDatasetResource(
        String datasetKey,
        String name,
        String datasetType,
        String tableExpression,
        String tenantColumn,
        Map<String, Object> model,
        List<BiDatasetFieldResource> fields,
        List<BiMetricResource> metrics,
        String status,
        String source
) {
    public BiDatasetResource {
        model = model == null ? Map.of() : Map.copyOf(model);
        fields = fields == null ? List.of() : List.copyOf(fields);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
