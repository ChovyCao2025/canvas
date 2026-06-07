package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;

public record PrivateDomainGroupMemberView(
        Long id,
        Long tenantId,
        String provider,
        String externalGroupId,
        String memberUserId,
        String memberType,
        String displayName,
        String unionIdHash,
        LocalDateTime joinTime,
        LocalDateTime syncedAt) {
}
