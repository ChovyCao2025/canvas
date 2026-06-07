package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record CreatorDeliverableCommand(
        Long collaborationId,
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
        Map<String, Object> metadata) {

    public CreatorDeliverableCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
