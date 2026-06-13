package org.chovy.canvas.conversation.api;

import java.util.List;

public record ConversationRouteCommand(
        List<String> requiredSkills,
        String targetTeam,
        Integer slaMinutes,
        String note) {
}
