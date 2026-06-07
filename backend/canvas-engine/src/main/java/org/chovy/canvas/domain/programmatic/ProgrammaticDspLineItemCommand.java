package org.chovy.canvas.domain.programmatic;

import java.math.BigDecimal;
import java.util.Map;

public record ProgrammaticDspLineItemCommand(
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
        Map<String, Object> metadata) {
}
