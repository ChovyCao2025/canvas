package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
