package org.chovy.canvas.conversation.api;

import java.util.List;
import java.util.Map;

public record ConversationRoutingAgentCommand(
        String agentKey,
        String displayName,
        String teamKey,
        String status,
        Integer maxCapacity,
        Integer currentLoad,
        List<String> skills,
        Map<String, Object> metadata) {
}
