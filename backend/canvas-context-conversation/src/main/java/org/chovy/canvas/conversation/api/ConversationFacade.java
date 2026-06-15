package org.chovy.canvas.conversation.api;

import java.util.List;
import java.util.Map;

public interface ConversationFacade {

    ConversationRecordResult recordInbound(ConversationInboundCommand command);

    ConversationWorkItemView ensureWorkItemForSession(Long tenantId, Long sessionId, String actor);

    ConversationWorkItemView assignWorkItem(Long tenantId, Long workItemId, ConversationAssignmentCommand command, String actor);

    ConversationWorkItemView updateWorkItemStatus(Long tenantId, Long workItemId, ConversationWorkItemStatusCommand command, String actor);

    ConversationRoutingAgentView upsertRoutingAgent(Long tenantId, ConversationRoutingAgentCommand command, String actor);

    ConversationRoutingRuleView upsertRoutingRule(Long tenantId, ConversationRoutingRuleCommand command, String actor);

    ConversationRouteResultView routeWorkItem(Long tenantId, Long workItemId, ConversationRouteCommand command, String actor);

    List<Map<String, Object>> recordAdapterInbound(Long tenantId, String adapterKey, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listSessions(Long tenantId, String userId, String channel, int limit);

    List<Map<String, Object>> listMessages(Long tenantId, Long sessionId, int limit);

    List<ConversationWorkItemView> inbox(Long tenantId, String status, String assignedTo, String channel, int limit);

    Map<String, Object> createTask(Long tenantId, Long workItemId, Map<String, Object> command, String actor);

    Map<String, Object> completeTask(Long tenantId, Long taskId, Map<String, Object> command, String actor);

    Map<String, Object> timeline(Long tenantId, Long workItemId, int messageLimit, int auditLimit);

    Map<String, Object> evaluateSlaBreaches(Long tenantId, int limit, String actor);

    List<Map<String, Object>> slaBreaches(Long tenantId, String status, int limit);

    Map<String, Object> generateAiReplySuggestion(Long tenantId, Long workItemId, Map<String, Object> command, String actor);

    Map<String, Object> reviewAiReplySuggestion(
            Long tenantId,
            Long workItemId,
            Long suggestionId,
            Map<String, Object> command,
            String actor);

    List<Map<String, Object>> listAiReplySuggestions(Long tenantId, Long workItemId, String status, int limit);

    Map<String, Object> ingestPrivateDomainSync(Long tenantId, Map<String, Object> command, String actor);

    List<Map<String, Object>> privateDomainContacts(
            Long tenantId,
            String provider,
            String ownerUserId,
            String keyword,
            int limit);

    List<Map<String, Object>> privateDomainGroups(Long tenantId, String provider, String ownerUserId, int limit);

    List<Map<String, Object>> privateDomainSyncRuns(Long tenantId, String provider, int limit);
}
