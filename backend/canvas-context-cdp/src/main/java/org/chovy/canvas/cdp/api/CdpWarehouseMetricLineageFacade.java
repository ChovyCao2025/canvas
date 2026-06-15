package org.chovy.canvas.cdp.api;

import java.util.List;

public interface CdpWarehouseMetricLineageFacade {

    MetricImpactView impact(Long tenantId, String datasetKey, String metricKey);

    record MetricImpactView(
            Long tenantId,
            String datasetKey,
            String metricKey,
            String expression,
            String valueType,
            List<String> allowedDimensions,
            List<FieldDependencyView> fieldDependencies,
            List<Object> lineageNodes,
            List<Object> lineageEdges,
            Object transitiveLineage,
            List<ChartImpactView> charts,
            List<DashboardImpactView> dashboards,
            List<String> warnings) {
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

    record FieldDependencyView(String fieldKey, String dependencyType) {
    }

    record ChartImpactView(
            String chartKey,
            String name,
            String chartType,
            String status,
            List<String> dimensions) {
        public ChartImpactView {
            dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        }
    }

    record DashboardImpactView(
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
