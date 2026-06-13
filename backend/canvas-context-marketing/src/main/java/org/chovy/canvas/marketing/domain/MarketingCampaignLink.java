package org.chovy.canvas.marketing.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MarketingCampaignLink(
        Long id,
        Long tenantId,
        Long campaignId,
        String resourceType,
        Long resourceId,
        CampaignKey resourceKey,
        String resourceName,
        String resourceRoute,
        String dependencyRole,
        CampaignLinkStatus linkStatus,
        boolean requiredForLaunch,
        Map<String, Object> metadata,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public MarketingCampaignLink {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(campaignId, "campaignId");
        resourceType = required(resourceType, "resourceType");
        Objects.requireNonNull(resourceKey, "resourceKey");
        dependencyRole = defaultString(dependencyRole, "SUPPORTING");
        linkStatus = linkStatus == null ? CampaignLinkStatus.ACTIVE : linkStatus;
        metadata = copyMap(metadata);
    }

    public static MarketingCampaignLink createExisting(Long id,
                                                       Long tenantId,
                                                       Long campaignId,
                                                       String resourceType,
                                                       Long resourceId,
                                                       CampaignKey resourceKey,
                                                       String resourceName,
                                                       String resourceRoute,
                                                       String dependencyRole,
                                                       String linkStatus,
                                                       boolean requiredForLaunch,
                                                       Map<String, Object> metadata,
                                                       String createdBy,
                                                       String updatedBy,
                                                       LocalDateTime createdAt,
                                                       LocalDateTime updatedAt) {
        return new MarketingCampaignLink(
                id,
                tenantId,
                campaignId,
                resourceType,
                resourceId,
                resourceKey,
                resourceName,
                resourceRoute,
                dependencyRole,
                CampaignLinkStatus.from(linkStatus),
                requiredForLaunch,
                metadata,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt);
    }

    public MarketingCampaignLink withId(Long id) {
        return new MarketingCampaignLink(
                id,
                tenantId,
                campaignId,
                resourceType,
                resourceId,
                resourceKey,
                resourceName,
                resourceRoute,
                dependencyRole,
                linkStatus,
                requiredForLaunch,
                metadata,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
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
