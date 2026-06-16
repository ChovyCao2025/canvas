package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiQueryDatasetView 视图。
 */
public record BiQueryDatasetView(
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * 字段列表。
         */
        List<BiQueryFieldView> fields,
        List<BiQueryMetricView> metrics) {
}
