package org.chovy.canvas.domain.programmatic;

import java.util.Map;

/**
 * ProgrammaticDspMutationExecuteCommand 承载 domain.programmatic 场景中的不可变数据快照。
 * @param dryRun dryRun 字段。
 * @param partialFailure partialFailure 字段。
 * @param metadata metadata 字段。
 */
public record ProgrammaticDspMutationExecuteCommand(
        Boolean dryRun,
        Boolean partialFailure,
        Map<String, Object> metadata
) {
}
