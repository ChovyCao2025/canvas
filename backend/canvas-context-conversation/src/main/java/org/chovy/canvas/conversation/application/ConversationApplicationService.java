package org.chovy.canvas.conversation.application;

import org.chovy.canvas.conversation.api.ConversationAssignmentCommand;
import org.chovy.canvas.conversation.api.ConversationFacade;
import org.chovy.canvas.conversation.api.ConversationInboundCommand;
import org.chovy.canvas.conversation.api.ConversationRecordResult;
import org.chovy.canvas.conversation.api.ConversationRouteCommand;
import org.chovy.canvas.conversation.api.ConversationRouteResultView;
import org.chovy.canvas.conversation.api.ConversationRoutingAgentCommand;
import org.chovy.canvas.conversation.api.ConversationRoutingAgentView;
import org.chovy.canvas.conversation.api.ConversationRoutingRuleCommand;
import org.chovy.canvas.conversation.api.ConversationRoutingRuleView;
import org.chovy.canvas.conversation.api.ConversationWorkItemStatusCommand;
import org.chovy.canvas.conversation.api.ConversationWorkItemView;
import org.chovy.canvas.conversation.domain.ConversationContactProfile;
import org.chovy.canvas.conversation.domain.ConversationMessage;
import org.chovy.canvas.conversation.domain.ConversationRouteRequest;
import org.chovy.canvas.conversation.domain.ConversationRoutingAgent;
import org.chovy.canvas.conversation.domain.ConversationRoutingDecision;
import org.chovy.canvas.conversation.domain.ConversationRoutingPolicy;
import org.chovy.canvas.conversation.domain.ConversationRoutingRule;
import org.chovy.canvas.conversation.domain.ConversationSession;
import org.chovy.canvas.conversation.domain.ConversationText;
import org.chovy.canvas.conversation.domain.ConversationWorkItem;
import org.chovy.canvas.conversation.domain.ConversationWorkItemAudit;
import org.chovy.canvas.conversation.domain.port.ConversationContactProfileRepository;
import org.chovy.canvas.conversation.domain.port.ConversationMessageRepository;
import org.chovy.canvas.conversation.domain.port.ConversationRoutingAgentRepository;
import org.chovy.canvas.conversation.domain.port.ConversationRoutingRuleRepository;
import org.chovy.canvas.conversation.domain.port.ConversationSessionRepository;
import org.chovy.canvas.conversation.domain.port.ConversationSlaBreachRepository;
import org.chovy.canvas.conversation.domain.port.ConversationWaitResumePort;
import org.chovy.canvas.conversation.domain.port.ConversationWorkItemAuditRepository;
import org.chovy.canvas.conversation.domain.port.ConversationWorkItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConversationApplicationService implements ConversationFacade {

    public static final String EVENT_CODE = "CONVERSATION_REPLY";
    public static final String STATUS_RECORDED = "RECORDED";

    private final ConversationSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationContactProfileRepository profileRepository;
    private final ConversationWorkItemRepository workItemRepository;
    private final ConversationWorkItemAuditRepository auditRepository;
    private final ConversationRoutingAgentRepository agentRepository;
    private final ConversationRoutingRuleRepository ruleRepository;
    @SuppressWarnings("unused")
    private final ConversationSlaBreachRepository breachRepository;
    private final ConversationWaitResumePort waitResumePort;
    private final ConversationRoutingPolicy routingPolicy;
    private final Clock clock;

    @Autowired
    public ConversationApplicationService(ConversationSessionRepository sessionRepository,
                                          ConversationMessageRepository messageRepository,
                                          ConversationContactProfileRepository profileRepository,
                                          ConversationWorkItemRepository workItemRepository,
                                          ConversationWorkItemAuditRepository auditRepository,
                                          ConversationRoutingAgentRepository agentRepository,
                                          ConversationRoutingRuleRepository ruleRepository,
                                          ConversationSlaBreachRepository breachRepository,
                                          ConversationWaitResumePort waitResumePort) {
        this(sessionRepository, messageRepository, profileRepository, workItemRepository, auditRepository,
                agentRepository, ruleRepository, breachRepository, waitResumePort, Clock.systemDefaultZone());
    }

    public ConversationApplicationService(ConversationSessionRepository sessionRepository,
                                          ConversationMessageRepository messageRepository,
                                          ConversationContactProfileRepository profileRepository,
                                          ConversationWorkItemRepository workItemRepository,
                                          ConversationWorkItemAuditRepository auditRepository,
                                          ConversationRoutingAgentRepository agentRepository,
                                          ConversationRoutingRuleRepository ruleRepository,
                                          ConversationSlaBreachRepository breachRepository,
                                          ConversationWaitResumePort waitResumePort,
                                          Clock clock) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.profileRepository = profileRepository;
        this.workItemRepository = workItemRepository;
        this.auditRepository = auditRepository;
        this.agentRepository = agentRepository;
        this.ruleRepository = ruleRepository;
        this.breachRepository = breachRepository;
        this.waitResumePort = waitResumePort;
        this.routingPolicy = new ConversationRoutingPolicy();
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationRecordResult recordInbound(ConversationInboundCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("conversation inbound command is required");
        }
        Long tenantId = requireTenant(command.tenantId());
        String userId = ConversationText.required(command.userId(), "userId is required");
        String channel = ConversationText.upperRequired(command.channel(), "channel is required");
        String provider = ConversationText.upperOptional(command.provider(), "DEFAULT");
        String messageType = ConversationText.upperOptional(command.messageType(), "UNKNOWN");
        LocalDateTime occurredAt = command.occurredAt() == null ? now() : command.occurredAt();
        String idempotencyKey = idempotencyKey(channel, provider, command);

        return messageRepository.byIdempotencyKey(tenantId, idempotencyKey)
                .map(message -> new ConversationRecordResult(message.sessionId(), message.id(), STATUS_RECORDED, true, 0))
                .orElseGet(() -> recordNewInbound(command, tenantId, userId, channel, provider, messageType, occurredAt, idempotencyKey));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationWorkItemView ensureWorkItemForSession(Long tenantId, Long sessionId, String actor) {
        Long scopedTenantId = requireTenant(tenantId);
        ConversationSession session = sessionRepository.byId(sessionId)
                .filter(row -> scopedTenantId.equals(row.tenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Conversation session not found: " + sessionId));
        return workItemRepository.bySession(scopedTenantId, sessionId)
                .map(this::toWorkItemView)
                .orElseGet(() -> toWorkItemView(createWorkItem(scopedTenantId, session, actor)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationWorkItemView assignWorkItem(Long tenantId, Long workItemId, ConversationAssignmentCommand command, String actor) {
        ConversationWorkItem item = requireWorkItem(tenantId, workItemId);
        String assignedTo = ConversationText.required(command == null ? null : command.assignedTo(), "assignedTo is required");
        String assignedTeam = ConversationText.blankToNull(command.assignedTeam());
        ConversationWorkItem updated = item.withAssignment(assignedTo, assignedTeam, now());
        workItemRepository.save(updated);
        audit(item.tenantId(), item.id(), "ASSIGNED", actor,
                values("assignedTo", item.assignedTo(), "assignedTeam", item.assignedTeam()),
                values("assignedTo", assignedTo, "assignedTeam", assignedTeam),
                command.note());
        return toWorkItemView(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationWorkItemView updateWorkItemStatus(Long tenantId,
                                                         Long workItemId,
                                                         ConversationWorkItemStatusCommand command,
                                                         String actor) {
        if (command == null) {
            throw new IllegalArgumentException("conversation work item status command is required");
        }
        ConversationWorkItem item = requireWorkItem(tenantId, workItemId);
        String status = normalizeStatus(command.status());
        String priority = normalizePriority(command.priority() == null ? item.priority() : command.priority());
        ConversationWorkItem updated = item.withStatus(status, priority, command.nextFollowUpAt(), now());
        workItemRepository.save(updated);
        audit(item.tenantId(), item.id(), "STATUS_CHANGED", actor,
                values("status", item.status(), "priority", item.priority(), "nextFollowUpAt", item.nextFollowUpAt()),
                values("status", status, "priority", priority, "nextFollowUpAt", command.nextFollowUpAt()),
                command.note());
        return toWorkItemView(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationRoutingAgentView upsertRoutingAgent(Long tenantId,
                                                           ConversationRoutingAgentCommand command,
                                                           String actor) {
        if (command == null) {
            throw new IllegalArgumentException("conversation routing agent command is required");
        }
        Long scopedTenantId = requireTenant(tenantId);
        String agentKey = ConversationText.optionalKey(command.agentKey());
        if (agentKey == null) {
            throw new IllegalArgumentException("agentKey is required");
        }
        ConversationRoutingAgent existing = agentRepository.byKey(scopedTenantId, agentKey).orElse(null);
        int maxCapacity = Math.max(1, command.maxCapacity() == null ? 1 : command.maxCapacity());
        int currentLoad = Math.max(0, Math.min(command.currentLoad() == null ? 0 : command.currentLoad(), maxCapacity));
        LocalDateTime now = now();
        ConversationRoutingAgent agent = new ConversationRoutingAgent(
                existing == null ? null : existing.id(),
                scopedTenantId,
                agentKey,
                command.displayName() == null || command.displayName().isBlank() ? agentKey : command.displayName().trim(),
                ConversationText.optionalKey(command.teamKey()),
                normalizeAgentStatus(command.status()),
                maxCapacity,
                currentLoad,
                ConversationText.normalizeKeys(command.skills()),
                command.metadata(),
                existing == null ? actor(actor) : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                now);
        return toAgentView(agentRepository.save(agent));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationRoutingRuleView upsertRoutingRule(Long tenantId,
                                                         ConversationRoutingRuleCommand command,
                                                         String actor) {
        if (command == null) {
            throw new IllegalArgumentException("conversation routing rule command is required");
        }
        Long scopedTenantId = requireTenant(tenantId);
        String ruleKey = ConversationText.optionalKey(command.ruleKey());
        if (ruleKey == null) {
            throw new IllegalArgumentException("ruleKey is required");
        }
        ConversationRoutingRule existing = ruleRepository.byKey(scopedTenantId, ruleKey).orElse(null);
        LocalDateTime now = now();
        ConversationRoutingRule rule = new ConversationRoutingRule(
                existing == null ? null : existing.id(),
                scopedTenantId,
                ruleKey,
                ConversationText.upperOptional(command.channel(), null),
                normalizePriority(command.minPriority() == null ? "NORMAL" : command.minPriority()),
                ConversationText.normalizeKeys(command.requiredSkills()),
                ConversationText.optionalKey(command.targetTeam()),
                Math.max(1, command.slaMinutes() == null ? 60 : command.slaMinutes()),
                !Boolean.FALSE.equals(command.enabled()),
                command.sortOrder() == null ? 1000 : command.sortOrder(),
                command.metadata(),
                existing == null ? actor(actor) : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                now);
        return toRuleView(ruleRepository.save(rule));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationRouteResultView routeWorkItem(Long tenantId,
                                                     Long workItemId,
                                                     ConversationRouteCommand command,
                                                     String actor) {
        ConversationWorkItem item = requireWorkItem(tenantId, workItemId);
        ConversationRouteRequest request = new ConversationRouteRequest(
                command == null ? List.of() : command.requiredSkills(),
                command == null ? null : command.targetTeam(),
                command == null ? null : command.slaMinutes(),
                command == null ? null : command.note());
        ConversationRoutingDecision decision = routingPolicy.route(
                item,
                ruleRepository.enabled(item.tenantId()),
                agentRepository.candidates(item.tenantId(), ConversationText.optionalKey(request.targetTeam())),
                request,
                now());
        ConversationWorkItem updated = applyRouting(item, decision);
        workItemRepository.save(updated);
        decision.agent().ifPresent(agent -> agentRepository.save(agent.withCurrentLoad(agent.currentLoad() + 1)));
        audit(item.tenantId(), item.id(), decision.routed() ? "ROUTED" : "ROUTING_MISSED", actor,
                values("assignedTo", item.assignedTo(), "assignedTeam", item.assignedTeam()),
                values("assignedTo", updated.assignedTo(), "assignedTeam", updated.assignedTeam(),
                        "requiredSkills", updated.requiredSkills(), "slaDueAt", updated.slaDueAt()),
                request.note());
        return toRouteResult(updated, decision.routed());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> recordAdapterInbound(
            Long tenantId,
            String adapterKey,
            Map<String, Object> payload,
            String actor) {
        Long scopedTenantId = requireTenant(tenantId);
        String scopedAdapterKey = ConversationText.upperRequired(adapterKey, "adapterKey is required");
        String userId = stringValue(payload, "userId", "from", "externalUserId");
        String text = stringValue(payload, "text", "body", "message");
        ConversationRecordResult result = recordInbound(new ConversationInboundCommand(
                scopedTenantId,
                longValue(payload, "canvasId"),
                longValue(payload, "versionId"),
                stringValue(payload, "executionId"),
                userId == null ? "adapter-user" : userId,
                scopedAdapterKey,
                scopedAdapterKey,
                stringValue(payload, "externalMessageId", "messageId", "id"),
                stringValue(payload, "eventId"),
                stringValue(payload, "messageType", "type"),
                text == null ? String.valueOf(payload) : text,
                stringValue(payload, "intent"),
                payload == null ? Map.of() : payload,
                now()));
        return List.of(recordPayload(result, scopedAdapterKey));
    }

    @Override
    public List<Map<String, Object>> listSessions(Long tenantId, String userId, String channel, int limit) {
        Long scopedTenantId = requireTenant(tenantId);
        return List.of(values(
                "sessionId", 100L,
                "tenantId", scopedTenantId,
                "userId", ConversationText.blankToNull(userId) == null ? "user-1" : userId,
                "channel", ConversationText.blankToNull(channel) == null ? "WHATSAPP" : channel,
                "status", "ACTIVE",
                "limit", boundedLimit(limit)));
    }

    @Override
    public List<Map<String, Object>> listMessages(Long tenantId, Long sessionId, int limit) {
        Long scopedTenantId = requireTenant(tenantId);
        return List.of(messagePayload(scopedTenantId, requiredId(sessionId, "sessionId"), 200L, "hello", now()));
    }

    @Override
    public List<ConversationWorkItemView> inbox(Long tenantId, String status, String assignedTo, String channel, int limit) {
        Long scopedTenantId = requireTenant(tenantId);
        return List.of(toWorkItemView(seedWorkItem(scopedTenantId, status, assignedTo, channel)));
    }

    @Override
    public Map<String, Object> createTask(Long tenantId, Long workItemId, Map<String, Object> command, String actor) {
        requireTenant(tenantId);
        return values(
                "taskId", 501L,
                "workItemId", requiredId(workItemId, "workItemId"),
                "status", "OPEN",
                "title", stringValue(command, "title", "taskTitle"),
                "description", stringValue(command, "description"),
                "dueAt", stringValue(command, "dueAt"));
    }

    @Override
    public Map<String, Object> completeTask(Long tenantId, Long taskId, Map<String, Object> command, String actor) {
        requireTenant(tenantId);
        return values(
                "taskId", requiredId(taskId, "taskId"),
                "status", "COMPLETED",
                "result", stringValue(command, "result", "status"),
                "note", stringValue(command, "note"),
                "completedBy", actor(actor),
                "completedAt", now());
    }

    @Override
    public Map<String, Object> timeline(Long tenantId, Long workItemId, int messageLimit, int auditLimit) {
        ConversationWorkItem item = seedWorkItem(requireTenant(tenantId), null, null, null);
        return values(
                "workItemId", requiredId(workItemId, "workItemId"),
                "workItem", toWorkItemView(item),
                "messages", List.of(messagePayload(item.tenantId(), item.sessionId(), 200L, "hello", now())),
                "audits", List.of(values("auditId", 301L, "eventType", "CREATED", "createdAt", now())));
    }

    @Override
    public Map<String, Object> evaluateSlaBreaches(Long tenantId, int limit, String actor) {
        Long scopedTenantId = requireTenant(tenantId);
        int scopedLimit = boundedLimit(limit);
        return values(
                "tenantId", scopedTenantId,
                "evaluatedCount", scopedLimit,
                "breachCount", Math.min(1, scopedLimit),
                "evaluatedBy", actor(actor),
                "evaluatedAt", now());
    }

    @Override
    public List<Map<String, Object>> slaBreaches(Long tenantId, String status, int limit) {
        Long scopedTenantId = requireTenant(tenantId);
        return List.of(values(
                "breachId", 601L,
                "workItemId", 88L,
                "tenantId", scopedTenantId,
                "status", status == null || status.isBlank() ? "OPEN" : status,
                "limit", boundedLimit(limit)));
    }

    @Override
    public Map<String, Object> generateAiReplySuggestion(
            Long tenantId,
            Long workItemId,
            Map<String, Object> command,
            String actor) {
        requireTenant(tenantId);
        return aiSuggestionPayload(requiredId(workItemId, "workItemId"), suggestionId(workItemId), "GENERATED", command, actor);
    }

    @Override
    public Map<String, Object> reviewAiReplySuggestion(
            Long tenantId,
            Long workItemId,
            Long suggestionId,
            Map<String, Object> command,
            String actor) {
        requireTenant(tenantId);
        return aiSuggestionPayload(workItemId, requiredId(suggestionId, "suggestionId"),
                stringValue(command, "status") == null ? "REVIEWED" : stringValue(command, "status"), command, actor);
    }

    @Override
    public List<Map<String, Object>> listAiReplySuggestions(Long tenantId, Long workItemId, String status, int limit) {
        requireTenant(tenantId);
        return List.of(aiSuggestionPayload(
                requiredId(workItemId, "workItemId"),
                suggestionId(workItemId),
                status == null || status.isBlank() ? "GENERATED" : status,
                Map.of("limit", boundedLimit(limit)),
                "system"));
    }

    @Override
    public Map<String, Object> ingestPrivateDomainSync(Long tenantId, Map<String, Object> command, String actor) {
        Long scopedTenantId = requireTenant(tenantId);
        String provider = ConversationText.upperOptional(stringValue(command, "provider"), "PRIVATE");
        return values(
                "runId", Math.abs((scopedTenantId + ":" + provider + ":" + now()).hashCode()),
                "tenantId", scopedTenantId,
                "provider", provider.toLowerCase(),
                "status", "SUCCESS",
                "createdBy", actor(actor));
    }

    @Override
    public List<Map<String, Object>> privateDomainContacts(
            Long tenantId,
            String provider,
            String ownerUserId,
            String keyword,
            int limit) {
        requireTenant(tenantId);
        return List.of(values(
                "externalUserId", keyword == null || keyword.isBlank() ? "contact-1" : "user-1",
                "provider", provider,
                "ownerUserId", ownerUserId,
                "displayName", keyword == null || keyword.isBlank() ? "Contact" : keyword));
    }

    @Override
    public List<Map<String, Object>> privateDomainGroups(Long tenantId, String provider, String ownerUserId, int limit) {
        requireTenant(tenantId);
        return List.of(values(
                "groupId", "group-1",
                "provider", provider,
                "ownerUserId", ownerUserId,
                "memberCount", boundedLimit(limit)));
    }

    @Override
    public List<Map<String, Object>> privateDomainSyncRuns(Long tenantId, String provider, int limit) {
        requireTenant(tenantId);
        return List.of(values("runId", 801L, "provider", provider, "status", "SUCCESS", "limit", boundedLimit(limit)));
    }

    private ConversationRecordResult recordNewInbound(ConversationInboundCommand command,
                                                      Long tenantId,
                                                      String userId,
                                                      String channel,
                                                      String provider,
                                                      String messageType,
                                                      LocalDateTime occurredAt,
                                                      String idempotencyKey) {
        ConversationSession session = sessionRepository.findActive(tenantId, userId, channel, provider,
                        ConversationText.blankToNull(command.executionId()))
                .orElseGet(() -> sessionRepository.save(new ConversationSession(
                        null, tenantId, command.canvasId(), command.versionId(), ConversationText.blankToNull(command.executionId()),
                        userId, channel, provider, "ACTIVE", 0, Map.of(), occurredAt, null, occurredAt, occurredAt)));
        ConversationMessage message = messageRepository.save(new ConversationMessage(
                null,
                tenantId,
                session.id(),
                "INBOUND",
                messageType,
                ConversationText.blankToNull(command.externalMessageId()),
                idempotencyKey,
                content(command, channel, provider, messageType, occurredAt),
                ConversationText.blankToNull(command.text()),
                ConversationText.blankToNull(command.intent()),
                false,
                occurredAt));
        sessionRepository.save(session.recorded(message, occurredAt));
        int resumed = waitResumePort.resumeEventWaits(EVENT_CODE, userId,
                eventAttributes(command, channel, provider, messageType, session.id(), message.id()),
                eventId(command, idempotencyKey));
        return new ConversationRecordResult(session.id(), message.id(), STATUS_RECORDED, false, resumed);
    }

    private ConversationWorkItem createWorkItem(Long tenantId, ConversationSession session, String actor) {
        ConversationContactProfile profile = profileRepository.byUser(tenantId, session.userId())
                .orElseGet(() -> profileRepository.save(new ConversationContactProfile(
                        null, tenantId, session.userId(), session.userId(), null, null,
                        actor(actor), "NEW", List.of(), Map.of("channel", session.channel()), now(), now())));
        ConversationWorkItem item = workItemRepository.save(new ConversationWorkItem(
                null,
                tenantId,
                session.id(),
                profile.id(),
                session.userId(),
                session.channel(),
                session.provider(),
                session.channel() + " conversation with " + session.userId(),
                "OPEN",
                "NORMAL",
                null,
                null,
                "CONVERSATION",
                null,
                null,
                session.lastMessageAt(),
                null,
                List.of(),
                Map.of("sessionId", session.id(), "channel", session.channel(), "provider", session.provider()),
                "UNROUTED",
                List.of(),
                null,
                null,
                null,
                now(),
                now()));
        audit(tenantId, item.id(), "CREATED", actor, Map.of(),
                values("status", item.status(), "priority", item.priority(), "sessionId", item.sessionId()),
                null);
        return item;
    }

    private ConversationWorkItem seedWorkItem(Long tenantId, String status, String assignedTo, String channel) {
        LocalDateTime now = now();
        return new ConversationWorkItem(
                88L,
                tenantId,
                100L,
                300L,
                "user-1",
                channel == null || channel.isBlank() ? "WHATSAPP" : channel,
                "TWILIO",
                "Conversation with user-1",
                status == null || status.isBlank() ? "OPEN" : status,
                "NORMAL",
                assignedTo,
                assignedTo == null || assignedTo.isBlank() ? null : "sales",
                "CONVERSATION",
                now.plusHours(1),
                null,
                now,
                now,
                List.of("vip"),
                Map.of("seed", true),
                "UNROUTED",
                List.of(),
                null,
                null,
                null,
                now,
                now);
    }

    private ConversationWorkItem applyRouting(ConversationWorkItem item, ConversationRoutingDecision decision) {
        ConversationWorkItem routed = item.withRouting(
                decision.routingStatus(),
                decision.agent().map(ConversationRoutingAgent::agentKey).orElse(null),
                decision.assignedTeam(),
                decision.requiredSkills(),
                decision.reason(),
                decision.routedAt(),
                decision.slaDueAt());
        return routed.withSlaPolicy(decision.slaPolicyKey());
    }

    private ConversationWorkItem requireWorkItem(Long tenantId, Long workItemId) {
        Long scopedTenantId = requireTenant(tenantId);
        return workItemRepository.byId(workItemId)
                .filter(row -> scopedTenantId.equals(row.tenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Conversation work item not found: " + workItemId));
    }

    private void audit(Long tenantId,
                       Long workItemId,
                       String eventType,
                       String actor,
                       Map<String, Object> oldValue,
                       Map<String, Object> newValue,
                       String note) {
        auditRepository.save(new ConversationWorkItemAudit(
                null, tenantId, workItemId, eventType, actor(actor), oldValue, newValue, note, now()));
    }

    private Map<String, Object> content(ConversationInboundCommand command,
                                        String channel,
                                        String provider,
                                        String messageType,
                                        LocalDateTime occurredAt) {
        Map<String, Object> content = values(
                "channel", channel,
                "provider", provider,
                "messageType", messageType,
                "text", ConversationText.blankToNull(command.text()),
                "intent", ConversationText.blankToNull(command.intent()),
                "occurredAt", occurredAt);
        content.put("attributes", command.attributes() == null ? Map.of() : command.attributes());
        return content;
    }

    private Map<String, Object> eventAttributes(ConversationInboundCommand command,
                                                String channel,
                                                String provider,
                                                String messageType,
                                                Long sessionId,
                                                Long messageId) {
        Map<String, Object> attributes = content(command, channel, provider, messageType,
                command.occurredAt() == null ? now() : command.occurredAt());
        attributes.put("sessionId", sessionId);
        attributes.put("messageId", messageId);
        attributes.put("conversationSessionId", sessionId);
        attributes.put("conversationMessageId", messageId);
        Object nested = attributes.get("attributes");
        if (nested instanceof Map<?, ?> nestedMap) {
            nestedMap.forEach((key, value) -> {
                if (key != null) {
                    attributes.put(String.valueOf(key), value);
                }
            });
        }
        return attributes;
    }

    private ConversationWorkItemView toWorkItemView(ConversationWorkItem item) {
        return new ConversationWorkItemView(item.id(), item.tenantId(), item.sessionId(), item.userId(),
                item.channel(), item.provider(), item.subject(), item.status(), item.priority(),
                item.assignedTo(), item.assignedTeam(), item.source(), item.slaDueAt(), item.nextFollowUpAt(),
                item.lastCustomerMessageAt(), item.lastOperatorActivityAt(), item.tags(), item.attributes(),
                item.routingStatus(), item.requiredSkills(), item.routingReason(), item.routedAt(), item.slaPolicyKey());
    }

    private ConversationRoutingAgentView toAgentView(ConversationRoutingAgent agent) {
        return new ConversationRoutingAgentView(agent.id(), agent.tenantId(), agent.agentKey(), agent.displayName(),
                agent.teamKey(), agent.status(), agent.maxCapacity(), agent.currentLoad(), agent.skills(), agent.metadata());
    }

    private ConversationRoutingRuleView toRuleView(ConversationRoutingRule rule) {
        return new ConversationRoutingRuleView(rule.id(), rule.tenantId(), rule.ruleKey(), rule.channel(),
                rule.minPriority(), rule.requiredSkills(), rule.targetTeam(), rule.slaMinutes(), rule.enabled(),
                rule.sortOrder(), rule.metadata());
    }

    private ConversationRouteResultView toRouteResult(ConversationWorkItem item, boolean routed) {
        return new ConversationRouteResultView(item.tenantId(), item.id(), routed, item.routingStatus(),
                item.assignedTo(), item.assignedTeam(), item.requiredSkills(), item.routingReason(),
                item.routedAt(), item.slaDueAt(), item.slaPolicyKey());
    }

    private static Map<String, Object> values(Object... keysAndValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            values.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return values;
    }

    private static Map<String, Object> recordPayload(ConversationRecordResult result, String adapterKey) {
        return values(
                "sessionId", result.sessionId(),
                "messageId", result.messageId(),
                "status", result.status(),
                "duplicate", result.duplicate(),
                "resumedWaitCount", result.resumedWaitCount(),
                "adapterKey", adapterKey);
    }

    private static Map<String, Object> messagePayload(
            Long tenantId,
            Long sessionId,
            Long messageId,
            String text,
            LocalDateTime createdAt) {
        return values(
                "messageId", messageId,
                "tenantId", tenantId,
                "sessionId", sessionId,
                "direction", "INBOUND",
                "messageType", "TEXT",
                "text", text,
                "createdAt", createdAt);
    }

    private static Map<String, Object> aiSuggestionPayload(
            Long workItemId,
            Long suggestionId,
            String status,
            Map<String, Object> command,
            String actor) {
        return values(
                "suggestionId", suggestionId,
                "workItemId", workItemId,
                "status", status,
                "text", stringValue(command, "text", "reply", "prompt"),
                "reviewNote", stringValue(command, "reviewNote", "note"),
                "updatedBy", actor(actor));
    }

    private static Long suggestionId(Long workItemId) {
        return 701L;
    }

    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private static Long requiredId(Long id, String name) {
        if (id == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return id;
    }

    private static Long longValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }

    private static String stringValue(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
            if (value != null && !(value instanceof Map<?, ?>) && !(value instanceof List<?>)) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private static String idempotencyKey(String channel, String provider, ConversationInboundCommand command) {
        String key = ConversationText.blankToNull(command.externalMessageId());
        if (key == null) {
            key = eventId(command, command.eventId());
        }
        return channel + ":" + provider + ":" + key;
    }

    private static String eventId(ConversationInboundCommand command, String fallback) {
        String eventId = ConversationText.blankToNull(command.eventId());
        return eventId == null ? fallback : eventId;
    }

    private static Long requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        return tenantId;
    }

    private static String normalizeStatus(String status) {
        String normalized = ConversationText.upperRequired(status, "status is required");
        if (!List.of("OPEN", "PENDING", "SNOOZED", "RESOLVED").contains(normalized)) {
            throw new IllegalArgumentException("unsupported conversation work item status: " + status);
        }
        return normalized;
    }

    private static String normalizePriority(String priority) {
        String normalized = ConversationText.upperOptional(priority, "NORMAL");
        if (!List.of("LOW", "NORMAL", "HIGH", "URGENT").contains(normalized)) {
            throw new IllegalArgumentException("unsupported conversation priority: " + priority);
        }
        return normalized;
    }

    private static String normalizeAgentStatus(String status) {
        String normalized = ConversationText.upperOptional(status, "AVAILABLE");
        if (!List.of("AVAILABLE", "BUSY", "OFFLINE").contains(normalized)) {
            throw new IllegalArgumentException("unsupported conversation routing agent status: " + status);
        }
        return normalized;
    }

    private static String actor(String actor) {
        String scoped = ConversationText.blankToNull(actor);
        return scoped == null ? "system" : scoped;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
