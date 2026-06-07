package org.chovy.canvas.domain.programmatic;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record ProgrammaticDspLineItemView(
        Long id,
        Long tenantId,
        Long seatId,
        Long campaignId,
        String lineItemKey,
        String lineItemName,
        String bidStrategy,
        BigDecimal maxBidCpm,
        BigDecimal dailyBudgetAmount,
        BigDecimal totalBudgetAmount,
        String pacingMode,
        Map<String, Object> targeting,
        Integer frequencyCap,
        String status,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
