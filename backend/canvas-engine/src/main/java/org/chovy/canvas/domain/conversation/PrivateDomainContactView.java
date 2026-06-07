package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PrivateDomainContactView(
        Long id,
        Long tenantId,
        String provider,
        String externalContactId,
        String userId,
        String displayName,
        String ownerUserId,
        String remark,
        String state,
        String addWay,
        List<String> tags,
        Map<String, Object> attributes,
        LocalDateTime syncedAt) {

    public PrivateDomainContactView {
        tags = tags == null ? List.of() : List.copyOf(tags);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
