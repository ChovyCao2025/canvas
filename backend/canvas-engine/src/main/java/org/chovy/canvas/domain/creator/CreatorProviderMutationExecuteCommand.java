package org.chovy.canvas.domain.creator;

import java.util.Map;

public record CreatorProviderMutationExecuteCommand(
        Boolean dryRun,
        Boolean partialFailure,
        Map<String, Object> metadata
) {
}
