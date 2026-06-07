package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatorPerformanceSummaryView(
        Long tenantId,
        Long campaignId,
        Long creatorId,
        Long collaborationId,
        int deliverableCount,
        int postedDeliverables,
        int overdueDeliverables,
        Long impressionCount,
        Long engagementCount,
        Long clickCount,
        Long conversionCount,
        BigDecimal revenueAmount,
        BigDecimal fixedFeeAmount,
        BigDecimal commissionAmount,
        BigDecimal totalCostAmount,
        BigDecimal roi,
        LocalDateTime evaluatedAt) {
}
