package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

public record GrowthActivityCommand(
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
        Map<String, Object> metadata) {
}
