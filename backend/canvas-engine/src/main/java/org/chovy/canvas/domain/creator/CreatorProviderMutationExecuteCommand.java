package org.chovy.canvas.domain.creator;

import java.util.Map;

/**
 * CreatorProviderMutationExecuteCommand 承载 domain.creator 场景中的不可变数据快照。
 * @param dryRun dryRun 字段。
 * @param partialFailure partialFailure 字段。
 * @param metadata metadata 字段。
 */
public record CreatorProviderMutationExecuteCommand(
        Boolean dryRun,
        Boolean partialFailure,
        Map<String, Object> metadata
) {
}
