package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseDatasetCatalogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseLineageEdgeDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseDatasetCatalogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseLineageEdgeMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseCatalogServiceTest {

    @Test
    void upsertDatasetNormalizesAndPersistsTenantDataset() {
        CdpWarehouseDatasetCatalogMapper datasetMapper = mock(CdpWarehouseDatasetCatalogMapper.class);
        CdpWarehouseCatalogService service = service(datasetMapper, mock(CdpWarehouseLineageEdgeMapper.class));

        CdpWarehouseCatalogService.DatasetView result = service.upsertDataset(9L,
                new CdpWarehouseCatalogService.DatasetCommand(
                        "tenant_user_wide", "dws", "canvas_dws.tenant_user_wide", "Tenant User Wide",
                        "audience", "canvas-engine", "ops", "wide table", 30,
                        "pii_related", null, "{\"fields\":[]}"));

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.layer()).isEqualTo("DWS");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.piiLevel()).isEqualTo("PII_RELATED");
        ArgumentCaptor<CdpWarehouseDatasetCatalogDO> row =
                ArgumentCaptor.forClass(CdpWarehouseDatasetCatalogDO.class);
        verify(datasetMapper).upsert(row.capture());
        assertThat(row.getValue().getDatasetKey()).isEqualTo("tenant_user_wide");
        assertThat(row.getValue().getPhysicalName()).isEqualTo("canvas_dws.tenant_user_wide");
    }

    @Test
    void upsertDatasetRejectsMissingRequiredFields() {
        CdpWarehouseCatalogService service = service(mock(CdpWarehouseDatasetCatalogMapper.class),
                mock(CdpWarehouseLineageEdgeMapper.class));

        assertThatThrownBy(() -> service.upsertDataset(9L,
                new CdpWarehouseCatalogService.DatasetCommand(
                        "", "DWS", "canvas_dws.table", null, null, null, null,
                        null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datasetKey is required");
    }

    @Test
    void listDatasetsIncludesBuiltInAndTenantRowsWithTenantOverride() {
        CdpWarehouseDatasetCatalogMapper datasetMapper = mock(CdpWarehouseDatasetCatalogMapper.class);
        when(datasetMapper.selectList(any())).thenReturn(List.of(
                dataset(1L, 0L, "cdp_ods_event_log", "ODS", "canvas_ods.cdp_event_log", "ACTIVE"),
                dataset(2L, 0L, "tenant_metric", "DWS", "shared.tenant_metric", "ACTIVE"),
                dataset(3L, 9L, "tenant_metric", "DWS", "tenant.tenant_metric", "ACTIVE")
        ));
        CdpWarehouseCatalogService service = service(datasetMapper, mock(CdpWarehouseLineageEdgeMapper.class));

        List<CdpWarehouseCatalogService.DatasetView> rows = service.listDatasets(9L, "dws", "active");

        assertThat(rows).extracting(CdpWarehouseCatalogService.DatasetView::datasetKey)
                .containsExactly("cdp_ods_event_log", "tenant_metric");
        assertThat(rows.get(1).tenantId()).isEqualTo(9L);
        assertThat(rows.get(1).physicalName()).isEqualTo("tenant.tenant_metric");
        verify(datasetMapper).selectList(any());
    }

    @Test
    void createLineageEdgeNormalizesDefaultsAndPersists() {
        CdpWarehouseLineageEdgeMapper lineageMapper = mock(CdpWarehouseLineageEdgeMapper.class);
        CdpWarehouseCatalogService service = service(mock(CdpWarehouseDatasetCatalogMapper.class), lineageMapper);

        CdpWarehouseCatalogService.LineageEdgeView result = service.createLineageEdge(9L,
                new CdpWarehouseCatalogService.LineageCommand(
                        "source", "target", "sql", "job#run", null, "daily rollup", null));

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.transformType()).isEqualTo("SQL");
        assertThat(result.dependencyType()).isEqualTo("HARD");
        assertThat(result.active()).isTrue();
        ArgumentCaptor<CdpWarehouseLineageEdgeDO> row =
                ArgumentCaptor.forClass(CdpWarehouseLineageEdgeDO.class);
        verify(lineageMapper).upsert(row.capture());
        assertThat(row.getValue().getUpstreamDatasetKey()).isEqualTo("source");
        assertThat(row.getValue().getDownstreamDatasetKey()).isEqualTo("target");
    }

    @Test
    void lineageGraphReturnsDirectEdgesAndCatalogNodes() {
        CdpWarehouseDatasetCatalogMapper datasetMapper = mock(CdpWarehouseDatasetCatalogMapper.class);
        CdpWarehouseLineageEdgeMapper lineageMapper = mock(CdpWarehouseLineageEdgeMapper.class);
        when(lineageMapper.selectList(any())).thenReturn(List.of(
                edge(1L, 0L, "cdp_ods_event_log", "cdp_dwd_user_event_fact", "CdpWarehouseAggregationService#dwdSql"),
                edge(2L, 0L, "cdp_dwd_user_event_fact", "cdp_dws_user_event_metric_daily", "CdpWarehouseAggregationService#dwsSql")
        ));
        when(datasetMapper.selectList(any())).thenReturn(List.of(
                dataset(1L, 0L, "cdp_ods_event_log", "ODS", "canvas_ods.cdp_event_log", "ACTIVE"),
                dataset(2L, 0L, "cdp_dwd_user_event_fact", "DWD", "canvas_dwd.cdp_user_event_fact", "ACTIVE"),
                dataset(3L, 0L, "cdp_dws_user_event_metric_daily", "DWS", "canvas_dws.user_event_metric_daily", "ACTIVE")
        ));
        CdpWarehouseCatalogService service = service(datasetMapper, lineageMapper);

        CdpWarehouseCatalogService.LineageGraph graph =
                service.lineage(9L, "cdp_dwd_user_event_fact", CdpWarehouseCatalogService.Direction.BOTH);

        assertThat(graph.tenantId()).isEqualTo(9L);
        assertThat(graph.nodes()).extracting(CdpWarehouseCatalogService.DatasetView::datasetKey)
                .containsExactly("cdp_ods_event_log", "cdp_dwd_user_event_fact", "cdp_dws_user_event_metric_daily");
        assertThat(graph.edges()).hasSize(2);
        assertThat(graph.edges()).extracting(CdpWarehouseCatalogService.LineageEdgeView::transformRef)
                .containsExactly("CdpWarehouseAggregationService#dwdSql", "CdpWarehouseAggregationService#dwsSql");
    }

    @Test
    void transitiveLineageTraversesUpstreamPathsWithDepth() {
        CdpWarehouseDatasetCatalogMapper datasetMapper = mock(CdpWarehouseDatasetCatalogMapper.class);
        CdpWarehouseLineageEdgeMapper lineageMapper = mock(CdpWarehouseLineageEdgeMapper.class);
        when(lineageMapper.selectList(any())).thenReturn(List.of(
                edge(1L, 0L, "source_events", "cdp_ods_event_log", "source_to_ods"),
                edge(2L, 0L, "cdp_ods_event_log", "cdp_dwd_user_event_fact", "ods_to_dwd"),
                edge(3L, 0L, "cdp_dwd_user_event_fact", "cdp_dws_user_event_metric_daily", "dwd_to_dws")
        ));
        when(datasetMapper.selectList(any())).thenReturn(List.of(
                dataset(1L, 0L, "source_events", "ODS", "source.events", "ACTIVE"),
                dataset(2L, 0L, "cdp_ods_event_log", "ODS", "canvas_ods.cdp_event_log", "ACTIVE"),
                dataset(3L, 0L, "cdp_dwd_user_event_fact", "DWD", "canvas_dwd.cdp_user_event_fact", "ACTIVE"),
                dataset(4L, 0L, "cdp_dws_user_event_metric_daily", "DWS", "canvas_dws.user_event_metric_daily", "ACTIVE")
        ));
        CdpWarehouseCatalogService service = service(datasetMapper, lineageMapper);

        CdpWarehouseCatalogService.TransitiveLineageGraph graph = service.transitiveLineage(
                9L,
                "cdp_dws_user_event_metric_daily",
                CdpWarehouseCatalogService.Direction.UPSTREAM,
                3);

        assertThat(graph.truncated()).isFalse();
        assertThat(graph.nodes()).extracting(node -> node.dataset().datasetKey())
                .containsExactly(
                        "cdp_dws_user_event_metric_daily",
                        "cdp_dwd_user_event_fact",
                        "cdp_ods_event_log",
                        "source_events");
        assertThat(graph.nodes()).extracting(CdpWarehouseCatalogService.LineageNodeView::depth)
                .containsExactly(0, 1, 2, 3);
        assertThat(graph.paths()).extracting(CdpWarehouseCatalogService.LineagePathView::datasetKeys)
                .contains(List.of(
                        "cdp_dws_user_event_metric_daily",
                        "cdp_dwd_user_event_fact",
                        "cdp_ods_event_log",
                        "source_events"));
    }

    @Test
    void transitiveLineageCapsDepthAndReportsTruncation() {
        CdpWarehouseDatasetCatalogMapper datasetMapper = mock(CdpWarehouseDatasetCatalogMapper.class);
        CdpWarehouseLineageEdgeMapper lineageMapper = mock(CdpWarehouseLineageEdgeMapper.class);
        when(lineageMapper.selectList(any())).thenReturn(List.of(
                edge(1L, 0L, "cdp_dwd_user_event_fact", "cdp_dws_user_event_metric_daily", "dwd_to_dws"),
                edge(2L, 0L, "cdp_dws_user_event_metric_daily", "audience_feature_wide", "dws_to_feature"),
                edge(3L, 0L, "audience_feature_wide", "segment_activation_export", "feature_to_export")
        ));
        when(datasetMapper.selectList(any())).thenReturn(List.of(
                dataset(1L, 0L, "cdp_dwd_user_event_fact", "DWD", "canvas_dwd.cdp_user_event_fact", "ACTIVE"),
                dataset(2L, 0L, "cdp_dws_user_event_metric_daily", "DWS", "canvas_dws.user_event_metric_daily", "ACTIVE"),
                dataset(3L, 0L, "audience_feature_wide", "DWS", "canvas_dws.audience_feature_wide", "ACTIVE")
        ));
        CdpWarehouseCatalogService service = service(datasetMapper, lineageMapper);

        CdpWarehouseCatalogService.TransitiveLineageGraph graph = service.transitiveLineage(
                9L,
                "cdp_dwd_user_event_fact",
                CdpWarehouseCatalogService.Direction.DOWNSTREAM,
                2);

        assertThat(graph.truncated()).isTrue();
        assertThat(graph.nodes()).extracting(node -> node.dataset().datasetKey())
                .containsExactly(
                        "cdp_dwd_user_event_fact",
                        "cdp_dws_user_event_metric_daily",
                        "audience_feature_wide");
        assertThat(graph.edges()).extracting(CdpWarehouseCatalogService.LineageEdgeView::transformRef)
                .containsExactly("dwd_to_dws", "dws_to_feature");
    }

    @Test
    void transitiveLineageWarnsOnCyclesWithoutInfiniteExpansion() {
        CdpWarehouseDatasetCatalogMapper datasetMapper = mock(CdpWarehouseDatasetCatalogMapper.class);
        CdpWarehouseLineageEdgeMapper lineageMapper = mock(CdpWarehouseLineageEdgeMapper.class);
        when(lineageMapper.selectList(any())).thenReturn(List.of(
                edge(1L, 0L, "dataset_a", "dataset_b", "a_to_b"),
                edge(2L, 0L, "dataset_b", "dataset_c", "b_to_c"),
                edge(3L, 0L, "dataset_c", "dataset_a", "c_to_a")
        ));
        when(datasetMapper.selectList(any())).thenReturn(List.of(
                dataset(1L, 0L, "dataset_a", "DWD", "a", "ACTIVE"),
                dataset(2L, 0L, "dataset_b", "DWD", "b", "ACTIVE"),
                dataset(3L, 0L, "dataset_c", "DWD", "c", "ACTIVE")
        ));
        CdpWarehouseCatalogService service = service(datasetMapper, lineageMapper);

        CdpWarehouseCatalogService.TransitiveLineageGraph graph = service.transitiveLineage(
                9L,
                "dataset_a",
                CdpWarehouseCatalogService.Direction.DOWNSTREAM,
                8);

        assertThat(graph.nodes()).extracting(node -> node.dataset().datasetKey())
                .containsExactly("dataset_a", "dataset_b", "dataset_c");
        assertThat(graph.warnings()).anySatisfy(warning ->
                assertThat(warning).contains("lineage cycle detected"));
        assertThat(graph.paths()).hasSize(2);
    }

    @Test
    void transitiveLineageUsesTenantEdgeOverride() {
        CdpWarehouseDatasetCatalogMapper datasetMapper = mock(CdpWarehouseDatasetCatalogMapper.class);
        CdpWarehouseLineageEdgeMapper lineageMapper = mock(CdpWarehouseLineageEdgeMapper.class);
        CdpWarehouseLineageEdgeDO builtIn = edge(1L, 0L, "source", "target", "job#rollup");
        builtIn.setDescription("built-in edge");
        CdpWarehouseLineageEdgeDO tenant = edge(2L, 9L, "source", "target", "job#rollup");
        tenant.setDescription("tenant edge");
        when(lineageMapper.selectList(any())).thenReturn(List.of(builtIn, tenant));
        when(datasetMapper.selectList(any())).thenReturn(List.of(
                dataset(1L, 0L, "source", "ODS", "source", "ACTIVE"),
                dataset(2L, 9L, "target", "DWS", "target", "ACTIVE")
        ));
        CdpWarehouseCatalogService service = service(datasetMapper, lineageMapper);

        CdpWarehouseCatalogService.TransitiveLineageGraph graph = service.transitiveLineage(
                9L,
                "source",
                CdpWarehouseCatalogService.Direction.DOWNSTREAM,
                1);

        assertThat(graph.edges()).singleElement().satisfies(edge -> {
            assertThat(edge.tenantId()).isEqualTo(9L);
            assertThat(edge.description()).isEqualTo("tenant edge");
        });
    }

    private CdpWarehouseCatalogService service(CdpWarehouseDatasetCatalogMapper datasetMapper,
                                               CdpWarehouseLineageEdgeMapper lineageMapper) {
        return new CdpWarehouseCatalogService(datasetMapper, lineageMapper);
    }

    private CdpWarehouseDatasetCatalogDO dataset(Long id, Long tenantId, String key, String layer,
                                                 String physicalName, String status) {
        CdpWarehouseDatasetCatalogDO row = new CdpWarehouseDatasetCatalogDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setDatasetKey(key);
        row.setLayer(layer);
        row.setPhysicalName(physicalName);
        row.setDisplayName(key);
        row.setSubjectArea("CDP");
        row.setSourceSystem("canvas-engine");
        row.setOwnerName("ops");
        row.setFreshnessSlaMinutes(30);
        row.setPiiLevel("NORMAL");
        row.setStatus(status);
        row.setSchemaJson("{}");
        return row;
    }

    private CdpWarehouseLineageEdgeDO edge(Long id, Long tenantId, String upstream, String downstream,
                                           String transformRef) {
        CdpWarehouseLineageEdgeDO row = new CdpWarehouseLineageEdgeDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setUpstreamDatasetKey(upstream);
        row.setDownstreamDatasetKey(downstream);
        row.setTransformType("SQL_INSERT_SELECT");
        row.setTransformRef(transformRef);
        row.setDependencyType("HARD");
        row.setDescription("lineage");
        row.setActive(true);
        return row;
    }
}
