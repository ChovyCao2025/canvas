package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingMonitoringFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证MarketingMonitoringApplicationService的关键兼容行为。
 */
class MarketingMonitoringApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T01:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    /**
     * 验证 sources items alerts and dispatches are tenant scoped and stateful 场景的兼容行为。
     */
    @Test
    void sourcesItemsAlertsAndDispatchesAreTenantScopedAndStateful() {
        MarketingMonitoringFacade service = new MarketingMonitoringApplicationService(CLOCK);

        Map<String, Object> source = service.execute(7L, "upsertSource",
                Map.of("sourceKey", "twitter-launch", "providerType", "twitter"), "operator-1");
        Map<String, Object> item = service.execute(7L, "ingestItem",
                Map.of("sourceId", source.get("id"), "externalId", "post-1", "sentimentLabel", "negative"), "operator-1");
        Map<String, Object> channel = service.execute(7L, "upsertAlertChannel",
                Map.of("channelKey", "ops", "channelType", "email"), "operator-1");
        Map<String, Object> dispatch = service.execute(7L, "dispatchAlert",
                Map.of("alertId", ((Map<?, ?>) item.get("alert")).get("id")), "operator-2");

        assertThat(source).containsEntry("tenantId", 7L)
                .containsEntry("sourceKey", "twitter-launch")
                .containsEntry("updatedBy", "operator-1");
        assertThat(item).containsEntry("tenantId", 7L)
                .containsEntry("sentimentLabel", "NEGATIVE");
        assertThat(channel).containsEntry("channelKey", "ops");
        assertThat(dispatch).containsEntry("status", "DISPATCHED")
                .containsEntry("channelCount", 1);

        List<Map<String, Object>> tenantItems = service.list(7L, "items",
                Map.of("sentimentLabel", "negative"), 1000);
        List<Map<String, Object>> otherTenantItems = service.list(8L, "items", Map.of(), 100);

        assertThat(tenantItems).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("id", item.get("id")));
        assertThat(otherTenantItems).isEmpty();
    }

    /**
     * 验证 provider credentials oauth anomalies and webhook return deterministic views 场景的兼容行为。
     */
    @Test
    void providerCredentialsOauthAnomaliesAndWebhookReturnDeterministicViews() {
        MarketingMonitoringFacade service = new MarketingMonitoringApplicationService(CLOCK);

        Map<String, Object> credential = service.execute(7L, "upsertProviderCredential",
                Map.of("credentialKey", "twitter-main", "providerType", "twitter", "authType", "oauth"),
                "operator-1");
        Map<String, Object> refreshed = service.execute(7L, "refreshProviderCredential",
                Map.of("credentialKey", "twitter-main", "force", true), "operator-2");
        Map<String, Object> auth = service.execute(7L, "startProviderOAuthAuthorization",
                Map.of("providerType", "twitter", "credentialKey", "twitter-main"), "operator-1");
        Map<String, Object> rule = service.execute(7L, "upsertAnomalyRule",
                Map.of("ruleKey", "spike", "metricKey", "mentions"), "operator-1");
        Map<String, Object> detection = service.execute(7L, "detectAnomalies",
                Map.of("ruleId", rule.get("id"), "score", 95), "operator-1");
        Map<String, Object> secret = service.execute(7L, "rotateWebhookSecret",
                Map.of("sourceId", 10L), "operator-1");

        assertThat(credential).containsEntry("credentialKey", "twitter-main")
                .containsEntry("status", "ACTIVE");
        assertThat(refreshed).containsEntry("status", "REFRESHED")
                .containsEntry("updatedBy", "operator-2");
        assertThat(auth).containsEntry("status", "STARTED")
                .containsKey("authorizationUrl");
        assertThat(detection).containsEntry("status", "DETECTED")
                .containsKey("events");
        assertThat(secret).containsEntry("sourceId", 10L)
                .containsKey("secretVersion");

        assertThat(service.list(7L, "providerCredentialEvents", Map.of("credentialKey", "twitter-main"), 100))
                .extracting(row -> row.get("eventType"))
                .contains("UPSERTED", "REFRESHED");
        assertThat(service.list(7L, "anomalies", Map.of("ruleId", rule.get("id")), 100)).hasSize(1);
    }

    /**
     * 验证 clamps list limits defaults tenant and rejects unknown operation 场景的兼容行为。
     */
    @Test
    void clampsListLimitsDefaultsTenantAndRejectsUnknownOperation() {
        MarketingMonitoringFacade service = new MarketingMonitoringApplicationService(CLOCK);

        service.execute(null, "upsertSource", Map.of("sourceKey", "default-source"), "");
        List<Map<String, Object>> rows = service.list(null, "sources", Map.of(), 1000);

        assertThat(rows).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("tenantId", 0L)
                        .containsEntry("createdBy", "system"));

        assertThatThrownBy(() -> service.execute(7L, "missingOperation", Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported marketing monitoring operation");
    }
}
