package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record GrowthActivityReportView(
        Long tenantId,
        Long activityId,
        ParticipationMetrics participation,
        ReferralMetrics referral,
        GrantMetrics grants,
        ConversionMetrics conversion,
        TaskMetrics task) {

    public record ParticipationMetrics(
            long totalParticipants,
            long activeParticipants) {
    }

    public record ReferralMetrics(
            long totalRelations,
            long qualifiedRelations,
            long pendingRelations,
            long rejectedRelations) {
    }

    public record GrantMetrics(
            long totalGrants,
            long reservedGrants,
            long successGrants,
            long failedGrants,
            long canceledGrants,
            long redeemedGrants,
            long expiredGrants,
            BigDecimal totalCost) {
    }

    public record ConversionMetrics(
            long conversionCount,
            BigDecimal conversionAmount,
            BigDecimal roi) {
    }

    public record TaskMetrics(
            long totalProgress,
            long completedProgress,
            BigDecimal completionRate) {
    }

    static BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
