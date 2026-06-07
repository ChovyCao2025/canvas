package org.chovy.canvas.domain.programmatic;

import java.util.Map;

public record ProgrammaticDspMutationExecuteCommand(
        Boolean dryRun,
        Boolean partialFailure,
        Map<String, Object> metadata
) {
}
