package org.chovy.canvas.bi.domain;

import java.util.Comparator;
import java.util.List;

public final class BiQueryDatasetCatalog {

    private static final BiQueryDataset CANVAS_DAILY_STATS = new BiQueryDataset(
            "canvas_daily_stats",
            List.of(
                    new BiQueryField("stat_date", "DIMENSION", "DATE"),
                    new BiQueryField("canvas_id", "DIMENSION", "NUMBER"),
                    new BiQueryField("canvas_name", "DIMENSION", "STRING"),
                    new BiQueryField("trigger_type", "DIMENSION", "STRING"),
                    new BiQueryField("total_executions", "MEASURE", "NUMBER"),
                    new BiQueryField("success_count", "MEASURE", "NUMBER"),
                    new BiQueryField("fail_count", "MEASURE", "NUMBER"),
                    new BiQueryField("running_count", "MEASURE", "NUMBER"),
                    new BiQueryField("unique_users", "MEASURE", "NUMBER"),
                    new BiQueryField("avg_duration_ms", "MEASURE", "NUMBER")),
            List.of(
                    new BiQueryMetric("total_executions", "NUMBER"),
                    new BiQueryMetric("success_count", "NUMBER"),
                    new BiQueryMetric("fail_count", "NUMBER"),
                    new BiQueryMetric("unique_users", "NUMBER"),
                    new BiQueryMetric("avg_duration_ms", "NUMBER"),
                    new BiQueryMetric("success_rate", "PERCENT")));

    public List<BiQueryDataset> datasets(Long tenantId) {
        return List.of(CANVAS_DAILY_STATS);
    }

    public BiQueryDataset dataset(Long tenantId, String datasetKey) {
        if (CANVAS_DAILY_STATS.datasetKey().equals(datasetKey)) {
            return CANVAS_DAILY_STATS;
        }
        throw new IllegalArgumentException("Unknown BI dataset: " + datasetKey);
    }

    public record BiQueryDataset(
            String datasetKey,
            List<BiQueryField> fields,
            List<BiQueryMetric> metrics) {

        public BiQueryDataset {
            fields = fields.stream()
                    .sorted(Comparator.comparing(BiQueryField::fieldKey))
                    .toList();
            metrics = metrics.stream()
                    .sorted(Comparator.comparing(BiQueryMetric::metricKey))
                    .toList();
        }
    }

    public record BiQueryField(
            String fieldKey,
            String role,
            String dataType) {
    }

    public record BiQueryMetric(
            String metricKey,
            String dataType) {
    }
}
