package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * PrivateDomainSyncRunView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param provider provider 字段。
 * @param syncType syncType 字段。
 * @param status status 字段。
 * @param requestedBy requestedBy 字段。
 * @param sourceCursor sourceCursor 字段。
 * @param nextCursor nextCursor 字段。
 * @param contactCount contactCount 字段。
 * @param contactUpserted contactUpserted 字段。
 * @param groupCount groupCount 字段。
 * @param groupUpserted groupUpserted 字段。
 * @param memberCount memberCount 字段。
 * @param memberUpserted memberUpserted 字段。
 * @param failedCount failedCount 字段。
 * @param errorMessage errorMessage 字段。
 * @param metadata metadata 字段。
 * @param startedAt startedAt 字段。
 * @param completedAt completedAt 字段。
 */
public record PrivateDomainSyncRunView(
        Long id,
        Long tenantId,
        String provider,
        String syncType,
        String status,
        String requestedBy,
        String sourceCursor,
        String nextCursor,
        Integer contactCount,
        Integer contactUpserted,
        Integer groupCount,
        Integer groupUpserted,
        Integer memberCount,
        Integer memberUpserted,
        Integer failedCount,
        String errorMessage,
        Map<String, Object> metadata,
        LocalDateTime startedAt,
        LocalDateTime completedAt) {

    public PrivateDomainSyncRunView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
