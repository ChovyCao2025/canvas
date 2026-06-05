package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseQualityService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseQualityControllerTest {

    @Test
    void listChecksUsesCurrentTenant() {
        CdpWarehouseQualityService service = mock(CdpWarehouseQualityService.class);
        List<CdpWarehouseQualityService.QualityCheckResult> checks = List.of(result("ODS_COUNT", "PASS"));
        when(service.recentChecks(9L, 20)).thenReturn(checks);
        CdpWarehouseQualityController controller = new CdpWarehouseQualityController(service, tenantResolver(9L));

        R<List<CdpWarehouseQualityService.QualityCheckResult>> response = controller.listChecks(20).block();

        assertThat(response.getData()).isSameAs(checks);
        verify(service).recentChecks(9L, 20);
    }

    @Test
    void reconcileOdsUsesCurrentTenantAndRequestWindow() {
        CdpWarehouseQualityService service = mock(CdpWarehouseQualityService.class);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 5, 11, 0);
        CdpWarehouseQualityService.QualityCheckResult result = result("ODS_COUNT", "PASS");
        when(service.reconcileOds(9L, from, to, 1, "operator")).thenReturn(result);
        CdpWarehouseQualityController.ReconcileOdsReq req = new CdpWarehouseQualityController.ReconcileOdsReq();
        req.setFrom(from);
        req.setTo(to);
        req.setTolerance(1L);
        req.setOperator("operator");
        CdpWarehouseQualityController controller = new CdpWarehouseQualityController(service, tenantResolver(9L));

        R<CdpWarehouseQualityService.QualityCheckResult> response = controller.reconcileOds(req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).reconcileOds(9L, from, to, 1, "operator");
    }

    @Test
    void aggregateLagUsesCurrentTenantAndThreshold() {
        CdpWarehouseQualityService service = mock(CdpWarehouseQualityService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        CdpWarehouseQualityService.QualityCheckResult result = result("AGGREGATE_LAG", "PASS");
        when(service.checkAggregateLag(9L, now, 30, "operator")).thenReturn(result);
        CdpWarehouseQualityController.AggregateLagReq req = new CdpWarehouseQualityController.AggregateLagReq();
        req.setNow(now);
        req.setMaxLagMinutes(30L);
        req.setOperator("operator");
        CdpWarehouseQualityController controller = new CdpWarehouseQualityController(service, tenantResolver(9L));

        R<CdpWarehouseQualityService.QualityCheckResult> response = controller.aggregateLag(req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).checkAggregateLag(9L, now, 30, "operator");
    }

    private CdpWarehouseQualityService.QualityCheckResult result(String type, String status) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        return new CdpWarehouseQualityService.QualityCheckResult(
                1L, 9L, type, status, 10L, 10L, 0L,
                now.minusMinutes(60), now, 1L, "{}", now, "operator");
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
