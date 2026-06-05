package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.analytics.AudienceMaterializationOperationsService;
import org.chovy.canvas.domain.analytics.AudienceMaterializationScheduleService;
import org.chovy.canvas.domain.analytics.AudienceMaterializationService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
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

class CdpWarehouseAudienceMaterializationControllerTest {

    @Test
    void materializeUsesCurrentTenantAndExplicitOperator() {
        AudienceMaterializationOperationsService service = mock(AudienceMaterializationOperationsService.class);
        AudienceMaterializationService.MaterializationResult result =
                new AudienceMaterializationService.MaterializationResult("SUCCESS", 42L);
        when(service.materialize(9L, 12L, "qa")).thenReturn(result);
        CdpWarehouseAudienceMaterializationController.MaterializeReq req =
                new CdpWarehouseAudienceMaterializationController.MaterializeReq();
        req.setOperator("qa");
        CdpWarehouseAudienceMaterializationController controller =
                new CdpWarehouseAudienceMaterializationController(
                        service, tenantResolver(9L, "TENANT_ADMIN", "alice"));

        R<AudienceMaterializationService.MaterializationResult> response =
                controller.materialize(12L, req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).materialize(9L, 12L, "qa");
    }

    @Test
    void materializeFallsBackToCurrentUsername() {
        AudienceMaterializationOperationsService service = mock(AudienceMaterializationOperationsService.class);
        AudienceMaterializationService.MaterializationResult result =
                new AudienceMaterializationService.MaterializationResult("FAILED", 0L);
        when(service.materialize(9L, 12L, "alice")).thenReturn(result);
        CdpWarehouseAudienceMaterializationController controller =
                new CdpWarehouseAudienceMaterializationController(
                        service, tenantResolver(9L, "TENANT_ADMIN", "alice"));

        R<AudienceMaterializationService.MaterializationResult> response =
                controller.materialize(12L, null).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).materialize(9L, 12L, "alice");
    }

    @Test
    void materializeGatedDelegatesCurrentTenantWindowAndAllowWarn() {
        AudienceMaterializationOperationsService service = mock(AudienceMaterializationOperationsService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T03:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T04:00:00");
        AudienceMaterializationOperationsService.GatedMaterializationResult result =
                new AudienceMaterializationOperationsService.GatedMaterializationResult(
                        9L,
                        12L,
                        "TRIGGERED",
                        "warehouse availability PASS",
                        null,
                        new AudienceMaterializationService.MaterializationResult("SUCCESS", 42L));
        when(service.materializeWithAvailabilityGate(9L, 12L, from, to, "HYBRID", true, "qa"))
                .thenReturn(result);
        CdpWarehouseAudienceMaterializationController.GatedMaterializeReq req =
                new CdpWarehouseAudienceMaterializationController.GatedMaterializeReq();
        req.setFrom(from);
        req.setTo(to);
        req.setMode("HYBRID");
        req.setAllowWarn(true);
        req.setOperator("qa");
        CdpWarehouseAudienceMaterializationController controller =
                new CdpWarehouseAudienceMaterializationController(
                        service, tenantResolver(9L, "TENANT_ADMIN", "alice"));

        R<AudienceMaterializationOperationsService.GatedMaterializationResult> response =
                controller.materializeGated(12L, req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).materializeWithAvailabilityGate(9L, 12L, from, to, "HYBRID", true, "qa");
    }

    @Test
    void materializeContractGatedDelegatesCurrentTenantWindowAndContract() {
        AudienceMaterializationOperationsService service = mock(AudienceMaterializationOperationsService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T03:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T04:00:00");
        AudienceMaterializationOperationsService.ContractGatedMaterializationResult result =
                new AudienceMaterializationOperationsService.ContractGatedMaterializationResult(
                        9L,
                        12L,
                        "audience_12",
                        "TRIGGERED",
                        "consumer availability PASS",
                        consumerAvailability("audience_12", "PASS", true, from, to),
                        new AudienceMaterializationService.MaterializationResult("SUCCESS", 42L));
        when(service.materializeWithConsumerAvailabilityContract(9L, 12L, "audience_12", from, to, "qa"))
                .thenReturn(result);
        CdpWarehouseAudienceMaterializationController.ContractGatedMaterializeReq req =
                new CdpWarehouseAudienceMaterializationController.ContractGatedMaterializeReq();
        req.setContractKey("audience_12");
        req.setFrom(from);
        req.setTo(to);
        req.setOperator("qa");
        CdpWarehouseAudienceMaterializationController controller =
                new CdpWarehouseAudienceMaterializationController(
                        service, tenantResolver(9L, "TENANT_ADMIN", "alice"));

        R<AudienceMaterializationOperationsService.ContractGatedMaterializationResult> response =
                controller.materializeContractGated(12L, req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(service).materializeWithConsumerAvailabilityContract(
                9L, 12L, "audience_12", from, to, "qa");
    }


    @Test
    void recentRunsUsesCurrentTenantAndFilters() {
        AudienceMaterializationOperationsService service = mock(AudienceMaterializationOperationsService.class);
        List<AudienceMaterializationOperationsService.RunView> runs = List.of(run());
        when(service.recentRuns(9L, 12L, "SUCCESS", 50)).thenReturn(runs);
        CdpWarehouseAudienceMaterializationController controller =
                new CdpWarehouseAudienceMaterializationController(
                        service, tenantResolver(9L, "TENANT_ADMIN", "alice"));

        R<List<AudienceMaterializationOperationsService.RunView>> response =
                controller.recentRuns(12L, "SUCCESS", 50).block();

        assertThat(response.getData()).isSameAs(runs);
        verify(service).recentRuns(9L, 12L, "SUCCESS", 50);
    }

    @Test
    void rollbackUsesCurrentTenantAndUsernameWhenOperatorMissing() {
        AudienceMaterializationOperationsService service = mock(AudienceMaterializationOperationsService.class);
        AudienceMaterializationOperationsService.RollbackView view =
                new AudienceMaterializationOperationsService.RollbackView(
                        301L,
                        9L,
                        12L,
                        2L,
                        "audience:bitmap:12:v:2",
                        3,
                        "SUCCESS",
                        "bad materialization",
                        "alice",
                        LocalDateTime.parse("2026-06-05T05:00:00"));
        when(service.rollback(9L, 12L, 2L, "alice", "bad materialization")).thenReturn(view);
        CdpWarehouseAudienceMaterializationController.RollbackReq req =
                new CdpWarehouseAudienceMaterializationController.RollbackReq();
        req.setTargetVersion(2L);
        req.setReason("bad materialization");
        CdpWarehouseAudienceMaterializationController controller =
                new CdpWarehouseAudienceMaterializationController(
                        service, tenantResolver(9L, "TENANT_ADMIN", "alice"));

        R<AudienceMaterializationOperationsService.RollbackView> response =
                controller.rollback(12L, req).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).rollback(9L, 12L, 2L, "alice", "bad materialization");
    }

    @Test
    void refreshDueUsesCurrentTenantAndExplicitOperator() {
        AudienceMaterializationOperationsService operations = mock(AudienceMaterializationOperationsService.class);
        AudienceMaterializationScheduleService schedule = mock(AudienceMaterializationScheduleService.class);
        AudienceMaterializationScheduleService.ScheduledRefreshResult result =
                new AudienceMaterializationScheduleService.ScheduledRefreshResult(
                        9L, 3, 2, 1, 1, 1, LocalDateTime.parse("2026-06-05T05:00:00"));
        when(schedule.refreshDue(eq(9L), any(LocalDateTime.class), eq(10), eq("qa"))).thenReturn(result);
        CdpWarehouseAudienceMaterializationController.RefreshDueReq req =
                new CdpWarehouseAudienceMaterializationController.RefreshDueReq();
        req.setLimit(10);
        req.setOperator("qa");
        CdpWarehouseAudienceMaterializationController controller =
                new CdpWarehouseAudienceMaterializationController(
                        operations, tenantResolver(9L, "TENANT_ADMIN", "alice"), schedule);

        R<AudienceMaterializationScheduleService.ScheduledRefreshResult> response =
                controller.refreshDue(req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(schedule).refreshDue(eq(9L), any(LocalDateTime.class), eq(10), eq("qa"));
    }

    @Test
    void refreshDueGatedUsesCurrentTenantWindowModeAndOperator() {
        AudienceMaterializationOperationsService operations = mock(AudienceMaterializationOperationsService.class);
        AudienceMaterializationScheduleService schedule = mock(AudienceMaterializationScheduleService.class);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        LocalDateTime from = LocalDateTime.parse("2026-06-05T04:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T05:00:00");
        AudienceMaterializationScheduleService.ScheduledRefreshResult refreshResult =
                new AudienceMaterializationScheduleService.ScheduledRefreshResult(
                        9L, 3, 2, 2, 0, 1, now);
        AudienceMaterializationScheduleService.GatedScheduledRefreshResult result =
                new AudienceMaterializationScheduleService.GatedScheduledRefreshResult(
                        9L,
                        "EXECUTED",
                        "warehouse availability PASS",
                        availability("PASS", from, to),
                        refreshResult);
        when(schedule.refreshDueWithAvailabilityGate(9L, now, 10, "qa", from, to, "HYBRID", true))
                .thenReturn(result);
        CdpWarehouseAudienceMaterializationController.GatedRefreshDueReq req =
                new CdpWarehouseAudienceMaterializationController.GatedRefreshDueReq();
        req.setNow(now);
        req.setFrom(from);
        req.setTo(to);
        req.setMode("HYBRID");
        req.setAllowWarn(true);
        req.setLimit(10);
        req.setOperator("qa");
        CdpWarehouseAudienceMaterializationController controller =
                new CdpWarehouseAudienceMaterializationController(
                        operations, tenantResolver(9L, "TENANT_ADMIN", "alice"), schedule);

        R<AudienceMaterializationScheduleService.GatedScheduledRefreshResult> response =
                controller.refreshDueGated(req).block();

        assertThat(response.getData()).isSameAs(result);
        verify(schedule).refreshDueWithAvailabilityGate(9L, now, 10, "qa", from, to, "HYBRID", true);
    }

    private AudienceMaterializationOperationsService.RunView run() {
        return new AudienceMaterializationOperationsService.RunView(
                101L,
                9L,
                12L,
                3L,
                "SUCCESS",
                42L,
                "audience:bitmap:12:v:3",
                null,
                LocalDateTime.parse("2026-06-05T03:00:00"),
                LocalDateTime.parse("2026-06-05T03:01:00"),
                "qa");
    }

    private TenantContextResolver tenantResolver(Long tenantId, String role, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, role, username)));
        return resolver;
    }

    private CdpWarehouseAvailabilityService.AvailabilityDecision availability(String status,
                                                                             LocalDateTime from,
                                                                             LocalDateTime to) {
        return new CdpWarehouseAvailabilityService.AvailabilityDecision(
                9L,
                "HYBRID",
                from,
                to,
                to,
                status,
                List.of(new CdpWarehouseAvailabilityService.AvailabilityGate(
                        "offline_aggregate",
                        status,
                        "test availability " + status,
                        to,
                        0L,
                        1)));
    }

    private CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation consumerAvailability(
            String contractKey,
            String status,
            boolean allowed,
            LocalDateTime from,
            LocalDateTime to) {
        return new CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation(
                9L,
                contractKey,
                "AUDIENCE",
                "12",
                "HYBRID",
                from,
                to,
                to,
                status,
                allowed,
                "BLOCK_ON_WARN",
                availability(status, from, to),
                List.of(),
                "consumer availability " + status);
    }
}
