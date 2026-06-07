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

    @Autowired
    public ConversationAiReplyService(ConversationWorkspaceService workspaceService,
                                      ConversationAiReplySuggestionMapper suggestionMapper,
                                      ConversationWorkItemAuditMapper auditMapper,
                                      ConversationAiReplyGenerator generator,
                                      ObjectMapper objectMapper) {
        this(workspaceService, suggestionMapper, auditMapper, generator, objectMapper, Clock.systemDefaultZone());
    }

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
    public ConversationAiReplySuggestionView generate(Long tenantId,
                                                      Long workItemId,
                                                      ConversationAiReplyGenerateCommand command,
                                                      String actor) {
        Long scopedTenantId = tenantId(tenantId);
        ConversationWorkspaceTimelineView timeline = workspaceService.timeline(scopedTenantId, workItemId, 50, 20);
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
        row.setUpdatedAt(now);
        suggestionMapper.insert(row);
        audit(scopedTenantId, row.getWorkItemId(), "AI_REPLY_SUGGESTED", actor,
                Map.of(),
                values("suggestionId", row.getId(), "status", row.getStatus(),
                        "sourceMessageId", row.getSourceMessageId(), "riskFlags", riskFlags),
                null);
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConversationAiReplySuggestionView review(Long tenantId,
                                                    Long workItemId,
                                                    Long suggestionId,
                                                    ConversationAiReplyReviewCommand command,
                                                    String actor) {
        Long scopedTenantId = tenantId(tenantId);
        ConversationAiReplySuggestionDO row = requireSuggestion(scopedTenantId, workItemId, suggestionId);
        if (!"DRAFT".equals(normalizeStatus(row.getStatus()))) {
            throw new IllegalStateException("only draft AI reply suggestions can be reviewed");
        }
        String decision = normalizeReviewDecision(command == null ? null : command.decision());
        LocalDateTime now = now();
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
        return toView(row);
    }

    public List<ConversationAiReplySuggestionView> list(Long tenantId,
                                                        Long workItemId,
                                                        ConversationAiReplySuggestionQuery query) {
        Long scopedTenantId = tenantId(tenantId);
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
        return safeList(suggestionMapper.selectList(wrapper)).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()) && workItemId.equals(row.getWorkItemId()))
                .map(this::toView)
                .toList();
    }

    private ConversationAiReplySuggestionDO requireSuggestion(Long tenantId, Long workItemId, Long suggestionId) {
        if (suggestionId == null) {
            throw new IllegalArgumentException("suggestionId is required");
        }
        if (workItemId == null) {
            throw new IllegalArgumentException("workItemId is required");
        }
        ConversationAiReplySuggestionDO row = suggestionMapper.selectById(suggestionId);
        if (row == null || !tenantId.equals(row.getTenantId()) || !workItemId.equals(row.getWorkItemId())) {
            throw new IllegalArgumentException("AI reply suggestion not found: " + suggestionId);
        }
        return row;
    }

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

    private Map<String, Object> promptContext(ConversationWorkspaceTimelineView timeline,
                                              ConversationMessageView sourceMessage,
                                              ConversationAiReplyGenerateCommand command) {
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
        return result;
    }

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

    private List<String> riskFlags(ConversationAiReplyGenerationResult result,
                                   ConversationMessageView sourceMessage,
                                   List<ConversationMessageView> messages) {
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        safeList(result.riskFlags()).stream()
                .map(this::flag)
                .filter(flag -> !flag.isBlank())
                .forEach(flags::add);
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
        return new ArrayList<>(flags);
    }

    private String riskText(ConversationAiReplyGenerationResult result,
                            ConversationMessageView sourceMessage,
                            List<ConversationMessageView> messages) {
        StringBuilder builder = new StringBuilder();
        if (sourceMessage != null) {
            builder.append(' ').append(sourceMessage.text());
            builder.append(' ').append(sourceMessage.intent());
        }
        if (result != null) {
            builder.append(' ').append(result.suggestedReplyText());
            builder.append(' ').append(result.intent());
        }
        safeList(messages).forEach(message -> {
            if (message != null) {
                builder.append(' ').append(message.text());
                builder.append(' ').append(message.intent());
            }
        });
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private List<String> groundingSnippets(ConversationAiReplyGenerationResult result,
                                           ConversationMessageView sourceMessage) {
        List<String> snippets = safeList(result.groundingSnippets()).stream()
                .filter(snippet -> !isBlank(snippet))
                .map(String::trim)
                .toList();
        if (!snippets.isEmpty() || sourceMessage == null || isBlank(sourceMessage.text())) {
            return snippets;
        }
        return List.of(sourceMessage.text().trim());
    }

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

    private ConversationAiReplySuggestionView toView(ConversationAiReplySuggestionDO row) {
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
                row.getUpdatedAt());
    }

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

    private Object jsonValue(Object value) {
        if (value instanceof LocalDateTime time) {
            return time.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
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
        return value;
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            if (value instanceof List<?> list) {
                return objectMapper.writeValueAsString(new ArrayList<>(list));
            }
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("conversation AI reply JSON serialization failed", ex);
        }
    }

    private String normalizeReviewDecision(String status) {
        String normalized = normalizeStatus(status);
        if (!REVIEW_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported AI reply review decision: " + status);
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = required(status, "status is required").toUpperCase(Locale.ROOT);
        if (!LIST_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported AI reply suggestion status: " + status);
        }
        return normalized;
    }

    private String normalizeOptionalUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private Long tenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private double boundedConfidence(double confidence) {
        if (Double.isNaN(confidence) || Double.isInfinite(confidence)) {
            return 0;
        }
        return Math.max(0, Math.min(1, confidence));
    }

    private String flag(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String actor(String actor) {
        return isBlank(actor) ? "system" : actor.trim();
    }

    private String required(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean containsAny(String text, String... needles) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && text.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
