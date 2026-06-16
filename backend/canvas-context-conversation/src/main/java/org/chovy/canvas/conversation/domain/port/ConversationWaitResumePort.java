package org.chovy.canvas.conversation.domain.port;

import java.util.Map;

/**
 * 将入站会话事件回填给等待型画布节点的端口。
 */
public interface ConversationWaitResumePort {

    /**
     * 使用入站事件恢复正在等待相同事件的执行。
     *
     * @param eventCode 等待节点订阅的事件编码
     * @param subject 事件主题
     * @param attributes 事件属性
     * @param eventId 事件幂等标识
     * @return 被恢复的等待执行数量
     */
    int resumeEventWaits(String eventCode, String subject, Map<String, Object> attributes, String eventId);
}
