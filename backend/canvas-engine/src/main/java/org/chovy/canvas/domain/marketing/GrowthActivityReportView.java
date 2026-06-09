package org.chovy.canvas.domain.marketing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * GrowthActivityReportView 承载对应领域的业务规则、流程编排和结果转换。
 */
public record GrowthActivityReportView(
        Long tenantId,
        Long activityId,
        ParticipationMetrics participation,
        ReferralMetrics referral,
        GrantMetrics grants,
        ConversionMetrics conversion,
        TaskMetrics task) {

    /**
     * ParticipationMetrics 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ParticipationMetrics(
            long totalParticipants,
            long activeParticipants) {
    }

    /**
     * ReferralMetrics 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ReferralMetrics(
            long totalRelations,
            long qualifiedRelations,
            long pendingRelations,
            long rejectedRelations) {
    }

    /**
     * GrantMetrics 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * ConversionMetrics 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ConversionMetrics(
            long conversionCount,
            BigDecimal conversionAmount,
            BigDecimal roi) {
    }

    /**
     * TaskMetrics 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TaskMetrics(
            long totalProgress,
            long completedProgress,
            BigDecimal completionRate) {
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param numerator numerator 参数，用于 rate 流程中的校验、计算或对象转换。
     * @param denominator denominator 参数，用于 rate 流程中的校验、计算或对象转换。
     * @return 返回 rate 计算得到的数量、金额或指标值。
     */
    static BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
