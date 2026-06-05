package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseReadinessControllerTest {

    @Test
    void readinessUsesCurrentTenant() {
        CdpWarehouseReadinessService service = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseReadinessService.ReadinessSummary summary = summary(9L, "PASS");
        when(service.readiness(9L)).thenReturn(summary);
        CdpWarehouseReadinessController controller =
                new CdpWarehouseReadinessController(service, tenantResolver(9L));

        R<CdpWarehouseReadinessService.ReadinessSummary> response = controller.readiness().block();

        assertThat(response.getData()).isSameAs(summary);
        verify(service).readiness(9L);
    }

    @Test
    void readinessDefaultsTenantWhenResolverIsAbsent() {
        CdpWarehouseReadinessService service = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseReadinessService.ReadinessSummary summary = summary(0L, "WARN");
        when(service.readiness(0L)).thenReturn(summary);
        CdpWarehouseReadinessController controller = new CdpWarehouseReadinessController(service);

        R<CdpWarehouseReadinessService.ReadinessSummary> response = controller.readiness().block();

        assertThat(response.getData().tenantId()).isZero();
        verify(service).readiness(0L);
    }

    private CdpWarehouseReadinessService.ReadinessSummary summary(Long tenantId, String status) {
        return new CdpWarehouseReadinessService.ReadinessSummary(
                tenantId,
                status,
                LocalDateTime.parse("2026-06-05T03:00:00"),
                List.of(new CdpWarehouseReadinessService.ReadinessSection("offline_sync", status, "ok")),
                new CdpWarehouseReadinessService.OfflineReadiness(status, "ok", 1, 0, 0, 1),
                new CdpWarehouseReadinessService.RealtimeReadiness(status, "ok", 1, 1, 0, 0),
                new CdpWarehouseReadinessService.IncidentReadiness(status, "ok", 0, 0, 0),
                new CdpWarehouseReadinessService.BiReadiness(status, "ok", 1, 1, 0),
                new CdpWarehouseReadinessService.AudienceMaterializationReadiness(status, "ok", 1, 1, 0));
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "alice")));
        return resolver;
    }
}
