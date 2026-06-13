package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationSession;

import java.util.Optional;

public interface ConversationSessionRepository {

    Optional<ConversationSession> findActive(Long tenantId, String userId, String channel, String provider, String executionId);

    Optional<ConversationSession> byId(Long id);

    ConversationSession save(ConversationSession session);
}
