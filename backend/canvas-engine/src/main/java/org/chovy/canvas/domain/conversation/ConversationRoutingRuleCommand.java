package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;

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
