package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationRoutingRule;

import java.util.List;
import java.util.Optional;

public interface ConversationRoutingRuleRepository {

    Optional<ConversationRoutingRule> byKey(Long tenantId, String ruleKey);

    List<ConversationRoutingRule> enabled(Long tenantId);

    ConversationRoutingRule save(ConversationRoutingRule rule);
}
