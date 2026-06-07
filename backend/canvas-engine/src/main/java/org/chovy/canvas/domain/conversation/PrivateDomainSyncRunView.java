package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record PrivateDomainSyncRunView(
        Long id,
        Long tenantId,
        String provider,
        String syncType,
        String status,
        String requestedBy,
        String sourceCursor,
        String nextCursor,
        Integer contactCount,
        Integer contactUpserted,
        Integer groupCount,
        Integer groupUpserted,
        Integer memberCount,
        Integer memberUpserted,
        Integer failedCount,
        String errorMessage,
        Map<String, Object> metadata,
        LocalDateTime startedAt,
        LocalDateTime completedAt) {

    public PrivateDomainSyncRunView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
