package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseExternalRealtimeJobProbeService;
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

class CdpWarehouseExternalRealtimeJobProbeControllerTest {

    @Test
    void upsertTargetUsesCurrentTenantAndBody() {
        CdpWarehouseExternalRealtimeJobProbeService service =
                mock(CdpWarehouseExternalRealtimeJobProbeService.class);
        CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView view = targetView(true);
        when(service.upsertTarget(eq(9L), any())).thenReturn(view);
        CdpWarehouseExternalRealtimeJobProbeController.TargetReq req =
                new CdpWarehouseExternalRealtimeJobProbeController.TargetReq();
        req.setPipelineKey("pipe");
        req.setJobKey("job-a");
        req.setEngineType("FLINK_REST");
        req.setEndpointUrl("http://flink/jobs/flink-1");
        req.setExternalJobId("flink-1");
        req.setEnabled(true);
        CdpWarehouseExternalRealtimeJobProbeController controller =
                new CdpWarehouseExternalRealtimeJobProbeController(service, tenantResolver(9L, "alice"));

        R<CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView> response =
                controller.upsertTarget(req).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).upsertTarget(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "pipe".equals(command.pipelineKey())
                        && "job-a".equals(command.jobKey())
                        && "FLINK_REST".equals(command.engineType())
                        && "http://flink/jobs/flink-1".equals(command.endpointUrl())));
    }

    @Test
    void listEnableAndScanUseCurrentTenant() {
        CdpWarehouseExternalRealtimeJobProbeService service =
                mock(CdpWarehouseExternalRealtimeJobProbeService.class);
        List<CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView> targets =
                List.of(targetView(true));
        CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView disabled = targetView(false);
        CdpWarehouseExternalRealtimeJobProbeService.ScanSummary scan =
                new CdpWarehouseExternalRealtimeJobProbeService.ScanSummary(9L, 1, 1, 0, 0, List.of());
        when(service.listTargets(9L, true, 20)).thenReturn(targets);
        when(service.setEnabled(9L, 7L, false)).thenReturn(disabled);
        when(service.scan(eq(9L), any())).thenReturn(scan);
        CdpWarehouseExternalRealtimeJobProbeController controller =
                new CdpWarehouseExternalRealtimeJobProbeController(service, tenantResolver(9L, "alice"));

        R<List<CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView>> listResponse =
                controller.listTargets(true, 20).block();
        R<CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView> enabledResponse =
                controller.setEnabled(7L, false).block();
        R<CdpWarehouseExternalRealtimeJobProbeService.ScanSummary> scanResponse =
                controller.scan(7L, 20).block();

        assertThat(listResponse.getData()).isSameAs(targets);
        assertThat(enabledResponse.getData()).isSameAs(disabled);
        assertThat(scanResponse.getData()).isSameAs(scan);
        verify(service).listTargets(9L, true, 20);
        verify(service).setEnabled(9L, 7L, false);
        verify(service).scan(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                command.targetId().equals(7L) && command.limit() == 20));
    }

    private CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView targetView(boolean enabled) {
        return new CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView(
                7L,
                9L,
                "pipe",
                "job-a",
                "FLINK_REST",
                "http://flink/jobs/flink-1",
                null,
                "flink-1",
                null,
                "flink/jobs/flink-1",
                enabled,
                "data-platform",
                300,
                null,
                LocalDateTime.parse("2026-06-05T05:00:00"),
                "PASS",
                "ok",
                null,
                null);
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
