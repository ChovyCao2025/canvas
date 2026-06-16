package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MauticInsightFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证MauticInsightApplicationService的关键兼容行为。
 */
class MauticInsightApplicationServiceTest {

    /**
     * 验证 audience membership is seeded and deterministic for compatibility 场景的兼容行为。
     */
    @Test
    void audienceMembershipIsSeededAndDeterministicForCompatibility() {
        MauticInsightFacade service = new MauticInsightApplicationService();

        Map<String, Object> matched = service.audienceMembership(1001L, "user-1");
        Map<String, Object> missed = service.audienceMembership(1001L, "user-9");
        Map<String, Object> unknown = service.audienceMembership(9999L, "user-1");

        assertThat(matched)
                .containsEntry("audienceId", 1001L)
                .containsEntry("audienceName", "VIP Buyers")
                .containsEntry("userId", "user-1")
                .containsEntry("status", "MATCHED")
                .containsEntry("statStatus", "READY")
                .containsEntry("estimatedSize", 128L)
                .containsEntry("latestRunStatus", "SUCCEEDED");
        assertThat((List<String>) matched.get("evidence")).containsExactly(
                "rule engine: RULE_DSL",
                "data source: CDP_PROFILE",
                "last computed at 2026-01-01T00:00:00",
                "bitmap membership matched");
        assertThat(missed).containsEntry("status", "NOT_MATCHED");
        assertThat(unknown)
                .containsEntry("audienceId", 9999L)
                .containsEntry("audienceName", null)
                .containsEntry("status", "UNKNOWN");
    }

    /**
     * 验证 journey path and suppression timeline return stable ordered payloads 场景的兼容行为。
     */
    @Test
    void journeyPathAndSuppressionTimelineReturnStableOrderedPayloads() {
        MauticInsightFacade service = new MauticInsightApplicationService();

        Map<String, Object> path = service.journeyPath("exec-100");
        Map<String, Object> timeline = service.suppressionTimeline("user-1");

        assertThat(path)
                .containsEntry("executionId", "exec-100")
                .containsEntry("successCount", 2)
                .containsEntry("failedCount", 1)
                .containsEntry("skippedCount", 0);
        List<Map<String, Object>> steps = (List<Map<String, Object>>) path.get("steps");
        assertThat(steps.stream().map(step -> step.get("nodeId")).toList())
                .containsExactly("start-1", "send-1", "wait-1");
        assertThat(timeline).containsEntry("userId", "user-1");
        List<Map<String, Object>> records = (List<Map<String, Object>>) timeline.get("records");
        assertThat(records.stream().map(record -> record.get("channel")).toList())
                .containsExactly("SMS", "EMAIL");
    }

    /**
     * 验证 channel preference applies default and honors explicit preferred channel 场景的兼容行为。
     */
    @Test
    void channelPreferenceAppliesDefaultAndHonorsExplicitPreferredChannel() {
        MauticInsightFacade service = new MauticInsightApplicationService();

        Map<String, Object> defaulted = service.channelPreference("user-1", null);
        Map<String, Object> explicit = service.channelPreference("user-1", "sms");

        assertThat(defaulted)
                .containsEntry("userId", "user-1")
                .containsEntry("requestedPreferredChannel", "EMAIL")
                .containsEntry("recommendedChannel", "EMAIL")
                .containsEntry("fallbackChannel", "PUSH");
        assertThat(explicit)
                .containsEntry("requestedPreferredChannel", "SMS")
                .containsEntry("recommendedChannel", "EMAIL")
                .containsEntry("fallbackChannel", "PUSH");
    }

    /**
     * 验证 publish health and frequency templates use compact seed data 场景的兼容行为。
     */
    @Test
    void publishHealthAndFrequencyTemplatesUseCompactSeedData() {
        MauticInsightFacade service = new MauticInsightApplicationService();

        Map<String, Object> health = service.publishHealth(3001L);
        List<Map<String, Object>> templates = service.frequencyTemplates();

        assertThat(health)
                .containsEntry("canvasId", 3001L)
                .containsEntry("canvasName", "Welcome Journey")
                .containsEntry("score", 100);
        List<Map<String, Object>> checks = (List<Map<String, Object>>) health.get("checks");
        assertThat(checks.stream().map(check -> check.get("checkKey")).toList())
                .containsExactly("CANVAS_ACTIVE", "PUBLISHED_VERSION", "GRAPH_PRESENT", "TRIGGER_PRESENT",
                        "SEND_NODE_PRESENT");
        assertThat(templates).extracting(row -> row.get("templateKey")).containsExactly(
                "global_weekly_guard",
                "journey_daily_default",
                "channel_daily_guard",
                "node_once_window");
    }

    /**
     * 验证 invalid read parameters throw illegal argument for controller envelope 场景的兼容行为。
     */
    @Test
    void invalidReadParametersThrowIllegalArgumentForControllerEnvelope() {
        MauticInsightFacade service = new MauticInsightApplicationService();

        assertThatThrownBy(() -> service.audienceMembership(null, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audienceId is required");
        assertThatThrownBy(() -> service.journeyPath(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("executionId is required");
        assertThatThrownBy(() -> service.publishHealth(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canvasId is required");
    }
}
