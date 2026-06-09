package org.chovy.canvas.domain.conversation;

/**
 * ConversationAssignmentCommand 承载 domain.conversation 场景中的不可变数据快照。
 * @param assignedTo assignedTo 字段。
 * @param assignedTeam assignedTeam 字段。
 * @param note note 字段。
 */
public record ConversationAssignmentCommand(
        String assignedTo,
        String assignedTeam,
        String note) {
}
