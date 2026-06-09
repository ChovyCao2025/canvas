package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ConversationRoutingRuleView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param ruleKey ruleKey 字段。
 * @param channel channel 字段。
 * @param minPriority minPriority 字段。
 * @param requiredSkills requiredSkills 字段。
 * @param targetTeam targetTeam 字段。
 * @param slaMinutes slaMinutes 字段。
 * @param enabled enabled 字段。
 * @param sortOrder sortOrder 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record ConversationRoutingRuleView(Long id,
                                          Long tenantId,
                                          String ruleKey,
                                          String channel,
                                          String minPriority,
                                          List<String> requiredSkills,
                                          String targetTeam,
                                          int slaMinutes,
                                          boolean enabled,
                                          int sortOrder,
                                          Map<String, Object> metadata,
                                          String createdBy,
                                          LocalDateTime createdAt,
                                          LocalDateTime updatedAt) {

    public ConversationRoutingRuleView {
        requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
