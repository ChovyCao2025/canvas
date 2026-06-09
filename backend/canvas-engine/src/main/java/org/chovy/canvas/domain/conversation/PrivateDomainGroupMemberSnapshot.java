package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * PrivateDomainGroupMemberSnapshot 承载 domain.conversation 场景中的不可变数据快照。
 * @param memberUserId memberUserId 字段。
 * @param memberType memberType 字段。
 * @param displayName displayName 字段。
 * @param unionIdHash unionIdHash 字段。
 * @param joinTime joinTime 字段。
 * @param rawPayload rawPayload 字段。
 */
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
