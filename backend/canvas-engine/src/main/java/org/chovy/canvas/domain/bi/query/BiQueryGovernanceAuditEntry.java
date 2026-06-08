package org.chovy.canvas.domain.bi.query;

import java.time.LocalDateTime;

/**
 * BiQueryGovernanceAuditEntry 承载 domain.bi.query 场景中的不可变数据快照。
 * @param id id 字段。
 * @param actorId actorId 字段。
 * @param actionKey actionKey 字段。
 * @param resourceType resourceType 字段。
 * @param detailJson detailJson 字段。
 * @param createdAt createdAt 字段。
 */
public record BiQueryGovernanceAuditEntry(
        Long id,
        String actorId,
        String actionKey,
        String resourceType,
        String detailJson,
        LocalDateTime createdAt
) {
}
