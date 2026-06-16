package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiQueryExplainResult 结果。
 */
public record BiQueryExplainResult(
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * sqlHash 字段值。
         */
        String sqlHash,
        /**
         * parametersCount 对应的统计数量。
         */
        int parametersCount,
        List<String> steps) {

    public BiQueryExplainResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
