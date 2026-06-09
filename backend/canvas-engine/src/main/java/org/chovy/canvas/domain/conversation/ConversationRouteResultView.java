package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ConversationRouteResultView 承载 domain.conversation 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param workItemId workItemId 字段。
 * @param routed routed 字段。
 * @param assignedTo assignedTo 字段。
 * @param assignedTeam assignedTeam 字段。
 * @param routingStatus routingStatus 字段。
 * @param routingReason routingReason 字段。
 * @param requiredSkills requiredSkills 字段。
 * @param slaDueAt slaDueAt 字段。
 */
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
