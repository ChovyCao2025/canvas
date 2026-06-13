package org.chovy.canvas.marketing.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MarketingCampaign(
        Long id,
        Long tenantId,
        CampaignKey campaignKey,
        String campaignName,
        String objective,
        CampaignStatus status,
        String primaryChannel,
        String ownerTeam,
        CampaignDateRange dateRange,
        CampaignBudget budget,
        Map<String, Object> brief,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public MarketingCampaign {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(campaignKey, "campaignKey");
        campaignName = defaultString(campaignName, campaignKey.value());
        objective = defaultString(objective, "UNSPECIFIED");
        status = status == null ? CampaignStatus.DRAFT : status;
        dateRange = dateRange == null ? CampaignDateRange.of(null, null) : dateRange;
        budget = budget == null ? CampaignBudget.of(null, null) : budget;
        brief = copyMap(brief);
    }

    public static MarketingCampaign createExisting(Long id,
                                                   Long tenantId,
                                                   CampaignKey campaignKey,
                                                   String campaignName,
                                                   String objective,
                                                   CampaignStatus status,
                                                   String primaryChannel,
                                                   String ownerTeam,
                                                   LocalDateTime startAt,
                                                   LocalDateTime endAt,
                                                   BigDecimal budgetAmount,
                                                   String currency,
                                                   Map<String, Object> brief,
                                                   String createdBy,
                                                   String updatedBy,
                                                   LocalDateTime createdAt,
                                                   LocalDateTime updatedAt) {
        return new MarketingCampaign(
                id,
                tenantId,
                campaignKey,
                campaignName,
                objective,
                status,
                primaryChannel,
                ownerTeam,
                CampaignDateRange.of(startAt, endAt),
                CampaignBudget.of(budgetAmount, currency),
                brief,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt);
    }

    public MarketingCampaign withId(Long id) {
        return new MarketingCampaign(
                id,
                tenantId,
                campaignKey,
                campaignName,
                objective,
                status,
                primaryChannel,
                ownerTeam,
                dateRange,
                budget,
                brief,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt);
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private static Map<String, Object> copyMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
