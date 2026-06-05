package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseCatalogService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseCatalogControllerTest {

    @Test
    void listDatasetsUsesCurrentTenantAndFilters() {
        CdpWarehouseCatalogService service = mock(CdpWarehouseCatalogService.class);
        List<CdpWarehouseCatalogService.DatasetView> datasets = List.of(dataset("cdp_ods_event_log"));
        when(service.listDatasets(9L, "ODS", "ACTIVE")).thenReturn(datasets);
        CdpWarehouseCatalogController controller = new CdpWarehouseCatalogController(service, tenantResolver(9L));

        R<List<CdpWarehouseCatalogService.DatasetView>> response =
                controller.listDatasets("ODS", "ACTIVE").block();

        assertThat(response.getData()).isSameAs(datasets);
        verify(service).listDatasets(9L, "ODS", "ACTIVE");
    }

    @Test
    void upsertDatasetDelegatesCurrentTenantAndRequestBody() {
        CdpWarehouseCatalogService service = mock(CdpWarehouseCatalogService.class);
        CdpWarehouseCatalogService.DatasetView dataset = dataset("tenant_metric");
        when(service.upsertDataset(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(dataset);
        CdpWarehouseCatalogController.DatasetReq req = new CdpWarehouseCatalogController.DatasetReq();
        req.setDatasetKey("tenant_metric");
        req.setLayer("DWS");
        req.setPhysicalName("canvas_dws.tenant_metric");
        req.setDisplayName("Tenant Metric");
        CdpWarehouseCatalogController controller = new CdpWarehouseCatalogController(service, tenantResolver(9L));

        R<CdpWarehouseCatalogService.DatasetView> response = controller.upsertDataset(req).block();

        assertThat(response.getData()).isSameAs(dataset);
        verify(service).upsertDataset(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "tenant_metric".equals(command.datasetKey())
                        && "DWS".equals(command.layer())
                        && "canvas_dws.tenant_metric".equals(command.physicalName())));
    }

    @Test
    void createLineageEdgeDelegatesCurrentTenantAndRequestBody() {
        CdpWarehouseCatalogService service = mock(CdpWarehouseCatalogService.class);
        CdpWarehouseCatalogService.LineageEdgeView edge = edge("source", "target");
        when(service.createLineageEdge(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(edge);
        CdpWarehouseCatalogController.LineageReq req = new CdpWarehouseCatalogController.LineageReq();
        req.setUpstreamDatasetKey("source");
        req.setDownstreamDatasetKey("target");
        req.setTransformType("SQL");
        CdpWarehouseCatalogController controller = new CdpWarehouseCatalogController(service, tenantResolver(9L));

        R<CdpWarehouseCatalogService.LineageEdgeView> response = controller.createLineageEdge(req).block();

        assertThat(response.getData()).isSameAs(edge);
        verify(service).createLineageEdge(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "source".equals(command.upstreamDatasetKey())
                        && "target".equals(command.downstreamDatasetKey())
                        && "SQL".equals(command.transformType())));
    }

    @Test
    void lineageDelegatesCurrentTenantDatasetKeyAndDirection() {
        CdpWarehouseCatalogService service = mock(CdpWarehouseCatalogService.class);
        CdpWarehouseCatalogService.LineageGraph graph = new CdpWarehouseCatalogService.LineageGraph(
                9L, "cdp_dwd_user_event_fact", CdpWarehouseCatalogService.Direction.BOTH,
                List.of(dataset("cdp_dwd_user_event_fact")), List.of());
        when(service.lineage(9L, "cdp_dwd_user_event_fact", CdpWarehouseCatalogService.Direction.BOTH))
                .thenReturn(graph);
        CdpWarehouseCatalogController controller = new CdpWarehouseCatalogController(service, tenantResolver(9L));

        R<CdpWarehouseCatalogService.LineageGraph> response =
                controller.lineage("cdp_dwd_user_event_fact", CdpWarehouseCatalogService.Direction.BOTH).block();

        assertThat(response.getData()).isSameAs(graph);
        verify(service).lineage(9L, "cdp_dwd_user_event_fact", CdpWarehouseCatalogService.Direction.BOTH);
    }

    @Test
    void transitiveLineageDelegatesCurrentTenantDatasetKeyDirectionAndDepth() {
        CdpWarehouseCatalogService service = mock(CdpWarehouseCatalogService.class);
        CdpWarehouseCatalogService.TransitiveLineageGraph graph =
                new CdpWarehouseCatalogService.TransitiveLineageGraph(
                        9L,
                        "cdp_dwd_user_event_fact",
                        CdpWarehouseCatalogService.Direction.DOWNSTREAM,
                        4,
                        false,
                        List.of(new CdpWarehouseCatalogService.LineageNodeView(
                                dataset("cdp_dwd_user_event_fact"),
                                0,
                                CdpWarehouseCatalogService.LineageRelation.SELF)),
                        List.of(),
                        List.of(),
                        List.of());
        when(service.transitiveLineage(
                9L,
                "cdp_dwd_user_event_fact",
                CdpWarehouseCatalogService.Direction.DOWNSTREAM,
                4)).thenReturn(graph);
        CdpWarehouseCatalogController controller = new CdpWarehouseCatalogController(service, tenantResolver(9L));

        R<CdpWarehouseCatalogService.TransitiveLineageGraph> response =
                controller.transitiveLineage(
                        "cdp_dwd_user_event_fact",
                        CdpWarehouseCatalogService.Direction.DOWNSTREAM,
                        4).block();

        assertThat(response.getData()).isSameAs(graph);
        verify(service).transitiveLineage(
                9L,
                "cdp_dwd_user_event_fact",
                CdpWarehouseCatalogService.Direction.DOWNSTREAM,
                4);
    }

    private CdpWarehouseCatalogService.DatasetView dataset(String key) {
        return new CdpWarehouseCatalogService.DatasetView(
                1L, 9L, key, "DWS", "canvas_dws." + key, key, "CDP",
                "canvas-engine", "ops", "dataset", 30, "NORMAL", "ACTIVE", "{}");
    }

    private CdpWarehouseCatalogService.LineageEdgeView edge(String upstream, String downstream) {
        return new CdpWarehouseCatalogService.LineageEdgeView(
                1L, 9L, upstream, downstream, "SQL", "job#run", "HARD", "edge", true);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
