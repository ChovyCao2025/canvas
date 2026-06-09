package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;

/**
 * PrivateDomainGroupMemberView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param provider provider 字段。
 * @param externalGroupId externalGroupId 字段。
 * @param memberUserId memberUserId 字段。
 * @param memberType memberType 字段。
 * @param displayName displayName 字段。
 * @param unionIdHash unionIdHash 字段。
 * @param joinTime joinTime 字段。
 * @param syncedAt syncedAt 字段。
 */
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
