package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;

public record PrivateDomainSyncCommand(
        String provider,
        String syncType,
        String sourceCursor,
        String nextCursor,
        List<PrivateDomainContactSnapshot> contacts,
        List<PrivateDomainGroupSnapshot> groups,
        Map<String, Object> metadata) {

    public PrivateDomainSyncCommand {
        contacts = contacts == null ? List.of() : List.copyOf(contacts);
        groups = groups == null ? List.of() : List.copyOf(groups);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
