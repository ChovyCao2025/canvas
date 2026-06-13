package org.chovy.canvas.bi.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BiDashboardReadinessPolicy {

    public BiDashboardReadinessReport evaluate(BiDashboard dashboard, List<BiChart> charts, List<BiDataset> datasets) {
        if (dashboard == null) {
            throw new IllegalArgumentException("dashboard is required");
        }
        Map<String, BiChart> chartsByKey = safeCharts(charts).stream()
                .collect(Collectors.toMap(chart -> chart.chartKey().value(), Function.identity(), (left, right) -> left));
        Map<Long, BiDataset> datasetsById = safeDatasets(datasets).stream()
                .collect(Collectors.toMap(BiDataset::id, Function.identity(), (left, right) -> left));
        List<BiDashboardReadinessIssue> blockers = new ArrayList<>();
        int publishedChartCount = 0;
        int draftDatasetCount = 0;
        for (String chartKey : dashboard.chartKeys()) {
            BiChart chart = chartsByKey.get(BiResourceKey.of(chartKey, "chartKey").value());
            if (chart == null) {
                blockers.add(issue("BLOCKER", "MISSING_CHART", "CHART", chartKey,
                        "dashboard references a chart that does not exist"));
                continue;
            }
            if (chart.status().published()) {
                publishedChartCount += 1;
            } else {
                blockers.add(issue("BLOCKER", "CHART_NOT_PUBLISHED", "CHART", chart.chartKey().value(),
                        "dashboard chart is not published"));
            }
            BiDataset dataset = datasetsById.get(chart.datasetId());
            if (dataset == null || !dataset.status().published()) {
                draftDatasetCount += 1;
                blockers.add(issue("BLOCKER", "DATASET_NOT_PUBLISHED", "DATASET", chart.datasetKey().value(),
                        "chart dataset is missing or not published"));
            }
        }
        boolean ready = blockers.isEmpty();
        return new BiDashboardReadinessReport(
                ready ? "READY" : "BLOCKED",
                ready,
                publishedChartCount,
                draftDatasetCount,
                blockers,
                List.of());
    }

    private static List<BiChart> safeCharts(List<BiChart> charts) {
        return charts == null ? List.of() : charts;
    }

    private static List<BiDataset> safeDatasets(List<BiDataset> datasets) {
        return datasets == null ? List.of() : datasets;
    }

    private static BiDashboardReadinessIssue issue(String severity,
                                                   String code,
                                                   String itemType,
                                                   String itemKey,
                                                   String message) {
        return new BiDashboardReadinessIssue(severity, code, itemType, itemKey, message);
    }
}
