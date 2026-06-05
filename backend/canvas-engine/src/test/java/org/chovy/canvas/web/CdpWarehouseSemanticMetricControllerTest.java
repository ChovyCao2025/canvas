package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseSemanticMetricService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseSemanticMetricControllerTest {

    @Test
    void listMetricsUsesCurrentTenantAndDatasetFilter() {
        CdpWarehouseSemanticMetricService service = mock(CdpWarehouseSemanticMetricService.class);
        List<CdpWarehouseSemanticMetricService.SemanticMetricView> rows = List.of(metric());
        when(service.listMetrics(9L, "canvas_daily_stats")).thenReturn(rows);
        CdpWarehouseSemanticMetricController controller =
                new CdpWarehouseSemanticMetricController(service, tenantResolver(9L));

        R<List<CdpWarehouseSemanticMetricService.SemanticMetricView>> response =
                controller.listMetrics("canvas_daily_stats").block();

        assertThat(response.getData()).isSameAs(rows);
        verify(service).listMetrics(9L, "canvas_daily_stats");
    }

    @Test
    void listMetricsDefaultsTenantWhenResolverIsAbsent() {
        CdpWarehouseSemanticMetricService service = mock(CdpWarehouseSemanticMetricService.class);
        List<CdpWarehouseSemanticMetricService.SemanticMetricView> rows = List.of(metric());
        when(service.listMetrics(0L, null)).thenReturn(rows);
        CdpWarehouseSemanticMetricController controller =
                new CdpWarehouseSemanticMetricController(service);

        R<List<CdpWarehouseSemanticMetricService.SemanticMetricView>> response =
                controller.listMetrics(null).block();

        assertThat(response.getData()).isSameAs(rows);
        verify(service).listMetrics(0L, null);
    }

    private CdpWarehouseSemanticMetricService.SemanticMetricView metric() {
        return new CdpWarehouseSemanticMetricService.SemanticMetricView(
                9L,
                "canvas_daily_stats",
                "success_rate",
                "SUM(success_count)",
                "PERCENT",
                List.of("stat_date"),
                "ALLOW_LIST",
                "BI_DATASET_SPEC");
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
