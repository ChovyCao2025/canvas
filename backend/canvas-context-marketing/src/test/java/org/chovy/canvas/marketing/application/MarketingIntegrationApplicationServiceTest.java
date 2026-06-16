package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 验证MarketingIntegrationApplicationService的关键兼容行为。
 */
class MarketingIntegrationApplicationServiceTest {

    private final MarketingIntegrationApplicationService service = new MarketingIntegrationApplicationService();

    /**
     * 验证 manages contracts probes runs and slo evaluations with deterministic compatibility payloads 场景的兼容行为。
     */
    @Test
    void managesContractsProbesRunsAndSloEvaluationsWithDeterministicCompatibilityPayloads() {
        Map<String, Object> contract = service.upsertContract(
                7L,
                Map.of("providerKey", "meta", "providerFamily", "SOCIAL", "status", "ACTIVE"),
                "operator-1");

        assertThat(contract)
                .containsEntry("contractId", 3001L)
                .containsEntry("tenantId", 7L)
                .containsEntry("providerKey", "meta")
                .containsEntry("providerFamily", "SOCIAL")
                .containsEntry("status", "ACTIVE")
                .containsEntry("updatedBy", "operator-1");

        assertThat(service.listContracts(7L, "ACTIVE", "SOCIAL", 5))
                .singleElement()
                .satisfies(row -> assertThat(row).containsEntry("contractId", 3001L));

        assertThat(service.listContractAuditEvents(7L, 3001L, 5))
                .singleElement()
                .satisfies(row -> assertThat(row).containsEntry("contractId", 3001L)
                        .containsEntry("eventType", "CONTRACT_UPDATED"));

        assertThat(service.archiveContract(7L, 3001L, "operator-1"))
                .containsEntry("contractId", 3001L)
                .containsEntry("status", "ARCHIVED")
                .containsEntry("updatedBy", "operator-1");

        assertThat(service.recordProbeRun(7L, 3001L, Map.of("probeKey", "auth"), "operator-1"))
                .containsEntry("contractId", 3001L)
                .containsEntry("probeRunId", 3101L)
                .containsEntry("status", "PASSED");

        assertThat(service.listProbeRuns(7L, "PASSED", "SOCIAL", 5))
                .singleElement()
                .satisfies(row -> assertThat(row).containsEntry("probeRunId", 3101L));

        assertThat(service.scanProbeRuns(7L, 5, "operator-1"))
                .containsEntry("scannedCount", 1)
                .containsEntry("triggeredBy", "operator-1");

        assertThat(service.listContractSloEvaluations(7L, 5))
                .singleElement()
                .satisfies(row -> assertThat(row).containsEntry("evaluationId", 3201L));

        assertThat(service.recordProbe(7L, 3001L, Map.of("probeKey", "auth"), "operator-1"))
                .containsEntry("contractId", 3001L)
                .containsEntry("probeId", 3301L);

        assertThat(service.listContractProbes(7L, 3001L, 5))
                .singleElement()
                .satisfies(row -> assertThat(row).containsEntry("probeId", 3301L));

        assertThat(service.listRecentProbes(7L, "ACTIVE", 5))
                .singleElement()
                .satisfies(row -> assertThat(row).containsEntry("status", "ACTIVE"));
    }
}
