package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record CreatorDeliverableView(
        Long id,
        Long tenantId,
        Long collaborationId,
        Long campaignId,
        Long creatorId,
        String deliverableKey,
        String contentType,
        String platform,
        LocalDateTime dueAt,
        LocalDateTime postedAt,
        String contentUrl,
        String status,
        Long impressionCount,
        Long likeCount,
        Long commentCount,
        Long shareCount,
        Long saveCount,
        Long clickCount,
        Long conversionCount,
        BigDecimal revenueAmount,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public CreatorDeliverableView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
