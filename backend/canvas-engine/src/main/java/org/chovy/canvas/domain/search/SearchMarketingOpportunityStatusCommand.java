package org.chovy.canvas.domain.search;

/**
 * SearchMarketingOpportunityStatusCommand 承载 domain.search 场景中的不可变数据快照。
 * @param status status 字段。
 * @param reason reason 字段。
 */
public record SearchMarketingOpportunityStatusCommand(
        String status,
        String reason) {
}
