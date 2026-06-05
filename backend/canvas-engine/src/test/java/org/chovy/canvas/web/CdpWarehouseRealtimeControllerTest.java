package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCheckpointService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimeControllerTest {

    @Test
    void statusUsesCurrentTenant() {
        CdpWarehouseRealtimeCheckpointService service = mock(CdpWarehouseRealtimeCheckpointService.class);
        CdpWarehouseRealtimeCheckpointService.RealtimeStatus status =
                new CdpWarehouseRealtimeCheckpointService.RealtimeStatus(9L, List.of(), 3, 1);
        when(service.status(9L)).thenReturn(status);
        CdpWarehouseRealtimeController controller =
                new CdpWarehouseRealtimeController(service, tenantResolver(9L));

        R<CdpWarehouseRealtimeCheckpointService.RealtimeStatus> response = controller.status().block();

        assertThat(response.getData()).isSameAs(status);
        verify(service).status(9L);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
