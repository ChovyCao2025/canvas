package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobControlService;
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

class CdpWarehouseRealtimeJobControllerTest {

    @Test
    void heartbeatUsesCurrentTenantAndBody() {
        CdpWarehouseRealtimeJobControlService service = mock(CdpWarehouseRealtimeJobControlService.class);
        CdpWarehouseRealtimeJobControlService.JobInstanceView view = jobView("PASS");
        when(service.heartbeat(eq(9L), any())).thenReturn(view);
        CdpWarehouseRealtimeJobController.HeartbeatReq req =
                new CdpWarehouseRealtimeJobController.HeartbeatReq();
        req.setPipelineKey("pipe");
        req.setJobKey("job-a");
        req.setEngineType("flink");
        req.setRuntimeStatus("running");
        CdpWarehouseRealtimeJobController controller =
                new CdpWarehouseRealtimeJobController(service, tenantResolver(9L, "alice"));

        R<CdpWarehouseRealtimeJobControlService.JobInstanceView> response =
                controller.heartbeat(req).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).heartbeat(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "pipe".equals(command.pipelineKey())
                        && "job-a".equals(command.jobKey())
                        && "flink".equals(command.engineType())));
    }

    @Test
    void statusUsesCurrentTenantAndFilters() {
        CdpWarehouseRealtimeJobControlService service = mock(CdpWarehouseRealtimeJobControlService.class);
        CdpWarehouseRealtimeJobControlService.JobStatusSummary summary =
                new CdpWarehouseRealtimeJobControlService.JobStatusSummary(9L, 1, 1, 0, 0, List.of(jobView("PASS")));
        when(service.status(9L, "pipe", 120, 20)).thenReturn(summary);
        CdpWarehouseRealtimeJobController controller =
                new CdpWarehouseRealtimeJobController(service, tenantResolver(9L, "alice"));

        R<CdpWarehouseRealtimeJobControlService.JobStatusSummary> response =
                controller.status("pipe", 120, 20).block();

        assertThat(response.getData()).isSameAs(summary);
        verify(service).status(9L, "pipe", 120, 20);
    }

    @Test
    void requestActionFallsBackToCurrentUsername() {
        CdpWarehouseRealtimeJobControlService service = mock(CdpWarehouseRealtimeJobControlService.class);
        CdpWarehouseRealtimeJobControlService.JobActionView view = actionView("PENDING");
        when(service.requestAction(eq(9L), any(), eq("alice"))).thenReturn(view);
        CdpWarehouseRealtimeJobController.ActionReq req =
                new CdpWarehouseRealtimeJobController.ActionReq();
        req.setPipelineKey("pipe");
        req.setJobKey("job-a");
        req.setAction("PAUSE");
        req.setReason("maintenance");
        CdpWarehouseRealtimeJobController controller =
                new CdpWarehouseRealtimeJobController(service, tenantResolver(9L, "alice"));

        R<CdpWarehouseRealtimeJobControlService.JobActionView> response =
                controller.requestAction(req).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).requestAction(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "pipe".equals(command.pipelineKey())
                        && "job-a".equals(command.jobKey())
                        && "PAUSE".equals(command.action())), eq("alice"));
    }

    @Test
    void pendingAckAndCompleteUseCurrentTenant() {
        CdpWarehouseRealtimeJobControlService service = mock(CdpWarehouseRealtimeJobControlService.class);
        List<CdpWarehouseRealtimeJobControlService.JobActionView> pending = List.of(actionView("PENDING"));
        CdpWarehouseRealtimeJobControlService.JobActionView ack = actionView("ACKNOWLEDGED");
        CdpWarehouseRealtimeJobControlService.JobActionView completed = actionView("COMPLETED");
        when(service.pendingActions(9L, "pipe", "job-a", 10)).thenReturn(pending);
        when(service.acknowledge(9L, 7L)).thenReturn(ack);
        when(service.complete(9L, 7L, "COMPLETED", "done")).thenReturn(completed);
        CdpWarehouseRealtimeJobController controller =
                new CdpWarehouseRealtimeJobController(service, tenantResolver(9L, "alice"));
        CdpWarehouseRealtimeJobController.CompleteReq completeReq =
                new CdpWarehouseRealtimeJobController.CompleteReq();
        completeReq.setStatus("COMPLETED");
        completeReq.setResultMessage("done");

        R<List<CdpWarehouseRealtimeJobControlService.JobActionView>> pendingResponse =
                controller.pendingActions("pipe", "job-a", 10).block();
        R<CdpWarehouseRealtimeJobControlService.JobActionView> ackResponse =
                controller.acknowledge(7L).block();
        R<CdpWarehouseRealtimeJobControlService.JobActionView> completeResponse =
                controller.complete(7L, completeReq).block();

        assertThat(pendingResponse.getData()).isSameAs(pending);
        assertThat(ackResponse.getData()).isSameAs(ack);
        assertThat(completeResponse.getData()).isSameAs(completed);
        verify(service).pendingActions(9L, "pipe", "job-a", 10);
        verify(service).acknowledge(9L, 7L);
        verify(service).complete(9L, 7L, "COMPLETED", "done");
    }

    private CdpWarehouseRealtimeJobControlService.JobInstanceView jobView(String healthStatus) {
        return new CdpWarehouseRealtimeJobControlService.JobInstanceView(
                1L,
                9L,
                "pipe",
                "job-a",
                "FLINK",
                "flink-1",
                "deployment/flink-1",
                "RUNNING",
                "RUNNING",
                LocalDateTime.parse("2026-06-05T05:00:00"),
                "{}",
                null,
                "data-platform",
                healthStatus,
                List.of(),
                null,
                null);
    }

    private CdpWarehouseRealtimeJobControlService.JobActionView actionView(String status) {
        return new CdpWarehouseRealtimeJobControlService.JobActionView(
                7L,
                9L,
                "pipe",
                "job-a",
                "PAUSE",
                status,
                "alice",
                "maintenance",
                LocalDateTime.parse("2026-06-05T05:00:00"),
                null,
                null,
                null);
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
