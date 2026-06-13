package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationWorkItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConversationWorkItemRepository {

    Optional<ConversationWorkItem> bySession(Long tenantId, Long sessionId);

    Optional<ConversationWorkItem> byId(Long id);

    List<ConversationWorkItem> dueForSla(Long tenantId, LocalDateTime now, int limit);

    ConversationWorkItem save(ConversationWorkItem workItem);
}
