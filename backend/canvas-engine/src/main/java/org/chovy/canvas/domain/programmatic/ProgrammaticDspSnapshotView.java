package org.chovy.canvas.domain.programmatic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record ProgrammaticDspSnapshotView(
        Long id,
        Long tenantId,
        Long seatId,
        Long campaignId,
        Long lineItemId,
        LocalDate snapshotDate,
        Long bidCount,
        Long winCount,
        Long impressionCount,
        Long clickCount,
        Long conversionCount,
        Long viewableImpressionCount,
        BigDecimal spendAmount,
        BigDecimal revenueAmount,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
