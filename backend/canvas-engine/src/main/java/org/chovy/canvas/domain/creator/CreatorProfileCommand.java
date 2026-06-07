package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreatorProfileCommand(
        String provider,
        String handle,
        String displayName,
        String creatorTier,
        String primaryChannel,
        Long followerCount,
        BigDecimal avgEngagementRate,
        List<String> tags,
        String status,
        String riskStatus,
        Map<String, Object> metadata) {

    public CreatorProfileCommand {
        tags = tags == null ? List.of() : List.copyOf(tags);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
