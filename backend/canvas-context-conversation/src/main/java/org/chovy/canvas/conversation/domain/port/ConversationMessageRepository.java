package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationMessage;

import java.util.Optional;

public interface ConversationMessageRepository {

    Optional<ConversationMessage> byIdempotencyKey(Long tenantId, String idempotencyKey);

    ConversationMessage save(ConversationMessage message);
}
