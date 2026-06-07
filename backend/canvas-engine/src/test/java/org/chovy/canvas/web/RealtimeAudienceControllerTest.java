package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.cdp.RealtimeAudienceService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeAudienceControllerTest {

    @Test
    void endpointsUseCurrentTenantAndOperator() throws Exception {
        RealtimeAudienceService service = mock(RealtimeAudienceService.class);
        RealtimeAudienceController controller = new RealtimeAudienceController(
                tenantResolver(42L, "alice"),
                service);
        RealtimeAudienceController.RealtimeEventRequest request = new RealtimeAudienceController.RealtimeEventRequest(
                "evt-1", "u1", Instant.parse("2026-06-03T00:00:00Z"), Map.of("event", "Paid"), false);
        RealtimeAudienceService.CdpEvent event = request.toEvent();
        when(service.processEvent(42L, 10L, event, false))
                .thenReturn(new RealtimeAudienceService.EventResult("UPDATED", "ADD", 10L, "evt-1", "u1"));
        when(service.createSnapshot(42L, 10L, "MANUAL", "alice"))
                .thenReturn(new RealtimeAudienceService.SnapshotResult(10L, 3L, "audience:bitmap:10", "MANUAL"));
        when(service.overlap(10L, 11L))
                .thenReturn(new RealtimeAudienceService.OverlapResult(3L, 2L, 1L, 33.3, 50.0));
        when(service.merge(10L, 11L))
                .thenReturn(new RealtimeAudienceService.SetOperationResult("READY", "MERGE", 4L, 50_000L));
        when(service.exclude(10L, 11L))
                .thenReturn(new RealtimeAudienceService.SetOperationResult("READY", "EXCLUDE", 2L, 50_000L));
        when(service.listSnapshots(42L, 10L, 100)).thenReturn(List.of());

        assertThat(controller.processEvent(10L, request).block().getData().status()).isEqualTo("UPDATED");
        assertThat(controller.snapshot(10L).block().getData().estimatedSize()).isEqualTo(3);
        assertThat(controller.overlap(10L, 11L).block().getData().intersectionCount()).isEqualTo(1);
        assertThat(controller.merge(10L, 11L).block().getData().resultSize()).isEqualTo(4);
        assertThat(controller.exclude(10L, 11L).block().getData().resultSize()).isEqualTo(2);
        assertThat(controller.snapshots(10L, null).block().getData()).isEmpty();

        verify(service).processEvent(42L, 10L, event, false);
        verify(service).createSnapshot(42L, 10L, "MANUAL", "alice");
        verify(service).listSnapshots(42L, 10L, 100);
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
