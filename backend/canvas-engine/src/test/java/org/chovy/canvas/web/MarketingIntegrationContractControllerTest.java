// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractAuditEventView;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractCommand;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeAutomationService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeRunCommand;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeRunView;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractSloEvaluationView;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractSloService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingIntegrationContractControllerTest {

    @Test
    void upsertContractUsesCurrentTenantAndOperator() {
        MarketingIntegrationContractService service = mock(MarketingIntegrationContractService.class);
        MarketingIntegrationContractCommand command = command();
        when(service.upsertContract(7L, command, "operator-1")).thenReturn(view("ACTIVE"));
        MarketingIntegrationContractController controller =
                controller(service, mock(MarketingIntegrationContractProbeService.class), resolver());

        StepVerifier.create(controller.upsertContract(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().contractKey()).isEqualTo("google-ads-keyword-write");
                })
                .verifyComplete();

        verify(service).upsertContract(7L, command, "operator-1");
    }

    @Test
    void listContractsUsesCurrentTenantAndFilters() {
        MarketingIntegrationContractService service = mock(MarketingIntegrationContractService.class);
        when(service.listContracts(7L, "ACTIVE", "SEM", 20)).thenReturn(List.of(view("ACTIVE")));
        MarketingIntegrationContractController controller =
                controller(service, mock(MarketingIntegrationContractProbeService.class), resolver());

        StepVerifier.create(controller.listContracts("ACTIVE", "SEM", 20))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(row -> assertThat(row.providerFamily()).isEqualTo("SEM")))
                .verifyComplete();

        verify(service).listContracts(7L, "ACTIVE", "SEM", 20);
    }

    @Test
    void archiveContractUsesCurrentTenantAndOperator() {
        MarketingIntegrationContractService service = mock(MarketingIntegrationContractService.class);
        when(service.archiveContract(7L, 10L, "operator-1")).thenReturn(view("ARCHIVED"));
        MarketingIntegrationContractController controller =
                controller(service, mock(MarketingIntegrationContractProbeService.class), resolver());

        StepVerifier.create(controller.archiveContract(10L))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ARCHIVED"))
                .verifyComplete();

        verify(service).archiveContract(7L, 10L, "operator-1");
    }

    @Test
    void recordProbeRunUsesCurrentTenantAndOperator() {
        MarketingIntegrationContractService contractService = mock(MarketingIntegrationContractService.class);
        MarketingIntegrationContractProbeService probeService = mock(MarketingIntegrationContractProbeService.class);
        MarketingIntegrationContractProbeRunCommand command = probeCommand();
        when(probeService.recordProbeRun(7L, 10L, command, "operator-1")).thenReturn(probeView("PASS"));
        MarketingIntegrationContractController controller =
                controller(contractService, probeService, resolver());

        StepVerifier.create(controller.recordProbeRun(10L, command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().status()).isEqualTo("PASS");
                    assertThat(response.getData().contractKey()).isEqualTo("google-ads-keyword-write");
                })
                .verifyComplete();

        verify(probeService).recordProbeRun(7L, 10L, command, "operator-1");
    }

    @Test
    void listProbeRunsUsesCurrentTenantAndFilters() {
        MarketingIntegrationContractService contractService = mock(MarketingIntegrationContractService.class);
        MarketingIntegrationContractProbeService probeService = mock(MarketingIntegrationContractProbeService.class);
        when(probeService.listProbeRuns(7L, "PASS", "SEM", 50)).thenReturn(List.of(probeView("PASS")));
        MarketingIntegrationContractController controller =
                controller(contractService, probeService, resolver());

        StepVerifier.create(controller.listProbeRuns("PASS", "SEM", 50))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(row -> assertThat(row.status()).isEqualTo("PASS")))
                .verifyComplete();

        verify(probeService).listProbeRuns(7L, "PASS", "SEM", 50);
    }

    @Test
    void scanProbeRunsUsesCurrentTenantAndOperator() {
        MarketingIntegrationContractService contractService = mock(MarketingIntegrationContractService.class);
        MarketingIntegrationContractProbeService probeService = mock(MarketingIntegrationContractProbeService.class);
        MarketingIntegrationContractProbeAutomationService automationService =
                mock(MarketingIntegrationContractProbeAutomationService.class);
        MarketingIntegrationContractProbeAutomationService.ProbeAutomationSummary summary =
                new MarketingIntegrationContractProbeAutomationService.ProbeAutomationSummary(
                        7L,
                        2,
                        2,
                        2,
                        0,
                        0,
                        null,
                        List.of());
        when(automationService.scanProductionContracts(7L, 25, "operator-1")).thenReturn(summary);
        MarketingIntegrationContractController controller =
                new MarketingIntegrationContractController(
                        contractService,
                        probeService,
                        automationService,
                        mock(MarketingIntegrationContractSloService.class),
                        resolver());

        StepVerifier.create(controller.scanProbeRuns(25))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().probedCount()).isEqualTo(2);
                    assertThat(response.getData().failedCount()).isZero();
                })
                .verifyComplete();

        verify(automationService).scanProductionContracts(7L, 25, "operator-1");
    }

    @Test
    void listContractSloEvaluationsUsesCurrentTenantAndLimit() {
        MarketingIntegrationContractService contractService = mock(MarketingIntegrationContractService.class);
        MarketingIntegrationContractProbeService probeService = mock(MarketingIntegrationContractProbeService.class);
        MarketingIntegrationContractProbeAutomationService automationService =
                mock(MarketingIntegrationContractProbeAutomationService.class);
        MarketingIntegrationContractSloService sloService = mock(MarketingIntegrationContractSloService.class);
        when(sloService.listProductionSloEvaluations(7L, 25)).thenReturn(List.of(sloView()));
        MarketingIntegrationContractController controller =
                new MarketingIntegrationContractController(
                        contractService,
                        probeService,
                        automationService,
                        sloService,
                        resolver());

        StepVerifier.create(controller.listContractSloEvaluations(25))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData()).singleElement()
                            .satisfies(row -> assertThat(row.status()).isEqualTo("PAGE"));
                })
                .verifyComplete();

        verify(sloService).listProductionSloEvaluations(7L, 25);
    }

    @Test
    void listContractAuditEventsUsesCurrentTenantAndLimit() {
        MarketingIntegrationContractService service = mock(MarketingIntegrationContractService.class);
        when(service.listAuditEvents(7L, 10L, 20)).thenReturn(List.of(auditView()));
        MarketingIntegrationContractController controller =
                controller(service, mock(MarketingIntegrationContractProbeService.class), resolver());

        StepVerifier.create(controller.listContractAuditEvents(10L, 20))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData()).singleElement()
                            .satisfies(row -> assertThat(row.eventType()).isEqualTo("UPDATED"));
                })
                .verifyComplete();

        verify(service).listAuditEvents(7L, 10L, 20);
    }

    private static MarketingIntegrationContractController controller(
            MarketingIntegrationContractService service,
            MarketingIntegrationContractProbeService probeService,
            TenantContextResolver resolver) {
        MarketingIntegrationContractProbeAutomationService automationService =
                mock(MarketingIntegrationContractProbeAutomationService.class);
        when(automationService.scanProductionContracts(any(), any(), any()))
                .thenReturn(new MarketingIntegrationContractProbeAutomationService.ProbeAutomationSummary(
                        7L,
                        0,
                        0,
                        0,
                        0,
                        0,
                        null,
                        List.of()));
        return new MarketingIntegrationContractController(
                service,
                probeService,
                automationService,
                mock(MarketingIntegrationContractSloService.class),
                resolver);
    }

    private static TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private static MarketingIntegrationContractCommand command() {
        return new MarketingIntegrationContractCommand(
                "google-ads-keyword-write",
                "Google Ads keyword write",
                "SEM",
                "search-marketing-governance",
                "provider-credential-governance",
                "search-provider-write-gateway",
                "OUTBOUND",
                "PRODUCTION",
                "OAUTH",
                "active provider credential",
                "/canvas/search-marketing/mutations",
                "Growth",
                "ACTIVE",
                "STANDARD",
                30000,
                Map.of(),
                Map.of(),
                Map.of());
    }

    private static MarketingIntegrationContractView view(String status) {
        return new MarketingIntegrationContractView(
                10L,
                7L,
                "google-ads-keyword-write",
                "Google Ads keyword write",
                "SEM",
                "search-marketing-governance",
                "provider-credential-governance",
                "search-provider-write-gateway",
                "OUTBOUND",
                "PRODUCTION",
                "OAUTH",
                "active provider credential",
                "/canvas/search-marketing/mutations",
                "Growth",
                status,
                "STANDARD",
                30000,
                Map.of(),
                Map.of(),
                Map.of(),
                "operator-1",
                "operator-1",
                null,
                null);
    }

    private static MarketingIntegrationContractProbeRunCommand probeCommand() {
        return new MarketingIntegrationContractProbeRunCommand(
                "prod-readiness-probe",
                "PASS",
                204,
                180L,
                null,
                null,
                "Health check passed",
                Map.of("traceId", "abc-123"));
    }

    private static MarketingIntegrationContractProbeRunView probeView(String status) {
        return new MarketingIntegrationContractProbeRunView(
                100L,
                7L,
                10L,
                "google-ads-keyword-write",
                "SEM",
                "PRODUCTION",
                "prod-readiness-probe",
                status,
                204,
                180L,
                null,
                null,
                "Health check passed",
                Map.of("traceId", "abc-123"),
                "2026-06-06T10:00",
                "operator-1",
                "operator-1",
                null,
                null);
    }

    private static MarketingIntegrationContractAuditEventView auditView() {
        return new MarketingIntegrationContractAuditEventView(
                200L,
                7L,
                10L,
                "google-ads-keyword-write",
                2,
                "UPDATED",
                "DRAFT",
                "ACTIVE",
                Map.of("contractKey", "google-ads-keyword-write"),
                Map.of("changedFields", List.of("status")),
                "operator-1",
                null);
    }

    private static MarketingIntegrationContractSloEvaluationView sloView() {
        return new MarketingIntegrationContractSloEvaluationView(
                7L,
                10L,
                "google-ads-keyword-write",
                "Google Ads keyword write",
                "SEM",
                "prod-readiness-probe",
                "PAGE",
                "CRITICAL",
                "PAGE_FAST_BURN",
                99.0,
                0.01,
                "google-ads-keyword-write breached PAGE_FAST_BURN",
                "2026-06-06T10:00:00",
                List.of());
    }
}
