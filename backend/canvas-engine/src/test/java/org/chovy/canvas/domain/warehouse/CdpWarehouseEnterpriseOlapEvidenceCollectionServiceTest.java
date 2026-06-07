package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-06T02:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 2, 0);

    @Test
    void runPersistsPassCountsFromAutomatedEvidence() {
        CdpWarehouseEnterpriseOlapEvidenceService evidenceService =
                mock(CdpWarehouseEnterpriseOlapEvidenceService.class);
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper.class);
        when(evidenceService.collectAutomatedEvidence(9L, "scheduler"))
                .thenReturn(new CdpWarehouseEnterpriseOlapEvidenceService.EvidenceBundle(
                        9L,
                        "PASS",
                        NOW,
                        List.of(
                                evidence("doris_metrics", "PASS"),
                                evidence("workload_isolation", "PASS"),
                                evidence("query_slo", "PASS"),
                                evidence("compaction_health", "PASS"),
                                evidence("ingestion_replay", "PASS"))));
        when(mapper.insert(any(CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO.class)))
                .thenAnswer(invocation -> {
                    CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO row = invocation.getArgument(0);
                    row.setId(501L);
                    return 1;
                });
        CdpWarehouseEnterpriseOlapEvidenceCollectionService service =
                new CdpWarehouseEnterpriseOlapEvidenceCollectionService(evidenceService, mapper, CLOCK);

        CdpWarehouseEnterpriseOlapEvidenceCollectionService.CollectionRunView view =
                service.run(9L, "SCHEDULED", "scheduler");

        assertThat(view.id()).isEqualTo(501L);
        assertThat(view.status()).isEqualTo("PASS");
        assertThat(view.evidenceCount()).isEqualTo(5);
        assertThat(view.passCount()).isEqualTo(5);
        assertThat(view.failCount()).isZero();
        ArgumentCaptor<CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO> insertCaptor =
                ArgumentCaptor.forClass(CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO.class);
        verify(mapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getStatus()).isEqualTo("RUNNING");
        assertThat(insertCaptor.getValue().getStartedAt()).isEqualTo(NOW);
        verify(mapper).updateFinished(eq(501L), eq("PASS"), eq(NOW), eq(5), eq(5), eq(0), eq(0),
                org.mockito.ArgumentMatchers.contains("recorded 5"));
    }

    @Test
    void runPersistsFailWhenCollectorThrows() {
        CdpWarehouseEnterpriseOlapEvidenceService evidenceService =
                mock(CdpWarehouseEnterpriseOlapEvidenceService.class);
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper.class);
        when(evidenceService.collectAutomatedEvidence(9L, "scheduler"))
                .thenThrow(new IllegalStateException("Doris metrics unavailable"));
        when(mapper.insert(any(CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO.class)))
                .thenAnswer(invocation -> {
                    CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO row = invocation.getArgument(0);
                    row.setId(502L);
                    return 1;
                });
        CdpWarehouseEnterpriseOlapEvidenceCollectionService service =
                new CdpWarehouseEnterpriseOlapEvidenceCollectionService(evidenceService, mapper, CLOCK);

        CdpWarehouseEnterpriseOlapEvidenceCollectionService.CollectionRunView view =
                service.run(9L, "SCHEDULED", "scheduler");

        assertThat(view.status()).isEqualTo("FAIL");
        assertThat(view.reason()).contains("Doris metrics unavailable");
        verify(mapper).updateFinished(eq(502L), eq("FAIL"), eq(NOW), eq(0), eq(0), eq(0), eq(0),
                org.mockito.ArgumentMatchers.contains("Doris metrics unavailable"));
    }

    @Test
    void recentRunsAreTenantScopedAndBounded() {
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper.class);
        when(mapper.listRecent(9L, 20)).thenReturn(List.of(runRow(501L, 9L, "WARN")));
        CdpWarehouseEnterpriseOlapEvidenceCollectionService service =
                new CdpWarehouseEnterpriseOlapEvidenceCollectionService(
                        mock(CdpWarehouseEnterpriseOlapEvidenceService.class), mapper, CLOCK);

        List<CdpWarehouseEnterpriseOlapEvidenceCollectionService.CollectionRunView> runs =
                service.recentRuns(9L, 20);

        assertThat(runs).singleElement().satisfies(run -> {
            assertThat(run.id()).isEqualTo(501L);
            assertThat(run.tenantId()).isEqualTo(9L);
            assertThat(run.status()).isEqualTo("WARN");
        });
        verify(mapper).listRecent(9L, 20);
    }

    @Test
    void proofEvidenceFailsWhenLatestCollectionRunIsMissingOrStale() {
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper.class);
        when(mapper.listRecent(9L, 1)).thenReturn(List.of());
        CdpWarehouseEnterpriseOlapEvidenceCollectionService service =
                new CdpWarehouseEnterpriseOlapEvidenceCollectionService(
                        mock(CdpWarehouseEnterpriseOlapEvidenceService.class), mapper, CLOCK);

        CdpWarehouseProductionReadinessProofService.ProofEvidence missing = service.proofEvidence(9L);

        assertThat(missing.key()).isEqualTo("enterprise_olap:evidence_collection");
        assertThat(missing.status()).isEqualTo("FAIL");
        assertThat(missing.reason()).contains("missing");

        when(mapper.listRecent(9L, 1)).thenReturn(List.of(staleRunRow()));
        CdpWarehouseProductionReadinessProofService.ProofEvidence stale = service.proofEvidence(9L);

        assertThat(stale.status()).isEqualTo("FAIL");
        assertThat(stale.reason()).contains("expired");
    }

    @Test
    void proofEvidenceUsesFreshLatestCollectionRunStatus() {
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper.class);
        when(mapper.listRecent(9L, 1)).thenReturn(List.of(runRow(501L, 9L, "WARN")));
        CdpWarehouseEnterpriseOlapEvidenceCollectionService service =
                new CdpWarehouseEnterpriseOlapEvidenceCollectionService(
                        mock(CdpWarehouseEnterpriseOlapEvidenceService.class), mapper, CLOCK);

        CdpWarehouseProductionReadinessProofService.ProofEvidence evidence = service.proofEvidence(9L);

        assertThat(evidence.key()).isEqualTo("enterprise_olap:evidence_collection");
        assertThat(evidence.status()).isEqualTo("WARN");
        assertThat(evidence.reason()).contains("collection WARN");
    }

    private CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView evidence(String key, String status) {
        return new CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView(
                null,
                9L,
                key,
                "doris",
                status,
                key + " " + status,
                NOW,
                NOW.plusMinutes(5),
                "{}",
                "scheduler");
    }

    private CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO runRow(Long id, Long tenantId, String status) {
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO row =
                new CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setTriggerType("SCHEDULED");
        row.setStatus(status);
        row.setStartedAt(NOW.minusMinutes(1));
        row.setFinishedAt(NOW);
        row.setEvidenceCount(5);
        row.setPassCount(3);
        row.setWarnCount("WARN".equals(status) ? 1 : 0);
        row.setFailCount("FAIL".equals(status) ? 1 : 0);
        row.setReason("collection " + status);
        row.setCreatedBy("scheduler");
        row.setCreatedAt(NOW.minusMinutes(1));
        row.setUpdatedAt(NOW);
        return row;
    }

    private CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO staleRunRow() {
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO row = runRow(502L, 9L, "PASS");
        row.setFinishedAt(NOW.minusMinutes(20));
        return row;
    }
}
