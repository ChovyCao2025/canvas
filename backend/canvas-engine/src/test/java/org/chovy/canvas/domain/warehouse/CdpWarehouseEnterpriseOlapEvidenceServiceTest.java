package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseEnterpriseOlapEvidenceDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSyntheticDataPathProbeRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseEnterpriseOlapEvidenceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseEnterpriseOlapEvidenceServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T01:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 1, 0);

    @Test
    void proofEvidencePassesWhenLiveDorisOperatorLedgerAndSyntheticReplayAreHealthy() {
        CdpWarehouseEnterpriseOlapDorisEvidenceClient doris = mock(CdpWarehouseEnterpriseOlapDorisEvidenceClient.class);
        when(doris.metrics()).thenReturn(metrics(8, 0.01, 120, 0, 4));
        when(doris.querySlo()).thenReturn(healthyQuerySlo(NOW));
        when(doris.workloadGroups()).thenReturn(List.of(
                controlledGroup("bi", 20),
                controlledGroup("ingestion", 8),
                controlledGroup("audience", 6)));
        CdpWarehouseEnterpriseOlapEvidenceMapper mapper = mapper(List.of(
                ledger("backup_restore", "PASS", NOW.minusHours(2), NOW.plusDays(7)),
                ledger("runbook_drill", "PASS", NOW.minusHours(3), NOW.plusDays(7))));
        CdpWarehouseSyntheticDataPathProbeService probes = mock(CdpWarehouseSyntheticDataPathProbeService.class);
        when(probes.recent(9L, 10)).thenReturn(List.of(probe("PASS", NOW.minusMinutes(20))));
        CdpWarehouseEnterpriseOlapEvidenceService service =
                service(mapper, doris, probes);

        List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence = service.proofEvidence(9L);

        assertThat(evidence).extracting(CdpWarehouseProductionReadinessProofService.ProofEvidence::key)
                .containsExactly(
                        "enterprise_olap:doris_metrics",
                        "enterprise_olap:workload_isolation",
                        "enterprise_olap:query_slo",
                        "enterprise_olap:backup_restore",
                        "enterprise_olap:compaction_health",
                        "enterprise_olap:ingestion_replay",
                        "enterprise_olap:runbook_drill");
        assertThat(evidence).allSatisfy(row -> assertThat(row.status()).isEqualTo("PASS"));
    }

    @Test
    void failsClosedWhenDorisMetricsCollectorIsMissingAndOperatorEvidenceIsExpired() {
        CdpWarehouseEnterpriseOlapEvidenceMapper mapper = mapper(List.of(
                ledger("backup_restore", "PASS", NOW.minusDays(9), NOW.minusDays(1)),
                ledger("runbook_drill", "PASS", NOW.minusDays(9), NOW.minusDays(1))));
        CdpWarehouseEnterpriseOlapEvidenceService service =
                service(mapper, null, null);

        List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence = service.proofEvidence(9L);

        assertThat(evidence).filteredOn(row -> row.key().equals("enterprise_olap:doris_metrics"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("Doris evidence client is not configured");
                });
        assertThat(evidence).filteredOn(row -> row.key().equals("enterprise_olap:backup_restore"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("expired");
                });
        assertThat(evidence).filteredOn(row -> row.key().equals("enterprise_olap:ingestion_replay"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("missing");
                });
    }

    @Test
    void failsWorkloadIsolationWhenRequiredGroupsHaveNoExplicitControls() {
        CdpWarehouseEnterpriseOlapDorisEvidenceClient doris = mock(CdpWarehouseEnterpriseOlapDorisEvidenceClient.class);
        when(doris.metrics()).thenReturn(metrics(4, 0, 100, 0, 2));
        when(doris.querySlo()).thenReturn(healthyQuerySlo(NOW));
        when(doris.workloadGroups()).thenReturn(List.of(
                uncontrolledGroup("bi"),
                uncontrolledGroup("ingestion"),
                uncontrolledGroup("audience")));
        CdpWarehouseEnterpriseOlapEvidenceService service =
                service(mapper(List.of()), doris, null);

        List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence = service.proofEvidence(9L);

        assertThat(evidence).filteredOn(row -> row.key().equals("enterprise_olap:workload_isolation"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("no explicit resource controls");
                });
    }

    @Test
    void fallsBackToOperatorIngestionReplayWhenSyntheticProbeReadFails() {
        CdpWarehouseEnterpriseOlapDorisEvidenceClient doris = mock(CdpWarehouseEnterpriseOlapDorisEvidenceClient.class);
        when(doris.metrics()).thenReturn(metrics(8, 0.01, 120, 0, 4));
        when(doris.querySlo()).thenReturn(healthyQuerySlo(NOW));
        when(doris.workloadGroups()).thenReturn(List.of(
                controlledGroup("bi", 20),
                controlledGroup("ingestion", 8),
                controlledGroup("audience", 6)));
        CdpWarehouseEnterpriseOlapEvidenceMapper mapper = mapper(List.of(
                ledger("backup_restore", "PASS", NOW.minusHours(2), NOW.plusDays(7)),
                ledger("ingestion_replay", "PASS", NOW.minusHours(1), NOW.plusDays(1)),
                ledger("runbook_drill", "PASS", NOW.minusHours(3), NOW.plusDays(7))));
        CdpWarehouseSyntheticDataPathProbeService probes = mock(CdpWarehouseSyntheticDataPathProbeService.class);
        when(probes.recent(9L, 10)).thenThrow(new IllegalStateException("probe table unavailable"));
        CdpWarehouseEnterpriseOlapEvidenceService service =
                service(mapper, doris, probes);

        List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence = service.proofEvidence(9L);

        assertThat(evidence).filteredOn(row -> row.key().equals("enterprise_olap:ingestion_replay"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("PASS");
                    assertThat(row.reason()).contains("ingestion_replay PASS");
                });
    }

    @Test
    void operatorEvidenceRejectsUnsupportedKeysAndPersistsSupportedRows() {
        CdpWarehouseEnterpriseOlapEvidenceMapper mapper = mapper(List.of());
        CdpWarehouseEnterpriseOlapEvidenceService service = service(mapper, null, null);

        CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView view = service.recordOperatorEvidence(9L,
                new CdpWarehouseEnterpriseOlapEvidenceService.EvidenceCommand(
                        "backup_restore",
                        "PASS",
                        "repository snapshot restored into validation cluster",
                        NOW.minusMinutes(10),
                        NOW.plusDays(7),
                        "{\"repository\":\"s3-prod\"}"),
                "ops");

        assertThat(view.evidenceKey()).isEqualTo("backup_restore");
        assertThat(view.status()).isEqualTo("PASS");
        ArgumentCaptor<CdpWarehouseEnterpriseOlapEvidenceDO> row =
                ArgumentCaptor.forClass(CdpWarehouseEnterpriseOlapEvidenceDO.class);
        verify(mapper).insert(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getSource()).isEqualTo("operator");
        assertThat(row.getValue().getCreatedBy()).isEqualTo("ops");

        assertThatThrownBy(() -> service.recordOperatorEvidence(9L,
                new CdpWarehouseEnterpriseOlapEvidenceService.EvidenceCommand(
                        "doris_metrics", "PASS", "not allowed", NOW, NOW.plusHours(1), "{}"),
                "ops"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operator evidence key");
    }

    @Test
    void automatedCollectionPersistsOnlyAutomatedEvidenceRows() {
        CdpWarehouseEnterpriseOlapDorisEvidenceClient doris = mock(CdpWarehouseEnterpriseOlapDorisEvidenceClient.class);
        when(doris.metrics()).thenReturn(metrics(8, 0.01, 120, 0, 4));
        when(doris.querySlo()).thenReturn(healthyQuerySlo(NOW));
        when(doris.workloadGroups()).thenReturn(List.of(
                controlledGroup("bi", 20),
                controlledGroup("ingestion", 8),
                controlledGroup("audience", 6)));
        CdpWarehouseEnterpriseOlapEvidenceMapper mapper = mapper(List.of(
                ledger("backup_restore", "PASS", NOW.minusHours(2), NOW.plusDays(7)),
                ledger("runbook_drill", "PASS", NOW.minusHours(3), NOW.plusDays(7))));
        CdpWarehouseSyntheticDataPathProbeService probes = mock(CdpWarehouseSyntheticDataPathProbeService.class);
        when(probes.recent(9L, 10)).thenReturn(List.of(probe("PASS", NOW.minusMinutes(20))));
        CdpWarehouseEnterpriseOlapEvidenceService service = service(mapper, doris, probes);

        CdpWarehouseEnterpriseOlapEvidenceService.EvidenceBundle bundle =
                service.collectAutomatedEvidence(9L, "enterprise-olap-evidence-scheduler");

        assertThat(bundle.status()).isEqualTo("PASS");
        assertThat(bundle.evidence()).extracting(CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView::evidenceKey)
                .containsExactly("doris_metrics", "workload_isolation", "query_slo", "compaction_health",
                        "ingestion_replay");
        ArgumentCaptor<CdpWarehouseEnterpriseOlapEvidenceDO> row =
                ArgumentCaptor.forClass(CdpWarehouseEnterpriseOlapEvidenceDO.class);
        verify(mapper, org.mockito.Mockito.times(5)).insert(row.capture());
        assertThat(row.getAllValues()).extracting(CdpWarehouseEnterpriseOlapEvidenceDO::getEvidenceKey)
                .containsExactly("doris_metrics", "workload_isolation", "query_slo", "compaction_health",
                        "ingestion_replay");
        assertThat(row.getAllValues()).extracting(CdpWarehouseEnterpriseOlapEvidenceDO::getCreatedBy)
                .containsOnly("enterprise-olap-evidence-scheduler");
    }

    @Test
    void querySloWarnsWhenRepresentativeProfilesEnterLatencyWarningBand() {
        CdpWarehouseEnterpriseOlapDorisEvidenceClient doris = mock(CdpWarehouseEnterpriseOlapDorisEvidenceClient.class);
        when(doris.metrics()).thenReturn(metrics(8, 0.01, 120, 0, 4));
        when(doris.querySlo()).thenReturn(List.of(
                querySlo("bi_dashboard", "bi", 20, 0, 2_100, 3_000, 100, 256_000_000L, NOW),
                querySlo("audience_materialization", "audience", 12, 0, 1_500, 2_500, 100, 256_000_000L, NOW),
                querySlo("ad_hoc_segment", "bi", 10, 0, 1_400, 2_400, 100, 256_000_000L, NOW)));
        when(doris.workloadGroups()).thenReturn(List.of(
                controlledGroup("bi", 20),
                controlledGroup("ingestion", 8),
                controlledGroup("audience", 6)));
        CdpWarehouseEnterpriseOlapEvidenceService service = service(mapper(List.of()), doris, null);

        List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence = service.proofEvidence(9L);

        assertThat(evidence).filteredOn(row -> row.key().equals("enterprise_olap:query_slo"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("WARN");
                    assertThat(row.reason()).contains("p95 latency");
                });
    }

    @Test
    void querySloFailsClosedWhenRequiredProfileIsMissingOrStale() {
        CdpWarehouseEnterpriseOlapDorisEvidenceClient doris = mock(CdpWarehouseEnterpriseOlapDorisEvidenceClient.class);
        when(doris.metrics()).thenReturn(metrics(8, 0.01, 120, 0, 4));
        when(doris.querySlo()).thenReturn(List.of(
                querySlo("bi_dashboard", "bi", 20, 0, 900, 1_200, 100, 256_000_000L, NOW),
                querySlo("audience_materialization", "audience", 20, 0, 900, 1_200, 100, 256_000_000L,
                        NOW.minusMinutes(20))));
        when(doris.workloadGroups()).thenReturn(List.of(
                controlledGroup("bi", 20),
                controlledGroup("ingestion", 8),
                controlledGroup("audience", 6)));
        CdpWarehouseEnterpriseOlapEvidenceService service = service(mapper(List.of()), doris, null);

        List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence = service.proofEvidence(9L);

        assertThat(evidence).filteredOn(row -> row.key().equals("enterprise_olap:query_slo"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("missing query SLO profiles ad_hoc_segment")
                            .contains("stale query SLO profiles audience_materialization");
                });
    }

    @Test
    void querySloFailsWhenRepresentativeProfileBreachesHardPolicy() {
        CdpWarehouseEnterpriseOlapDorisEvidenceClient doris = mock(CdpWarehouseEnterpriseOlapDorisEvidenceClient.class);
        when(doris.metrics()).thenReturn(metrics(8, 0.01, 120, 0, 4));
        when(doris.querySlo()).thenReturn(List.of(
                querySlo("bi_dashboard", "bi", 20, 1, 900, 1_200, 100, 256_000_000L, NOW),
                querySlo("audience_materialization", "audience", 20, 0, 900, 1_200, 100, 256_000_000L, NOW),
                querySlo("ad_hoc_segment", "bi", 20, 0, 900, 1_200, 100, 256_000_000L, NOW)));
        when(doris.workloadGroups()).thenReturn(List.of(
                controlledGroup("bi", 20),
                controlledGroup("ingestion", 8),
                controlledGroup("audience", 6)));
        CdpWarehouseEnterpriseOlapEvidenceService service = service(mapper(List.of()), doris, null);

        List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence = service.proofEvidence(9L);

        assertThat(evidence).filteredOn(row -> row.key().equals("enterprise_olap:query_slo"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("error rate");
                });
    }

    @Test
    void querySloFailsWhenLatencyQueueOrMemoryBreachesHardPolicy() {
        assertQuerySloHardFailure(
                querySlo("bi_dashboard", "bi", 20, 0, 900, 8_000, 100, 256_000_000L, NOW),
                "p99 latency");
        assertQuerySloHardFailure(
                querySlo("bi_dashboard", "bi", 20, 0, 900, 1_200, 3_000, 256_000_000L, NOW),
                "queue wait");
        assertQuerySloHardFailure(
                querySlo("bi_dashboard", "bi", 20, 0, 900, 1_200, 100, 4L * 1024L * 1024L * 1024L, NOW),
                "peak memory");
        assertQuerySloHardFailure(
                querySlo("bi_dashboard", "bi", 4, 0, 900, 1_200, 100, 256_000_000L, NOW),
                "sample count");
    }

    private CdpWarehouseEnterpriseOlapEvidenceService service(
            CdpWarehouseEnterpriseOlapEvidenceMapper mapper,
            CdpWarehouseEnterpriseOlapDorisEvidenceClient doris,
            CdpWarehouseSyntheticDataPathProbeService probes) {
        return new CdpWarehouseEnterpriseOlapEvidenceService(
                mapper,
                provider(doris),
                provider(probes),
                CLOCK);
    }

    private CdpWarehouseEnterpriseOlapEvidenceMapper mapper(List<CdpWarehouseEnterpriseOlapEvidenceDO> rows) {
        CdpWarehouseEnterpriseOlapEvidenceMapper mapper = mock(CdpWarehouseEnterpriseOlapEvidenceMapper.class);
        when(mapper.listRecent(9L, 100)).thenReturn(rows);
        when(mapper.insert(any(CdpWarehouseEnterpriseOlapEvidenceDO.class)))
                .thenAnswer(invocation -> {
            CdpWarehouseEnterpriseOlapEvidenceDO row = invocation.getArgument(0);
            row.setId(77L);
            return 1;
        });
        return mapper;
    }

    private CdpWarehouseEnterpriseOlapEvidenceDO ledger(
            String key,
            String status,
            LocalDateTime measuredAt,
            LocalDateTime expiresAt) {
        CdpWarehouseEnterpriseOlapEvidenceDO row = new CdpWarehouseEnterpriseOlapEvidenceDO();
        row.setId((long) key.hashCode());
        row.setTenantId(9L);
        row.setEvidenceKey(key);
        row.setSource("operator");
        row.setStatus(status);
        row.setReason(key + " " + status);
        row.setMeasuredAt(measuredAt);
        row.setExpiresAt(expiresAt);
        row.setEvidenceJson("{}");
        row.setCreatedBy("ops");
        return row;
    }

    private CdpWarehouseEnterpriseOlapDorisEvidenceClient.DorisMetricsEvidence metrics(
            double compactionScore,
            double errRate,
            double latencyMs,
            double queueSize,
            double tabletCount) {
        return new CdpWarehouseEnterpriseOlapDorisEvidenceClient.DorisMetricsEvidence(
                NOW,
                List.of(
                        endpoint("FE", Map.of(
                                "doris_fe_qps", 12.0,
                                "doris_fe_query_err_rate", errRate,
                                "doris_fe_query_latency_ms", latencyMs,
                                "doris_fe_max_tablet_compaction_score", compactionScore,
                                "doris_fe_tablet_num", tabletCount)),
                        endpoint("BE", Map.of(
                                "doris_be_query_scan_bytes_per_second", 200.0,
                                "fragment_thread_pool_queue_size", queueSize,
                                "doris_be_tablet_base_max_compaction_score", compactionScore,
                                "doris_be_tablet_cumulative_max_compaction_score", compactionScore,
                                "doris_be_max_disk_io_util_percent", 20.0))));
    }

    private CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence endpoint(
            String role,
            Map<String, Double> metrics) {
        return new CdpWarehouseEnterpriseOlapDorisEvidenceClient.MetricsEndpointEvidence(
                "http://" + role.toLowerCase() + ":8030/metrics",
                role,
                NOW,
                metrics);
    }

    private CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence controlledGroup(
            String name,
            int maxConcurrency) {
        return new CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence(
                name,
                10.0,
                80.0,
                10.0,
                80.0,
                maxConcurrency,
                100,
                30_000L,
                104_857_600L,
                104_857_600L);
    }

    private CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence uncontrolledGroup(String name) {
        return new CdpWarehouseEnterpriseOlapDorisEvidenceClient.WorkloadGroupEvidence(
                name,
                null,
                null,
                null,
                null,
                Integer.MAX_VALUE,
                0,
                0L,
                -1L,
                -1L);
    }

    private void assertQuerySloHardFailure(
            CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence failingProfile,
            String reason) {
        CdpWarehouseEnterpriseOlapDorisEvidenceClient doris = mock(CdpWarehouseEnterpriseOlapDorisEvidenceClient.class);
        when(doris.metrics()).thenReturn(metrics(8, 0.01, 120, 0, 4));
        when(doris.querySlo()).thenReturn(List.of(
                failingProfile,
                querySlo("audience_materialization", "audience", 20, 0, 900, 1_200, 100, 256_000_000L, NOW),
                querySlo("ad_hoc_segment", "bi", 20, 0, 900, 1_200, 100, 256_000_000L, NOW)));
        when(doris.workloadGroups()).thenReturn(List.of(
                controlledGroup("bi", 20),
                controlledGroup("ingestion", 8),
                controlledGroup("audience", 6)));
        CdpWarehouseEnterpriseOlapEvidenceService service = service(mapper(List.of()), doris, null);

        List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence = service.proofEvidence(9L);

        assertThat(evidence).filteredOn(row -> row.key().equals("enterprise_olap:query_slo"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains(reason);
                });
    }

    private List<CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence> healthyQuerySlo(
            LocalDateTime measuredAt) {
        return List.of(
                querySlo("bi_dashboard", "bi", 20, 0, 900, 1_200, 100, 256_000_000L, measuredAt),
                querySlo("audience_materialization", "audience", 20, 0, 1_000, 1_300, 100, 256_000_000L,
                        measuredAt),
                querySlo("ad_hoc_segment", "bi", 20, 0, 1_100, 1_500, 100, 256_000_000L, measuredAt));
    }

    private CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence querySlo(
            String profileKey,
            String workloadGroup,
            long sampleCount,
            long errorCount,
            double p95LatencyMs,
            double p99LatencyMs,
            double maxQueueWaitMs,
            long maxPeakMemoryBytes,
            LocalDateTime measuredAt) {
        return new CdpWarehouseEnterpriseOlapDorisEvidenceClient.QuerySloEvidence(
                profileKey,
                workloadGroup,
                sampleCount,
                errorCount,
                p95LatencyMs,
                p99LatencyMs,
                maxQueueWaitMs,
                maxPeakMemoryBytes,
                measuredAt);
    }

    private CdpWarehouseSyntheticDataPathProbeService.ProbeRunView probe(String status, LocalDateTime finishedAt) {
        return new CdpWarehouseSyntheticDataPathProbeService.ProbeRunView(
                1L,
                9L,
                "enterprise-olap",
                "MYSQL_CDC",
                "warehouse-probe-1",
                "__warehouse_probe__",
                "__warehouse_probe_user_1",
                true,
                status,
                status,
                status,
                status,
                "PASS".equals(status) ? 1L : 0L,
                finishedAt.minusSeconds(5),
                finishedAt,
                null,
                "[]",
                finishedAt.minusSeconds(5),
                finishedAt);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
