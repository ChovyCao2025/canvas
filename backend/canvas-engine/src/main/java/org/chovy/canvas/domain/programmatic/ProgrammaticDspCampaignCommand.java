package org.chovy.canvas.domain.programmatic;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * ProgrammaticDspCampaignCommand 承载 domain.programmatic 场景中的不可变数据快照。
 * @param campaignKey campaignKey 字段。
 * @param campaignName campaignName 字段。
 * @param objective objective 字段。
 * @param budgetAmount budgetAmount 字段。
 * @param currency currency 字段。
 * @param startAt startAt 字段。
 * @param endAt endAt 字段。
 * @param status status 字段。
 * @param metadata metadata 字段。
 */
public record ProgrammaticDspCampaignCommand(
        String campaignKey,
        String campaignName,
        String objective,
        BigDecimal budgetAmount,
        String currency,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        Map<String, Object> metadata) {
}
