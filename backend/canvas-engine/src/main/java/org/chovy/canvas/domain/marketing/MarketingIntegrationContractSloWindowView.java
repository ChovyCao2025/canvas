package org.chovy.canvas.domain.marketing;

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
