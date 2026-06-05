package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessIncidentService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseReadinessIncidentControllerTest {

    @Test
    void scanDelegatesCurrentTenant() {
        CdpWarehouseReadinessIncidentService service = mock(CdpWarehouseReadinessIncidentService.class);
        CdpWarehouseReadinessIncidentService.ScanResult result =
                new CdpWarehouseReadinessIncidentService.ScanResult(9L, "FAIL", 5, 2, 3, 0);
        when(service.scan(9L)).thenReturn(result);
        CdpWarehouseReadinessIncidentController controller =
                new CdpWarehouseReadinessIncidentController(service, tenantResolver(9L));

        R<CdpWarehouseReadinessIncidentService.ScanResult> response = controller.scan().block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).scan(9L);
    }

    @Test
    void scanDefaultsTenantWhenResolverIsAbsent() {
        CdpWarehouseReadinessIncidentService service = mock(CdpWarehouseReadinessIncidentService.class);
        CdpWarehouseReadinessIncidentService.ScanResult result =
                new CdpWarehouseReadinessIncidentService.ScanResult(0L, "WARN", 5, 1, 4, 0);
        when(service.scan(0L)).thenReturn(result);
        CdpWarehouseReadinessIncidentController controller =
                new CdpWarehouseReadinessIncidentController(service);

        R<CdpWarehouseReadinessIncidentService.ScanResult> response = controller.scan().block();

        assertThat(response.getData().tenantId()).isZero();
        verify(service).scan(0L);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
