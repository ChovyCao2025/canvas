package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationRoutingAgent;

import java.util.List;
import java.util.Optional;

public interface ConversationRoutingAgentRepository {

    Optional<ConversationRoutingAgent> byKey(Long tenantId, String agentKey);

    List<ConversationRoutingAgent> candidates(Long tenantId, String teamKey);

    ConversationRoutingAgent save(ConversationRoutingAgent agent);
}
