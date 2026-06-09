package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CreatorPerformanceSummaryView 承载 domain.creator 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param campaignId campaignId 字段。
 * @param creatorId creatorId 字段。
 * @param collaborationId collaborationId 字段。
 * @param deliverableCount deliverableCount 字段。
 * @param postedDeliverables postedDeliverables 字段。
 * @param overdueDeliverables overdueDeliverables 字段。
 * @param impressionCount impressionCount 字段。
 * @param engagementCount engagementCount 字段。
 * @param clickCount clickCount 字段。
 * @param conversionCount conversionCount 字段。
 * @param revenueAmount revenueAmount 字段。
 * @param fixedFeeAmount fixedFeeAmount 字段。
 * @param commissionAmount commissionAmount 字段。
 * @param totalCostAmount totalCostAmount 字段。
 * @param roi roi 字段。
 * @param evaluatedAt evaluatedAt 字段。
 */
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
