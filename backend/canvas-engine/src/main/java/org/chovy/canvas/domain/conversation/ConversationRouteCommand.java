package org.chovy.canvas.domain.conversation;

import java.util.List;

/**
 * ConversationRouteCommand 承载 domain.conversation 场景中的不可变数据快照。
 * @param requiredSkills requiredSkills 字段。
 * @param targetTeam targetTeam 字段。
 * @param slaMinutes slaMinutes 字段。
 * @param note note 字段。
 */
public record ConversationRouteCommand(List<String> requiredSkills,
                                       String targetTeam,
                                       Integer slaMinutes,
                                       String note) {

    public ConversationRouteCommand {
        requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
    }
}
