package org.chovy.canvas.domain.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ConversationAiReplySuggestionDO;
import org.chovy.canvas.dal.dataobject.ConversationWorkItemAuditDO;
import org.chovy.canvas.dal.mapper.ConversationAiReplySuggestionMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemAuditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ConversationAiReplyService 编排 domain.conversation 场景的领域业务规则。
 */
@Service
public class ConversationAiReplyService {

    private static final Set<String> REVIEW_STATUSES = Set.of("ACCEPTED", "REJECTED");
    private static final Set<String> LIST_STATUSES = Set.of("DRAFT", "ACCEPTED", "REJECTED", "EXPIRED");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ConversationWorkspaceService workspaceService;
    private final ConversationAiReplySuggestionMapper suggestionMapper;
    private final ConversationWorkItemAuditMapper auditMapper;
    private final ConversationAiReplyGenerator generator;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 ConversationAiReplyService 实例并注入 domain.conversation 场景依赖。
     * @param workspaceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param suggestionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param generator generator 参数，用于 ConversationAiReplyService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public ConversationAiReplyService(ConversationWorkspaceService workspaceService,
                                      ConversationAiReplySuggestionMapper suggestionMapper,
                                      ConversationWorkItemAuditMapper auditMapper,
                                      ConversationAiReplyGenerator generator,
                                      ObjectMapper objectMapper) {
        this(workspaceService, suggestionMapper, auditMapper, generator, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 ConversationAiReplyService 流程，围绕 conversation ai reply service 完成校验、计算或结果组装。
     *
     * @param workspaceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param suggestionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param generator generator 参数，用于 ConversationAiReplyService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    ConversationAiReplyService(ConversationWorkspaceService workspaceService,
                               ConversationAiReplySuggestionMapper suggestionMapper,
                               ConversationWorkItemAuditMapper auditMapper,
                               ConversationAiReplyGenerator generator,
                               ObjectMapper objectMapper,
                               Clock clock) {
        this.workspaceService = workspaceService;
        this.suggestionMapper = suggestionMapper;
        this.auditMapper = auditMapper;
        this.generator = generator;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 基于会话工单时间线生成 AI 回复建议。
     * 方法会读取工单上下文、调用回复生成器、落库建议草稿并记录工单审计；生成器失败时使用兜底回复，当前不会直接发送给用户。
     */
    public ConversationAiReplySuggestionView generate(Long tenantId,
                                                      Long workItemId,
                                                      ConversationAiReplyGenerateCommand command,
                                                      String actor) {
        Long scopedTenantId = tenantId(tenantId);
        ConversationWorkspaceTimelineView timeline = workspaceService.timeline(scopedTenantId, workItemId, 50, 20);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (timeline == null || timeline.workItem() == null || !scopedTenantId.equals(timeline.workItem().tenantId())) {
            throw new IllegalArgumentException("Conversation work item not found: " + workItemId);
        }
        ConversationMessageView sourceMessage = latestInboundText(timeline.messages());
        Map<String, Object> promptContext = promptContext(timeline, sourceMessage, command);
        ConversationAiReplyGenerationContext context = new ConversationAiReplyGenerationContext(
                scopedTenantId,
                timeline.workItem(),
                timeline.contactProfile(),
                timeline.session(),
                timeline.messages(),
                timeline.tasks(),
                promptContext,
                command == null ? new ConversationAiReplyGenerateCommand(
                        null, null, null, null, null, Map.of(), null, null) : command);
        ConversationAiReplyGenerationResult result = generator.generate(context);
        if (result == null) {
            result = new ConversationAiReplyGenerationResult(
                    "我需要再确认一下上下文后回复您。",
                    "neutral",
                    "NEEDS_CONTEXT",
                    0.35,
                    List.of("GENERATOR_FALLBACK"),
                    List.of(),
                    null,
                    null,
                    "fallback",
                    "NO_RESULT",
                    true);
        }
        List<String> riskFlags = riskFlags(result, sourceMessage, timeline.messages());
        List<String> groundingSnippets = groundingSnippets(result, sourceMessage);
        LocalDateTime now = now();
        ConversationAiReplySuggestionDO row = new ConversationAiReplySuggestionDO();
        row.setTenantId(scopedTenantId);
        row.setWorkItemId(timeline.workItem().id());
        row.setSessionId(timeline.workItem().sessionId());
        row.setSourceMessageId(sourceMessage == null ? null : sourceMessage.id());
        row.setPromptContextJson(writeJson(promptContext));
        row.setSuggestedReplyText(required(result.suggestedReplyText(), "suggested reply text is required"));
        row.setTone(defaultString(result.tone(), defaultString(command == null ? null : command.tone(), "neutral")));
        row.setIntent(defaultString(result.intent(), defaultString(command == null ? null : command.intent(), "GENERAL")));
        row.setConfidence(boundedConfidence(result.confidence()));
        row.setRiskFlagsJson(writeJson(riskFlags));
        row.setGroundingSnippetsJson(writeJson(groundingSnippets));
        row.setProviderId(result.providerId());
        row.setTemplateId(result.templateId());
        row.setModelKey(blankToNull(result.modelKey()));
        row.setProviderStatus(defaultString(result.providerStatus(), "UNKNOWN"));
        row.setFallbackUsed(result.fallbackUsed());
        row.setStatus("DRAFT");
        row.setGeneratedBy(actor(actor));
        row.setCreatedAt(now);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(now);
        suggestionMapper.insert(row);
        audit(scopedTenantId, row.getWorkItemId(), "AI_REPLY_SUGGESTED", actor,
                Map.of(),
                values("suggestionId", row.getId(), "status", row.getStatus(),
                        "sourceMessageId", row.getSourceMessageId(), "riskFlags", riskFlags),
                null);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 审阅 AI 回复建议并记录人工决策。
     * 仅 DRAFT 建议可被接受或拒绝，审阅人、时间和备注会写入建议记录，同时追加工单审计。
     */
    public ConversationAiReplySuggestionView review(Long tenantId,
                                                    Long workItemId,
                                                    Long suggestionId,
                                                    ConversationAiReplyReviewCommand command,
                                                    String actor) {
        Long scopedTenantId = tenantId(tenantId);
        ConversationAiReplySuggestionDO row = requireSuggestion(scopedTenantId, workItemId, suggestionId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!"DRAFT".equals(normalizeStatus(row.getStatus()))) {
            throw new IllegalStateException("only draft AI reply suggestions can be reviewed");
        }
        String decision = normalizeReviewDecision(command == null ? null : command.decision());
        LocalDateTime now = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationAiReplySuggestionDO update = new ConversationAiReplySuggestionDO();
        update.setId(row.getId());
        update.setStatus(decision);
        update.setReviewedBy(actor(actor));
        update.setReviewedAt(now);
        update.setReviewNote(blankToNull(command == null ? null : command.note()));
        update.setUpdatedAt(now);
        suggestionMapper.updateById(update);

        row.setStatus(decision);
        row.setReviewedBy(update.getReviewedBy());
        row.setReviewedAt(now);
        row.setReviewNote(update.getReviewNote());
        row.setUpdatedAt(now);
        audit(scopedTenantId, row.getWorkItemId(), "AI_REPLY_REVIEWED", actor,
                values("status", "DRAFT"),
                values("suggestionId", row.getId(), "status", decision),
                command == null ? null : command.note());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询租户内指定工单的 AI 回复建议列表。
     * 可按状态过滤，返回结果按创建时间倒序并受 limit 限制。
     */
    public List<ConversationAiReplySuggestionView> list(Long tenantId,
                                                        Long workItemId,
                                                        ConversationAiReplySuggestionQuery query) {
        // 准备本次流程的上下文、默认值和中间结果。
        Long scopedTenantId = tenantId(tenantId);
        // 校验策略输入和默认值，避免无效配置进入持久化或查询流程。
        if (workItemId == null) {
            throw new IllegalArgumentException("workItemId is required");
        }
        ConversationAiReplySuggestionQuery effectiveQuery = query == null
                ? new ConversationAiReplySuggestionQuery(null, 50)
                : query;
        LambdaQueryWrapper<ConversationAiReplySuggestionDO> wrapper =
                new LambdaQueryWrapper<ConversationAiReplySuggestionDO>()
                        .eq(ConversationAiReplySuggestionDO::getTenantId, scopedTenantId)
                        .eq(ConversationAiReplySuggestionDO::getWorkItemId, workItemId)
                        .orderByDesc(ConversationAiReplySuggestionDO::getCreatedAt)
                        .last("LIMIT " + boundedLimit(effectiveQuery.limit()));
        if (!isBlank(effectiveQuery.status())) {
            wrapper.eq(ConversationAiReplySuggestionDO::getStatus, normalizeStatus(effectiveQuery.status()));
        }
        // 访问持久化数据，读取现有配置或写入本次变更。
        return safeList(suggestionMapper.selectList(wrapper)).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()) && workItemId.equals(row.getWorkItemId()))
                .map(this::toView)
                .toList();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workItemId 业务对象 ID，用于定位具体记录。
     * @param suggestionId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireSuggestion 流程生成的业务结果。
     */
    private ConversationAiReplySuggestionDO requireSuggestion(Long tenantId, Long workItemId, Long suggestionId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (suggestionId == null) {
            throw new IllegalArgumentException("suggestionId is required");
        }
        if (workItemId == null) {
            throw new IllegalArgumentException("workItemId is required");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationAiReplySuggestionDO row = suggestionMapper.selectById(suggestionId);
        if (row == null || !tenantId.equals(row.getTenantId()) || !workItemId.equals(row.getWorkItemId())) {
            throw new IllegalArgumentException("AI reply suggestion not found: " + suggestionId);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param messages messages 参数，用于 latestInboundText 流程中的校验、计算或对象转换。
     * @return 返回 latestInboundText 流程生成的业务结果。
     */
    private ConversationMessageView latestInboundText(List<ConversationMessageView> messages) {
        List<ConversationMessageView> safeMessages = messages == null ? List.of() : messages;
        for (int i = safeMessages.size() - 1; i >= 0; i--) {
            ConversationMessageView message = safeMessages.get(i);
            if (message != null
                    && "INBOUND".equals(normalizeOptionalUpper(message.direction()))
                    && !isBlank(message.text())) {
                return message;
            }
        }
        return null;
    }

    /**
     * 执行 promptContext 流程，围绕 prompt context 完成校验、计算或结果组装。
     *
     * @param timeline 时间参数，用于计算窗口、过期或审计时间。
     * @param sourceMessage source message 参数，用于 promptContext 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 promptContext 流程生成的业务结果。
     */
    private Map<String, Object> promptContext(ConversationWorkspaceTimelineView timeline,
                                              ConversationMessageView sourceMessage,
                                              ConversationAiReplyGenerateCommand command) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> result = new LinkedHashMap<>();
        ConversationWorkItemView workItem = timeline.workItem();
        result.put("workItemId", workItem.id());
        result.put("sessionId", workItem.sessionId());
        result.put("channel", workItem.channel());
        result.put("provider", workItem.provider());
        result.put("subject", workItem.subject());
        result.put("priority", workItem.priority());
        result.put("status", workItem.status());
        result.put("assignedTo", workItem.assignedTo());
        result.put("assignedTeam", workItem.assignedTeam());
        result.put("tags", workItem.tags());
        result.put("requiredSkills", workItem.requiredSkills());
        result.put("contact", contactContext(timeline.contactProfile()));
        result.put("session", sessionContext(timeline.session()));
        result.put("latestInboundMessage", messageContext(sourceMessage));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        result.put("messages", safeList(timeline.messages()).stream().map(this::messageContext).toList());
        result.put("openTasks", safeList(timeline.tasks()).stream()
                .filter(task -> task != null && !"DONE".equals(normalizeOptionalUpper(task.status())))
                .map(this::taskContext)
                .toList());
        if (command != null && !isBlank(command.operatorInstruction())) {
            result.put("operatorInstruction", command.operatorInstruction().trim());
        }
        if (command != null && !isBlank(command.tone())) {
            result.put("requestedTone", command.tone().trim());
        }
        if (command != null && !isBlank(command.intent())) {
            result.put("requestedIntent", command.intent().trim());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 执行 contactContext 流程，围绕 contact context 完成校验、计算或结果组装。
     *
     * @param contact contact 参数，用于 contactContext 流程中的校验、计算或对象转换。
     * @return 返回 contactContext 流程生成的业务结果。
     */
    private Map<String, Object> contactContext(ConversationContactProfileView contact) {
        if (contact == null) {
            return Map.of();
        }
        return values(
                "userId", contact.userId(),
                "displayName", contact.displayName(),
                "owner", contact.owner(),
                "lifecycleStage", contact.lifecycleStage(),
                "tags", contact.tags(),
                "attributes", contact.attributes());
    }

    /**
     * 执行 sessionContext 流程，围绕 session context 完成校验、计算或结果组装。
     *
     * @param session session 参数，用于 sessionContext 流程中的校验、计算或对象转换。
     * @return 返回 sessionContext 流程生成的业务结果。
     */
    private Map<String, Object> sessionContext(ConversationSessionView session) {
        if (session == null) {
            return Map.of();
        }
        return values(
                "executionId", session.executionId(),
                "status", session.status(),
                "turnCount", session.turnCount(),
                "context", session.context());
    }

    /**
     * 执行 messageContext 流程，围绕 message context 完成校验、计算或结果组装。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 messageContext 流程生成的业务结果。
     */
    private Map<String, Object> messageContext(ConversationMessageView message) {
        if (message == null) {
            return Map.of();
        }
        return values(
                "id", message.id(),
                "direction", message.direction(),
                "messageType", message.messageType(),
                "text", message.text(),
                "intent", message.intent(),
                "createdAt", message.createdAt());
    }

    /**
     * 执行 taskContext 流程，围绕 task context 完成校验、计算或结果组装。
     *
     * @param task task 参数，用于 taskContext 流程中的校验、计算或对象转换。
     * @return 返回 taskContext 流程生成的业务结果。
     */
    private Map<String, Object> taskContext(ConversationSopTaskView task) {
        if (task == null) {
            return Map.of();
        }
        return values(
                "taskKey", task.taskKey(),
                "title", task.title(),
                "status", task.status(),
                "assignee", task.assignee(),
                "dueAt", task.dueAt(),
                "metadata", task.metadata());
    }

    /**
     * 执行 riskFlags 流程，围绕 risk flags 完成校验、计算或结果组装。
     *
     * @param result result 参数，用于 riskFlags 流程中的校验、计算或对象转换。
     * @param sourceMessage source message 参数，用于 riskFlags 流程中的校验、计算或对象转换。
     * @param messages messages 参数，用于 riskFlags 流程中的校验、计算或对象转换。
     * @return 返回 risk flags 汇总后的集合、分页或映射视图。
     */
    private List<String> riskFlags(ConversationAiReplyGenerationResult result,
                                   ConversationMessageView sourceMessage,
                                   List<ConversationMessageView> messages) {
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        safeList(result.riskFlags()).stream()
                .map(this::flag)
                .filter(flag -> !flag.isBlank())
                .forEach(flags::add);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sourceMessage == null) {
            flags.add("MISSING_CONTEXT");
        }
        if (boundedConfidence(result.confidence()) < 0.5) {
            flags.add("LOW_CONFIDENCE");
        }
        String text = riskText(result, sourceMessage, messages);
        if (containsAny(text, "refund", "退款", "chargeback", "退费")) {
            flags.add("SENSITIVE_REFUND");
        }
        if (containsAny(text, "legal", "lawyer", "lawsuit", "法律", "律师", "起诉")) {
            flags.add("SENSITIVE_LEGAL");
        }
        if (containsAny(text, "privacy", "personal data", "隐私", "个人信息")) {
            flags.add("SENSITIVE_PRIVACY");
        }
        if (containsAny(text, "payment", "invoice", "card", "支付", "发票", "扣款")) {
            flags.add("SENSITIVE_PAYMENT");
        }
        if (containsAny(text, "cancel", "unsubscribe", "取消", "退订")) {
            flags.add("SENSITIVE_CANCELLATION");
        }
        if (containsAny(text, "complaint", "angry", "投诉", "不满")) {
            flags.add("SENSITIVE_COMPLAINT");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ArrayList<>(flags);
    }

    /**
     * 执行 riskText 流程，围绕 risk text 完成校验、计算或结果组装。
     *
     * @param result result 参数，用于 riskText 流程中的校验、计算或对象转换。
     * @param sourceMessage source message 参数，用于 riskText 流程中的校验、计算或对象转换。
     * @param messages messages 参数，用于 riskText 流程中的校验、计算或对象转换。
     * @return 返回 risk text 生成的文本或业务键。
     */
    private String riskText(ConversationAiReplyGenerationResult result,
                            ConversationMessageView sourceMessage,
                            List<ConversationMessageView> messages) {
        StringBuilder builder = new StringBuilder();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sourceMessage != null) {
            builder.append(' ').append(sourceMessage.text());
            builder.append(' ').append(sourceMessage.intent());
        }
        if (result != null) {
            builder.append(' ').append(result.suggestedReplyText());
            builder.append(' ').append(result.intent());
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        safeList(messages).forEach(message -> {
            if (message != null) {
                builder.append(' ').append(message.text());
                builder.append(' ').append(message.intent());
            }
        });
        // 汇总前面计算出的状态和明细，返回给调用方。
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    /**
     * 执行 groundingSnippets 流程，围绕 grounding snippets 完成校验、计算或结果组装。
     *
     * @param result result 参数，用于 groundingSnippets 流程中的校验、计算或对象转换。
     * @param sourceMessage source message 参数，用于 groundingSnippets 流程中的校验、计算或对象转换。
     * @return 返回 grounding snippets 汇总后的集合、分页或映射视图。
     */
    private List<String> groundingSnippets(ConversationAiReplyGenerationResult result,
                                           ConversationMessageView sourceMessage) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<String> snippets = safeList(result.groundingSnippets()).stream()
                .filter(snippet -> !isBlank(snippet))
                .map(String::trim)
                .toList();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!snippets.isEmpty() || sourceMessage == null || isBlank(sourceMessage.text())) {
            return snippets;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.of(sourceMessage.text().trim());
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workItemId 业务对象 ID，用于定位具体记录。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param oldValue 待处理值，用于规则计算或转换。
     * @param newValue 待处理值，用于规则计算或转换。
     * @param note note 参数，用于 audit 流程中的校验、计算或对象转换。
     */
    private void audit(Long tenantId,
                       Long workItemId,
                       String eventType,
                       String actor,
                       Map<String, Object> oldValue,
                       Map<String, Object> newValue,
                       String note) {
        ConversationWorkItemAuditDO row = new ConversationWorkItemAuditDO();
        row.setTenantId(tenantId);
        row.setWorkItemId(workItemId);
        row.setEventType(eventType);
        row.setActor(actor(actor));
        row.setOldValueJson(writeJson(oldValue));
        row.setNewValueJson(writeJson(newValue));
        row.setNote(blankToNull(note));
        row.setCreatedAt(now());
        auditMapper.insert(row);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ConversationAiReplySuggestionView toView(ConversationAiReplySuggestionDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ConversationAiReplySuggestionView(
                row.getId(),
                row.getTenantId(),
                row.getWorkItemId(),
                row.getSessionId(),
                row.getSourceMessageId(),
                readMap(row.getPromptContextJson()),
                row.getSuggestedReplyText(),
                row.getTone(),
                row.getIntent(),
                row.getConfidence() == null ? 0 : row.getConfidence(),
                readStringList(row.getRiskFlagsJson()),
                readStringList(row.getGroundingSnippetsJson()),
                row.getProviderId(),
                row.getTemplateId(),
                row.getModelKey(),
                row.getProviderStatus(),
                Boolean.TRUE.equals(row.getFallbackUsed()),
                row.getStatus(),
                row.getGeneratedBy(),
                row.getReviewedBy(),
                row.getReviewedAt(),
                row.getReviewNote(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 执行 values 流程，围绕 values 完成校验、计算或结果组装。
     *
     * @param keysAndValues keys and values 参数，用于 values 流程中的校验、计算或对象转换。
     * @return 返回 values 流程生成的业务结果。
     */
    private Map<String, Object> values(Object... keysAndValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            Object key = keysAndValues[i];
            Object value = keysAndValues[i + 1];
            if (key != null) {
                result.put(String.valueOf(key), jsonValue(value));
            }
        }
        return result;
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 jsonValue 流程生成的业务结果。
     */
    private Object jsonValue(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof LocalDateTime time) {
            return time.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), jsonValue(entry.getValue()));
                }
            }
            return result;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::jsonValue).toList();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return value;
    }

    /**
     * 查询或读取业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 readMap 流程生成的业务结果。
     */
    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 read string list 汇总后的集合、分页或映射视图。
     */
    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 write json 生成的文本或业务键。
     */
    private String writeJson(Object value) {
        try {
            if (value instanceof List<?> list) {
                return objectMapper.writeValueAsString(new ArrayList<>(list));
            }
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalArgumentException("conversation AI reply JSON serialization failed", ex);
        }
    }

    /**
     * 规范化输入值。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeReviewDecision(String status) {
        String normalized = normalizeStatus(status);
        if (!REVIEW_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported AI reply review decision: " + status);
        }
        return normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        String normalized = required(status, "status is required").toUpperCase(Locale.ROOT);
        if (!LIST_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported AI reply suggestion status: " + status);
        }
        return normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param confidence confidence 参数，用于 boundedConfidence 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private double boundedConfidence(double confidence) {
        if (Double.isNaN(confidence) || Double.isInfinite(confidence)) {
            return 0;
        }
        return Math.max(0, Math.min(1, confidence));
    }

    /**
     * 执行 flag 流程，围绕 flag 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 flag 生成的文本或业务键。
     */
    private String flag(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String actor) {
        return isBlank(actor) ? "system" : actor.trim();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param text text 参数，用于 containsAny 流程中的校验、计算或对象转换。
     * @param needles needles 参数，用于 containsAny 流程中的校验、计算或对象转换。
     * @return 返回 contains any 的布尔判断结果。
     */
    private boolean containsAny(String text, String... needles) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (text == null || text.isBlank()) {
            return false;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && text.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return false;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
