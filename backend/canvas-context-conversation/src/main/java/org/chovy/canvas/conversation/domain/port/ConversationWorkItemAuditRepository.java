package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationWorkItemAudit;

/**
 * 工单审计事件的领域仓储端口。
 */
public interface ConversationWorkItemAuditRepository {

    /**
     * 保存工单审计事件并返回带持久化标识的领域对象。
     *
     * @param audit 待保存的审计事件
     * @return 保存后的审计事件
     */
    ConversationWorkItemAudit save(ConversationWorkItemAudit audit);
}
