package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ConversationRoutingAgent(
        Long id,
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

    public ConversationRoutingAgent {
        skills = DomainMaps.copyList(skills);
        metadata = DomainMaps.copy(metadata);
    }

    public ConversationRoutingAgent withId(Long id) {
        return new ConversationRoutingAgent(id, tenantId, agentKey, displayName, teamKey, status, maxCapacity,
                currentLoad, skills, metadata, createdBy, createdAt, updatedAt);
    }

    public ConversationRoutingAgent withCurrentLoad(int currentLoad) {
        return new ConversationRoutingAgent(id, tenantId, agentKey, displayName, teamKey, status, maxCapacity,
                Math.max(0, currentLoad), skills, metadata, createdBy, createdAt, updatedAt);
    }

    boolean canHandle(String teamKey, List<String> requiredSkills) {
        return "AVAILABLE".equals(status)
                && currentLoad < maxCapacity
                && (teamKey == null || teamKey.equals(this.teamKey))
                && skills.containsAll(requiredSkills);
    }
}
