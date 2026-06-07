package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationRouteResultView(Long tenantId,
                                          Long workItemId,
                                          boolean routed,
                                          String assignedTo,
                                          String assignedTeam,
                                          String routingStatus,
                                          String routingReason,
                                          List<String> requiredSkills,
                                          LocalDateTime slaDueAt) {

    public ConversationRouteResultView {
        requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
    }
}
