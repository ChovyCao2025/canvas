package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobIncidentService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimeJobIncidentControllerTest {

    @Test
    void scanDelegatesCurrentTenantAndFilters() {
        CdpWarehouseRealtimeJobIncidentService service =
                mock(CdpWarehouseRealtimeJobIncidentService.class);
        CdpWarehouseRealtimeJobIncidentService.ScanResult result =
                new CdpWarehouseRealtimeJobIncidentService.ScanResult(9L, 3, 2, 1, 0);
        when(service.scan(9L, "pipe", 120, 20)).thenReturn(result);
        CdpWarehouseRealtimeJobIncidentController controller =
                new CdpWarehouseRealtimeJobIncidentController(service, tenantResolver(9L));

        R<CdpWarehouseRealtimeJobIncidentService.ScanResult> response =
                controller.scan("pipe", 120, 20).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).scan(9L, "pipe", 120, 20);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
