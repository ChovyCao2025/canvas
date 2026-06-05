package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineIncidentService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimePipelineIncidentControllerTest {

    @Test
    void scanDelegatesCurrentTenantAndRecentLimit() {
        CdpWarehouseRealtimePipelineIncidentService service =
                mock(CdpWarehouseRealtimePipelineIncidentService.class);
        CdpWarehouseRealtimePipelineIncidentService.ScanResult result =
                new CdpWarehouseRealtimePipelineIncidentService.ScanResult(9L, 3, 2, 1, 0);
        when(service.scan(9L, 7)).thenReturn(result);
        CdpWarehouseRealtimePipelineIncidentController controller =
                new CdpWarehouseRealtimePipelineIncidentController(service, tenantResolver(9L));

        R<CdpWarehouseRealtimePipelineIncidentService.ScanResult> response =
                controller.scan(7).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).scan(9L, 7);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
