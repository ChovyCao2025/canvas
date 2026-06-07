package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseSyntheticDataPathProbeService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseSyntheticDataPathProbeSourceModeControllerTest {

    @Test
    void runPassesSourceModeToProbeService() {
        CdpWarehouseSyntheticDataPathProbeService service =
                mock(CdpWarehouseSyntheticDataPathProbeService.class);
        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView view = runView();
        when(service.run(eq(9L), any())).thenReturn(view);
        CdpWarehouseSyntheticDataPathProbeController controller =
                new CdpWarehouseSyntheticDataPathProbeController(service, tenantResolver());

        R<CdpWarehouseSyntheticDataPathProbeService.ProbeRunView> response =
                controller.run("ods-cert", "__warehouse_probe_custom", false, 1, 0, "MYSQL_CDC").block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).run(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "ods-cert".equals(command.probeKey())
                        && "__warehouse_probe_custom".equals(command.eventCode())
                        && Boolean.FALSE.equals(command.strict())
                        && Integer.valueOf(1).equals(command.verifyAttempts())
                        && Integer.valueOf(0).equals(command.verifyDelayMs())
                        && "MYSQL_CDC".equals(command.sourceMode())));
    }

    private CdpWarehouseSyntheticDataPathProbeService.ProbeRunView runView() {
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        return new CdpWarehouseSyntheticDataPathProbeService.ProbeRunView(
                7L,
                9L,
                "ods-cert",
                "MYSQL_CDC",
                "warehouse-probe-1",
                "__warehouse_probe__",
                "__warehouse_probe_user_1",
                true,
                "PASS",
                "PASS",
                "SKIPPED",
                "PASS",
                1L,
                now,
                now,
                null,
                "[]",
                null,
                null);
    }

    private TenantContextResolver tenantResolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(9L, "TENANT_ADMIN", "alice")));
        return resolver;
    }
}
