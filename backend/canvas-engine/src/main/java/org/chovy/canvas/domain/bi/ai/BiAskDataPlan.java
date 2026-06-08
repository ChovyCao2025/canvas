package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiSort;

import java.util.List;

/**
 * BiAskDataPlan 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param dimensions dimensions 字段。
 * @param metrics metrics 字段。
 * @param filters filters 字段。
 * @param sorts sorts 字段。
 * @param limit limit 字段。
 * @param explanation explanation 字段。
 */
public record BiAskDataPlan(
        String datasetKey,
        List<String> dimensions,
        List<String> metrics,
        List<BiFilter> filters,
        List<BiSort> sorts,
        int limit,
        String explanation
) {
    public BiAskDataPlan {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        filters = filters == null ? List.of() : List.copyOf(filters);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }
}
