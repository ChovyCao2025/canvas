package org.chovy.canvas.domain.programmatic;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ProgrammaticDspLineItemCommand 承载 domain.programmatic 场景中的不可变数据快照。
 * @param seatId seatId 字段。
 * @param campaignId campaignId 字段。
 * @param lineItemKey lineItemKey 字段。
 * @param lineItemName lineItemName 字段。
 * @param bidStrategy bidStrategy 字段。
 * @param maxBidCpm maxBidCpm 字段。
 * @param dailyBudgetAmount dailyBudgetAmount 字段。
 * @param totalBudgetAmount totalBudgetAmount 字段。
 * @param pacingMode pacingMode 字段。
 * @param targeting targeting 字段。
 * @param frequencyCap frequencyCap 字段。
 * @param status status 字段。
 * @param metadata metadata 字段。
 */
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
