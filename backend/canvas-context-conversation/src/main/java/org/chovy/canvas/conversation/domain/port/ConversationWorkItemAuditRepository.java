package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationWorkItemAudit;

public interface ConversationWorkItemAuditRepository {

    ConversationWorkItemAudit save(ConversationWorkItemAudit audit);
}
