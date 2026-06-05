package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.chart.BiChartResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardWidget;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdpWarehouseMetricLineageServiceTest {

    @Test
    void impactIncludesMetricDependenciesLineageChartsAndDashboards() {
        CdpWarehouseCatalogService catalogService = mock(CdpWarehouseCatalogService.class);
        BiChartResourceService chartService = mock(BiChartResourceService.class);
        BiDashboardResourceService dashboardService = mock(BiDashboardResourceService.class);
        when(catalogService.lineage(9L, "canvas_daily_stats", CdpWarehouseCatalogService.Direction.BOTH))
                .thenReturn(lineage());
        when(catalogService.transitiveLineage(
                9L,
                "canvas_daily_stats",
                CdpWarehouseCatalogService.Direction.BOTH,
                null)).thenReturn(transitiveLineage(List.of()));
        when(chartService.list(9L)).thenReturn(List.of(
                chart("success-chart", "canvas_daily_stats", List.of("success_rate")),
                chart("other-chart", "canvas_daily_stats", List.of("total_executions"))));
        when(dashboardService.list(9L)).thenReturn(List.of(
                dashboard("ops-dashboard", "canvas_daily_stats", List.of(
                        widget("success-widget", List.of("success_rate")),
                        widget("other-widget", List.of("total_executions")))),
                dashboard("other-dashboard", "other_dataset", List.of(widget("ignored", List.of("success_rate"))))));
        CdpWarehouseMetricLineageService service = new CdpWarehouseMetricLineageService(
                resolver(), catalogService, chartService, dashboardService);

        CdpWarehouseMetricLineageService.MetricImpactView impact =
                service.impact(9L, "canvas_daily_stats", "success_rate");

        assertThat(impact.tenantId()).isEqualTo(9L);
        assertThat(impact.expression()).contains("success_count");
        assertThat(impact.allowedDimensions()).containsExactly("stat_date", "canvas_id");
        assertThat(impact.fieldDependencies()).extracting(CdpWarehouseMetricLineageService.FieldDependencyView::fieldKey)
                .contains("success_count", "total_executions", "stat_date", "canvas_id");
        assertThat(impact.lineageNodes()).hasSize(2);
        assertThat(impact.lineageEdges()).hasSize(1);
        assertThat(impact.transitiveLineage()).isNotNull();
        assertThat(impact.transitiveLineage().nodes()).extracting(node -> node.dataset().datasetKey())
                .contains("canvas_daily_stats", "canvas_execution_trace", "marketing_metric_export");
        assertThat(impact.transitiveLineage().paths()).hasSize(2);
        assertThat(impact.charts()).singleElement()
                .extracting(CdpWarehouseMetricLineageService.ChartImpactView::chartKey)
                .isEqualTo("success-chart");
        assertThat(impact.dashboards()).singleElement().satisfies(dashboard -> {
            assertThat(dashboard.dashboardKey()).isEqualTo("ops-dashboard");
            assertThat(dashboard.widgetKeys()).containsExactly("success-widget");
        });
        assertThat(impact.warnings()).isEmpty();
    }

    @Test
    void impactIncludesTransitiveLineageWarnings() {
        CdpWarehouseCatalogService catalogService = mock(CdpWarehouseCatalogService.class);
        when(catalogService.lineage(9L, "canvas_daily_stats", CdpWarehouseCatalogService.Direction.BOTH))
                .thenReturn(lineage());
        when(catalogService.transitiveLineage(
                9L,
                "canvas_daily_stats",
                CdpWarehouseCatalogService.Direction.BOTH,
                null)).thenReturn(transitiveLineage(List.of("lineage cycle detected: a -> b -> a")));
        CdpWarehouseMetricLineageService service =
                new CdpWarehouseMetricLineageService(resolver(), catalogService, null, null);

        CdpWarehouseMetricLineageService.MetricImpactView impact =
                service.impact(9L, "canvas_daily_stats", "success_rate");

        assertThat(impact.transitiveLineage()).isNotNull();
        assertThat(impact.warnings()).contains(
                "lineage cycle detected: a -> b -> a",
                "BI chart resource service is unavailable",
                "BI dashboard resource service is unavailable");
    }

    @Test
    void impactReturnsWarningsWhenOptionalSourcesAreUnavailable() {
        CdpWarehouseMetricLineageService service =
                new CdpWarehouseMetricLineageService(resolver(), null, null, null);

        CdpWarehouseMetricLineageService.MetricImpactView impact =
                service.impact(9L, "canvas_daily_stats", "success_rate");

        assertThat(impact.fieldDependencies()).isNotEmpty();
        assertThat(impact.lineageNodes()).isEmpty();
        assertThat(impact.charts()).isEmpty();
        assertThat(impact.dashboards()).isEmpty();
        assertThat(impact.warnings()).contains(
                "warehouse catalog service is unavailable",
                "BI chart resource service is unavailable",
                "BI dashboard resource service is unavailable");
    }

    @Test
    void unknownMetricIsRejected() {
        CdpWarehouseMetricLineageService service =
                new CdpWarehouseMetricLineageService(resolver(), null, null, null);

        assertThatThrownBy(() -> service.impact(9L, "canvas_daily_stats", "missing_metric"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown metric");
    }

    private BiDatasetSpecResolver resolver() {
        return new BiDatasetSpecResolver() {
            @Override
            public BiDatasetSpec dataset(String datasetKey, Long tenantId) {
                return datasetSpec();
            }

            @Override
            public List<BiDatasetSpec> datasets(Long tenantId) {
                return List.of(datasetSpec());
            }
        };
    }

    private BiDatasetSpec datasetSpec() {
        return new BiDatasetSpec(
                "canvas_daily_stats",
                "canvas_dws.canvas_daily_stats",
                "tenant_id",
                Map.of(
                        "stat_date", new BiFieldSpec("stat_date", "stat_date", BiFieldSpec.Role.DIMENSION, "DATE"),
                        "canvas_id", new BiFieldSpec("canvas_id", "canvas_id", BiFieldSpec.Role.DIMENSION, "NUMBER"),
                        "total_executions", new BiFieldSpec(
                                "total_executions", "total_executions", BiFieldSpec.Role.MEASURE, "NUMBER"),
                        "success_count", new BiFieldSpec(
                                "success_count", "success_count", BiFieldSpec.Role.MEASURE, "NUMBER")),
                Map.of(
                        "success_rate", new BiMetricSpec(
                                "success_rate",
                                "CASE WHEN SUM(total_executions) > 0 THEN SUM(success_count) / SUM(total_executions) ELSE 0 END",
                                "PERCENT",
                                List.of("stat_date", "canvas_id")),
                        "total_executions", new BiMetricSpec(
                                "total_executions",
                                "SUM(total_executions)",
                                "NUMBER",
                                List.of("stat_date"))));
    }

    private CdpWarehouseCatalogService.LineageGraph lineage() {
        CdpWarehouseCatalogService.DatasetView source = dataset("canvas_execution_trace");
        CdpWarehouseCatalogService.DatasetView target = dataset("canvas_daily_stats");
        CdpWarehouseCatalogService.LineageEdgeView edge = new CdpWarehouseCatalogService.LineageEdgeView(
                1L,
                9L,
                "canvas_execution_trace",
                "canvas_daily_stats",
                "SQL",
                "aggregation",
                "HARD",
                "daily stats aggregation",
                true);
        return new CdpWarehouseCatalogService.LineageGraph(
                9L,
                "canvas_daily_stats",
                CdpWarehouseCatalogService.Direction.BOTH,
                List.of(source, target),
                List.of(edge));
    }

    private CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage(List<String> warnings) {
        CdpWarehouseCatalogService.DatasetView source = dataset("canvas_execution_trace");
        CdpWarehouseCatalogService.DatasetView target = dataset("canvas_daily_stats");
        CdpWarehouseCatalogService.DatasetView export = dataset("marketing_metric_export");
        CdpWarehouseCatalogService.LineageEdgeView upstream = new CdpWarehouseCatalogService.LineageEdgeView(
                1L,
                9L,
                "canvas_execution_trace",
                "canvas_daily_stats",
                "SQL",
                "aggregation",
                "HARD",
                "daily stats aggregation",
                true);
        CdpWarehouseCatalogService.LineageEdgeView downstream = new CdpWarehouseCatalogService.LineageEdgeView(
                2L,
                9L,
                "canvas_daily_stats",
                "marketing_metric_export",
                "EXPORT",
                "reverse-etl",
                "HARD",
                "activation export",
                true);
        return new CdpWarehouseCatalogService.TransitiveLineageGraph(
                9L,
                "canvas_daily_stats",
                CdpWarehouseCatalogService.Direction.BOTH,
                3,
                false,
                List.of(
                        new CdpWarehouseCatalogService.LineageNodeView(
                                target, 0, CdpWarehouseCatalogService.LineageRelation.SELF),
                        new CdpWarehouseCatalogService.LineageNodeView(
                                source, 1, CdpWarehouseCatalogService.LineageRelation.UPSTREAM),
                        new CdpWarehouseCatalogService.LineageNodeView(
                                export, 1, CdpWarehouseCatalogService.LineageRelation.DOWNSTREAM)),
                List.of(upstream, downstream),
                List.of(
                        new CdpWarehouseCatalogService.LineagePathView(
                                List.of("canvas_daily_stats", "canvas_execution_trace"),
                                1,
                                CdpWarehouseCatalogService.LineageRelation.UPSTREAM),
                        new CdpWarehouseCatalogService.LineagePathView(
                                List.of("canvas_daily_stats", "marketing_metric_export"),
                                1,
                                CdpWarehouseCatalogService.LineageRelation.DOWNSTREAM)),
                warnings);
    }

    private CdpWarehouseCatalogService.DatasetView dataset(String datasetKey) {
        return new CdpWarehouseCatalogService.DatasetView(
                1L,
                9L,
                datasetKey,
                "DWS",
                "canvas_dws." + datasetKey,
                datasetKey,
                "BI",
                "canvas-engine",
                "data-platform",
                datasetKey,
                60,
                "NORMAL",
                "ACTIVE",
                "{}");
    }

    private BiChartResource chart(String chartKey, String datasetKey, List<String> metrics) {
        return new BiChartResource(
                chartKey,
                chartKey,
                "LINE",
                datasetKey,
                new BiQueryRequest(datasetKey, List.of("stat_date"), metrics, List.of(), List.of(), 100),
                Map.of(),
                Map.of(),
                "PUBLISHED",
                "PERSISTED");
    }

    private BiDashboardResource dashboard(String dashboardKey,
                                          String datasetKey,
                                          List<BiDashboardWidget> widgets) {
        return new BiDashboardResource(
                new BiDashboardPreset(
                        dashboardKey,
                        dashboardKey,
                        "dashboard",
                        datasetKey,
                        widgets,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()),
                "PUBLISHED",
                3,
                "PERSISTED");
    }

    private BiDashboardWidget widget(String widgetKey, List<String> metrics) {
        return new BiDashboardWidget(
                widgetKey,
                widgetKey,
                "LINE",
                List.of("stat_date"),
                metrics,
                0,
                0,
                6,
                4,
                "default");
    }
}
