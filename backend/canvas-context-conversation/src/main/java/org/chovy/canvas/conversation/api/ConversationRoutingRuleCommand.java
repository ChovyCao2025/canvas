package org.chovy.canvas.conversation.api;

import java.util.List;
import java.util.Map;

public record ConversationRoutingRuleCommand(
        String ruleKey,
        String channel,
        String minPriority,
        List<String> requiredSkills,
        String targetTeam,
        Integer slaMinutes,
        Boolean enabled,
        Integer sortOrder,
        Map<String, Object> metadata) {
}
