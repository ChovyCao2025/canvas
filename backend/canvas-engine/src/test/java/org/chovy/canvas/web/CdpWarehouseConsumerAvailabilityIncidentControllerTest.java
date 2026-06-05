package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityIncidentService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseConsumerAvailabilityIncidentControllerTest {

    @Test
    void scanDelegatesCurrentTenantContractWindowAndOperator() {
        CdpWarehouseConsumerAvailabilityIncidentService service =
                mock(CdpWarehouseConsumerAvailabilityIncidentService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T11:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T12:00:00");
        CdpWarehouseConsumerAvailabilityIncidentService.ScanResult result =
                new CdpWarehouseConsumerAvailabilityIncidentService.ScanResult(
                        9L, "bi_revenue", "BI_METRIC", from, to, "WARN",
                        1, 1, 0, 0, 0, from, to);
        when(service.scan(9L, "bi_revenue", "BI_METRIC", from, to, "qa")).thenReturn(result);
        CdpWarehouseConsumerAvailabilityIncidentController.ScanReq req =
                new CdpWarehouseConsumerAvailabilityIncidentController.ScanReq();
        req.setContractKey("bi_revenue");
        req.setConsumerType("BI_METRIC");
        req.setFrom(from);
        req.setTo(to);
        req.setOperator("qa");
        CdpWarehouseConsumerAvailabilityIncidentController controller =
                new CdpWarehouseConsumerAvailabilityIncidentController(service, tenantResolver(9L));

        R<CdpWarehouseConsumerAvailabilityIncidentService.ScanResult> response =
                controller.scan(req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).scan(9L, "bi_revenue", "BI_METRIC", from, to, "qa");
    }

    @Test
    void scanDefaultsTenantAndOperatorWhenResolverIsAbsent() {
        CdpWarehouseConsumerAvailabilityIncidentService service =
                mock(CdpWarehouseConsumerAvailabilityIncidentService.class);
        CdpWarehouseConsumerAvailabilityIncidentService.ScanResult result =
                new CdpWarehouseConsumerAvailabilityIncidentService.ScanResult(
                        0L, null, null, null, null, "PASS",
                        0, 0, 0, 0, 0, null, null);
        when(service.scan(0L, null, null, null, null, "system")).thenReturn(result);
        CdpWarehouseConsumerAvailabilityIncidentController controller =
                new CdpWarehouseConsumerAvailabilityIncidentController(service);

        R<CdpWarehouseConsumerAvailabilityIncidentService.ScanResult> response =
                controller.scan(null).block();

        assertThat(response.getData().tenantId()).isZero();
        verify(service).scan(0L, null, null, null, null, "system");
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
