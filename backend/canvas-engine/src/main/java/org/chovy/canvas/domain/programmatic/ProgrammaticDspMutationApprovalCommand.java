package org.chovy.canvas.domain.programmatic;

/**
 * ProgrammaticDspMutationApprovalCommand 承载 domain.programmatic 场景中的不可变数据快照。
 * @param decision decision 字段。
 * @param reason reason 字段。
 */
public record ProgrammaticDspMutationApprovalCommand(
        String decision,
        String reason
) {
}
