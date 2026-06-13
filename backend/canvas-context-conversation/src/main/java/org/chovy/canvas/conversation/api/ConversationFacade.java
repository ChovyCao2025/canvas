package org.chovy.canvas.conversation.api;

public interface ConversationFacade {

    ConversationRecordResult recordInbound(ConversationInboundCommand command);

    ConversationWorkItemView ensureWorkItemForSession(Long tenantId, Long sessionId, String actor);

    ConversationWorkItemView assignWorkItem(Long tenantId, Long workItemId, ConversationAssignmentCommand command, String actor);

    ConversationWorkItemView updateWorkItemStatus(Long tenantId, Long workItemId, ConversationWorkItemStatusCommand command, String actor);

    ConversationRoutingAgentView upsertRoutingAgent(Long tenantId, ConversationRoutingAgentCommand command, String actor);

    ConversationRoutingRuleView upsertRoutingRule(Long tenantId, ConversationRoutingRuleCommand command, String actor);

    ConversationRouteResultView routeWorkItem(Long tenantId, Long workItemId, ConversationRouteCommand command, String actor);
}
