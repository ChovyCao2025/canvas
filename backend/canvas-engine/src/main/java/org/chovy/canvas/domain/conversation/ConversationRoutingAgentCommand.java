package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;

public record ConversationRoutingAgentCommand(String agentKey,
                                              String displayName,
                                              String teamKey,
                                              String status,
                                              Integer maxCapacity,
                                              Integer currentLoad,
                                              List<String> skills,
                                              Map<String, Object> metadata) {

    public ConversationRoutingAgentCommand {
        skills = skills == null ? List.of() : List.copyOf(skills);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
