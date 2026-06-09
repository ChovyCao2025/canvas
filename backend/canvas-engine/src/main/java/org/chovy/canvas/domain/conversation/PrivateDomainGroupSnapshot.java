package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * PrivateDomainGroupSnapshot 承载 domain.conversation 场景中的不可变数据快照。
 * @param externalGroupId externalGroupId 字段。
 * @param name name 字段。
 * @param ownerUserId ownerUserId 字段。
 * @param status status 字段。
 * @param memberCount memberCount 字段。
 * @param createdAtRemote createdAtRemote 字段。
 * @param members members 字段。
 * @param rawPayload rawPayload 字段。
 */
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
