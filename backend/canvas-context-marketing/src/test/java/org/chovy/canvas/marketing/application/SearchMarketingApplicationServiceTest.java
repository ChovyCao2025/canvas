package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.chovy.canvas.marketing.api.SearchMarketingFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证SearchMarketingApplicationService的关键兼容行为。
 */
class SearchMarketingApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T03:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    /**
     * 验证 sources keywords snapshots and summary are tenant scoped and deterministic 场景的兼容行为。
     */
    @Test
    void sourcesKeywordsSnapshotsAndSummaryAreTenantScopedAndDeterministic() {
        SearchMarketingFacade service = new SearchMarketingApplicationService(CLOCK);

        Map<String, Object> source = service.upsertSource(7L,
                Map.of("provider", "google", "sourceKey", "gsc-main", "channel", "SEO"), "operator-1");
        Map<String, Object> keyword = service.upsertKeyword(7L,
                Map.of("keywordText", "canvas marketing", "channel", "SEO", "status", "active"), "operator-1");
        Map<String, Object> snapshot = service.upsertSnapshot(7L,
                Map.of("sourceId", source.get("id"), "keywordId", keyword.get("id"), "snapshotDate", "2026-06-14",
                        "impressionCount", 100, "clickCount", 10), "operator-1");

        assertThat(source).containsEntry("tenantId", 7L)
                .containsEntry("id", 1L)
                .containsEntry("provider", "GOOGLE")
                .containsEntry("status", "ACTIVE")
                .containsEntry("updatedBy", "operator-1");
        assertThat(keyword).containsEntry("id", 1L)
                .containsEntry("keywordText", "canvas marketing")
                .containsEntry("status", "ACTIVE");
        assertThat(snapshot).containsEntry("sourceId", 1L)
                .containsEntry("keywordId", 1L)
                .containsEntry("snapshotDate", LocalDate.parse("2026-06-14"));

        assertThat(service.listSources(7L, "google", "SEO", true, 1000)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("id", 1L));
        assertThat(service.listKeywords(7L, "SEO", "active", 1000)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("id", 1L));
        assertThat(service.listSnapshots(8L, null, null, null, null, null, 100)).isEmpty();
        assertThat(service.summary(7L, "SEO", 1L, 1L, LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30")))
                .containsEntry("tenantId", 7L)
                .containsEntry("snapshotCount", 1)
                .containsEntry("impressionCount", 100L)
                .containsEntry("clickCount", 10L);
    }

    /**
     * 验证 opportunities mutations sync and impact windows support compatibility transitions 场景的兼容行为。
     */
    @Test
    void opportunitiesMutationsSyncAndImpactWindowsSupportCompatibilityTransitions() {
        SearchMarketingFacade service = new SearchMarketingApplicationService(CLOCK);

        Map<String, Object> source = service.upsertSource(7L, Map.of("provider", "google", "channel", "PAID"), "operator-1");
        Map<String, Object> opportunity = service.evaluateOpportunities(7L,
                Map.of("channel", "PAID", "sourceId", source.get("id"), "severity", "high"), "operator-1");
        Map<String, Object> status = service.updateOpportunityStatus(7L, 1L,
                Map.of("status", "accepted", "reason", "ship it"), "operator-2");
        Map<String, Object> mutation = service.createOpportunityMutation(7L, 1L,
                Map.of("sourceId", source.get("id"), "mutationKey", "bid-raise", "mutationType", "BID"), "operator-2");
        Map<String, Object> approval = service.approveMutation(7L, 1L,
                Map.of("decision", "approved", "reason", "safe"), "operator-3");
        Map<String, Object> execution = service.executeMutation(7L, 1L,
                Map.of("dryRun", false, "partialFailure", false), "operator-3");
        Map<String, Object> reconcile = service.reconcileMutation(7L, 1L, "operator-3");
        Map<String, Object> sync = service.syncSource(7L, 1L,
                Map.of("windowStart", "2026-06-01", "windowEnd", "2026-06-14"), "operator-4");
        Map<String, Object> due = service.syncDue(7L, Map.of(), "operator-4");
        Map<String, Object> impact = service.evaluateDueImpactWindows(7L, Map.of("limit", 5), "operator-4");

        assertThat(opportunity).containsEntry("status", "OPEN")
                .containsEntry("severity", "HIGH");
        assertThat(status).containsEntry("status", "ACCEPTED")
                .containsEntry("updatedBy", "operator-2");
        assertThat(mutation).containsEntry("opportunityId", 1L)
                .containsEntry("status", "PENDING")
                .containsEntry("approvalStatus", "PENDING");
        assertThat(approval).containsEntry("approvalStatus", "APPROVED");
        assertThat(execution).containsEntry("status", "EXECUTED");
        assertThat(reconcile).containsEntry("reconciliationStatus", "RECONCILED");
        assertThat(sync).containsEntry("runType", "PERFORMANCE")
                .containsEntry("status", "SUCCEEDED");
        assertThat(due).containsEntry("limit", 50)
                .containsEntry("scheduledCount", 1);
        assertThat(impact).containsEntry("limit", 5)
                .containsEntry("evaluatedCount", 1);
        assertThat(service.listProviderChanges(7L, 1L, 1L, "google", "RECONCILED", 100)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("mutationId", 1L));
    }

    /**
     * 验证 readiness defaults limits and validation follow compatibility rules 场景的兼容行为。
     */
    @Test
    void readinessDefaultsLimitsAndValidationFollowCompatibilityRules() {
        SearchMarketingFacade service = new SearchMarketingApplicationService(CLOCK);

        service.upsertSource(null, Map.of("provider", "google"), "");

        assertThat(service.readiness(null))
                .containsEntry("tenantId", 0L)
                .containsEntry("status", "READY")
                .containsEntry("productionReady", true);
        assertThat(service.listSources(null, null, null, null, 1000)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("tenantId", 0L)
                        .containsEntry("updatedBy", "system"));

        assertThatThrownBy(() -> service.updateOpportunityStatus(7L, 99L, Map.of("status", ""), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status is required");
    }
}
