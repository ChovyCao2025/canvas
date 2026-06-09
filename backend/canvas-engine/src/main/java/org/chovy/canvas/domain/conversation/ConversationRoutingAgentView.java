package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ConversationRoutingAgentView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param agentKey agentKey 字段。
 * @param displayName displayName 字段。
 * @param teamKey teamKey 字段。
 * @param status status 字段。
 * @param maxCapacity maxCapacity 字段。
 * @param currentLoad currentLoad 字段。
 * @param skills skills 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record ConversationRoutingAgentView(Long id,
                                           Long tenantId,
                                           String agentKey,
                                           String displayName,
                                           String teamKey,
                                           String status,
                                           int maxCapacity,
                                           int currentLoad,
                                           List<String> skills,
                                           Map<String, Object> metadata,
                                           String createdBy,
                                           LocalDateTime createdAt,
                                           LocalDateTime updatedAt) {

    public ConversationRoutingAgentView {
        skills = skills == null ? List.of() : List.copyOf(skills);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
