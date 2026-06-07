package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public record PrivateDomainGroupMemberSnapshot(
        String memberUserId,
        String memberType,
        String displayName,
        String unionIdHash,
        LocalDateTime joinTime,
        Map<String, Object> rawPayload) {

    public PrivateDomainGroupMemberSnapshot {
        rawPayload = rawPayload == null ? Map.of() : Map.copyOf(rawPayload);
    }
}
