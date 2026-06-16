package org.chovy.canvas.conversation.api;

/**
 * 入站消息记录后的处理结果。
 *
 * @param sessionId 会话标识
 * @param messageId 消息标识
 * @param status 记录状态
 * @param duplicate 是否命中幂等重复
 * @param resumedWaitCount 被恢复的等待节点数量
 */
public record ConversationRecordResult(
        /**
         * 会话标识。
         */
        Long sessionId,
        /**
         * 消息标识。
         */
        Long messageId,
        /**
         * 记录状态。
         */
        String status,
        /**
         * 是否命中幂等重复。
         */
        boolean duplicate,
        /**
         * 被恢复的等待节点数量。
         */
        int resumedWaitCount) {
}
