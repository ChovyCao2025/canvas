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

/**
 * 编排会话记录、工单、路由和演示查询能力的应用服务。
 */
@Service
public class ConversationApplicationService implements ConversationFacade {

    /**
     * 入站会话消息用于恢复等待节点的事件编码。
     */
    public static final String EVENT_CODE = "CONVERSATION_REPLY";

    /**
     * 入站消息成功记录后的状态值。
     */
    public static final String STATUS_RECORDED = "RECORDED";

    /**
     * 会话仓储。
     */
    private final ConversationSessionRepository sessionRepository;

    /**
     * 会话消息仓储。
     */
    private final ConversationMessageRepository messageRepository;

    /**
     * 联系人画像仓储。
     */
    private final ConversationContactProfileRepository profileRepository;

    /**
     * 会话工单仓储。
     */
    private final ConversationWorkItemRepository workItemRepository;

    /**
     * 工单审计仓储。
     */
    private final ConversationWorkItemAuditRepository auditRepository;

    /**
     * 路由坐席仓储。
     */
    private final ConversationRoutingAgentRepository agentRepository;

    /**
     * 路由规则仓储。
     */
    private final ConversationRoutingRuleRepository ruleRepository;

    /**
     * SLA 违约仓储，保留给 SLA 检测路径使用。
     */
    @SuppressWarnings("unused")
    private final ConversationSlaBreachRepository breachRepository;

    /**
     * 等待节点恢复端口。
     */
    private final ConversationWaitResumePort waitResumePort;

    /**
     * 领域路由策略。
     */
    private final ConversationRoutingPolicy routingPolicy;

    /**
     * 生成业务时间的时钟。
     */
    private final Clock clock;

    /**
     * 使用 Spring 注入的必需依赖创建应用服务。
     *
     * @param sessionRepository 会话仓储
     * @param messageRepository 消息仓储
     * @param profileRepository 联系人画像仓储
     * @param workItemRepository 工单仓储
     * @param auditRepository 审计仓储
     * @param agentRepository 路由坐席仓储
     * @param ruleRepository 路由规则仓储
     * @param breachRepository SLA 违约仓储
     * @param waitResumePort 等待恢复端口
     */
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

    /**
     * 使用指定时钟创建应用服务，测试可传入固定时钟。
     *
     * @param sessionRepository 会话仓储
     * @param messageRepository 消息仓储
     * @param profileRepository 联系人画像仓储
     * @param workItemRepository 工单仓储
     * @param auditRepository 审计仓储
     * @param agentRepository 路由坐席仓储
     * @param ruleRepository 路由规则仓储
     * @param breachRepository SLA 违约仓储
     * @param waitResumePort 等待恢复端口
     * @param clock 生成业务时间的时钟
     */
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

    /**
     * 记录入站消息，幂等命中时直接返回已有消息结果。
     *
     * @param command 入站消息命令
     * @return 消息记录结果
     */
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

