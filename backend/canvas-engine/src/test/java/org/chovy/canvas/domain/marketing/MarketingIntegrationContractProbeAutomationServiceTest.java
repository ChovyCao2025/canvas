package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeRunDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeRunMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingIntegrationContractProbeAutomationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void scanProductionContractsExecutesClientAndPersistsPassAndFailEvidence() {
        MarketingIntegrationContractMapper contractMapper = mock(MarketingIntegrationContractMapper.class);
        MarketingIntegrationContractProbeService probeService = mock(MarketingIntegrationContractProbeService.class);
        MarketingIntegrationContractProbeClient probeClient = mock(MarketingIntegrationContractProbeClient.class);
        MarketingIntegrationContractProbeAutomationService service =
                new MarketingIntegrationContractProbeAutomationService(contractMapper, probeService, probeClient, CLOCK);
        MarketingIntegrationContractDO google = contract(10L, "google-ads-keyword-write", "SEM", "ACTIVE", "PRODUCTION");
        MarketingIntegrationContractDO creator = contract(20L, "creator-auth-refresh", "CREATOR", "ACTIVE", "PRODUCTION");
        when(contractMapper.selectList(any())).thenReturn(List.of(google, creator));
        when(probeClient.probe(argThat(target ->
                target != null && "google-ads-keyword-write".equals(target.contractKey()))))
                .thenReturn(new MarketingIntegrationContractProbeClient.ProbeResult(
                        "PASS",
                        204,
                        180L,
                        null,
                        null,
                        "Provider health endpoint passed",
                        Map.of("transport", "http")));
        when(probeClient.probe(argThat(target ->
                target != null && "creator-auth-refresh".equals(target.contractKey()))))
                .thenThrow(new IllegalStateException("credential refresh endpoint timed out"));
        when(probeService.recordProbeRun(
                any(),
                any(),
                any(MarketingIntegrationContractProbeRunCommand.class),
                any()))
                .thenAnswer(invocation -> probeView(
                        invocation.getArgument(1),
                        invocation.getArgument(2, MarketingIntegrationContractProbeRunCommand.class).status()));

        MarketingIntegrationContractProbeAutomationService.ProbeAutomationSummary summary =
                service.scanProductionContracts(7L, 50, "probe-scheduler");

        assertThat(summary.tenantId()).isEqualTo(7L);
        assertThat(summary.candidateCount()).isEqualTo(2);
        assertThat(summary.probedCount()).isEqualTo(2);
        assertThat(summary.passedCount()).isEqualTo(1);
        assertThat(summary.failedCount()).isEqualTo(1);
        assertThat(summary.skippedCount()).isZero();
        assertThat(summary.evaluatedAt()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        assertThat(summary.results()).hasSize(2);
        verify(probeService).recordProbeRun(
                eq(7L),
                eq(10L),
                argThat(command -> "prod-readiness-probe".equals(command.probeKey())
                        && "PASS".equals(command.status())
                        && Integer.valueOf(204).equals(command.httpStatusCode())
                        && Long.valueOf(180L).equals(command.latencyMs())
                        && "http".equals(command.evidence().get("transport"))),
                eq("probe-scheduler"));
        verify(probeService).recordProbeRun(
                eq(7L),
                eq(20L),
                argThat(command -> "prod-readiness-probe".equals(command.probeKey())
                        && "FAIL".equals(command.status())
                        && command.problemTypeUri().equals("urn:canvas:marketing-integration:probe-failure")
                        && command.errorMessage().contains("credential refresh endpoint timed out")
                        && "IllegalStateException".equals(command.evidence().get("exceptionType"))),
                eq("probe-scheduler"));
    }

    @Test
    void scanProductionContractsFiltersToActiveProductionContractsAndBoundsLimit() {
        MarketingIntegrationContractMapper contractMapper = mock(MarketingIntegrationContractMapper.class);
        MarketingIntegrationContractProbeService probeService = mock(MarketingIntegrationContractProbeService.class);
        MarketingIntegrationContractProbeClient probeClient = mock(MarketingIntegrationContractProbeClient.class);
        MarketingIntegrationContractProbeAutomationService service =
                new MarketingIntegrationContractProbeAutomationService(contractMapper, probeService, probeClient, CLOCK);
        when(contractMapper.selectList(any())).thenReturn(List.of(
                contract(10L, "prod-active-1", "SEM", "ACTIVE", "PRODUCTION"),
                contract(11L, "prod-active-2", "SEM", "ACTIVE", "PRODUCTION"),
                contract(12L, "draft-contract", "SEM", "DRAFT", "PRODUCTION"),
                contract(13L, "staging-contract", "SEM", "ACTIVE", "STAGING")));
        when(probeClient.probe(any())).thenReturn(new MarketingIntegrationContractProbeClient.ProbeResult(
                "PASS",
                200,
                90L,
                null,
                null,
                "Provider health endpoint passed",
                Map.of()));
        when(probeService.recordProbeRun(
                any(),
                any(),
                any(MarketingIntegrationContractProbeRunCommand.class),
                any()))
                .thenAnswer(invocation -> probeView(
                        invocation.getArgument(1),
                        invocation.getArgument(2, MarketingIntegrationContractProbeRunCommand.class).status()));

        MarketingIntegrationContractProbeAutomationService.ProbeAutomationSummary summary =
                service.scanProductionContracts(7L, 1, "operator-1");

        assertThat(summary.candidateCount()).isEqualTo(4);
        assertThat(summary.probedCount()).isEqualTo(1);
        assertThat(summary.skippedCount()).isEqualTo(3);
        verify(probeClient, times(1)).probe(argThat(target ->
                target != null && "prod-active-1".equals(target.contractKey())));
    }

    @Test
    void scanProductionContractsSyncsProbeResultsIntoMonitoringAlerts() {
        MarketingIntegrationContractMapper contractMapper = mock(MarketingIntegrationContractMapper.class);
        MarketingIntegrationContractProbeRunMapper probeMapper =
                mock(MarketingIntegrationContractProbeRunMapper.class);
        MarketingIntegrationContractProbeClient probeClient = mock(MarketingIntegrationContractProbeClient.class);
        MarketingIntegrationContractProbeAlertService alertService =
                mock(MarketingIntegrationContractProbeAlertService.class);
        MarketingIntegrationContractProbeService probeService =
                new MarketingIntegrationContractProbeService(
                        contractMapper,
                        probeMapper,
                        new ObjectMapper(),
                        alertService,
                        CLOCK);
        MarketingIntegrationContractProbeAutomationService service =
                new MarketingIntegrationContractProbeAutomationService(contractMapper, probeService, probeClient, CLOCK);
        MarketingIntegrationContractDO google = contract(10L, "google-ads-keyword-write", "SEM", "ACTIVE", "PRODUCTION");
        MarketingIntegrationContractDO creator = contract(20L, "creator-auth-refresh", "CREATOR", "ACTIVE", "PRODUCTION");
        when(contractMapper.selectList(any())).thenReturn(List.of(google, creator));
        when(contractMapper.selectById(10L)).thenReturn(google);
        when(contractMapper.selectById(20L)).thenReturn(creator);
        when(probeMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            MarketingIntegrationContractProbeRunDO row = invocation.getArgument(0);
            row.setId(100L + row.getContractId());
            return 1;
        }).when(probeMapper).insert(any(MarketingIntegrationContractProbeRunDO.class));
        when(probeClient.probe(argThat(target ->
                target != null && "google-ads-keyword-write".equals(target.contractKey()))))
                .thenReturn(new MarketingIntegrationContractProbeClient.ProbeResult(
                        "PASS",
                        204,
                        180L,
                        null,
                        null,
                        "Provider health endpoint passed",
                        Map.of("transport", "http")));
        when(probeClient.probe(argThat(target ->
                target != null && "creator-auth-refresh".equals(target.contractKey()))))
                .thenThrow(new IllegalStateException("credential refresh endpoint timed out"));

        service.scanProductionContracts(7L, 50, "probe-scheduler");

        verify(alertService).syncProbeResult(
                eq(7L),
                eq(google),
                argThat(view -> view != null && "PASS".equals(view.status())),
                eq("probe-scheduler"));
        verify(alertService).syncProbeResult(
                eq(7L),
                eq(creator),
                argThat(view -> view != null && "FAIL".equals(view.status())),
                eq("probe-scheduler"));
    }

    private static MarketingIntegrationContractDO contract(
            Long id,
            String contractKey,
            String providerFamily,
            String status,
            String environment) {
        MarketingIntegrationContractDO row = new MarketingIntegrationContractDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setContractKey(contractKey);
        row.setDisplayName(contractKey);
        row.setProviderFamily(providerFamily);
        row.setSourceCapabilityKey("search-marketing-governance");
        row.setTargetCapabilityKey("provider-credential-governance");
        row.setAssetKey("marketing-integration-contract-registry");
        row.setDirection("OUTBOUND");
        row.setEnvironment(environment);
        row.setAuthMode("OAUTH");
        row.setCredentialDependency("active provider credential");
        row.setApiRoot("https://provider.example.com/health");
        row.setOwnerTeam("Growth");
        row.setStatus(status);
        row.setSlaTier("STANDARD");
        row.setTimeoutMs(30000);
        row.setRetryPolicyJson("{}");
        row.setSchemaContractJson("{}");
        row.setMetadataJson("{\"probePath\":\"/health\"}");
        return row;
    }

    private static MarketingIntegrationContractProbeRunView probeView(Long contractId, String status) {
        return new MarketingIntegrationContractProbeRunView(
                100L + contractId,
                7L,
                contractId,
                "contract-" + contractId,
                "SEM",
                "PRODUCTION",
                "prod-readiness-probe",
                status,
                "PASS".equals(status) ? 204 : null,
                "PASS".equals(status) ? 180L : null,
                null,
                "PASS".equals(status) ? null : "credential refresh endpoint timed out",
                "PASS".equals(status) ? "Provider health endpoint passed" : "Automatic probe failed",
                Map.of(),
                "2026-06-06T10:00",
                "probe-scheduler",
                "probe-scheduler",
                null,
                null);
    }
}
