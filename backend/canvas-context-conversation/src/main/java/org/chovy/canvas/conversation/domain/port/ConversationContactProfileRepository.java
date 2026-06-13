package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationContactProfile;

import java.util.Optional;

public interface ConversationContactProfileRepository {

    Optional<ConversationContactProfile> byUser(Long tenantId, String userId);

    ConversationContactProfile save(ConversationContactProfile profile);
}
