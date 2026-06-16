package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehousePrivacyApplicationService 的核心行为。
 */
class CdpWarehousePrivacyApplicationServiceTest {

    /**
     * 执行 CdpWarehousePrivacyApplicationService 对应的 CDP 业务操作。
     */
    private final CdpWarehousePrivacyApplicationService service = new CdpWarehousePrivacyApplicationService();

    /**
     * 执行 managesErasureRequestsTombstonesAndDecisionsWithDeterministicCompatibilityPayloads 对应的 CDP 业务操作。
     */
    @Test
    void managesErasureRequestsTombstonesAndDecisionsWithDeterministicCompatibilityPayloads() {
        Map<String, Object> erasure = service.createErasureRequest(
                7L,
                Map.of("subjectType", "USER_ID", "subjectValue", "user-1", "reason", "gdpr"),
                "operator-1");

        assertThat(erasure)
                .containsEntry("requestId", 1001L)
                .containsEntry("tenantId", 7L)
                .containsEntry("subjectValue", "user-1")
                .containsEntry("status", "REQUESTED")
                .containsEntry("createdBy", "operator-1");

        assertThat(service.recordAssetProof(7L, 1001L, Map.of("assetKey", "profile"), "auditor"))
                .containsEntry("requestId", 1001L)
                .containsEntry("proofId", 1101L)
                .containsEntry("assetKey", "profile")
                .containsEntry("recordedBy", "auditor");

        assertThat(service.executeErasure(7L, 1001L, Map.of("mode", "dry-run"), "operator-1"))
                .containsEntry("requestId", 1001L)
                .containsEntry("executionId", 1201L)
                .containsEntry("status", "SUCCEEDED");

        assertThat(service.rebuildAudienceBitmaps(7L, 1001L, Map.of("audienceId", 88L), "operator-1"))
                .containsEntry("requestId", 1001L)
                .containsEntry("rebuildId", 1301L)
                .containsEntry("status", "QUEUED");

        assertThat(service.runAudienceRebuildAutomation(7L, Map.of("strategy", "recent"), "operator-1"))
                .containsEntry("runId", 1401L)
                .containsEntry("status", "COMPLETED")
                .containsEntry("triggeredBy", "operator-1");

        assertThat(service.getAudienceRebuildAutomationRun(7L, 1401L))
                .containsEntry("runId", 1401L)
                .containsEntry("status", "COMPLETED");

        assertThat(service.listAudienceRebuildAutomationRuns(7L, 5))
                .singleElement()
                .satisfies(run -> assertThat(run).containsEntry("runId", 1401L));

        assertThat(service.recentErasureRequests(7L, "REQUESTED", 5))
                .singleElement()
                .satisfies(request -> assertThat(request).containsEntry("requestId", 1001L));

        assertThat(service.getErasureRequest(7L, 1001L))
                .containsEntry("requestId", 1001L)
                .containsEntry("tenantId", 7L);

        assertThat(service.erasureSummary(7L))
                .containsEntry("tenantId", 7L)
                .containsEntry("openCount", 1);

        assertThat(service.createTombstone(7L, Map.of("subjectValue", "user-1"), "operator-1"))
                .containsEntry("tombstoneId", 2001L)
                .containsEntry("status", "ACTIVE");

        assertThat(service.createTombstoneFromErasureRequest(7L, Map.of("requestId", 1001L), "operator-1"))
                .containsEntry("tombstoneId", 2002L)
                .containsEntry("requestId", 1001L);

        assertThat(service.revokeTombstone(7L, 2001L, Map.of("reason", "appeal"), "operator-1"))
                .containsEntry("tombstoneId", 2001L)
                .containsEntry("status", "REVOKED");

        assertThat(service.listTombstones(7L, "ACTIVE", 5))
                .singleElement()
                .satisfies(tombstone -> assertThat(tombstone).containsEntry("tombstoneId", 2001L));

        assertThat(service.tombstoneDecision(7L, "USER_ID", "user-1"))
                .containsEntry("blocked", true)
                .containsEntry("subjectValue", "user-1");
    }
}
