package org.chovy.canvas.conversation.api;

/**
 * 工单人工分配请求。
 *
 * @param assignedTo 目标处理人
 * @param assignedTeam 目标处理团队
 * @param note 分配原因或备注
 */
public record ConversationAssignmentCommand(
        /**
         * 目标处理人。
         */
        String assignedTo,
        /**
         * 目标处理团队。
         */
        String assignedTeam,
        /**
         * 分配原因或备注。
         */
        String note) {
}
