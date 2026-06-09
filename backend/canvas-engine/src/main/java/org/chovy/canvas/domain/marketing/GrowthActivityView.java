package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * GrowthActivityView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param activityKey activityKey 字段。
 * @param activityName activityName 字段。
 * @param activityType activityType 字段。
 * @param status status 字段。
 * @param campaignId campaignId 字段。
 * @param objective objective 字段。
 * @param ownerTeam ownerTeam 字段。
 * @param startAt startAt 字段。
 * @param endAt endAt 字段。
 * @param channelScope channelScope 字段。
 * @param audienceRefs audienceRefs 字段。
 * @param riskPolicyRef riskPolicyRef 字段。
 * @param experimentRef experimentRef 字段。
 * @param dashboardRef dashboardRef 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param updatedBy updatedBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record GrowthActivityView(
        Long id,
        Long tenantId,
        String activityKey,
        String activityName,
        String activityType,
        String status,
        Long campaignId,
        String objective,
        String ownerTeam,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String channelScope,
        Map<String, Object> audienceRefs,
        String riskPolicyRef,
        String experimentRef,
        String dashboardRef,
        Map<String, Object> metadata,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
