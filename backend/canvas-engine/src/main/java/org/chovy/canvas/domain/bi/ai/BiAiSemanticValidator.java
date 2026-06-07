package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardWidget;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryCompiler;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

import java.util.List;

final class BiAiSemanticValidator {

    private final BiDatasetSpecResolver datasetSpecResolver;
    private final BiQueryCompiler compiler = new BiQueryCompiler();

    BiAiSemanticValidator(BiDatasetSpecResolver datasetSpecResolver) {
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
    }

    List<BiDatasetSpec> catalog(String datasetKey, Long tenantId) {
        String scopedDatasetKey = trimToNull(datasetKey);
        if (scopedDatasetKey == null) {
            return datasetSpecResolver.datasets(tenantId);
        }
        return List.of(datasetSpecResolver.dataset(scopedDatasetKey, tenantId));
    }

    BiDatasetSpec validateQuery(BiQueryRequest query, Long tenantId) {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        BiDatasetSpec dataset = datasetSpecResolver.dataset(query.datasetKey(), tenantId);
        compiler.compile(dataset, query, tenantId);
        return dataset;
    }

    void validateResultDataset(BiQueryResult result, String datasetKey, String fieldName) {
        if (result == null) {
            return;
        }
        if (!result.datasetKey().equals(datasetKey)) {
            throw new IllegalArgumentException(fieldName + " dataset does not match query dataset");
        }
    }

    void validateDashboardDraft(BiDashboardDraftPlan plan, Long tenantId) {
        if (plan == null || plan.dashboard() == null) {
            throw new IllegalArgumentException("dashboard draft is required");
        }
        BiDashboardPreset dashboard = plan.dashboard();
        String datasetKey = required(dashboard.datasetKey(), "dashboard datasetKey");
        datasetSpecResolver.dataset(datasetKey, tenantId);
        for (BiDashboardWidget widget : dashboard.widgets()) {
            validateWidget(datasetKey, widget, tenantId);
        }
        for (BiChartResource chart : plan.charts()) {
            if (!datasetKey.equals(chart.datasetKey())) {
                throw new IllegalArgumentException("chart dataset does not match dashboard dataset");
            }
            validateQuery(chart.query(), tenantId);
        }
    }

    private void validateWidget(String datasetKey, BiDashboardWidget widget, Long tenantId) {
        if (widget == null) {
            throw new IllegalArgumentException("dashboard widget is required");
        }
        validateQuery(new BiQueryRequest(
                datasetKey,
                widget.dimensions(),
                widget.metrics(),
                List.of(),
                List.of(),
                100), tenantId);
    }

    private String required(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
