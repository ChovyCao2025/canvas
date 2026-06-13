package org.chovy.canvas.marketing.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarketingCampaignReadinessPolicyTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private final MarketingCampaignReadinessPolicy policy = new MarketingCampaignReadinessPolicy();

    @Test
    void blocksInactiveCampaignWithNoLaunchDependencies() {
        MarketingCampaignReadinessReport report = policy.evaluate(campaign(CampaignStatus.DRAFT), List.of(), CLOCK);

        assertThat(report.generatedAt()).isEqualTo("2026-06-06T10:00");
        assertThat(report.status()).isEqualTo("BLOCKED");
        assertThat(report.productionReady()).isFalse();
        assertThat(report.requiredLinkCount()).isZero();
        assertThat(report.blockers())
                .extracting(MarketingCampaignReadinessIssue::itemKey)
                .containsExactly(
                        "spring-launch",
                        "launch-required-links",
                        "primary-dependency",
                        "measurement-dependency");
    }

    @Test
    void blocksRequiredResourcesThatAreNotActive() {
        MarketingCampaignReadinessReport report = policy.evaluate(campaign(CampaignStatus.ACTIVE), List.of(
                link(20L, "JOURNEY", "launch-journey", "PRIMARY", CampaignLinkStatus.BLOCKED, true),
                link(21L, "BI_DASHBOARD", "launch-bi", "MEASUREMENT", CampaignLinkStatus.ACTIVE, true)), CLOCK);

        assertThat(report.status()).isEqualTo("BLOCKED");
        assertThat(report.requiredLinkCount()).isEqualTo(2);
        assertThat(report.activeRequiredLinkCount()).isEqualTo(1);
        assertThat(report.blockers())
                .anySatisfy(finding -> {
                    assertThat(finding.itemType()).isEqualTo("RESOURCE_LINK");
                    assertThat(finding.itemKey()).isEqualTo("JOURNEY:launch-journey");
                    assertThat(finding.reason()).isEqualTo("linkStatus=BLOCKED");
                });
    }

    @Test
    void reportsReadyWhenPrimaryAndMeasurementDependenciesAreActive() {
        MarketingCampaignReadinessReport report = policy.evaluate(campaign(CampaignStatus.ACTIVE), List.of(
                link(20L, "JOURNEY", "launch-journey", "PRIMARY", CampaignLinkStatus.ACTIVE, true),
                link(21L, "BI_DASHBOARD", "launch-bi", "MEASUREMENT", CampaignLinkStatus.ACTIVE, true)), CLOCK);

        assertThat(report.status()).isEqualTo("READY");
        assertThat(report.productionReady()).isTrue();
        assertThat(report.blockerCount()).isZero();
        assertThat(report.warningCount()).isZero();
    }

    @Test
    void reportsDegradedWhenOnlyOptionalLinksNeedTriage() {
        MarketingCampaignReadinessReport report = policy.evaluate(campaign(CampaignStatus.ACTIVE), List.of(
                link(20L, "JOURNEY", "launch-journey", "PRIMARY", CampaignLinkStatus.ACTIVE, true),
                link(21L, "BI_DASHBOARD", "launch-bi", "MEASUREMENT", CampaignLinkStatus.ACTIVE, true),
                link(22L, "CONTENT_RELEASE", "optional-copy", "SUPPORTING", CampaignLinkStatus.MISSING, false)), CLOCK);

        assertThat(report.status()).isEqualTo("DEGRADED");
        assertThat(report.productionReady()).isTrue();
        assertThat(report.warningCount()).isEqualTo(1);
        assertThat(report.warnings()).singleElement()
                .satisfies(finding -> assertThat(finding.itemType()).isEqualTo("OPTIONAL_RESOURCE_LINK"));
    }

    private static MarketingCampaign campaign(CampaignStatus status) {
        return new MarketingCampaign(
                10L,
                7L,
                CampaignKey.of("spring-launch", "campaignKey"),
                "Spring launch",
                "ACQUISITION",
                status,
                null,
                null,
                CampaignDateRange.of(null, null),
                new CampaignBudget(BigDecimal.ZERO, "CNY"),
                Map.of(),
                "operator-1",
                "operator-1",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00"));
    }

    private static MarketingCampaignLink link(Long id,
                                              String type,
                                              String key,
                                              String role,
                                              CampaignLinkStatus status,
                                              boolean requiredForLaunch) {
        return new MarketingCampaignLink(
                id,
                7L,
                10L,
                type,
                300L + id,
                CampaignKey.of(key, "resourceKey"),
                key,
                "/resources/" + key,
                role,
                status,
                requiredForLaunch,
                Map.of(),
                "operator-1",
                "operator-1",
                null,
                null);
    }
}
