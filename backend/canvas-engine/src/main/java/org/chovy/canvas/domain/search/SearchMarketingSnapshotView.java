package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record SearchMarketingSnapshotView(
        Long id,
        Long tenantId,
        Long sourceId,
        Long keywordId,
        String channel,
        LocalDate snapshotDate,
        String device,
        String country,
        String queryGroupKey,
        Long impressionCount,
        Long clickCount,
        BigDecimal costAmount,
        Long conversionCount,
        BigDecimal revenueAmount,
        BigDecimal averagePosition,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingSnapshotView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
