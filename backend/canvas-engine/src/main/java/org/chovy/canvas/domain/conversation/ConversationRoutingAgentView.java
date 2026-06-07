package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
