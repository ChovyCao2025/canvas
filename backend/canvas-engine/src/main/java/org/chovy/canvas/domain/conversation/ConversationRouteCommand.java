package org.chovy.canvas.domain.conversation;

import java.util.List;

public record ConversationRouteCommand(List<String> requiredSkills,
                                       String targetTeam,
                                       Integer slaMinutes,
                                       String note) {

    public ConversationRouteCommand {
        requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
    }
}
