package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.chovy.canvas.cdp.api.CdpWarehouseMetricLineageFacade;
import org.springframework.stereotype.Service;

@Service
public class CdpWarehouseMetricLineageApplicationService implements CdpWarehouseMetricLineageFacade {

    private static final String EXPRESSION_FIELD = "EXPRESSION_FIELD";
    private static final Map<String, MetricSpec> BUILT_IN_METRICS = Map.of(
            key("canvas_daily_stats", "success_rate"),
            new MetricSpec("SUM(success_count)", "PERCENT", List.of("stat_date"), List.of("success_count")));

    @Override
    public MetricImpactView impact(Long tenantId, String datasetKey, String metricKey) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        String scopedDatasetKey = required(datasetKey, "datasetKey");
        String scopedMetricKey = required(metricKey, "metricKey");
        MetricSpec metric = BUILT_IN_METRICS.getOrDefault(key(scopedDatasetKey, scopedMetricKey),
                new MetricSpec(scopedMetricKey, "DECIMAL", List.of(), List.of()));
        return new MetricImpactView(
                scopedTenantId,
                scopedDatasetKey,
                scopedMetricKey,
                metric.expression(),
                metric.valueType(),
                metric.allowedDimensions(),
                fieldDependencies(metric),
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of());
    }

    private static List<FieldDependencyView> fieldDependencies(MetricSpec metric) {
        return metric.fields().stream()
                .filter(field -> referencesField(metric.expression(), field))
                .map(field -> new FieldDependencyView(field, EXPRESSION_FIELD))
                .toList();
    }

    private static boolean referencesField(String expression, String field) {
        return Pattern.compile("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(field) + "(?![A-Za-z0-9_])")
                .matcher(expression)
                .find();
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String key(String datasetKey, String metricKey) {
        return datasetKey + ":" + metricKey;
    }

    private record MetricSpec(
            String expression,
            String valueType,
            List<String> allowedDimensions,
            List<String> fields) {
    }
}
