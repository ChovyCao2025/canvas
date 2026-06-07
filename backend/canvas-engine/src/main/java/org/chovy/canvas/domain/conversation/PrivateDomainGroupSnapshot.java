package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PrivateDomainGroupSnapshot(
        String externalGroupId,
        String name,
        String ownerUserId,
        String status,
        Integer memberCount,
        LocalDateTime createdAtRemote,
        List<PrivateDomainGroupMemberSnapshot> members,
        Map<String, Object> rawPayload) {

    public PrivateDomainGroupSnapshot {
        members = members == null ? List.of() : List.copyOf(members);
        rawPayload = rawPayload == null ? Map.of() : Map.copyOf(rawPayload);
    }
}
