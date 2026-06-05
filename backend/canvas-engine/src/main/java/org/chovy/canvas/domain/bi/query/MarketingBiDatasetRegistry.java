package org.chovy.canvas.domain.bi.query;

import java.util.List;
import java.util.Map;

public final class MarketingBiDatasetRegistry {

    private static final BiDatasetSpec CANVAS_DAILY_STATS = new BiDatasetSpec(
            "canvas_daily_stats",
            "canvas_dws.canvas_daily_stats",
            "tenant_id",
            Map.ofEntries(
                    Map.entry("stat_date", new BiFieldSpec("stat_date", "stat_date", BiFieldSpec.Role.DIMENSION, "DATE")),
                    Map.entry("canvas_id", new BiFieldSpec("canvas_id", "canvas_id", BiFieldSpec.Role.DIMENSION, "NUMBER")),
                    Map.entry("canvas_name", new BiFieldSpec("canvas_name", "canvas_name", BiFieldSpec.Role.DIMENSION, "STRING")),
                    Map.entry("trigger_type", new BiFieldSpec("trigger_type", "trigger_type", BiFieldSpec.Role.DIMENSION, "STRING")),
                    Map.entry("total_executions", new BiFieldSpec("total_executions", "total_executions", BiFieldSpec.Role.MEASURE, "NUMBER")),
                    Map.entry("success_count", new BiFieldSpec("success_count", "success_count", BiFieldSpec.Role.MEASURE, "NUMBER")),
                    Map.entry("fail_count", new BiFieldSpec("fail_count", "fail_count", BiFieldSpec.Role.MEASURE, "NUMBER")),
                    Map.entry("running_count", new BiFieldSpec("running_count", "running_count", BiFieldSpec.Role.MEASURE, "NUMBER")),
                    Map.entry("unique_users", new BiFieldSpec("unique_users", "unique_users", BiFieldSpec.Role.MEASURE, "NUMBER")),
                    Map.entry("avg_duration_ms", new BiFieldSpec("avg_duration_ms", "avg_duration_ms", BiFieldSpec.Role.MEASURE, "NUMBER"))
            ),
            Map.of(
                    "total_executions", new BiMetricSpec(
                            "total_executions", "SUM(total_executions)", "NUMBER", commonDimensions()),
                    "success_count", new BiMetricSpec("success_count", "SUM(success_count)", "NUMBER", commonDimensions()),
                    "fail_count", new BiMetricSpec("fail_count", "SUM(fail_count)", "NUMBER", commonDimensions()),
                    "unique_users", new BiMetricSpec("unique_users", "SUM(unique_users)", "NUMBER", commonDimensions()),
                    "avg_duration_ms", new BiMetricSpec(
                            "avg_duration_ms",
                            "CASE WHEN SUM(total_executions) > 0 THEN SUM(total_duration_ms) / SUM(total_executions) ELSE 0 END",
                            "NUMBER",
                            commonDimensions()),
                    "success_rate", new BiMetricSpec(
                            "success_rate",
                            "CASE WHEN SUM(total_executions) > 0 THEN SUM(success_count) / SUM(total_executions) ELSE 0 END",
                            "PERCENT",
                            commonDimensions())
            )
    );

    private MarketingBiDatasetRegistry() {
    }

    private static List<String> commonDimensions() {
        return List.of("stat_date", "canvas_id", "canvas_name", "trigger_type");
    }

    public static List<BiDatasetSpec> datasets() {
        return List.of(CANVAS_DAILY_STATS);
    }

    public static BiDatasetSpec dataset(String datasetKey) {
        if (CANVAS_DAILY_STATS.datasetKey().equals(datasetKey)) {
            return CANVAS_DAILY_STATS;
        }
        throw new IllegalArgumentException("Unknown BI dataset: " + datasetKey);
    }
}
