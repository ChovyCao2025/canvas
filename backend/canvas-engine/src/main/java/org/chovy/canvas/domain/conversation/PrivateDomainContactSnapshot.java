package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;

public record PrivateDomainContactSnapshot(
        String externalContactId,
        String displayName,
        String avatarUrl,
        String corpName,
        String gender,
        String unionIdHash,
        String ownerUserId,
        String remark,
        String state,
        String addWay,
        List<String> tags,
        Map<String, Object> attributes,
        Map<String, Object> rawPayload) {

    public PrivateDomainContactSnapshot {
        tags = tags == null ? List.of() : List.copyOf(tags);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        rawPayload = rawPayload == null ? Map.of() : Map.copyOf(rawPayload);
    }
}
