package org.chovy.canvas.conversation.api;

import java.time.LocalDateTime;

/**
 * 更新工单状态和跟进信息的请求。
 *
 * @param status 新的工单状态
 * @param priority 新的工单优先级
 * @param nextFollowUpAt 下一次跟进时间
 * @param note 状态变更备注
 */
public record ConversationWorkItemStatusCommand(
        /**
         * 新的工单状态。
         */
        String status,
        /**
         * 新的工单优先级。
         */
        String priority,
        /**
         * 下一次跟进时间。
         */
        LocalDateTime nextFollowUpAt,
        /**
         * 状态变更备注。
         */
        String note) {
}
