package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeObservationDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeRunDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeObservationMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeRunMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingIntegrationContractProbeServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void recordProbeNormalizesAndPersistsRuntimeEvidence() {
        Harness harness = harness();
        when(harness.contractMapper.selectById(10L))
                .thenReturn(contract(10L, 7L, "google-ads-keyword-write", "PRODUCTION"));
        doAnswer(invocation -> {
            MarketingIntegrationContractProbeRunDO row = invocation.getArgument(0);
            row.setId(100L);
            return 1;
        }).when(harness.probeMapper).insert(any(MarketingIntegrationContractProbeRunDO.class));

        MarketingIntegrationContractProbeView view = harness.service.recordProbe(
                7L,
                10L,
                command("live connectivity", null, "pass"),
                "operator-1");

        assertThat(view.id()).isEqualTo(100L);
        assertThat(view.contractKey()).isEqualTo("google-ads-keyword-write");
        assertThat(view.probeKey()).isEqualTo("live-connectivity");
        assertThat(view.environment()).isEqualTo("PRODUCTION");
        assertThat(view.status()).isEqualTo("PASS");
        assertThat(view.observedAt()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        assertThat(view.evidence()).containsEntry("traceId", "trace-1");
        verify(harness.probeMapper).insert(argThat((MarketingIntegrationContractProbeRunDO row) ->
                row.getTenantId().equals(7L)
                        && row.getContractId().equals(10L)
                        && row.getContractKey().equals("google-ads-keyword-write")
                        && row.getProbeKey().equals("live-connectivity")
                        && row.getStatus().equals("PASS")
                        && row.getHttpStatusCode().equals(200)
                        && row.getLatencyMs().equals(123L)
                        && row.getEvidenceJson().contains("trace-1")
                        && row.getCreatedBy().equals("operator-1")));
    }

    @Test
    void recordProbeFallsBackToContractEnvironmentAndRejectsInvalidStatus() {
        Harness harness = harness();
        when(harness.contractMapper.selectById(10L))
                .thenReturn(contract(10L, 7L, "monitoring-webhook-ingest", "STAGING"));
        doAnswer(invocation -> {
            MarketingIntegrationContractProbeRunDO row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        }).when(harness.probeMapper).insert(any(MarketingIntegrationContractProbeRunDO.class));

        MarketingIntegrationContractProbeView view = harness.service.recordProbe(
                7L,
                10L,
                command("provider webhook", "", "warn"),
                "operator-1");

        assertThat(view.environment()).isEqualTo("STAGING");
        assertThat(view.status()).isEqualTo("WARN");
        assertThatThrownBy(() -> harness.service.recordProbe(
                7L,
                10L,
                command("provider webhook", "production", "healthy"),
                "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported integration probe status");
    }

    @Test
    void recordProbeUpdatesExistingProbeKeyInsteadOfDuplicatingLatestEvidence() {
        Harness harness = harness();
        when(harness.contractMapper.selectById(10L))
                .thenReturn(contract(10L, 7L, "google-ads-keyword-write", "PRODUCTION"));
        MarketingIntegrationContractProbeRunDO existing = probeRun(300L, 7L, 10L, "FAIL");
        existing.setProbeKey("prod-readiness-probe");
        when(harness.probeMapper.selectOne(any())).thenReturn(existing);

        MarketingIntegrationContractProbeView view = harness.service.recordProbe(
                7L,
                10L,
                command("prod-readiness-probe", null, "pass"),
                "operator-2");

        assertThat(view.id()).isEqualTo(300L);
        assertThat(view.status()).isEqualTo("PASS");
        verify(harness.probeMapper).updateById(argThat((MarketingIntegrationContractProbeRunDO row) ->
                row.getId().equals(300L)
                        && row.getStatus().equals("PASS")
                        && row.getUpdatedBy().equals("operator-2")));
        verify(harness.probeMapper, never()).insert(any(MarketingIntegrationContractProbeRunDO.class));
    }

    @Test
    void listContractProbesValidatesTenantOwnershipAndLimits() {
        Harness harness = harness();
        when(harness.contractMapper.selectById(10L))
                .thenReturn(contract(10L, 7L, "google-ads-keyword-write", "PRODUCTION"));
        when(harness.probeMapper.selectList(any()))
                .thenReturn(List.of(probeRun(100L, 7L, 10L, "PASS")));

        List<MarketingIntegrationContractProbeView> rows =
                harness.service.listContractProbes(7L, 10L, 500);

        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.id()).isEqualTo(100L);
                    assertThat(row.status()).isEqualTo("PASS");
                });

        when(harness.contractMapper.selectById(11L))
                .thenReturn(contract(11L, 8L, "foreign-contract", "PRODUCTION"));
        assertThatThrownBy(() -> harness.service.listContractProbes(7L, 11L, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("integration contract does not belong to tenant");
    }

    @Test
    void listRecentProbesCanFilterByNormalizedStatus() {
        Harness harness = harness();
        when(harness.probeMapper.selectList(any()))
                .thenReturn(List.of(probeRun(100L, 7L, 10L, "FAIL")));

        List<MarketingIntegrationContractProbeView> rows =
                harness.service.listRecentProbes(7L, "fail", 20);

        assertThat(rows).singleElement()
                .satisfies(row -> assertThat(row.status()).isEqualTo("FAIL"));
    }

    @Test
    void recordProbeRunUpsertsLatestProbeEvidence() {
        Harness harness = harness();
        when(harness.contractMapper.selectById(10L))
                .thenReturn(contract(10L, 7L, "google-ads-keyword-write", "PRODUCTION"));
        when(harness.probeMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            MarketingIntegrationContractProbeRunDO row = invocation.getArgument(0);
            row.setId(200L);
            return 1;
        }).when(harness.probeMapper).insert(any(MarketingIntegrationContractProbeRunDO.class));

        MarketingIntegrationContractProbeRunView view = harness.service.recordProbeRun(
                7L,
                10L,
                probeRunCommand(" production health ", "pass", 204, 180L),
                "operator-1");

        assertThat(view.id()).isEqualTo(200L);
        assertThat(view.contractKey()).isEqualTo("google-ads-keyword-write");
        assertThat(view.providerFamily()).isEqualTo("SEM");
        assertThat(view.probeKey()).isEqualTo("production-health");
        assertThat(view.status()).isEqualTo("PASS");
        assertThat(view.observedAt()).isEqualTo("2026-06-06T10:00");
        verify(harness.probeMapper).insert(argThat((MarketingIntegrationContractProbeRunDO row) ->
                row.getProviderFamily().equals("SEM")
                        && row.getProbeKey().equals("production-health")
                        && row.getStatus().equals("PASS")
                        && row.getHttpStatusCode().equals(204)
                        && row.getLatencyMs().equals(180L)
                        && row.getSummary().equals("Health check passed")
                        && row.getUpdatedBy().equals("operator-1")));
    }

    @Test
    void recordProbeRunSyncsProductionProbeStatusIntoAlerts() {
        Harness harness = harness();
        MarketingIntegrationContractDO contract =
                contract(10L, 7L, "google-ads-keyword-write", "PRODUCTION");
        when(harness.contractMapper.selectById(10L)).thenReturn(contract);
        when(harness.probeMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            MarketingIntegrationContractProbeRunDO row = invocation.getArgument(0);
            row.setId(201L);
            return 1;
        }).when(harness.probeMapper).insert(any(MarketingIntegrationContractProbeRunDO.class));

        MarketingIntegrationContractProbeRunView view = harness.service.recordProbeRun(
                7L,
                10L,
                probeRunCommand("production-health", "fail", 503, 900L),
                "operator-1");

        assertThat(view.status()).isEqualTo("FAIL");
        verify(harness.alertService).syncProbeResult(7L, contract, view, "operator-1");
    }

    @Test
    void recordProbeRunAppendsObservationHistoryAndSyncsSlo() {
        Harness harness = harness();
        MarketingIntegrationContractDO contract =
                contract(10L, 7L, "google-ads-keyword-write", "PRODUCTION");
        when(harness.contractMapper.selectById(10L)).thenReturn(contract);
        when(harness.probeMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            MarketingIntegrationContractProbeRunDO row = invocation.getArgument(0);
            row.setId(202L);
            return 1;
        }).when(harness.probeMapper).insert(any(MarketingIntegrationContractProbeRunDO.class));

        MarketingIntegrationContractProbeRunView view = harness.service.recordProbeRun(
                7L,
                10L,
                probeRunCommand("production-health", "fail", 503, 900L),
                "operator-1");

        assertThat(view.id()).isEqualTo(202L);
        verify(harness.observationMapper).insert(argThat((MarketingIntegrationContractProbeObservationDO row) ->
                row.getTenantId().equals(7L)
                        && row.getContractId().equals(10L)
                        && row.getProbeRunId().equals(202L)
                        && row.getContractKey().equals("google-ads-keyword-write")
                        && row.getProviderFamily().equals("SEM")
                        && row.getProbeKey().equals("production-health")
                        && row.getEnvironment().equals("PRODUCTION")
                        && row.getStatus().equals("FAIL")
                        && row.getHttpStatusCode().equals(503)
                        && row.getLatencyMs().equals(900L)
                        && row.getEvidenceJson().contains("trace-1")
                        && row.getCreatedBy().equals("operator-1")));
        verify(harness.sloService).evaluateAndSyncContract(
                7L,
                contract,
                "production-health",
                "operator-1");
    }

    @Test
    void recordProbeRunUpdatesExistingProbeKeyAndValidatesMetrics() {
        Harness harness = harness();
        when(harness.contractMapper.selectById(10L))
                .thenReturn(contract(10L, 7L, "google-ads-keyword-write", "PRODUCTION"));
        MarketingIntegrationContractProbeRunDO existing = probeRun(200L, 7L, 10L, "FAIL");
        existing.setProbeKey("production-health");
        when(harness.probeMapper.selectOne(any())).thenReturn(existing);

        MarketingIntegrationContractProbeRunView view = harness.service.recordProbeRun(
                7L,
                10L,
                probeRunCommand("production-health", "warn", 503, 900L),
                "operator-2");

        assertThat(view.id()).isEqualTo(200L);
        assertThat(view.status()).isEqualTo("WARN");
        verify(harness.probeMapper).updateById(argThat((MarketingIntegrationContractProbeRunDO row) ->
                row.getId().equals(200L)
                        && row.getStatus().equals("WARN")
                        && row.getUpdatedBy().equals("operator-2")));
        verify(harness.probeMapper, never()).insert(any(MarketingIntegrationContractProbeRunDO.class));

        assertThatThrownBy(() -> harness.service.recordProbeRun(
                7L,
                10L,
                probeRunCommand("production-health", "pass", 99, 100L),
                "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("httpStatusCode must be between");

        assertThatThrownBy(() -> harness.service.recordProbeRun(
                7L,
                10L,
                probeRunCommand("production-health", "pass", 200, -1L),
                "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latencyMs must be non-negative");
    }

    @Test
    void listProbeRunsFiltersByStatusAndProviderFamily() {
        Harness harness = harness();
        MarketingIntegrationContractProbeRunDO row = probeRun(200L, 7L, 10L, "FAIL");
        row.setProviderFamily("SEM");
        when(harness.probeMapper.selectList(any())).thenReturn(List.of(row));

        List<MarketingIntegrationContractProbeRunView> rows =
                harness.service.listProbeRuns(7L, "fail", "sem", 20);

        assertThat(rows).singleElement()
                .satisfies(view -> {
                    assertThat(view.status()).isEqualTo("FAIL");
                    assertThat(view.providerFamily()).isEqualTo("SEM");
                });
    }

    private static Harness harness() {
        MarketingIntegrationContractMapper contractMapper = mock(MarketingIntegrationContractMapper.class);
        MarketingIntegrationContractProbeRunMapper probeMapper =
                mock(MarketingIntegrationContractProbeRunMapper.class);
        MarketingIntegrationContractProbeObservationMapper observationMapper =
                mock(MarketingIntegrationContractProbeObservationMapper.class);
        MarketingIntegrationContractProbeAlertService alertService =
                mock(MarketingIntegrationContractProbeAlertService.class);
        MarketingIntegrationContractSloService sloService =
                mock(MarketingIntegrationContractSloService.class);
        return new Harness(
                contractMapper,
                probeMapper,
                observationMapper,
                alertService,
                sloService,
                new MarketingIntegrationContractProbeService(
                        contractMapper,
                        probeMapper,
                        observationMapper,
                        new ObjectMapper(),
                        alertService,
                        sloService,
                        CLOCK));
    }

    private static MarketingIntegrationContractProbeCommand command(
            String probeKey,
            String environment,
            String status) {
        return new MarketingIntegrationContractProbeCommand(
                probeKey,
                environment,
                status,
                200,
                123L,
                null,
                "https://errors.example.com/provider-connectivity",
                "Provider connectivity verified",
                "Provider endpoint returned 200",
                null,
                Map.of("traceId", "trace-1"));
    }

    private static MarketingIntegrationContractProbeRunCommand probeRunCommand(
            String probeKey,
            String status,
            Integer httpStatusCode,
            Long latencyMs) {
        return new MarketingIntegrationContractProbeRunCommand(
                probeKey,
                status,
                httpStatusCode,
                latencyMs,
                "urn:canvas:integration:health",
                status.equalsIgnoreCase("pass") ? null : "provider returned non-success status",
                "Health check passed",
                Map.of("traceId", "trace-1"));
    }

    private static MarketingIntegrationContractDO contract(
            Long id,
            Long tenantId,
            String contractKey,
            String environment) {
        MarketingIntegrationContractDO row = new MarketingIntegrationContractDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setContractKey(contractKey);
        row.setProviderFamily("SEM");
        row.setEnvironment(environment);
        return row;
    }

    private static MarketingIntegrationContractProbeRunDO probeRun(
            Long id,
            Long tenantId,
            Long contractId,
            String status) {
        MarketingIntegrationContractProbeRunDO row = new MarketingIntegrationContractProbeRunDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setContractId(contractId);
        row.setContractKey("google-ads-keyword-write");
        row.setProviderFamily("SEM");
        row.setProbeKey("live-connectivity");
        row.setEnvironment("PRODUCTION");
        row.setStatus(status);
        row.setObservedAt(LocalDateTime.parse("2026-06-06T10:00:00"));
        row.setEvidenceJson("{\"traceId\":\"trace-1\"}");
        row.setCreatedBy("operator-1");
        return row;
    }

    private record Harness(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeRunMapper probeMapper,
            MarketingIntegrationContractProbeObservationMapper observationMapper,
            MarketingIntegrationContractProbeAlertService alertService,
            MarketingIntegrationContractSloService sloService,
            MarketingIntegrationContractProbeService service) {
    }
}
