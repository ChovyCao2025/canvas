package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseTableDriftIncidentService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseTableDriftIncidentControllerTest {

    @Test
    void scanDelegatesCurrentTenantAndMode() {
        CdpWarehouseTableDriftIncidentService service =
                mock(CdpWarehouseTableDriftIncidentService.class);
        CdpWarehouseTableDriftIncidentService.ScanResult result =
                new CdpWarehouseTableDriftIncidentService.ScanResult(9L, true, 2, 1, 1, 0, 0);
        when(service.scan(9L, true, "qa")).thenReturn(result);
        CdpWarehouseTableDriftIncidentController controller =
                new CdpWarehouseTableDriftIncidentController(service, tenantResolver(9L));

        R<CdpWarehouseTableDriftIncidentService.ScanResult> response =
                controller.scan(true, "qa").block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).scan(9L, true, "qa");
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
