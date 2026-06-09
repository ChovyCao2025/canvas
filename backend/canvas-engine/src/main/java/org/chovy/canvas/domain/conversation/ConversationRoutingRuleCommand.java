package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;

/**
 * ConversationRoutingRuleCommand 承载 domain.conversation 场景中的不可变数据快照。
 * @param ruleKey ruleKey 字段。
 * @param channel channel 字段。
 * @param minPriority minPriority 字段。
 * @param requiredSkills requiredSkills 字段。
 * @param targetTeam targetTeam 字段。
 * @param slaMinutes slaMinutes 字段。
 * @param enabled enabled 字段。
 * @param sortOrder sortOrder 字段。
 * @param metadata metadata 字段。
 */
public record ConversationRoutingRuleCommand(String ruleKey,
                                             String channel,
                                             String minPriority,
                                             List<String> requiredSkills,
                                             String targetTeam,
                                             Integer slaMinutes,
                                             Boolean enabled,
                                             Integer sortOrder,
                                             Map<String, Object> metadata) {

    public ConversationRoutingRuleCommand {
        requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
