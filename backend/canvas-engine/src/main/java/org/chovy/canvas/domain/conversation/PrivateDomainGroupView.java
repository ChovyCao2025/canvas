package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PrivateDomainGroupView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param provider provider 字段。
 * @param externalGroupId externalGroupId 字段。
 * @param name name 字段。
 * @param ownerUserId ownerUserId 字段。
 * @param status status 字段。
 * @param memberCount memberCount 字段。
 * @param createdAtRemote createdAtRemote 字段。
 * @param members members 字段。
 * @param syncedAt syncedAt 字段。
 */
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
