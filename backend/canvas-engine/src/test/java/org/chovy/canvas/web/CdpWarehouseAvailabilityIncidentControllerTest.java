package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityIncidentService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseAvailabilityIncidentControllerTest {

    @Test
    void scanDelegatesCurrentTenantWindowModeAndOperator() {
        CdpWarehouseAvailabilityIncidentService service = mock(CdpWarehouseAvailabilityIncidentService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T11:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T12:00:00");
        CdpWarehouseAvailabilityIncidentService.ScanResult result =
                new CdpWarehouseAvailabilityIncidentService.ScanResult(
                        9L, "HYBRID", from, to, "WARN", 2, 1, 0, 1, 0);
        when(service.scan(9L, from, to, "HYBRID", "qa")).thenReturn(result);
        CdpWarehouseAvailabilityIncidentController.ScanReq req =
                new CdpWarehouseAvailabilityIncidentController.ScanReq();
        req.setFrom(from);
        req.setTo(to);
        req.setMode("HYBRID");
        req.setOperator("qa");
        CdpWarehouseAvailabilityIncidentController controller =
                new CdpWarehouseAvailabilityIncidentController(service, tenantResolver(9L));

        R<CdpWarehouseAvailabilityIncidentService.ScanResult> response = controller.scan(req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).scan(9L, from, to, "HYBRID", "qa");
    }

    @Test
    void scanDefaultsTenantAndOperatorWhenResolverIsAbsent() {
        CdpWarehouseAvailabilityIncidentService service = mock(CdpWarehouseAvailabilityIncidentService.class);
        CdpWarehouseAvailabilityIncidentService.ScanResult result =
                new CdpWarehouseAvailabilityIncidentService.ScanResult(
                        0L, "HYBRID", null, null, "PASS", 2, 0, 0, 2, 0);
        when(service.scan(0L, null, null, "HYBRID", "system")).thenReturn(result);
        CdpWarehouseAvailabilityIncidentController controller =
                new CdpWarehouseAvailabilityIncidentController(service);

        R<CdpWarehouseAvailabilityIncidentService.ScanResult> response = controller.scan(null).block();

        assertThat(response.getData().tenantId()).isZero();
        verify(service).scan(0L, null, null, "HYBRID", "system");
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
