package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseMetricLineageService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseMetricLineageControllerTest {

    @Test
    void impactUsesCurrentTenantDatasetAndMetric() {
        CdpWarehouseMetricLineageService service = mock(CdpWarehouseMetricLineageService.class);
        CdpWarehouseMetricLineageService.MetricImpactView impact = impact(9L);
        when(service.impact(9L, "canvas_daily_stats", "success_rate")).thenReturn(impact);
        CdpWarehouseMetricLineageController controller =
                new CdpWarehouseMetricLineageController(service, tenantResolver(9L));

        R<CdpWarehouseMetricLineageService.MetricImpactView> response =
                controller.impact("canvas_daily_stats", "success_rate").block();

        assertThat(response.getData()).isSameAs(impact);
        verify(service).impact(9L, "canvas_daily_stats", "success_rate");
    }

    @Test
    void impactDefaultsTenantWhenResolverIsAbsent() {
        CdpWarehouseMetricLineageService service = mock(CdpWarehouseMetricLineageService.class);
        CdpWarehouseMetricLineageService.MetricImpactView impact = impact(0L);
        when(service.impact(0L, "canvas_daily_stats", "success_rate")).thenReturn(impact);
        CdpWarehouseMetricLineageController controller =
                new CdpWarehouseMetricLineageController(service);

        R<CdpWarehouseMetricLineageService.MetricImpactView> response =
                controller.impact("canvas_daily_stats", "success_rate").block();

        assertThat(response.getData().tenantId()).isZero();
        verify(service).impact(0L, "canvas_daily_stats", "success_rate");
    }

    private CdpWarehouseMetricLineageService.MetricImpactView impact(Long tenantId) {
        return new CdpWarehouseMetricLineageService.MetricImpactView(
                tenantId,
                "canvas_daily_stats",
                "success_rate",
                "SUM(success_count)",
                "PERCENT",
                List.of("stat_date"),
                List.of(new CdpWarehouseMetricLineageService.FieldDependencyView(
                        "success_count", "EXPRESSION_FIELD")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
