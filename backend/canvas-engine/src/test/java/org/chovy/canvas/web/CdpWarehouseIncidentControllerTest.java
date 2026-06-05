package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseIncidentService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseIncidentControllerTest {

    @Test
    void listUsesCurrentTenantAndFilters() {
        CdpWarehouseIncidentService service = mock(CdpWarehouseIncidentService.class);
        List<CdpWarehouseIncidentService.IncidentView> incidents = List.of(incident());
        when(service.listIncidents(9L, "OPEN", 20)).thenReturn(incidents);
        CdpWarehouseIncidentController controller = new CdpWarehouseIncidentController(service, tenantResolver(9L));

        R<List<CdpWarehouseIncidentService.IncidentView>> response = controller.list("OPEN", 20).block();

        assertThat(response.getData()).isSameAs(incidents);
        verify(service).listIncidents(9L, "OPEN", 20);
    }

    @Test
    void acknowledgeUsesCurrentTenantAndOperator() {
        CdpWarehouseIncidentService service = mock(CdpWarehouseIncidentService.class);
        when(service.acknowledge(9L, 1L, "operator")).thenReturn(true);
        CdpWarehouseIncidentController.OperatorReq req = new CdpWarehouseIncidentController.OperatorReq();
        req.setOperator("operator");
        CdpWarehouseIncidentController controller = new CdpWarehouseIncidentController(service, tenantResolver(9L));

        R<Boolean> response = controller.acknowledge(1L, req).block();

        assertThat(response.getData()).isTrue();
        verify(service).acknowledge(9L, 1L, "operator");
    }

    @Test
    void resolveUsesCurrentTenantAndOperator() {
        CdpWarehouseIncidentService service = mock(CdpWarehouseIncidentService.class);
        when(service.resolve(9L, 1L, "operator")).thenReturn(true);
        CdpWarehouseIncidentController.OperatorReq req = new CdpWarehouseIncidentController.OperatorReq();
        req.setOperator("operator");
        CdpWarehouseIncidentController controller = new CdpWarehouseIncidentController(service, tenantResolver(9L));

        R<Boolean> response = controller.resolve(1L, req).block();

        assertThat(response.getData()).isTrue();
        verify(service).resolve(9L, 1L, "operator");
    }

    private CdpWarehouseIncidentService.IncidentView incident() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        return new CdpWarehouseIncidentService.IncidentView(
                1L, 9L, "QUALITY:ODS_COUNT", "WAREHOUSE_QUALITY_CHECK", 11L,
                "WARN", "OPEN", "title", "description", 2L,
                now.minusHours(1), now, null, null, null, null);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
