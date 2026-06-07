package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseProductionReadinessProofServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 6, 5, 10, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 6, 5, 11, 0);

    @Test
    void proofPassesWhenReadinessAvailabilityAndContractsPass() {
        CdpWarehouseReadinessService readiness = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseAvailabilityService availability = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService contracts = mock(CdpWarehouseConsumerAvailabilityService.class);
        when(readiness.readiness(9L)).thenReturn(readinessSummary("PASS"));
        when(availability.evaluate(9L, FROM, TO, "HYBRID")).thenReturn(availabilityDecision("PASS"));
        when(contracts.evaluateContract(9L, "bi_daily_active_users", FROM, TO))
                .thenReturn(contractEvaluation("bi_daily_active_users", "PASS", true));
        when(contracts.evaluateContract(9L, "audience_12", FROM, TO))
                .thenReturn(contractEvaluation("audience_12", "PASS", true));
        CdpWarehouseProductionReadinessProofService service =
                new CdpWarehouseProductionReadinessProofService(readiness, availability, contracts);

        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                service.proof(9L, FROM, TO, "HYBRID", List.of("bi_daily_active_users", "audience_12"));

        assertThat(proof.status()).isEqualTo("PASS");
        assertThat(proof.tenantId()).isEqualTo(9L);
        assertThat(proof.windowStart()).isEqualTo(FROM);
        assertThat(proof.windowEnd()).isEqualTo(TO);
        assertThat(proof.mode()).isEqualTo("HYBRID");
        assertThat(proof.evidence()).extracting(CdpWarehouseProductionReadinessProofService.ProofEvidence::key)
                .containsExactly("warehouse_readiness", "window_availability",
                        "consumer_contract:bi_daily_active_users", "consumer_contract:audience_12");
        assertThat(proof.contracts()).hasSize(2);
        verify(readiness).readiness(9L);
        verify(availability).evaluate(9L, FROM, TO, "HYBRID");
        verify(contracts).evaluateContract(9L, "bi_daily_active_users", FROM, TO);
        verify(contracts).evaluateContract(9L, "audience_12", FROM, TO);
    }

    @Test
    void proofWarnsWhenNoContractKeysAreProvided() {
        CdpWarehouseReadinessService readiness = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseAvailabilityService availability = mock(CdpWarehouseAvailabilityService.class);
        when(readiness.readiness(9L)).thenReturn(readinessSummary("PASS"));
        when(availability.evaluate(9L, FROM, TO, "OFFLINE")).thenReturn(availabilityDecision("PASS"));
        CdpWarehouseProductionReadinessProofService service =
                new CdpWarehouseProductionReadinessProofService(
                        readiness,
                        availability,
                        (CdpWarehouseConsumerAvailabilityService) null);

        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                service.proof(9L, FROM, TO, "OFFLINE", List.of());

        assertThat(proof.status()).isEqualTo("WARN");
        assertThat(proof.evidence()).extracting(CdpWarehouseProductionReadinessProofService.ProofEvidence::key)
                .contains("consumer_contracts");
        assertThat(proof.evidence()).filteredOn(row -> row.key().equals("consumer_contracts"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("WARN");
                    assertThat(row.reason()).contains("no consumer contracts requested");
                });
    }

    @Test
    void proofFailsWhenRequestedContractIsBlocked() {
        CdpWarehouseReadinessService readiness = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseAvailabilityService availability = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService contracts = mock(CdpWarehouseConsumerAvailabilityService.class);
        when(readiness.readiness(9L)).thenReturn(readinessSummary("PASS"));
        when(availability.evaluate(9L, FROM, TO, "HYBRID")).thenReturn(availabilityDecision("PASS"));
        when(contracts.evaluateContract(9L, "audience_12", FROM, TO))
                .thenReturn(contractEvaluation("audience_12", "WARN", false));
        CdpWarehouseProductionReadinessProofService service =
                new CdpWarehouseProductionReadinessProofService(readiness, availability, contracts);

        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                service.proof(9L, FROM, TO, "HYBRID", List.of("audience_12"));

        assertThat(proof.status()).isEqualTo("FAIL");
        assertThat(proof.contracts()).singleElement().satisfies(contract -> {
            assertThat(contract.contractKey()).isEqualTo("audience_12");
            assertThat(contract.allowed()).isFalse();
            assertThat(contract.status()).isEqualTo("FAIL");
            assertThat(contract.reason()).contains("blocked");
        });
    }

    @Test
    void proofFailsClosedWhenContractsAreRequestedButServiceIsMissing() {
        CdpWarehouseReadinessService readiness = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseAvailabilityService availability = mock(CdpWarehouseAvailabilityService.class);
        when(readiness.readiness(9L)).thenReturn(readinessSummary("PASS"));
        when(availability.evaluate(9L, FROM, TO, "HYBRID")).thenReturn(availabilityDecision("PASS"));
        CdpWarehouseProductionReadinessProofService service =
                new CdpWarehouseProductionReadinessProofService(
                        readiness,
                        availability,
                        (CdpWarehouseConsumerAvailabilityService) null);

        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                service.proof(9L, FROM, TO, "HYBRID", List.of("audience_12"));

        assertThat(proof.status()).isEqualTo("FAIL");
        assertThat(proof.evidence()).filteredOn(row -> row.key().equals("consumer_contract:audience_12"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("not configured");
                });
    }

    @Test
    void proofIncludesPrivacyErasureBacklogEvidenceWhenConfigured() {
        CdpWarehouseReadinessService readiness = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseAvailabilityService availability = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService contracts = mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehousePrivacyErasureService privacy = mock(CdpWarehousePrivacyErasureService.class);
        when(readiness.readiness(9L)).thenReturn(readinessSummary("PASS"));
        when(availability.evaluate(9L, FROM, TO, "HYBRID")).thenReturn(availabilityDecision("PASS"));
        when(contracts.evaluateContract(9L, "audience_12", FROM, TO))
                .thenReturn(contractEvaluation("audience_12", "PASS", true));
        when(privacy.summary(9L)).thenReturn(new CdpWarehousePrivacyErasureService.BacklogSummary(
                9L,
                "WARN",
                1,
                0,
                0,
                1,
                TO.plusMinutes(1),
                "privacy erasure backlog has active requests"));
        CdpWarehouseProductionReadinessProofService service =
                new CdpWarehouseProductionReadinessProofService(
                        readiness,
                        availability,
                        contracts,
                        provider(privacy));

        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                service.proof(9L, FROM, TO, "HYBRID", List.of("audience_12"));

        assertThat(proof.status()).isEqualTo("WARN");
        assertThat(proof.privacyErasureBacklog()).isNotNull();
        assertThat(proof.evidence()).filteredOn(row -> row.key().equals("privacy_erasure_backlog"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("WARN");
                    assertThat(row.reason()).contains("active requests");
        });
        verify(privacy).summary(9L);
    }

    @Test
    void proofFailsWhenPrivacyErasureBacklogFails() {
        CdpWarehouseReadinessService readiness = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseAvailabilityService availability = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService contracts = mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehousePrivacyErasureService privacy = mock(CdpWarehousePrivacyErasureService.class);
        when(readiness.readiness(9L)).thenReturn(readinessSummary("PASS"));
        when(availability.evaluate(9L, FROM, TO, "HYBRID")).thenReturn(availabilityDecision("PASS"));
        when(contracts.evaluateContract(9L, "audience_12", FROM, TO))
                .thenReturn(contractEvaluation("audience_12", "PASS", true));
        when(privacy.summary(9L)).thenReturn(new CdpWarehousePrivacyErasureService.BacklogSummary(
                9L,
                "FAIL",
                2,
                1,
                1,
                1,
                TO.plusMinutes(1),
                "privacy erasure backlog has failed or overdue requests"));
        CdpWarehouseProductionReadinessProofService service =
                new CdpWarehouseProductionReadinessProofService(
                        readiness,
                        availability,
                        contracts,
                        provider(privacy));

        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                service.proof(9L, FROM, TO, "HYBRID", List.of("audience_12"));

        assertThat(proof.status()).isEqualTo("FAIL");
        assertThat(proof.privacyErasureBacklog()).isNotNull();
        assertThat(proof.evidence()).filteredOn(row -> row.key().equals("privacy_erasure_backlog"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("failed or overdue");
                });
        verify(privacy).summary(9L);
    }

    @Test
    void proofFailsClosedWhenPrivacyErasureBacklogSummaryThrows() {
        CdpWarehouseReadinessService readiness = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseAvailabilityService availability = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService contracts = mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehousePrivacyErasureService privacy = mock(CdpWarehousePrivacyErasureService.class);
        when(readiness.readiness(9L)).thenReturn(readinessSummary("PASS"));
        when(availability.evaluate(9L, FROM, TO, "HYBRID")).thenReturn(availabilityDecision("PASS"));
        when(contracts.evaluateContract(9L, "audience_12", FROM, TO))
                .thenReturn(contractEvaluation("audience_12", "PASS", true));
        when(privacy.summary(9L)).thenThrow(new IllegalStateException("ledger unavailable"));
        CdpWarehouseProductionReadinessProofService service =
                new CdpWarehouseProductionReadinessProofService(
                        readiness,
                        availability,
                        contracts,
                        provider(privacy));

        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                service.proof(9L, FROM, TO, "HYBRID", List.of("audience_12"));

        assertThat(proof.status()).isEqualTo("FAIL");
        assertThat(proof.privacyErasureBacklog()).isNull();
        assertThat(proof.evidence()).filteredOn(row -> row.key().equals("privacy_erasure_backlog"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("ledger unavailable");
        });
        verify(privacy).summary(9L);
    }

    @Test
    void proofIncludesEnterpriseOlapReadinessWhenConfigured() {
        CdpWarehouseReadinessService readiness = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseAvailabilityService availability = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService contracts = mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehouseEnterpriseOlapReadinessService enterprise =
                mock(CdpWarehouseEnterpriseOlapReadinessService.class);
        when(readiness.readiness(9L)).thenReturn(readinessSummary("PASS"));
        when(availability.evaluate(9L, FROM, TO, "HYBRID")).thenReturn(availabilityDecision("PASS"));
        when(contracts.evaluateContract(9L, "audience_12", FROM, TO))
                .thenReturn(contractEvaluation("audience_12", "PASS", true));
        when(enterprise.evaluateFromProductionEvidence(eq(9L), anyList()))
                .thenReturn(enterpriseReadiness("FAIL"));
        CdpWarehouseProductionReadinessProofService service =
                new CdpWarehouseProductionReadinessProofService(
                        readiness,
                        availability,
                        contracts,
                        null,
                        provider(enterprise));

        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                service.proof(9L, FROM, TO, "HYBRID", List.of("audience_12"));

        assertThat(proof.status()).isEqualTo("FAIL");
        assertThat(proof.enterpriseOlapReadiness()).isNotNull();
        assertThat(proof.enterpriseOlapReadiness().status()).isEqualTo("FAIL");
        assertThat(proof.evidence()).filteredOn(row -> row.key().equals("enterprise_olap_readiness"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("missing critical gates");
                });
        ArgumentCaptor<List<CdpWarehouseProductionReadinessProofService.ProofEvidence>> evidenceCaptor =
                ArgumentCaptor.captor();
        verify(enterprise).evaluateFromProductionEvidence(eq(9L), evidenceCaptor.capture());
        assertThat(evidenceCaptor.getValue())
                .extracting(CdpWarehouseProductionReadinessProofService.ProofEvidence::key)
                .contains("warehouse_readiness", "window_availability", "consumer_contract:audience_12");
    }

    @Test
    void proofAddsOperationalEnterpriseOlapEvidenceBeforeReadinessEvaluation() {
        CdpWarehouseReadinessService readiness = mock(CdpWarehouseReadinessService.class);
        CdpWarehouseAvailabilityService availability = mock(CdpWarehouseAvailabilityService.class);
        CdpWarehouseConsumerAvailabilityService contracts = mock(CdpWarehouseConsumerAvailabilityService.class);
        CdpWarehouseEnterpriseOlapReadinessService enterprise =
                mock(CdpWarehouseEnterpriseOlapReadinessService.class);
        CdpWarehouseEnterpriseOlapEvidenceService enterpriseEvidence =
                mock(CdpWarehouseEnterpriseOlapEvidenceService.class);
        CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionService.class);
        when(readiness.readiness(9L)).thenReturn(readinessSummary("PASS"));
        when(availability.evaluate(9L, FROM, TO, "HYBRID")).thenReturn(availabilityDecision("PASS"));
        when(contracts.evaluateContract(9L, "audience_12", FROM, TO))
                .thenReturn(contractEvaluation("audience_12", "PASS", true));
        when(enterpriseEvidence.proofEvidence(9L)).thenReturn(List.of(
                new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:doris_metrics", "PASS", "metrics fresh"),
                new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:workload_isolation", "PASS", "groups configured"),
                new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:query_slo", "PASS", "query SLO in policy"),
                new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:backup_restore", "PASS", "restore drilled"),
                new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:compaction_health", "PASS", "compaction healthy"),
                new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:ingestion_replay", "PASS", "replay proven"),
                new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:runbook_drill", "PASS", "runbook drilled")));
        when(collectionService.proofEvidence(9L)).thenReturn(
                new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:evidence_collection", "PASS", "collection fresh"));
        when(enterprise.evaluateFromProductionEvidence(eq(9L), anyList()))
                .thenReturn(enterpriseReadiness("PASS"));
        CdpWarehouseProductionReadinessProofService service =
                new CdpWarehouseProductionReadinessProofService(
                        readiness,
                        availability,
                        contracts,
                        null,
                        provider(enterprise),
                        provider(enterpriseEvidence),
                        provider(collectionService));

        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof =
                service.proof(9L, FROM, TO, "HYBRID", List.of("audience_12"));

        assertThat(proof.status()).isEqualTo("PASS");
        assertThat(proof.evidence())
                .extracting(CdpWarehouseProductionReadinessProofService.ProofEvidence::key)
                .containsSubsequence(
                        "enterprise_olap:doris_metrics",
                        "enterprise_olap:workload_isolation",
                        "enterprise_olap:query_slo",
                        "enterprise_olap:backup_restore",
                        "enterprise_olap:compaction_health",
                        "enterprise_olap:ingestion_replay",
                        "enterprise_olap:runbook_drill",
                        "enterprise_olap:evidence_collection",
                        "enterprise_olap_readiness");
        ArgumentCaptor<List<CdpWarehouseProductionReadinessProofService.ProofEvidence>> evidenceCaptor =
                ArgumentCaptor.captor();
        verify(enterpriseEvidence).proofEvidence(9L);
        verify(collectionService).proofEvidence(9L);
        verify(enterprise).evaluateFromProductionEvidence(eq(9L), evidenceCaptor.capture());
        assertThat(evidenceCaptor.getValue())
                .extracting(CdpWarehouseProductionReadinessProofService.ProofEvidence::key)
                .contains("enterprise_olap:doris_metrics", "enterprise_olap:evidence_collection",
                        "enterprise_olap:query_slo", "enterprise_olap:runbook_drill");
    }

    private CdpWarehouseReadinessService.ReadinessSummary readinessSummary(String status) {
        return new CdpWarehouseReadinessService.ReadinessSummary(
                9L,
                status,
                TO.plusMinutes(1),
                List.of(new CdpWarehouseReadinessService.ReadinessSection("offline_sync", status, "ok")),
                new CdpWarehouseReadinessService.OfflineReadiness(status, "ok", 1, 0, 0, 1),
                new CdpWarehouseReadinessService.RealtimeReadiness(status, "ok", 1, 1, 0, 0),
                new CdpWarehouseReadinessService.IncidentReadiness(status, "ok", 0, 0, 0),
                new CdpWarehouseReadinessService.BiReadiness(status, "ok", 1, 1, 0),
                new CdpWarehouseReadinessService.AudienceMaterializationReadiness(status, "ok", 1, 1, 0));
    }

    private CdpWarehouseAvailabilityService.AvailabilityDecision availabilityDecision(String status) {
        return new CdpWarehouseAvailabilityService.AvailabilityDecision(
                9L,
                "HYBRID",
                FROM,
                TO,
                TO.plusMinutes(1),
                status,
                List.of(new CdpWarehouseAvailabilityService.AvailabilityGate(
                        "offline_aggregate",
                        status,
                        "ok",
                        TO,
                        0L,
                        1)));
    }

    private CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation contractEvaluation(
            String contractKey,
            String status,
            boolean allowed) {
        return new CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation(
                9L,
                contractKey,
                "AUDIENCE",
                contractKey,
                "HYBRID",
                FROM,
                TO,
                TO.plusMinutes(1),
                status,
                allowed,
                "BLOCK_ON_WARN",
                availabilityDecision(status),
                List.of(),
                allowed
                        ? "consumer availability " + status + " allowed by BLOCK_ON_WARN"
                        : "consumer availability " + status + " blocked by BLOCK_ON_WARN");
    }

    private CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness enterpriseReadiness(String status) {
        return new CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness(
                9L,
                status,
                TO.plusMinutes(1),
                "enterprise OLAP readiness " + status + ": missing critical gates doris_metrics",
                List.of(new CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapGate(
                        "doris_metrics",
                        status,
                        "missing critical gates",
                        true,
                        "doris")),
                "PASS".equals(status) ? List.of() : List.of("doris_metrics"));
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
