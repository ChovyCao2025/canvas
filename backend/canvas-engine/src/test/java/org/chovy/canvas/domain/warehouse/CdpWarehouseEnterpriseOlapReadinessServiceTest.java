package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseEnterpriseOlapReadinessServiceTest {

    private final CdpWarehouseEnterpriseOlapReadinessService service =
            new CdpWarehouseEnterpriseOlapReadinessService();

    @Test
    void evaluatePassesWhenEveryRequiredGatePasses() {
        CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness readiness =
                service.evaluate(9L, List.of(
                        gate("warehouse_readiness", "PASS"),
                        gate("window_availability", "PASS"),
                        gate("consumer_contracts", "PASS"),
                        gate("privacy_erasure_backlog", "PASS"),
                        gate("doris_metrics", "PASS"),
                        gate("evidence_collection", "PASS"),
                        gate("workload_isolation", "PASS"),
                        gate("query_slo", "PASS"),
                        gate("backup_restore", "PASS"),
                        gate("compaction_health", "PASS"),
                        gate("ingestion_replay", "PASS"),
                        gate("runbook_drill", "PASS")));

        assertThat(readiness.tenantId()).isEqualTo(9L);
        assertThat(readiness.status()).isEqualTo("PASS");
        assertThat(readiness.missingCriticalGates()).isEmpty();
        assertThat(readiness.gates())
                .extracting(CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapGate::key)
                .containsExactly(
                        "warehouse_readiness",
                        "window_availability",
                        "consumer_contracts",
                        "privacy_erasure_backlog",
                        "doris_metrics",
                        "evidence_collection",
                        "workload_isolation",
                        "query_slo",
                        "backup_restore",
                        "compaction_health",
                        "ingestion_replay",
                        "runbook_drill");
    }

    @Test
    void evaluateFailsClosedWhenOperationalEvidenceIsMissing() {
        CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness readiness =
                service.evaluate(9L, List.of(
                        gate("warehouse_readiness", "PASS"),
                        gate("window_availability", "PASS"),
                        gate("consumer_contracts", "PASS"),
                        gate("privacy_erasure_backlog", "PASS")));

        assertThat(readiness.status()).isEqualTo("FAIL");
        assertThat(readiness.missingCriticalGates()).containsExactly(
                "doris_metrics",
                "evidence_collection",
                "workload_isolation",
                "query_slo",
                "backup_restore",
                "compaction_health",
                "ingestion_replay",
                "runbook_drill");
        assertThat(readiness.gates()).filteredOn(gate -> gate.key().equals("doris_metrics"))
                .singleElement()
                .satisfies(gate -> {
                    assertThat(gate.status()).isEqualTo("FAIL");
                    assertThat(gate.reason()).contains("missing critical enterprise OLAP evidence");
                });
    }

    @Test
    void evaluateNormalizesUnknownStatusToFail() {
        CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness readiness =
                service.evaluate(9L, List.of(
                        gate("warehouse_readiness", "PASS"),
                        gate("window_availability", "PASS"),
                        gate("consumer_contracts", "PASS"),
                        gate("privacy_erasure_backlog", "PASS"),
                        gate("doris_metrics", "UNKNOWN"),
                        gate("evidence_collection", "PASS"),
                        gate("workload_isolation", "PASS"),
                        gate("query_slo", "PASS"),
                        gate("backup_restore", "PASS"),
                        gate("compaction_health", "PASS"),
                        gate("ingestion_replay", "PASS"),
                        gate("runbook_drill", "PASS")));

        assertThat(readiness.status()).isEqualTo("FAIL");
        assertThat(readiness.gates()).filteredOn(gate -> gate.key().equals("doris_metrics"))
                .singleElement()
                .satisfies(gate -> assertThat(gate.status()).isEqualTo("FAIL"));
    }

    @Test
    void evaluateWarnsWhenCriticalGateWarnsAndNoGateFails() {
        CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness readiness =
                service.evaluate(9L, List.of(
                        gate("warehouse_readiness", "PASS"),
                        gate("window_availability", "PASS"),
                        gate("consumer_contracts", "WARN"),
                        gate("privacy_erasure_backlog", "PASS"),
                        gate("doris_metrics", "PASS"),
                        gate("evidence_collection", "PASS"),
                        gate("workload_isolation", "PASS"),
                        gate("query_slo", "PASS"),
                        gate("backup_restore", "PASS"),
                        gate("compaction_health", "PASS"),
                        gate("ingestion_replay", "PASS"),
                        gate("runbook_drill", "PASS")));

        assertThat(readiness.status()).isEqualTo("WARN");
        assertThat(readiness.summary()).contains("WARN");
    }

    @Test
    void evaluateFromProductionEvidenceAggregatesExistingProofSignals() {
        CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapReadiness readiness =
                service.evaluateFromProductionEvidence(9L, List.of(
                        evidence("warehouse_readiness", "PASS", "ready"),
                        evidence("window_availability", "PASS", "available"),
                        evidence("consumer_contract:bi_daily_active_users", "PASS", "bi ok"),
                        evidence("consumer_contract:audience_12", "PASS", "audience ok"),
                        evidence("privacy_erasure_backlog", "PASS", "privacy ok"),
                        evidence("enterprise_olap:doris_metrics", "PASS", "metrics fresh"),
                        evidence("enterprise_olap:evidence_collection", "PASS", "collection fresh"),
                        evidence("enterprise_olap:workload_isolation", "PASS", "groups configured"),
                        evidence("enterprise_olap:query_slo", "PASS", "query SLO in policy"),
                        evidence("enterprise_olap:backup_restore", "PASS", "restore drill passed"),
                        evidence("enterprise_olap:compaction_health", "PASS", "compaction healthy"),
                        evidence("enterprise_olap:ingestion_replay", "PASS", "synthetic proof passed"),
                        evidence("enterprise_olap:runbook_drill", "PASS", "runbook drilled")));

        assertThat(readiness.status()).isEqualTo("PASS");
        assertThat(readiness.gates()).filteredOn(gate -> gate.key().equals("consumer_contracts"))
                .singleElement()
                .satisfies(gate -> {
                    assertThat(gate.status()).isEqualTo("PASS");
                    assertThat(gate.reason()).contains("2 consumer contracts");
                });
    }

    private CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapGate gate(
            String key,
            String status) {
        return new CdpWarehouseEnterpriseOlapReadinessService.EnterpriseOlapGate(
                key,
                status,
                key + " " + status,
                true,
                "test");
    }

    private CdpWarehouseProductionReadinessProofService.ProofEvidence evidence(
            String key,
            String status,
            String reason) {
        return new CdpWarehouseProductionReadinessProofService.ProofEvidence(key, status, reason);
    }
}
