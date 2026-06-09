package org.chovy.canvas.domain.creator;

/**
 * CreatorProviderMutationApprovalCommand 承载 domain.creator 场景中的不可变数据快照。
 * @param decision decision 字段。
 * @param reason reason 字段。
 */
public record CreatorProviderMutationApprovalCommand(
        String decision,
        String reason
) {
}
