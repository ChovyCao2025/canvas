package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildAutomationService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureExecutionService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehousePrivacyErasureControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 12, 0);

    @Test
    void createUsesCurrentTenantAndRequestBody() {
        CdpWarehousePrivacyErasureService service = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyErasureService.ErasureRequestCommand command =
                new CdpWarehousePrivacyErasureService.ErasureRequestCommand(
                        "dsr-1001", "USER_ID", "user-123456",
                        "GDPR delete", "privacy-ops", NOW.plusHours(2), List.of("CDP_USER_PROFILE"));
        CdpWarehousePrivacyErasureService.ErasureRequestView view = requestView(101L, "PENDING");
        when(service.create(9L, command)).thenReturn(view);
        CdpWarehousePrivacyErasureController controller =
                new CdpWarehousePrivacyErasureController(service, tenantResolver(9L));

        R<CdpWarehousePrivacyErasureService.ErasureRequestView> response =
                controller.create(command).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).create(9L, command);
    }

    @Test
    void recordProofUsesCurrentTenantAndRequestId() {
        CdpWarehousePrivacyErasureService service = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyErasureService.AssetProofCommand command =
                new CdpWarehousePrivacyErasureService.AssetProofCommand(
                        "CDP_USER_PROFILE", "CDP", "DELETE", "PASS",
                        1L, 1L, "removed", null, "privacy-ops", NOW);
        CdpWarehousePrivacyErasureService.ErasureRequestView view = requestView(101L, "PASS");
        when(service.recordAssetProof(9L, 101L, command)).thenReturn(view);
        CdpWarehousePrivacyErasureController controller =
                new CdpWarehousePrivacyErasureController(service, tenantResolver(9L));

        R<CdpWarehousePrivacyErasureService.ErasureRequestView> response =
                controller.recordProof(101L, command).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).recordAssetProof(9L, 101L, command);
    }

    @Test
    void executeUsesCurrentTenantRequestIdAndBody() {
        CdpWarehousePrivacyErasureService service = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyErasureExecutionService executionService =
                mock(CdpWarehousePrivacyErasureExecutionService.class);
        CdpWarehousePrivacyErasureExecutionService.ErasureExecutionCommand command =
                new CdpWarehousePrivacyErasureExecutionService.ErasureExecutionCommand(
                        "user-123456", false, "privacy-ops", List.of("CDP_USER_PROFILE"));
        CdpWarehousePrivacyErasureExecutionService.ErasureExecutionResult result =
                new CdpWarehousePrivacyErasureExecutionService.ErasureExecutionResult(
                        9L,
                        101L,
                        "PASS",
                        false,
                        List.of(new CdpWarehousePrivacyErasureExecutionService.AssetExecutionResult(
                                "CDP_USER_PROFILE", "PASS", 1, 1, "deleted rows", null)));
        when(executionService.execute(9L, 101L, command)).thenReturn(result);
        CdpWarehousePrivacyErasureController controller =
                new CdpWarehousePrivacyErasureController(service, executionService, tenantResolver(9L));

        R<CdpWarehousePrivacyErasureExecutionService.ErasureExecutionResult> response =
                controller.execute(101L, command).block();

        assertThat(response.getData()).isSameAs(result);
        verify(executionService).execute(9L, 101L, command);
    }

    @Test
    void rebuildAudienceBitmapsUsesCurrentTenantRequestIdAndBody() {
        CdpWarehousePrivacyErasureService service = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyErasureExecutionService executionService =
                mock(CdpWarehousePrivacyErasureExecutionService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand command =
                new CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand(
                        "privacy-ops", 50, List.of(12L, 13L));
        CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult result =
                new CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult(
                        9L,
                        101L,
                        "PASS",
                        false,
                        false,
                        2,
                        2,
                        0,
                        0,
                        List.of(new CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceRebuildItem(
                                12L, "SUCCESS", 10, "rebuilt audience 12")));
        when(rebuildService.rebuild(9L, 101L, command)).thenReturn(result);
        CdpWarehousePrivacyErasureController controller =
                new CdpWarehousePrivacyErasureController(
                        service, executionService, rebuildService, tenantResolver(9L));

        R<CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult> response =
                controller.rebuildAudienceBitmaps(101L, command).block();

        assertThat(response.getData()).isSameAs(result);
        verify(rebuildService).rebuild(9L, 101L, command);
    }

    @Test
    void runAudienceBitmapRebuildAutomationUsesCurrentTenantAndBody() {
        CdpWarehousePrivacyErasureService service = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyErasureExecutionService executionService =
                mock(CdpWarehousePrivacyErasureExecutionService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                        "privacy-ops", 25, 100, true);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult(
                        9L, "PASS", 2, 1, 1, 1, 0, List.of());
        when(automationService.run(9L, command)).thenReturn(result);
        CdpWarehousePrivacyErasureController controller =
                new CdpWarehousePrivacyErasureController(
                        service, executionService, rebuildService, automationService, tenantResolver(9L));

        R<CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult> response =
                controller.runAudienceBitmapRebuildAutomation(command).block();

        assertThat(response.getData()).isSameAs(result);
        verify(automationService).run(9L, command);
    }

    @Test
    void runAudienceBitmapRebuildAutomationRecordsHistoryWhenConfigured() {
        CdpWarehousePrivacyErasureService service = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyErasureExecutionService executionService =
                mock(CdpWarehousePrivacyErasureExecutionService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService runService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                        "privacy-ops", 25, 100, true);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult(
                        9L, "PASS", 2, 1, 1, 1, 0, List.of());
        when(runService.runAndRecord(9L, command, "MANUAL")).thenReturn(
                automationRunView(201L, "MANUAL", "PASS", result));
        CdpWarehousePrivacyErasureController controller =
                new CdpWarehousePrivacyErasureController(
                        service, executionService, rebuildService, automationService, runService, tenantResolver(9L));

        R<CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult> response =
                controller.runAudienceBitmapRebuildAutomation(command).block();

        assertThat(response.getData()).isSameAs(result);
        verify(runService).runAndRecord(9L, command, "MANUAL");
        verify(automationService, never()).run(9L, command);
    }

    @Test
    void audienceBitmapRebuildAutomationRunHistoryUsesCurrentTenant() {
        CdpWarehousePrivacyErasureService service = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService runService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult(
                        9L, "PASS", 2, 1, 1, 1, 0, List.of());
        List<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.AutomationRunView> rows =
                List.of(automationRunView(201L, "MANUAL", "PASS", result));
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.AutomationRunView row =
                automationRunView(201L, "MANUAL", "PASS", result);
        when(runService.recent(9L, 10)).thenReturn(rows);
        when(runService.get(9L, 201L)).thenReturn(row);
        CdpWarehousePrivacyErasureController controller =
                new CdpWarehousePrivacyErasureController(
                        service, null, null, automationService, runService, tenantResolver(9L));

        assertThat(controller.recentAudienceBitmapRebuildAutomationRuns(10).block().getData()).isSameAs(rows);
        assertThat(controller.getAudienceBitmapRebuildAutomationRun(201L).block().getData()).isSameAs(row);
        verify(runService).recent(9L, 10);
        verify(runService).get(9L, 201L);
    }

    @Test
    void recentGetAndSummaryUseCurrentTenant() {
        CdpWarehousePrivacyErasureService service = mock(CdpWarehousePrivacyErasureService.class);
        List<CdpWarehousePrivacyErasureService.ErasureRequestView> rows =
                List.of(requestView(101L, "RUNNING"));
        CdpWarehousePrivacyErasureService.ErasureRequestView row = requestView(101L, "RUNNING");
        CdpWarehousePrivacyErasureService.BacklogSummary summary =
                new CdpWarehousePrivacyErasureService.BacklogSummary(
                        9L, "WARN", 1, 0, 0, 1, NOW, "active requests");
        when(service.recent(9L, "RUNNING", 10)).thenReturn(rows);
        when(service.get(9L, 101L)).thenReturn(row);
        when(service.summary(9L)).thenReturn(summary);
        CdpWarehousePrivacyErasureController controller =
                new CdpWarehousePrivacyErasureController(service, tenantResolver(9L));

        assertThat(controller.recent("RUNNING", 10).block().getData()).isSameAs(rows);
        assertThat(controller.get(101L).block().getData()).isSameAs(row);
        assertThat(controller.summary().block().getData()).isSameAs(summary);
        verify(service).recent(9L, "RUNNING", 10);
        verify(service).get(9L, 101L);
        verify(service).summary(9L);
    }

    private CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.AutomationRunView automationRunView(
            Long id,
            String triggerSource,
            String status,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result) {
        return new CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.AutomationRunView(
                id,
                9L,
                triggerSource,
                status,
                "privacy-ops",
                25,
                100,
                true,
                result.scanned(),
                result.eligible(),
                result.triggered(),
                result.skipped(),
                result.failed(),
                "{\"status\":\"" + status + "\"}",
                null,
                NOW,
                NOW.plusMinutes(1),
                NOW,
                NOW.plusMinutes(1),
                result);
    }

    private CdpWarehousePrivacyErasureService.ErasureRequestView requestView(Long id, String status) {
        return new CdpWarehousePrivacyErasureService.ErasureRequestView(
                id,
                9L,
                "dsr-" + id,
                "USER_ID",
                "hash-" + id,
                "us***56",
                "GDPR delete",
                "privacy-ops",
                status,
                NOW.plusHours(2),
                NOW,
                "PASS".equals(status) ? NOW.plusMinutes(1) : null,
                "[]",
                "[]",
                List.of(),
                NOW,
                NOW);
    }

    private TenantContextResolver tenantResolver(Long tenantId) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", "operator")));
        return resolver;
    }
}
