package org.chovy.canvas.conversation.domain;

import java.util.List;

public record ConversationRouteRequest(
        List<String> requiredSkills,
        String targetTeam,
        Integer slaMinutes,
        String note) {

    public ConversationRouteRequest {
        requiredSkills = DomainMaps.copyList(requiredSkills);
    }
}
