package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.GrowthActivityFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证GrowthActivityApplicationService的关键兼容行为。
 */
class GrowthActivityApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    /**
     * 验证 activity resources are tenant scoped stateful and deterministic 场景的兼容行为。
     */
    @Test
    void activityResourcesAreTenantScopedStatefulAndDeterministic() {
        GrowthActivityFacade service = new GrowthActivityApplicationService(CLOCK);

        Map<String, Object> activity = service.upsertActivity(7L,
                Map.of("activityKey", "spring-referral", "activityType", "referral", "status", "draft"),
                "operator-1");
        Map<String, Object> pool = service.execute(7L, 1L, "upsertRewardPool",
                Map.of("poolKey", "coupon-pool", "rewardType", "coupon"), "operator-1");
        Map<String, Object> grant = service.execute(7L, 1L, "createGrant",
                Map.of("participantId", 200L, "poolId", pool.get("id")), "operator-1");
        Map<String, Object> code = service.execute(7L, 1L, "generateReferralCode",
                Map.of("participantId", 200L), "operator-1");
        Map<String, Object> relation = service.execute(7L, 1L, "upsertReferral",
                Map.of("referrerParticipantId", 200L, "refereeParticipantId", 201L), "operator-1");
        Map<String, Object> task = service.execute(7L, 1L, "upsertTask",
                Map.of("taskKey", "share", "taskType", "referral"), "operator-1");
        Map<String, Object> progress = service.execute(7L, 1L, "recordTaskProgress",
                Map.of("participantId", 200L, "taskId", task.get("id"), "progressValue", 1), "operator-1");

        assertThat(activity).containsEntry("tenantId", 7L)
                .containsEntry("id", 1L)
                .containsEntry("activityKey", "spring-referral")
                .containsEntry("status", "DRAFT")
                .containsEntry("updatedBy", "operator-1");
        assertThat(pool).containsEntry("activityId", 1L)
                .containsEntry("poolKey", "coupon-pool")
                .containsEntry("status", "ACTIVE");
        assertThat(grant).containsEntry("activityId", 1L)
                .containsEntry("status", "PENDING");
        assertThat(code).containsEntry("code", "GA-1-200");
        assertThat(relation).containsEntry("status", "PENDING");
        assertThat(progress).containsEntry("status", "RECORDED");

        assertThat(service.listActivities(7L, "referral", "draft", 1000)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("id", 1L));
        assertThat(service.list(7L, 1L, "rewardPools", Map.of(), 1000)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("id", pool.get("id")));
        assertThat(service.list(8L, 1L, "rewardPools", Map.of(), 100)).isEmpty();
    }

    /**
     * 验证 major state transitions update existing rows by target id 场景的兼容行为。
     */
    @Test
    void majorStateTransitionsUpdateExistingRowsByTargetId() {
        GrowthActivityFacade service = new GrowthActivityApplicationService(CLOCK);

        service.upsertActivity(7L, Map.of("activityKey", "launch"), "operator-1");
        Map<String, Object> grant = service.execute(7L, 1L, "createGrant",
                Map.of("participantId", 200L), "operator-1");
        Map<String, Object> relation = service.execute(7L, 1L, "upsertReferral",
                Map.of("referrerParticipantId", 200L, "refereeParticipantId", 201L), "operator-1");
        Map<String, Object> progress = service.execute(7L, 1L, "recordTaskProgress",
                Map.of("participantId", 200L, "taskId", 99L), "operator-1");

        assertThat(service.transitionActivity(7L, 1L, "publish", "operator-2"))
                .containsEntry("status", "PUBLISHED")
                .containsEntry("updatedBy", "operator-2");
        assertThat(service.transitionActivity(7L, 1L, "pause", "operator-2"))
                .containsEntry("status", "PAUSED");
        assertThat(service.transitionActivity(7L, 1L, "close", "operator-2"))
                .containsEntry("status", "CLOSED");
        assertThat(service.execute(7L, 1L, "retryGrant", Map.of("grantId", grant.get("id")), "operator-2"))
                .containsEntry("status", "RETRYING");
        assertThat(service.execute(7L, 1L, "reconcileGrant",
                Map.of("grantId", grant.get("id"), "providerStatus", "issued"), "operator-2"))
                .containsEntry("status", "RECONCILED")
                .containsEntry("providerStatus", "issued");
        assertThat(service.execute(7L, 1L, "cancelGrant", Map.of("grantId", grant.get("id")), "operator-2"))
                .containsEntry("status", "CANCELLED");
        assertThat(service.execute(7L, 1L, "qualifyReferral",
                Map.of("relationId", relation.get("id"), "qualified", true), "operator-2"))
                .containsEntry("status", "QUALIFIED");
        assertThat(service.execute(7L, 1L, "resetTaskProgress",
                Map.of("progressId", progress.get("id")), "operator-2"))
                .containsEntry("status", "RESET");
    }

    /**
     * 验证 readiness report limits defaults and validation follow compatibility rules 场景的兼容行为。
     */
    @Test
    void readinessReportLimitsDefaultsAndValidationFollowCompatibilityRules() {
        GrowthActivityFacade service = new GrowthActivityApplicationService(CLOCK);

        service.upsertActivity(null, Map.of("activityKey", "default-tenant"), "");
        service.execute(null, 1L, "upsertRewardPool", Map.of("poolKey", "pool"), "");
        service.execute(null, 1L, "upsertTask", Map.of("taskKey", "task"), "");

        assertThat(service.readiness(null, 1L))
                .containsEntry("tenantId", 0L)
                .containsEntry("activityId", 1L)
                .containsEntry("status", "READY")
                .containsEntry("productionReady", true);
        assertThat(service.report(null, 1L))
                .containsEntry("rewardPoolCount", 1)
                .containsEntry("taskCount", 1);
        assertThat(service.listActivities(null, null, null, 1000)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("tenantId", 0L)
                        .containsEntry("createdBy", "system"));

        assertThatThrownBy(() -> service.execute(7L, 1L, "missingOperation", Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported growth activity operation");
    }
}
