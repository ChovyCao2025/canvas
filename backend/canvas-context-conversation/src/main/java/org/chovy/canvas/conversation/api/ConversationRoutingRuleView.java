package org.chovy.canvas.conversation.api;

import java.util.List;
import java.util.Map;

public record ConversationRoutingRuleView(
        Long id,
        Long tenantId,
        String ruleKey,
        String channel,
        String minPriority,
        List<String> requiredSkills,
        String targetTeam,
        int slaMinutes,
        boolean enabled,
        int sortOrder,
        Map<String, Object> metadata) {
}
