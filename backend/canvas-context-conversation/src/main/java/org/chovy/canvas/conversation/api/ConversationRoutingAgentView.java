package org.chovy.canvas.conversation.api;

import java.util.List;
import java.util.Map;

public record ConversationRoutingAgentView(
        Long id,
        Long tenantId,
        String agentKey,
        String displayName,
        String teamKey,
        String status,
        int maxCapacity,
        int currentLoad,
        List<String> skills,
        Map<String, Object> metadata) {
}
