package org.chovy.canvas.domain.search;

import java.util.Map;

public record SearchMarketingMutationExecuteCommand(
        Boolean dryRun,
        Boolean partialFailure,
        Map<String, Object> metadata) {
}
