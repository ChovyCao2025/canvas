package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseSyntheticDataPathProbeService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseSyntheticDataPathProbeControllerTest {

    @Test
    void runUsesCurrentTenantAndRequestParameters() {
        CdpWarehouseSyntheticDataPathProbeService service =
                mock(CdpWarehouseSyntheticDataPathProbeService.class);
        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView view = runView("PASS");
        when(service.run(eq(9L), any())).thenReturn(view);
        CdpWarehouseSyntheticDataPathProbeController controller =
                new CdpWarehouseSyntheticDataPathProbeController(service, tenantResolver(9L, "alice"));

        R<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView> response =
                controller.run("ods-cert", "__warehouse_probe_custom", false, 1, 0).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).run(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "ods-cert".equals(command.probeKey())
                        && "__warehouse_probe_custom".equals(command.eventCode())
                        && Boolean.FALSE.equals(command.strict())
                        && command.verifyAttempts() == 1
                        && command.verifyDelayMs() == 0));
    }

    @Test
    void recentUsesCurrentTenantAndLimit() {
        CdpWarehouseSyntheticDataPathProbeService service =
                mock(CdpWarehouseSyntheticDataPathProbeService.class);
        List<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView> rows = List.of(runView("PASS"));
        when(service.recent(9L, 25)).thenReturn(rows);
        CdpWarehouseSyntheticDataPathProbeController controller =
                new CdpWarehouseSyntheticDataPathProbeController(service, tenantResolver(9L, "alice"));

        R<List<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView>> response =
                controller.recent(25).block();

        assertThat(response.getData()).isSameAs(rows);
        verify(service).recent(9L, 25);
    }

    private CdpWarehouseSyntheticDataPathProbeService.ProbeRunView runView(String status) {
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        return new CdpWarehouseSyntheticDataPathProbeService.ProbeRunView(
                7L,
                9L,
                "ods-cert",
                "warehouse-probe-1",
                "__warehouse_probe__",
                "__warehouse_probe_user_1",
                true,
                status,
                status,
                status,
                1L,
                now,
                now,
                null,
                "[]",
                null,
                null);
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
