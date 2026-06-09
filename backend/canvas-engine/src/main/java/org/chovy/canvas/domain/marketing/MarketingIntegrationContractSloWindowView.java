package org.chovy.canvas.domain.marketing;

/**
 * MarketingIntegrationContractSloWindowView 承载 domain.marketing 场景中的不可变数据快照。
 * @param ruleKey ruleKey 字段。
 * @param windowKey windowKey 字段。
 * @param windowMinutes windowMinutes 字段。
 * @param totalCount totalCount 字段。
 * @param badCount badCount 字段。
 * @param badRatio badRatio 字段。
 * @param burnRate burnRate 字段。
 * @param thresholdBurnRate thresholdBurnRate 字段。
 * @param sufficient sufficient 字段。
 * @param breached breached 字段。
 * @param windowStart windowStart 字段。
 * @param windowEnd windowEnd 字段。
 */
public record MarketingIntegrationContractSloWindowView(
        String ruleKey,
        String windowKey,
        Integer windowMinutes,
        Long totalCount,
        Long badCount,
        Double badRatio,
        Double burnRate,
        Double thresholdBurnRate,
        Boolean sufficient,
        Boolean breached,
        String windowStart,
        String windowEnd) {
}
