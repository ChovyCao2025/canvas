package org.chovy.canvas.marketing.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 提供MarketingCampaignReadinessPolicy的业务能力。
 */
public class MarketingCampaignReadinessPolicy {

    /**
     * 执行evaluate业务操作。
     */
    public MarketingCampaignReadinessReport evaluate(MarketingCampaign campaign,
                                                     List<MarketingCampaignLink> links,
                                                     Clock clock) {
        List<MarketingCampaignLink> safeLinks = links == null ? List.of() : List.copyOf(links);
        Clock safeClock = clock == null ? Clock.systemDefaultZone() : clock;
        List<MarketingCampaignReadinessIssue> blockers = new ArrayList<>();
        List<MarketingCampaignReadinessIssue> warnings = new ArrayList<>();

        // 生产上线必须先确认活动本身可投放，避免后续资源检查掩盖主状态问题。
        if (campaign.status() != CampaignStatus.ACTIVE) {
            blockers.add(finding(
                    "BLOCKER",
                    "CAMPAIGN_STATUS",
                    campaign.campaignKey().value(),
                    "Campaign is not active",
                    "campaign status must be ACTIVE before production launch",
                    null));
        }

        List<MarketingCampaignLink> requiredLinks = safeLinks.stream()
                .filter(MarketingCampaignLink::requiredForLaunch)
                .toList();
        List<MarketingCampaignLink> activeRequiredLinks = requiredLinks.stream()
                .filter(link -> link.linkStatus() == CampaignLinkStatus.ACTIVE)
                .toList();

        if (requiredLinks.isEmpty()) {
            blockers.add(finding(
                    "BLOCKER",
                    "RESOURCE_LINK",
                    "launch-required-links",
                    "No launch-required resources are linked",
                    "attach journey, content, activation, provider-write, or measurement resources before launch",
                    null));
        }

        requiredLinks.stream()
                .filter(link -> link.linkStatus() != CampaignLinkStatus.ACTIVE)
                .forEach(link -> blockers.add(finding(
                        "BLOCKER",
                        "RESOURCE_LINK",
                        link.resourceType() + ":" + link.resourceKey().value(),
                        "Launch-required resource is not active",
                        "linkStatus=" + link.linkStatus().name(),
                        link.resourceRoute())));

        boolean hasPrimary = activeRequiredLinks.stream()
                .anyMatch(link -> "PRIMARY".equals(link.dependencyRole()));
        if (!hasPrimary) {
            blockers.add(finding(
                    "BLOCKER",
                    "RESOURCE_COVERAGE",
                    "primary-dependency",
                    "Primary launch dependency is missing",
                    "at least one active launch-required resource must have dependencyRole=PRIMARY",
                    null));
        }

        boolean hasMeasurement = activeRequiredLinks.stream()
                .anyMatch(link -> "MEASUREMENT".equals(link.dependencyRole())
                        || "BI_DASHBOARD".equals(link.resourceType()));
        if (!hasMeasurement) {
            blockers.add(finding(
                    "BLOCKER",
                    "RESOURCE_COVERAGE",
                    "measurement-dependency",
                    "Measurement dependency is missing",
                    "attach an active launch-required BI dashboard or dependencyRole=MEASUREMENT resource",
                    null));
        }

        safeLinks.stream()
                .filter(link -> !link.requiredForLaunch())
                .filter(link -> link.linkStatus() == CampaignLinkStatus.BLOCKED
                        || link.linkStatus() == CampaignLinkStatus.MISSING)
                .forEach(link -> warnings.add(finding(
                        "WARNING",
                        "OPTIONAL_RESOURCE_LINK",
                        link.resourceType() + ":" + link.resourceKey().value(),
                        "Optional linked resource needs triage",
                        "linkStatus=" + link.linkStatus().name(),
                        link.resourceRoute())));

        // 阻断项决定是否可上线，警告项只降低准备度状态，保持旧版兼容的三态输出。
        String status = blockers.isEmpty() ? (warnings.isEmpty() ? "READY" : "DEGRADED") : "BLOCKED";
        return new MarketingCampaignReadinessReport(
                campaign.tenantId(),
                campaign.id(),
                campaign.campaignKey().value(),
                campaign.campaignName(),
                LocalDateTime.now(safeClock).withNano(0).toString(),
                status,
                blockers.isEmpty(),
                requiredLinks.size(),
                activeRequiredLinks.size(),
                blockers.size(),
                warnings.size(),
                blockers,
                warnings,
                safeLinks);
    }

    /**
     * 查找ing业务对象。
     */
    private static MarketingCampaignReadinessIssue finding(String severity,
                                                           String itemType,
                                                           String itemKey,
                                                           String title,
                                                           String reason,
                                                           String route) {
        return new MarketingCampaignReadinessIssue(severity, itemType, itemKey, title, reason, route);
    }
}
