package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiMetricDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseMetricChangeReviewDO;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiMetricMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseMetricChangeReviewMapper;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseMetricChangeReviewServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requestChangeCapturesImpactSnapshotAndRisk() {
        CdpWarehouseMetricChangeReviewMapper reviewMapper = mock(CdpWarehouseMetricChangeReviewMapper.class);
        when(reviewMapper.insert(any(CdpWarehouseMetricChangeReviewDO.class))).thenAnswer(invocation -> {
            CdpWarehouseMetricChangeReviewDO row = invocation.getArgument(0);
            row.setId(12L);
            return 1;
        });
        CdpWarehouseMetricChangeReviewService service = service(reviewMapper);

        CdpWarehouseMetricChangeReviewService.MetricChangeReviewView view = service.requestChange(
                9L,
                "operator-1",
                new CdpWarehouseMetricChangeReviewService.MetricChangeCommand(
                        "canvas_daily_stats",
                        "success_rate",
                        "SUM(success_count) / SUM(total_executions)",
                        List.of("stat_date"),
                        "align success denominator"));

        assertThat(view.id()).isEqualTo(12L);
        assertThat(view.status()).isEqualTo(CdpWarehouseMetricChangeReviewService.STATUS_PENDING_REVIEW);
        assertThat(view.riskLevel()).isEqualTo("MEDIUM");
        assertThat(view.currentMetric().allowedDimensions()).containsExactly("stat_date", "canvas_id");
        assertThat(view.proposedMetric().allowedDimensions()).containsExactly("stat_date");
        assertThat(view.impact().fieldDependencyCount()).isGreaterThan(0);
        assertThat(view.impact().warnings()).contains(
                "warehouse catalog service is unavailable",
                "BI chart resource service is unavailable",
                "BI dashboard resource service is unavailable");

        ArgumentCaptor<CdpWarehouseMetricChangeReviewDO> row =
                ArgumentCaptor.forClass(CdpWarehouseMetricChangeReviewDO.class);
        verify(reviewMapper).insert(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getRequestedBy()).isEqualTo("operator-1");
        assertThat(row.getValue().getCurrentSnapshotJson()).contains("success_rate");
        assertThat(row.getValue().getProposedSnapshotJson()).contains("SUM(success_count)");
    }

    @Test
    void requestChangeStoresTransitiveImpactSummaryAndRaisesRiskForDownstreamImpact() {
        CdpWarehouseMetricChangeReviewMapper reviewMapper = mock(CdpWarehouseMetricChangeReviewMapper.class);
        when(reviewMapper.insert(any(CdpWarehouseMetricChangeReviewDO.class))).thenAnswer(invocation -> {
            CdpWarehouseMetricChangeReviewDO row = invocation.getArgument(0);
            row.setId(13L);
            return 1;
        });
        CdpWarehouseMetricLineageService lineageService = mock(CdpWarehouseMetricLineageService.class);
        when(lineageService.impact(9L, "canvas_daily_stats", "success_rate"))
                .thenReturn(metricImpactWithTransitiveDownstream());
        CdpWarehouseMetricChangeReviewService service =
                service(reviewMapper, mock(BiDatasetMapper.class), mock(BiMetricMapper.class), lineageService);

        CdpWarehouseMetricChangeReviewService.MetricChangeReviewView view = service.requestChange(
                9L,
                "operator-1",
                new CdpWarehouseMetricChangeReviewService.MetricChangeCommand(
                        "canvas_daily_stats",
                        "success_rate",
                        "SUM(success_count) / SUM(total_executions)",
                        List.of("stat_date"),
                        "align success denominator"));

        assertThat(view.riskLevel()).isEqualTo("HIGH");
        assertThat(view.impact().transitiveLineageNodeCount()).isEqualTo(2);
        assertThat(view.impact().transitiveLineageEdgeCount()).isEqualTo(1);
        assertThat(view.impact().transitivePathCount()).isEqualTo(1);
        assertThat(view.impact().transitiveDownstreamNodeCount()).isEqualTo(1);
        assertThat(view.impact().transitiveTruncated()).isFalse();
    }

    @Test
    void requestChangeRejectsMeasureAsAllowedDimension() {
        CdpWarehouseMetricChangeReviewMapper reviewMapper = mock(CdpWarehouseMetricChangeReviewMapper.class);
        CdpWarehouseMetricChangeReviewService service = service(reviewMapper);

        assertThatThrownBy(() -> service.requestChange(9L, "operator-1",
                new CdpWarehouseMetricChangeReviewService.MetricChangeCommand(
                        "canvas_daily_stats",
                        "success_rate",
                        "SUM(success_count)",
                        List.of("success_count"),
                        "bad dimension")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a dimension field");
        verify(reviewMapper, never()).insert(any(CdpWarehouseMetricChangeReviewDO.class));
    }

    @Test
    void approveAndRejectRequirePendingReview() {
        CdpWarehouseMetricChangeReviewMapper reviewMapper = mock(CdpWarehouseMetricChangeReviewMapper.class);
        CdpWarehouseMetricChangeReviewDO row = reviewRow(CdpWarehouseMetricChangeReviewService.STATUS_PENDING_REVIEW);
        when(reviewMapper.selectOne(any())).thenReturn(row);
        CdpWarehouseMetricChangeReviewService service = service(reviewMapper);

        CdpWarehouseMetricChangeReviewService.MetricChangeReviewView approved =
                service.approve(9L, 55L, "reviewer-1", "impact reviewed");

        assertThat(approved.status()).isEqualTo(CdpWarehouseMetricChangeReviewService.STATUS_APPROVED);
        assertThat(approved.reviewedBy()).isEqualTo("reviewer-1");
        verify(reviewMapper).updateById(any(CdpWarehouseMetricChangeReviewDO.class));

        row.setStatus(CdpWarehouseMetricChangeReviewService.STATUS_APPROVED);
        assertThatThrownBy(() -> service.reject(9L, 55L, "reviewer-1", "reject"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only pending");
    }

    @Test
    void applyApprovedReviewUpdatesPersistedMetricContract() {
        CdpWarehouseMetricChangeReviewMapper reviewMapper = mock(CdpWarehouseMetricChangeReviewMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        CdpWarehouseMetricChangeReviewDO row = reviewRow(CdpWarehouseMetricChangeReviewService.STATUS_APPROVED);
        when(reviewMapper.selectOne(any())).thenReturn(row);
        when(datasetMapper.selectOne(any())).thenReturn(datasetRow());
        when(metricMapper.selectOne(any())).thenReturn(metricRow());
        when(metricMapper.updateMetricContract(eq(9L), eq(77L), eq("success_rate"),
                eq("SUM(success_count) / SUM(total_executions)"), any()))
                .thenReturn(1);
        CdpWarehouseMetricChangeReviewService service =
                service(reviewMapper, datasetMapper, metricMapper);

        CdpWarehouseMetricChangeReviewService.MetricChangeReviewView applied =
                service.apply(9L, 55L);

        assertThat(applied.status()).isEqualTo(CdpWarehouseMetricChangeReviewService.STATUS_APPLIED);
        assertThat(applied.appliedAt()).isNotNull();
        verify(metricMapper).updateMetricContract(eq(9L), eq(77L), eq("success_rate"),
                eq("SUM(success_count) / SUM(total_executions)"),
                org.mockito.ArgumentMatchers.contains("stat_date"));
        verify(reviewMapper).updateById(any(CdpWarehouseMetricChangeReviewDO.class));
    }

    @Test
    void applyFailsWhenMetricChangedAfterReview() {
        CdpWarehouseMetricChangeReviewMapper reviewMapper = mock(CdpWarehouseMetricChangeReviewMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        when(reviewMapper.selectOne(any()))
                .thenReturn(reviewRow(CdpWarehouseMetricChangeReviewService.STATUS_APPROVED));
        when(datasetMapper.selectOne(any())).thenReturn(datasetRow());
        BiMetricDO changed = metricRow();
        changed.setExpression("SUM(other_count)");
        when(metricMapper.selectOne(any())).thenReturn(changed);
        CdpWarehouseMetricChangeReviewService service =
                service(reviewMapper, datasetMapper, metricMapper);

        assertThatThrownBy(() -> service.apply(9L, 55L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("changed since review");
        verify(metricMapper, never()).updateMetricContract(any(), any(), any(), any(), any());
    }

    private CdpWarehouseMetricChangeReviewService service(CdpWarehouseMetricChangeReviewMapper reviewMapper) {
        return service(reviewMapper, mock(BiDatasetMapper.class), mock(BiMetricMapper.class));
    }

    private CdpWarehouseMetricChangeReviewService service(CdpWarehouseMetricChangeReviewMapper reviewMapper,
                                                          BiDatasetMapper datasetMapper,
                                                          BiMetricMapper metricMapper) {
        BiDatasetSpecResolver resolver = resolver();
        CdpWarehouseMetricLineageService lineageService =
                new CdpWarehouseMetricLineageService(resolver, null, null, null);
        return service(reviewMapper, datasetMapper, metricMapper, lineageService);
    }

    private CdpWarehouseMetricChangeReviewService service(CdpWarehouseMetricChangeReviewMapper reviewMapper,
                                                          BiDatasetMapper datasetMapper,
                                                          BiMetricMapper metricMapper,
                                                          CdpWarehouseMetricLineageService lineageService) {
        BiDatasetSpecResolver resolver = resolver();
        return new CdpWarehouseMetricChangeReviewService(
                reviewMapper,
                datasetMapper,
                metricMapper,
                resolver,
                lineageService,
                objectMapper);
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
                        "success_count", new BiFieldSpec(
                                "success_count", "success_count", BiFieldSpec.Role.MEASURE, "NUMBER"),
                        "total_executions", new BiFieldSpec(
                                "total_executions", "total_executions", BiFieldSpec.Role.MEASURE, "NUMBER")),
                Map.of("success_rate", new BiMetricSpec(
                        "success_rate",
                        "SUM(success_count)",
                        "PERCENT",
                        List.of("stat_date", "canvas_id"))));
    }

    private CdpWarehouseMetricChangeReviewDO reviewRow(String status) {
        CdpWarehouseMetricChangeReviewDO row = new CdpWarehouseMetricChangeReviewDO();
        row.setId(55L);
        row.setTenantId(9L);
        row.setDatasetKey("canvas_daily_stats");
        row.setMetricKey("success_rate");
        row.setChangeType("METRIC_CONTRACT");
        row.setStatus(status);
        row.setRiskLevel("MEDIUM");
        row.setRequestedBy("operator-1");
        row.setRequestReason("align denominator");
        row.setCurrentSnapshotJson(json(new CdpWarehouseMetricChangeReviewService.MetricSnapshot(
                "success_rate", "SUM(success_count)", "PERCENT", List.of("stat_date", "canvas_id"))));
        row.setProposedSnapshotJson(json(new CdpWarehouseMetricChangeReviewService.MetricSnapshot(
                "success_rate",
                "SUM(success_count) / SUM(total_executions)",
                "PERCENT",
                List.of("stat_date"))));
        row.setImpactSummaryJson(json(new CdpWarehouseMetricChangeReviewService.ImpactSummary(
                1, 0, 0, 0, 0, List.of())));
        return row;
    }

    private BiDatasetDO datasetRow() {
        BiDatasetDO row = new BiDatasetDO();
        row.setId(77L);
        row.setTenantId(9L);
        row.setDatasetKey("canvas_daily_stats");
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiMetricDO metricRow() {
        BiMetricDO row = new BiMetricDO();
        row.setId(88L);
        row.setTenantId(9L);
        row.setDatasetId(77L);
        row.setMetricKey("success_rate");
        row.setExpression("SUM(success_count)");
        row.setAllowedDimensionsJson("[\"stat_date\",\"canvas_id\"]");
        row.setStatus("ACTIVE");
        return row;
    }

    private CdpWarehouseMetricLineageService.MetricImpactView metricImpactWithTransitiveDownstream() {
        CdpWarehouseCatalogService.DatasetView dataset = datasetView("canvas_daily_stats");
        CdpWarehouseCatalogService.DatasetView export = datasetView("marketing_metric_export");
        CdpWarehouseCatalogService.LineageEdgeView edge = new CdpWarehouseCatalogService.LineageEdgeView(
                1L,
                9L,
                "canvas_daily_stats",
                "marketing_metric_export",
                "EXPORT",
                "reverse-etl",
                "HARD",
                "activation export",
                true);
        CdpWarehouseCatalogService.TransitiveLineageGraph transitiveLineage =
                new CdpWarehouseCatalogService.TransitiveLineageGraph(
                        9L,
                        "canvas_daily_stats",
                        CdpWarehouseCatalogService.Direction.BOTH,
                        3,
                        false,
                        List.of(
                                new CdpWarehouseCatalogService.LineageNodeView(
                                        dataset, 0, CdpWarehouseCatalogService.LineageRelation.SELF),
                                new CdpWarehouseCatalogService.LineageNodeView(
                                        export, 1, CdpWarehouseCatalogService.LineageRelation.DOWNSTREAM)),
                        List.of(edge),
                        List.of(new CdpWarehouseCatalogService.LineagePathView(
                                List.of("canvas_daily_stats", "marketing_metric_export"),
                                1,
                                CdpWarehouseCatalogService.LineageRelation.DOWNSTREAM)),
                        List.of());
        return new CdpWarehouseMetricLineageService.MetricImpactView(
                9L,
                "canvas_daily_stats",
                "success_rate",
                "SUM(success_count)",
                "PERCENT",
                List.of("stat_date", "canvas_id"),
                List.of(new CdpWarehouseMetricLineageService.FieldDependencyView(
                        "success_count",
                        "EXPRESSION_FIELD")),
                List.of(dataset),
                List.of(),
                transitiveLineage,
                List.of(),
                List.of(),
                List.of());
    }

    private CdpWarehouseCatalogService.DatasetView datasetView(String datasetKey) {
        return new CdpWarehouseCatalogService.DatasetView(
                1L,
                9L,
                datasetKey,
                "DWS",
                "canvas_dws." + datasetKey,
                datasetKey,
                "CDP",
                "canvas-engine",
                "ops",
                datasetKey,
                60,
                "NORMAL",
                "ACTIVE",
                "{}");
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
