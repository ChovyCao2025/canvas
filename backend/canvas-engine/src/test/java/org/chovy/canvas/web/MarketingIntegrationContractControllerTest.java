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
