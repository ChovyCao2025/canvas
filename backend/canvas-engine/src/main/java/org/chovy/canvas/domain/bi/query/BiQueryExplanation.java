package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiQueryExplanation 承载 domain.bi.query 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param sqlHash sqlHash 字段。
 * @param parametersCount parametersCount 字段。
 * @param steps steps 字段。
 */
public record BiQueryExplanation(
        String datasetKey,
        String sqlHash,
        int parametersCount,
        List<String> steps
) {
    public BiQueryExplanation {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