        // 幂等键命中时不再恢复等待节点，避免重复事件造成二次推进。
        return messageRepository.byIdempotencyKey(tenantId, idempotencyKey)
                .map(message -> new ConversationRecordResult(message.sessionId(), message.id(), STATUS_RECORDED, true, 0))
                .orElseGet(() -> recordNewInbound(command, tenantId, userId, channel, provider, messageType, occurredAt, idempotencyKey));
    }

    /**
     * 确保会话已沉淀为可处理工单。
     *
     * @param tenantId 租户标识
     * @param sessionId 会话标识
     * @param actor 操作者
     * @return 工单视图
     */
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

    /**
     * 分配工单处理人或处理团队。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param command 分配命令
     * @param actor 操作者
     * @return 更新后的工单视图
     */
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

    /**
     * 更新工单状态和优先级。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param command 状态更新命令
     * @param actor 操作者
     * @return 更新后的工单视图
     */
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

    /**
     * 新建或更新路由坐席配置。
     *
     * @param tenantId 租户标识
     * @param command 坐席配置命令
     * @param actor 操作者
     * @return 坐席视图
     */
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

    /**
     * 新建或更新路由规则配置。
     *
     * @param tenantId 租户标识
     * @param command 规则配置命令
     * @param actor 操作者
     * @return 规则视图
     */
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

    /**
     * 对工单执行路由并写入审计。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param command 路由命令
     * @param actor 操作者
     * @return 路由结果视图
     */
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
        // 成功路由后立即增加坐席负载，使后续路由看到最新承接量。
        decision.agent().ifPresent(agent -> agentRepository.save(agent.withCurrentLoad(agent.currentLoad() + 1)));
        audit(item.tenantId(), item.id(), decision.routed() ? "ROUTED" : "ROUTING_MISSED", actor,
                values("assignedTo", item.assignedTo(), "assignedTeam", item.assignedTeam()),
                values("assignedTo", updated.assignedTo(), "assignedTeam", updated.assignedTeam(),
                        "requiredSkills", updated.requiredSkills(), "slaDueAt", updated.slaDueAt()),
                request.note());
        return toRouteResult(updated, decision.routed());
    }

    /**
     * 将适配器原始入站载荷规整为会话入站命令。
     *
     * @param tenantId 租户标识
     * @param adapterKey 适配器键
     * @param payload 原始载荷
     * @param actor 操作者
     * @return 入站记录摘要列表
     */
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

    /**
     * 查询会话演示数据。
     *
     * @param tenantId 租户标识
     * @param userId 用户过滤条件
     * @param channel 渠道过滤条件
     * @param limit 返回数量上限
     * @return 会话摘要列表
     */
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

    /**
     * 查询会话消息演示数据。
     *
     * @param tenantId 租户标识
     * @param sessionId 会话标识
     * @param limit 返回数量上限
     * @return 消息摘要列表
     */
    @Override
    public List<Map<String, Object>> listMessages(Long tenantId, Long sessionId, int limit) {
        Long scopedTenantId = requireTenant(tenantId);
        return List.of(messagePayload(scopedTenantId, requiredId(sessionId, "sessionId"), 200L, "hello", now()));
    }

    /**
     * 查询工单收件箱演示数据。
     *
     * @param tenantId 租户标识
     * @param status 状态过滤条件
     * @param assignedTo 处理人过滤条件
     * @param channel 渠道过滤条件
     * @param limit 返回数量上限
     * @return 工单视图列表
     */
    @Override
    public List<ConversationWorkItemView> inbox(Long tenantId, String status, String assignedTo, String channel, int limit) {
        Long scopedTenantId = requireTenant(tenantId);
        return List.of(toWorkItemView(seedWorkItem(scopedTenantId, status, assignedTo, channel)));
    }

    /**
     * 创建外部待办演示数据。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param command 创建参数
     * @param actor 操作者
     * @return 待办摘要
     */
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

    /**
     * 完成外部待办演示数据。
     *
     * @param tenantId 租户标识
     * @param taskId 待办标识
     * @param command 完成参数
     * @param actor 操作者
     * @return 待办摘要
     */
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

    /**
     * 汇总工单时间线演示数据。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param messageLimit 消息数量上限
     * @param auditLimit 审计数量上限
     * @return 时间线摘要
     */
    @Override
    public Map<String, Object> timeline(Long tenantId, Long workItemId, int messageLimit, int auditLimit) {
        ConversationWorkItem item = seedWorkItem(requireTenant(tenantId), null, null, null);
        return values(
                "workItemId", requiredId(workItemId, "workItemId"),
                "workItem", toWorkItemView(item),
                "messages", List.of(messagePayload(item.tenantId(), item.sessionId(), 200L, "hello", now())),
                "audits", List.of(values("auditId", 301L, "eventType", "CREATED", "createdAt", now())));
    }

    /**
     * 执行 SLA 违约检测演示逻辑。
     *
     * @param tenantId 租户标识
     * @param limit 检测数量上限
     * @param actor 操作者
     * @return SLA 检测摘要
     */
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

    /**
     * 查询 SLA 违约演示数据。
     *
     * @param tenantId 租户标识
     * @param status 状态过滤条件
     * @param limit 返回数量上限
     * @return SLA 违约摘要列表
     */
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

    /**
     * 生成 AI 回复建议演示数据。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param command 生成参数
     * @param actor 操作者
     * @return AI 回复建议摘要
     */
    @Override
    public Map<String, Object> generateAiReplySuggestion(
            Long tenantId,
            Long workItemId,
            Map<String, Object> command,
            String actor) {
        requireTenant(tenantId);
        return aiSuggestionPayload(requiredId(workItemId, "workItemId"), suggestionId(workItemId), "GENERATED", command, actor);
    }

    /**
     * 审核 AI 回复建议演示数据。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param suggestionId 建议标识
     * @param command 审核参数
     * @param actor 操作者
     * @return AI 回复建议摘要
     */
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

    /**
     * 查询 AI 回复建议演示数据。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param status 状态过滤条件
     * @param limit 返回数量上限
     * @return AI 回复建议摘要列表
     */
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

    /**
     * 摄入私域同步演示数据。
     *
     * @param tenantId 租户标识
     * @param command 同步参数
     * @param actor 操作者
     * @return 同步运行摘要
     */
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

    /**
     * 查询私域联系人演示数据。
     *
     * @param tenantId 租户标识
     * @param provider 私域服务商
     * @param ownerUserId 归属用户标识
     * @param keyword 搜索关键词
     * @param limit 返回数量上限
     * @return 私域联系人摘要列表
     */
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

    /**
     * 查询私域社群演示数据。
     *
     * @param tenantId 租户标识
     * @param provider 私域服务商
     * @param ownerUserId 归属用户标识
     * @param limit 返回数量上限
     * @return 私域社群摘要列表
     */
    @Override
    public List<Map<String, Object>> privateDomainGroups(Long tenantId, String provider, String ownerUserId, int limit) {
        requireTenant(tenantId);
        return List.of(values(
                "groupId", "group-1",
                "provider", provider,
                "ownerUserId", ownerUserId,
                "memberCount", boundedLimit(limit)));
    }

    /**
     * 查询私域同步运行演示数据。
     *
     * @param tenantId 租户标识
     * @param provider 私域服务商
     * @param limit 返回数量上限
     * @return 私域同步运行摘要列表
     */
    @Override
    public List<Map<String, Object>> privateDomainSyncRuns(Long tenantId, String provider, int limit) {
        requireTenant(tenantId);
        return List.of(values("runId", 801L, "provider", provider, "status", "SUCCESS", "limit", boundedLimit(limit)));
    }

    /**
     * 记录未命中幂等的新入站消息。
     *
     * @param command 原始入站命令
     * @param tenantId 租户标识
     * @param userId 用户标识
     * @param channel 渠道
     * @param provider 服务商
     * @param messageType 消息类型
     * @param occurredAt 消息发生时间
     * @param idempotencyKey 幂等键
     * @return 新消息记录结果
     */
    private ConversationRecordResult recordNewInbound(ConversationInboundCommand command,
                                                      Long tenantId,
                                                      String userId,
                                                      String channel,
                                                      String provider,
                                                      String messageType,
                                                      LocalDateTime occurredAt,
                                                      String idempotencyKey) {
        // 查找或创建活跃会话后再写消息，确保消息始终归属到一个会话。
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

    /**
     * 为会话创建对应工单和联系人画像。
     *
     * @param tenantId 租户标识
     * @param session 会话领域对象
     * @param actor 操作者
     * @return 新建工单
     */
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

    /**
     * 构造演示用工单。
     *
     * @param tenantId 租户标识
     * @param status 状态覆盖值
     * @param assignedTo 处理人覆盖值
     * @param channel 渠道覆盖值
     * @return 演示工单
     */
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

    /**
     * 将路由决策应用到工单。
     *
     * @param item 原始工单
     * @param decision 路由决策
     * @return 应用路由后的工单
     */
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

    /**
     * 按租户和主键加载工单，不存在时抛出业务异常。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @return 匹配的工单
     */
    private ConversationWorkItem requireWorkItem(Long tenantId, Long workItemId) {
        Long scopedTenantId = requireTenant(tenantId);
        return workItemRepository.byId(workItemId)
                .filter(row -> scopedTenantId.equals(row.tenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Conversation work item not found: " + workItemId));
    }

    /**
     * 写入工单审计事件。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param eventType 事件类型
     * @param actor 操作者
     * @param oldValue 变更前值
     * @param newValue 变更后值
     * @param note 审计备注
     */
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

    /**
     * 组装消息内容快照。
     *
     * @param command 入站命令
     * @param channel 渠道
     * @param provider 服务商
     * @param messageType 消息类型
     * @param occurredAt 消息发生时间
     * @return 消息内容快照
     */
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

    /**
     * 组装恢复等待节点所需的事件属性。
     *
     * @param command 入站命令
     * @param channel 渠道
     * @param provider 服务商
     * @param messageType 消息类型
     * @param sessionId 会话标识
     * @param messageId 消息标识
     * @return 等待恢复事件属性
     */
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
            // 将适配器 attributes 展平到顶层，方便等待节点按业务字段匹配。
            nestedMap.forEach((key, value) -> {
                if (key != null) {
                    attributes.put(String.valueOf(key), value);
                }
            });
        }
        return attributes;
    }

    /**
     * 将工单领域对象转换为对外视图。
     *
     * @param item 工单领域对象
     * @return 工单视图
     */
    private ConversationWorkItemView toWorkItemView(ConversationWorkItem item) {
        return new ConversationWorkItemView(item.id(), item.tenantId(), item.sessionId(), item.userId(),
                item.channel(), item.provider(), item.subject(), item.status(), item.priority(),
                item.assignedTo(), item.assignedTeam(), item.source(), item.slaDueAt(), item.nextFollowUpAt(),
                item.lastCustomerMessageAt(), item.lastOperatorActivityAt(), item.tags(), item.attributes(),
                item.routingStatus(), item.requiredSkills(), item.routingReason(), item.routedAt(), item.slaPolicyKey());
    }

    /**
     * 将路由坐席领域对象转换为对外视图。
     *
     * @param agent 路由坐席领域对象
     * @return 路由坐席视图
     */
    private ConversationRoutingAgentView toAgentView(ConversationRoutingAgent agent) {
        return new ConversationRoutingAgentView(agent.id(), agent.tenantId(), agent.agentKey(), agent.displayName(),
                agent.teamKey(), agent.status(), agent.maxCapacity(), agent.currentLoad(), agent.skills(), agent.metadata());
    }

    /**
     * 将路由规则领域对象转换为对外视图。
     *
     * @param rule 路由规则领域对象
     * @return 路由规则视图
     */
    private ConversationRoutingRuleView toRuleView(ConversationRoutingRule rule) {
        return new ConversationRoutingRuleView(rule.id(), rule.tenantId(), rule.ruleKey(), rule.channel(),
                rule.minPriority(), rule.requiredSkills(), rule.targetTeam(), rule.slaMinutes(), rule.enabled(),
                rule.sortOrder(), rule.metadata());
    }

    /**
     * 将路由后的工单转换为路由结果视图。
     *
     * @param item 路由后的工单
     * @param routed 是否路由成功
     * @return 路由结果视图
     */
    private ConversationRouteResultView toRouteResult(ConversationWorkItem item, boolean routed) {
        return new ConversationRouteResultView(item.tenantId(), item.id(), routed, item.routingStatus(),
                item.assignedTo(), item.assignedTeam(), item.requiredSkills(), item.routingReason(),
                item.routedAt(), item.slaDueAt(), item.slaPolicyKey());
    }

    /**
     * 按键值对顺序构造有序 Map。
     *
     * @param keysAndValues 交替出现的键和值
     * @return 有序 Map
     */
    private static Map<String, Object> values(Object... keysAndValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            values.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return values;
    }

    /**
     * 将入站记录结果转换为适配器响应载荷。
     *
     * @param result 入站记录结果
     * @param adapterKey 适配器键
     * @return 适配器响应载荷
     */
    private static Map<String, Object> recordPayload(ConversationRecordResult result, String adapterKey) {
        return values(
                "sessionId", result.sessionId(),
                "messageId", result.messageId(),
                "status", result.status(),
                "duplicate", result.duplicate(),
                "resumedWaitCount", result.resumedWaitCount(),
                "adapterKey", adapterKey);
    }

    /**
     * 构造演示消息载荷。
     *
     * @param tenantId 租户标识
     * @param sessionId 会话标识
     * @param messageId 消息标识
     * @param text 消息文本
     * @param createdAt 创建时间
     * @return 消息载荷
     */
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

    /**
     * 构造 AI 回复建议演示载荷。
     *
     * @param workItemId 工单标识
     * @param suggestionId 建议标识
     * @param status 建议状态
     * @param command 请求参数
     * @param actor 操作者
     * @return AI 建议载荷
     */
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

    /**
     * 根据工单生成稳定的演示建议标识。
     *
     * @param workItemId 工单标识
     * @return 演示建议标识
     */
    private static Long suggestionId(Long workItemId) {
        return 701L;
    }

    /**
     * 将列表数量限制在演示接口允许的范围内。
     *
     * @param limit 原始数量
     * @return 1 到 100 之间的数量
     */
    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 校验标识必填。
     *
     * @param id 标识值
     * @param name 字段名称
     * @return 已校验标识
     */
    private static Long requiredId(Long id, String name) {
        if (id == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return id;
    }

    /**
     * 从载荷中读取 Long 值。
     *
     * @param payload 原始载荷
     * @param key 字段键
     * @return 解析后的 Long 值
     */
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

    /**
     * 按多个候选键读取非空字符串值。
     *
     * @param payload 原始载荷
     * @param keys 候选字段键
     * @return 字符串值或 null
     */
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

    /**
     * 计算入站消息幂等键。
     *
     * @param channel 渠道
     * @param provider 服务商
     * @param command 入站命令
     * @return 幂等键
     */
    private static String idempotencyKey(String channel, String provider, ConversationInboundCommand command) {
        String key = ConversationText.blankToNull(command.externalMessageId());
        if (key == null) {
            // 外部消息标识缺失时退回事件标识，仍保证同一事件只处理一次。
            key = eventId(command, command.eventId());
        }
        return channel + ":" + provider + ":" + key;
    }

    /**
     * 解析入站事件标识。
     *
     * @param command 入站命令
     * @param fallback 兜底标识
     * @return 事件标识
     */
    private static String eventId(ConversationInboundCommand command, String fallback) {
        String eventId = ConversationText.blankToNull(command.eventId());
        return eventId == null ? fallback : eventId;
    }

    /**
     * 校验租户标识必填。
     *
     * @param tenantId 租户标识
     * @return 已校验租户标识
     */
    private static Long requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        return tenantId;
    }

    /**
     * 规范化并校验工单状态。
     *
     * @param status 原始状态
     * @return 规范化状态
     */
    private static String normalizeStatus(String status) {
        String normalized = ConversationText.upperRequired(status, "status is required");
        if (!List.of("OPEN", "PENDING", "SNOOZED", "RESOLVED").contains(normalized)) {
            throw new IllegalArgumentException("unsupported conversation work item status: " + status);
        }
        return normalized;
    }

    /**
     * 规范化并校验工单优先级。
     *
     * @param priority 原始优先级
     * @return 规范化优先级
     */
    private static String normalizePriority(String priority) {
        String normalized = ConversationText.upperOptional(priority, "NORMAL");
        if (!List.of("LOW", "NORMAL", "HIGH", "URGENT").contains(normalized)) {
            throw new IllegalArgumentException("unsupported conversation priority: " + priority);
        }
        return normalized;
    }

    /**
     * 规范化并校验坐席状态。
     *
     * @param status 原始坐席状态
     * @return 规范化坐席状态
     */
    private static String normalizeAgentStatus(String status) {
        String normalized = ConversationText.upperOptional(status, "AVAILABLE");
        if (!List.of("AVAILABLE", "BUSY", "OFFLINE").contains(normalized)) {
            throw new IllegalArgumentException("unsupported conversation routing agent status: " + status);
        }
        return normalized;
    }

    /**
     * 返回操作者，空白时使用系统身份。
     *
     * @param actor 原始操作者
     * @return 规范化操作者
     */
    private static String actor(String actor) {
        String scoped = ConversationText.blankToNull(actor);
        return scoped == null ? "system" : scoped;
    }

    /**
     * 返回当前业务时间。
     *
     * @return 当前本地时间
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
