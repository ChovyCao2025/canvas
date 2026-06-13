package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationSlaBreach;

import java.util.Optional;

public interface ConversationSlaBreachRepository {

    Optional<ConversationSlaBreach> openByWorkItem(Long tenantId, Long workItemId);

    ConversationSlaBreach save(ConversationSlaBreach breach);
}
