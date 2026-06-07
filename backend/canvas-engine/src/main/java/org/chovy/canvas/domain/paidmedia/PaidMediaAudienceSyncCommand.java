package org.chovy.canvas.domain.paidmedia;

import java.util.List;
import java.util.Map;

public record PaidMediaAudienceSyncCommand(
        Long destinationId,
        Long audienceId,
        List<String> userIds,
        String externalOperationId,
        Map<String, Object> metadata) {

    public PaidMediaAudienceSyncCommand {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
