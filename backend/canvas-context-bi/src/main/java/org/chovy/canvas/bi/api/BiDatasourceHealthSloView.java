package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiDatasourceHealthSloView 视图。
 */
public record BiDatasourceHealthSloView(
        /**
         * totalChecks 对应的数据集合。
         */
        int totalChecks,
        /**
         * availableChecks 对应的数据集合。
         */
        int availableChecks,
        /**
         * unavailableChecks 对应的数据集合。
         */
        int unavailableChecks,
        /**
         * availabilityRate 字段值。
         */
        double availabilityRate,
        List<Map<String, Object>> sources) {

    public BiDatasourceHealthSloView {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
