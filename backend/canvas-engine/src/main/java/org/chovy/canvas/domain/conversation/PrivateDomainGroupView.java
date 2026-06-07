package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;

public record PrivateDomainGroupView(
        Long id,
        Long tenantId,
        String provider,
        String externalGroupId,
        String name,
        String ownerUserId,
        String status,
        Integer memberCount,
        LocalDateTime createdAtRemote,
        List<PrivateDomainGroupMemberView> members,
        LocalDateTime syncedAt) {

    public PrivateDomainGroupView {
        members = members == null ? List.of() : List.copyOf(members);
    }
}
