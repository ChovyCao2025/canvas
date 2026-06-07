package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.chart.BiChartResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardWidget;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class CdpWarehouseMetricLineageService {

    private static final String DEPENDENCY_EXPRESSION_FIELD = "EXPRESSION_FIELD";
    private static final String DEPENDENCY_ALLOWED_DIMENSION = "ALLOWED_DIMENSION";

    private final BiDatasetSpecResolver datasetSpecResolver;
    private final CdpWarehouseCatalogService catalogService;
    private final BiChartResourceService chartResourceService;
    private final BiDashboardResourceService dashboardResourceService;

    @Autowired
    public CdpWarehouseMetricLineageService(
            ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider,
            ObjectProvider<CdpWarehouseCatalogService> catalogServiceProvider,
            ObjectProvider<BiChartResourceService> chartResourceServiceProvider,
            ObjectProvider<BiDashboardResourceService> dashboardResourceServiceProvider) {
        this(
                datasetSpecResolverProvider == null
                        ? BiDatasetSpecResolver.builtIn()
                        : datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                catalogServiceProvider == null ? null : catalogServiceProvider.getIfAvailable(),
                chartResourceServiceProvider == null ? null : chartResourceServiceProvider.getIfAvailable(),
                dashboardResourceServiceProvider == null ? null : dashboardResourceServiceProvider.getIfAvailable());
    }

    CdpWarehouseMetricLineageService(BiDatasetSpecResolver datasetSpecResolver,
                                     CdpWarehouseCatalogService catalogService,
                                     BiChartResourceService chartResourceService,
                                     BiDashboardResourceService dashboardResourceService) {
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
        this.catalogService = catalogService;
        this.chartResourceService = chartResourceService;
        this.dashboardResourceService = dashboardResourceService;
    }

    public MetricImpactView impact(Long tenantId, String datasetKey, String metricKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDatasetKey = required(datasetKey, "datasetKey");
        String scopedMetricKey = required(metricKey, "metricKey");
        BiDatasetSpec dataset = datasetSpecResolver.dataset(scopedDatasetKey, scopedTenantId);
        BiMetricSpec metric = dataset.metrics().get(scopedMetricKey);
        if (metric == null) {
            throw new IllegalArgumentException("Unknown metric: " + scopedMetricKey);
        }

        List<String> warnings = new ArrayList<>();
        CdpWarehouseCatalogService.LineageGraph lineage = lineage(scopedTenantId, scopedDatasetKey, warnings);
        CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage =
                transitiveLineage(scopedTenantId, scopedDatasetKey, warnings);
        return new MetricImpactView(
                scopedTenantId,
                scopedDatasetKey,
                scopedMetricKey,
                metric.expression(),
                metric.valueType(),
                metric.allowedDimensions(),
                fieldDependencies(dataset, metric),
                lineage == null ? List.of() : lineage.nodes(),
                lineage == null ? List.of() : lineage.edges(),
                transitiveLineage,
                impactedCharts(scopedTenantId, scopedDatasetKey, scopedMetricKey, warnings),
                impactedDashboards(scopedTenantId, scopedDatasetKey, scopedMetricKey, warnings),
                warnings);
    }

    private List<FieldDependencyView> fieldDependencies(BiDatasetSpec dataset, BiMetricSpec metric) {
        Map<String, FieldDependencyView> result = new LinkedHashMap<>();
        for (String fieldKey : dataset.fields().keySet()) {
            if (referencesField(metric.expression(), fieldKey)) {
                result.put(fieldKey + ":" + DEPENDENCY_EXPRESSION_FIELD,
                        new FieldDependencyView(fieldKey, DEPENDENCY_EXPRESSION_FIELD));
            }
        }
        for (String dimension : metric.allowedDimensions()) {
            result.put(dimension + ":" + DEPENDENCY_ALLOWED_DIMENSION,
                    new FieldDependencyView(dimension, DEPENDENCY_ALLOWED_DIMENSION));
        }
        return List.copyOf(result.values());
    }

    private CdpWarehouseCatalogService.LineageGraph lineage(Long tenantId,
                                                            String datasetKey,
                                                            List<String> warnings) {
        if (catalogService == null) {
            addWarning(warnings, "warehouse catalog service is unavailable");
            return null;
        }
        try {
            return catalogService.lineage(tenantId, datasetKey, CdpWarehouseCatalogService.Direction.BOTH);
        } catch (RuntimeException e) {
            addWarning(warnings, "warehouse catalog lineage unavailable: " + e.getMessage());
            return null;
        }
    }

    private CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage(Long tenantId,
                                                                                String datasetKey,
                                                                                List<String> warnings) {
        if (catalogService == null) {
            addWarning(warnings, "warehouse catalog service is unavailable");
            return null;
        }
        try {
            CdpWarehouseCatalogService.TransitiveLineageGraph graph =
                    catalogService.transitiveLineage(tenantId, datasetKey, CdpWarehouseCatalogService.Direction.BOTH, null);
            if (graph == null) {
                return null;
            }
            for (String warning : graph.warnings()) {
                addWarning(warnings, warning);
            }
            return graph;
        } catch (RuntimeException e) {
            addWarning(warnings, "warehouse catalog transitive lineage unavailable: " + e.getMessage());
            return null;
        }
    }

    private List<ChartImpactView> impactedCharts(Long tenantId,
                                                 String datasetKey,
                                                 String metricKey,
                                                 List<String> warnings) {
        if (chartResourceService == null) {
            warnings.add("BI chart resource service is unavailable");
            return List.of();
        }
        try {
            return chartResourceService.list(tenantId).stream()
                    .filter(chart -> chart != null
                            && datasetKey.equals(chart.datasetKey())
                            && chart.query() != null
                            && chart.query().metrics().contains(metricKey))
                    .map(chart -> new ChartImpactView(
                            chart.chartKey(),
                            chart.name(),
                            chart.chartType(),
                            chart.status(),
                            chart.query().dimensions()))
                    .toList();
        } catch (RuntimeException e) {
            warnings.add("BI chart impact unavailable: " + e.getMessage());
            return List.of();
        }
    }

    private List<DashboardImpactView> impactedDashboards(Long tenantId,
                                                         String datasetKey,
                                                         String metricKey,
                                                         List<String> warnings) {
        if (dashboardResourceService == null) {
            warnings.add("BI dashboard resource service is unavailable");
            return List.of();
        }
        try {
            List<DashboardImpactView> result = new ArrayList<>();
            for (BiDashboardResource dashboard : dashboardResourceService.list(tenantId)) {
                if (dashboard == null || dashboard.preset() == null
                        || !datasetKey.equals(dashboard.preset().datasetKey())) {
                    continue;
                }
                List<String> widgetKeys = dashboard.preset().widgets().stream()
                        .filter(widget -> widgetUsesMetric(widget, metricKey))
                        .map(BiDashboardWidget::widgetKey)
                        .toList();
                if (!widgetKeys.isEmpty()) {
                    result.add(new DashboardImpactView(
                            dashboard.preset().dashboardKey(),
                            dashboard.preset().title(),
                            dashboard.status(),
                            dashboard.version(),
                            widgetKeys));
                }
            }
            return result;
        } catch (RuntimeException e) {
            warnings.add("BI dashboard impact unavailable: " + e.getMessage());
            return List.of();
        }
    }

    private boolean widgetUsesMetric(BiDashboardWidget widget, String metricKey) {
        return widget != null && widget.metrics() != null && widget.metrics().contains(metricKey);
    }

    private boolean referencesField(String expression, String fieldKey) {
        if (!hasText(expression) || !hasText(fieldKey)) {
            return false;
        }
        return Pattern.compile("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(fieldKey) + "(?![A-Za-z0-9_])")
                .matcher(expression)
                .find();
    }

    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void addWarning(List<String> warnings, String warning) {
        if (hasText(warning) && !warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

    public record MetricImpactView(
            Long tenantId,
            String datasetKey,
            String metricKey,
            String expression,
            String valueType,
            List<String> allowedDimensions,
            List<FieldDependencyView> fieldDependencies,
            List<CdpWarehouseCatalogService.DatasetView> lineageNodes,
            List<CdpWarehouseCatalogService.LineageEdgeView> lineageEdges,
            CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage,
            List<ChartImpactView> charts,
            List<DashboardImpactView> dashboards,
            List<String> warnings) {
        public MetricImpactView(Long tenantId,
                                String datasetKey,
                                String metricKey,
                                String expression,
                                String valueType,
                                List<String> allowedDimensions,
                                List<FieldDependencyView> fieldDependencies,
                                List<CdpWarehouseCatalogService.DatasetView> lineageNodes,
                                List<CdpWarehouseCatalogService.LineageEdgeView> lineageEdges,
                                List<ChartImpactView> charts,
                                List<DashboardImpactView> dashboards,
                                List<String> warnings) {
            this(tenantId, datasetKey, metricKey, expression, valueType, allowedDimensions,
                    fieldDependencies, lineageNodes, lineageEdges, null, charts, dashboards, warnings);
        }

        public MetricImpactView {
            allowedDimensions = allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions);
            fieldDependencies = fieldDependencies == null ? List.of() : List.copyOf(fieldDependencies);
            lineageNodes = lineageNodes == null ? List.of() : List.copyOf(lineageNodes);
            lineageEdges = lineageEdges == null ? List.of() : List.copyOf(lineageEdges);
            charts = charts == null ? List.of() : List.copyOf(charts);
            dashboards = dashboards == null ? List.of() : List.copyOf(dashboards);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    public record FieldDependencyView(String fieldKey, String dependencyType) {
    }

    public record ChartImpactView(
            String chartKey,
            String name,
            String chartType,
            String status,
            List<String> dimensions) {
        public ChartImpactView {
            dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        }
    }

    public record DashboardImpactView(
            String dashboardKey,
            String title,
            String status,
            int version,
            List<String> widgetKeys) {
        public DashboardImpactView {
            widgetKeys = widgetKeys == null ? List.of() : List.copyOf(widgetKeys);
        }
    }
}
