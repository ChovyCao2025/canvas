package org.chovy.canvas.domain.search;

/**
 * SearchMarketingMutationApprovalCommand 承载 domain.search 场景中的不可变数据快照。
 * @param decision decision 字段。
 * @param reason reason 字段。
 */
public record SearchMarketingMutationApprovalCommand(String decision, String reason) {
}
