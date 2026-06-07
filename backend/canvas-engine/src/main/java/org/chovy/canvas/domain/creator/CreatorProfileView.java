package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record CreatorProfileView(
        Long id,
        Long tenantId,
        String provider,
        String handle,
        String handleKey,
        String displayName,
        String creatorTier,
        String primaryChannel,
        Long followerCount,
        BigDecimal avgEngagementRate,
        List<String> tags,
        String status,
        String riskStatus,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public CreatorProfileView {
        tags = tags == null ? List.of() : List.copyOf(tags);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
