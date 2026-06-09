package org.chovy.canvas.domain.creator;

/**
 * CreatorProviderMutationQuery 承载 domain.creator 场景中的不可变数据快照。
 * @param campaignId campaignId 字段。
 * @param collaborationId collaborationId 字段。
 * @param status status 字段。
 * @param approvalStatus approvalStatus 字段。
 * @param limit limit 字段。
 */
public record CreatorProviderMutationQuery(
        Long campaignId,
        Long collaborationId,
        String status,
        String approvalStatus,
        Integer limit
) {
}
