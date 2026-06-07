package org.chovy.canvas.domain.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ConversationContactProfileDO;
import org.chovy.canvas.dal.dataobject.ConversationMessageDO;
import org.chovy.canvas.dal.dataobject.ConversationSessionDO;
import org.chovy.canvas.dal.dataobject.ConversationSopTaskDO;
import org.chovy.canvas.dal.dataobject.ConversationWorkItemAuditDO;
import org.chovy.canvas.dal.dataobject.ConversationWorkItemDO;
import org.chovy.canvas.dal.mapper.ConversationContactProfileMapper;
import org.chovy.canvas.dal.mapper.ConversationMessageMapper;
import org.chovy.canvas.dal.mapper.ConversationSessionMapper;
import org.chovy.canvas.dal.mapper.ConversationSopTaskMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemAuditMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ConversationWorkspaceService {

    private static final Set<String> STATUSES = Set.of("OPEN", "PENDING", "SNOOZED", "RESOLVED");
    private static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ConversationSessionMapper sessionMapper;
    private final ConversationMessageMapper messageMapper;
    private final ConversationContactProfileMapper contactProfileMapper;
    private final ConversationWorkItemMapper workItemMapper;
    private final ConversationSopTaskMapper taskMapper;
    private final ConversationWorkItemAuditMapper auditMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ConversationWorkspaceService(ConversationSessionMapper sessionMapper,
                                        ConversationMessageMapper messageMapper,
                                        ConversationContactProfileMapper contactProfileMapper,
                                        ConversationWorkItemMapper workItemMapper,
                                        ConversationSopTaskMapper taskMapper,
                                        ConversationWorkItemAuditMapper auditMapper,
                                        ObjectMapper objectMapper) {
        this(sessionMapper, messageMapper, contactProfileMapper, workItemMapper, taskMapper, auditMapper,
                objectMapper, Clock.systemDefaultZone());
    }

    ConversationWorkspaceService(ConversationSessionMapper sessionMapper,
                                 ConversationMessageMapper messageMapper,
                                 ConversationContactProfileMapper contactProfileMapper,
                                 ConversationWorkItemMapper workItemMapper,
                                 ConversationSopTaskMapper taskMapper,
                                 ConversationWorkItemAuditMapper auditMapper,
                                 ObjectMapper objectMapper,
                                 Clock clock) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.contactProfileMapper = contactProfileMapper;
        this.workItemMapper = workItemMapper;
        this.taskMapper = taskMapper;
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public ConversationWorkItemView ensureWorkItemForSession(Long tenantId, Long sessionId, String actor) {
        Long scopedTenantId = tenantId(tenantId);
        ConversationSessionDO session = requireSession(scopedTenantId, sessionId);
        ConversationWorkItemDO existing = findBySession(scopedTenantId, sessionId);
        if (existing != null) {
            return toWorkItemView(existing);
        }
        ConversationContactProfileDO profile = ensureContactProfile(scopedTenantId, session, actor);
        ConversationWorkItemDO row = new ConversationWorkItemDO();
        row.setTenantId(scopedTenantId);
        row.setSessionId(session.getId());
        row.setContactProfileId(profile.getId());
        row.setUserId(session.getUserId());
        row.setChannel(session.getChannel());
        row.setProvider(defaultString(session.getProvider(), "DEFAULT"));
        row.setSubject(subject(session));
        row.setStatus("OPEN");
        row.setPriority("NORMAL");
        row.setSource("CONVERSATION");
        row.setLastCustomerMessageAt(session.getLastMessageAt());
        row.setTagsJson("[]");
        row.setAttributesJson(writeJson(Map.of(
                "sessionId", session.getId(),
                "channel", session.getChannel(),
                "provider", defaultString(session.getProvider(), "DEFAULT"))));
        row.setRoutingStatus("UNROUTED");
        row.setRequiredSkillsJson("[]");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        workItemMapper.insert(row);
        audit(scopedTenantId, row.getId(), "CREATED", actor,
                Map.of(),
                Map.of("status", row.getStatus(), "priority", row.getPriority(), "sessionId", row.getSessionId()),
                null);
        return toWorkItemView(row);
    }

    public List<ConversationWorkItemView> inbox(Long tenantId, ConversationInboxQuery query) {
        Long scopedTenantId = tenantId(tenantId);
        ConversationInboxQuery scopedQuery = query == null ? new ConversationInboxQuery(null, null, null, 50) : query;
        LambdaQueryWrapper<ConversationWorkItemDO> wrapper = new LambdaQueryWrapper<ConversationWorkItemDO>()
                .eq(ConversationWorkItemDO::getTenantId, scopedTenantId)
                .orderByDesc(ConversationWorkItemDO::getPriority)
                .orderByDesc(ConversationWorkItemDO::getLastCustomerMessageAt)
                .last("LIMIT " + boundedLimit(scopedQuery.limit()));
        if (!isBlank(scopedQuery.status())) {
            wrapper.eq(ConversationWorkItemDO::getStatus, normalizeStatus(scopedQuery.status()));
        }
        if (!isBlank(scopedQuery.assignedTo())) {
            wrapper.eq(ConversationWorkItemDO::getAssignedTo, scopedQuery.assignedTo().trim());
        }
        if (!isBlank(scopedQuery.channel())) {
            wrapper.eq(ConversationWorkItemDO::getChannel, scopedQuery.channel().trim().toUpperCase(Locale.ROOT));
        }
        return workItemMapper.selectList(wrapper).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .map(this::toWorkItemView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ConversationWorkItemView assign(Long tenantId,
                                           Long workItemId,
                                           ConversationAssignmentCommand command,
                                           String actor) {
        ConversationWorkItemDO row = requireWorkItem(tenantId(tenantId), workItemId);
        String assignedTo = required(command == null ? null : command.assignedTo(), "assignedTo is required");
        String assignedTeam = blankToNull(command.assignedTeam());
        Map<String, Object> oldValues = values(
                "assignedTo", row.getAssignedTo(),
                "assignedTeam", row.getAssignedTeam());
        ConversationWorkItemDO update = new ConversationWorkItemDO();
        update.setId(row.getId());
        update.setAssignedTo(assignedTo);
        update.setAssignedTeam(assignedTeam);
        update.setLastOperatorActivityAt(now());
        update.setUpdatedAt(now());
        workItemMapper.updateById(update);

        row.setAssignedTo(assignedTo);
        row.setAssignedTeam(assignedTeam);
        row.setLastOperatorActivityAt(update.getLastOperatorActivityAt());
        row.setUpdatedAt(update.getUpdatedAt());
        audit(row.getTenantId(), row.getId(), "ASSIGNED", actor, oldValues,
                values("assignedTo", assignedTo, "assignedTeam", assignedTeam),
                command.note());
        return toWorkItemView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConversationWorkItemView updateStatus(Long tenantId,
                                                 Long workItemId,
                                                 ConversationWorkItemStatusCommand command,
                                                 String actor) {
        ConversationWorkItemDO row = requireWorkItem(tenantId(tenantId), workItemId);
        String status = normalizeStatus(command == null ? null : command.status());
        String priority = normalizePriority(command.priority() == null ? row.getPriority() : command.priority());
        Map<String, Object> oldValues = values(
                "status", row.getStatus(),
                "priority", row.getPriority(),
                "nextFollowUpAt", row.getNextFollowUpAt());
        ConversationWorkItemDO update = new ConversationWorkItemDO();
        update.setId(row.getId());
        update.setStatus(status);
        update.setPriority(priority);
        update.setNextFollowUpAt(command.nextFollowUpAt());
        update.setLastOperatorActivityAt(now());
        update.setUpdatedAt(now());
        workItemMapper.updateById(update);

        row.setStatus(status);
        row.setPriority(priority);
        row.setNextFollowUpAt(command.nextFollowUpAt());
        row.setLastOperatorActivityAt(update.getLastOperatorActivityAt());
        row.setUpdatedAt(update.getUpdatedAt());
        audit(row.getTenantId(), row.getId(), "STATUS_CHANGED", actor, oldValues,
                values("status", status, "priority", priority, "nextFollowUpAt", command.nextFollowUpAt()),
                command.note());
        return toWorkItemView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConversationSopTaskView createTask(Long tenantId,
                                              Long workItemId,
                                              ConversationSopTaskCommand command,
                                              String actor) {
        ConversationWorkItemDO workItem = requireWorkItem(tenantId(tenantId), workItemId);
        if (command == null) {
            throw new IllegalArgumentException("conversation SOP task command is required");
        }
        ConversationSopTaskDO row = new ConversationSopTaskDO();
        row.setTenantId(workItem.getTenantId());
        row.setWorkItemId(workItem.getId());
        row.setTaskKey(required(command.taskKey(), "taskKey is required"));
        row.setTitle(required(command.title(), "title is required"));
        row.setStatus("TODO");
        row.setAssignee(blankToNull(command.assignee()));
        row.setDueAt(command.dueAt());
        row.setMetadataJson(writeJson(command.metadata()));
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        taskMapper.insert(row);
        audit(workItem.getTenantId(), workItem.getId(), "TASK_CREATED", actor, Map.of(),
                values("taskId", row.getId(), "taskKey", row.getTaskKey(), "title", row.getTitle()),
                null);
        return toTaskView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConversationSopTaskView completeTask(Long tenantId,
                                                Long taskId,
                                                ConversationSopTaskCompletionCommand command,
                                                String actor) {
        Long scopedTenantId = tenantId(tenantId);
        ConversationSopTaskDO row = taskMapper.selectById(taskId);
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("Conversation SOP task not found: " + taskId);
        }
        ConversationWorkItemDO workItem = requireWorkItem(scopedTenantId, row.getWorkItemId());
        ConversationSopTaskDO update = new ConversationSopTaskDO();
        update.setId(row.getId());
        update.setStatus("DONE");
        update.setCompletedBy(actor(actor));
        update.setCompletedAt(now());
        update.setUpdatedAt(now());
        taskMapper.updateById(update);

        row.setStatus("DONE");
        row.setCompletedBy(update.getCompletedBy());
        row.setCompletedAt(update.getCompletedAt());
        row.setUpdatedAt(update.getUpdatedAt());
        audit(scopedTenantId, workItem.getId(), "TASK_COMPLETED", actor, values("status", "TODO"),
                values("taskId", row.getId(), "status", "DONE", "completedBy", update.getCompletedBy()),
                command == null ? null : command.note());
        return toTaskView(row);
    }

    public ConversationWorkspaceTimelineView timeline(Long tenantId, Long workItemId, int messageLimit, int auditLimit) {
        Long scopedTenantId = tenantId(tenantId);
        ConversationWorkItemDO workItem = requireWorkItem(scopedTenantId, workItemId);
        ConversationContactProfileDO profile = workItem.getContactProfileId() == null
                ? null
                : contactProfileMapper.selectById(workItem.getContactProfileId());
        if (profile != null && !scopedTenantId.equals(profile.getTenantId())) {
            profile = null;
        }
        ConversationSessionDO session = sessionMapper.selectById(workItem.getSessionId());
        ConversationSessionView sessionView = session == null || !scopedTenantId.equals(session.getTenantId())
                ? null
                : toSessionView(session);
        List<ConversationMessageView> messages = messageMapper.selectList(new LambdaQueryWrapper<ConversationMessageDO>()
                        .eq(ConversationMessageDO::getTenantId, scopedTenantId)
                        .eq(ConversationMessageDO::getSessionId, workItem.getSessionId())
                        .orderByAsc(ConversationMessageDO::getCreatedAt)
                        .last("LIMIT " + boundedLimit(messageLimit)))
                .stream()
                .map(this::toMessageView)
                .toList();
        List<ConversationSopTaskView> tasks = taskMapper.selectList(new LambdaQueryWrapper<ConversationSopTaskDO>()
                        .eq(ConversationSopTaskDO::getTenantId, scopedTenantId)
                        .eq(ConversationSopTaskDO::getWorkItemId, workItem.getId())
                        .orderByAsc(ConversationSopTaskDO::getDueAt)
                        .orderByAsc(ConversationSopTaskDO::getId))
                .stream()
                .map(this::toTaskView)
                .toList();
        List<ConversationWorkItemAuditView> audits = auditMapper.selectList(new LambdaQueryWrapper<ConversationWorkItemAuditDO>()
                        .eq(ConversationWorkItemAuditDO::getTenantId, scopedTenantId)
                        .eq(ConversationWorkItemAuditDO::getWorkItemId, workItem.getId())
                        .orderByDesc(ConversationWorkItemAuditDO::getCreatedAt)
                        .last("LIMIT " + boundedLimit(auditLimit)))
                .stream()
                .map(this::toAuditView)
                .toList();
        return new ConversationWorkspaceTimelineView(
                toWorkItemView(workItem),
                profile == null ? null : toContactView(profile),
                sessionView,
                messages,
                tasks,
                audits);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordInboundMessage(Long tenantId,
                                     ConversationSessionDO session,
                                     ConversationMessageDO message,
                                     ConversationIngressReq req,
                                     LocalDateTime occurredAt) {
        if (session == null || session.getId() == null || message == null) {
            return;
        }
        Long scopedTenantId = tenantId(tenantId);
        ConversationWorkItemDO row = findBySession(scopedTenantId, session.getId());
        if (row == null) {
            ensureWorkItemForSession(scopedTenantId, session.getId(), "system");
            row = findBySession(scopedTenantId, session.getId());
        }
        if (row == null) {
            return;
        }
        Map<String, Object> attributes = new LinkedHashMap<>(readMap(row.getAttributesJson()));
        attributes.put("lastMessageId", message.getId());
        putIfPresent(attributes, "lastText", message.getTextContent());
        putIfPresent(attributes, "lastIntent", message.getIntent());
        if (req != null && req.attributes() != null && !req.attributes().isEmpty()) {
            attributes.put("lastAttributes", req.attributes());
        }
        ConversationWorkItemDO update = new ConversationWorkItemDO();
        update.setId(row.getId());
        update.setStatus("OPEN");
        update.setPriority(inboundPriority(row.getPriority(), message.getIntent(), req));
        update.setLastCustomerMessageAt(occurredAt == null ? now() : occurredAt);
        update.setAttributesJson(writeJson(attributes));
        update.setUpdatedAt(now());
        workItemMapper.updateById(update);
        audit(scopedTenantId, row.getId(), "INBOUND_MESSAGE", "system",
                values("status", row.getStatus(), "lastCustomerMessageAt", row.getLastCustomerMessageAt()),
                values("status", update.getStatus(), "lastCustomerMessageAt", update.getLastCustomerMessageAt(),
                        "messageId", message.getId()),
                null);
    }

    private ConversationContactProfileDO ensureContactProfile(Long tenantId, ConversationSessionDO session, String actor) {
        ConversationContactProfileDO existing = contactProfileMapper.selectOne(new LambdaQueryWrapper<ConversationContactProfileDO>()
                .eq(ConversationContactProfileDO::getTenantId, tenantId)
                .eq(ConversationContactProfileDO::getUserId, session.getUserId())
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        ConversationContactProfileDO row = new ConversationContactProfileDO();
        row.setTenantId(tenantId);
        row.setUserId(session.getUserId());
        row.setDisplayName(session.getUserId());
        row.setPrivateDomainSource(session.getChannel());
        row.setOwner(blankToNull(actor));
        row.setLifecycleStage("NEW");
        row.setTagsJson("[]");
        row.setAttributesJson("{}");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        contactProfileMapper.insert(row);
        return row;
    }

    private ConversationSessionDO requireSession(Long tenantId, Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId is required");
        }
        ConversationSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null || !tenantId.equals(session.getTenantId())) {
            throw new IllegalArgumentException("Conversation session not found: " + sessionId);
        }
        return session;
    }

    private ConversationWorkItemDO requireWorkItem(Long tenantId, Long workItemId) {
        if (workItemId == null) {
            throw new IllegalArgumentException("workItemId is required");
        }
        ConversationWorkItemDO row = workItemMapper.selectById(workItemId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("Conversation work item not found: " + workItemId);
        }
        return row;
    }

    private ConversationWorkItemDO findBySession(Long tenantId, Long sessionId) {
        return workItemMapper.selectOne(new LambdaQueryWrapper<ConversationWorkItemDO>()
                .eq(ConversationWorkItemDO::getTenantId, tenantId)
                .eq(ConversationWorkItemDO::getSessionId, sessionId)
                .last("LIMIT 1"));
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

    private ConversationWorkItemView toWorkItemView(ConversationWorkItemDO row) {
        return new ConversationWorkItemView(
                row.getId(),
                row.getTenantId(),
                row.getSessionId(),
                row.getContactProfileId(),
                row.getUserId(),
                row.getChannel(),
                row.getProvider(),
                row.getSubject(),
                row.getStatus(),
                row.getPriority(),
                row.getAssignedTo(),
                row.getAssignedTeam(),
                row.getSource(),
                row.getSlaDueAt(),
                row.getNextFollowUpAt(),
                row.getLastCustomerMessageAt(),
                row.getLastOperatorActivityAt(),
                readStringList(row.getTagsJson()),
                readMap(row.getAttributesJson()),
                defaultString(row.getRoutingStatus(), "UNROUTED"),
                readStringList(row.getRequiredSkillsJson()),
                row.getRoutingReason(),
                row.getRoutedAt(),
                row.getSlaPolicyKey(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private ConversationContactProfileView toContactView(ConversationContactProfileDO row) {
        return new ConversationContactProfileView(
                row.getId(),
                row.getTenantId(),
                row.getUserId(),
                row.getDisplayName(),
                row.getExternalContactId(),
                row.getPrivateDomainSource(),
                row.getOwner(),
                row.getLifecycleStage(),
                readStringList(row.getTagsJson()),
                readMap(row.getAttributesJson()),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private ConversationSopTaskView toTaskView(ConversationSopTaskDO row) {
        return new ConversationSopTaskView(
                row.getId(),
                row.getTenantId(),
                row.getWorkItemId(),
                row.getTaskKey(),
                row.getTitle(),
                row.getStatus(),
                row.getAssignee(),
                row.getDueAt(),
                row.getCompletedBy(),
                row.getCompletedAt(),
                readMap(row.getMetadataJson()),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private ConversationWorkItemAuditView toAuditView(ConversationWorkItemAuditDO row) {
        return new ConversationWorkItemAuditView(
                row.getId(),
                row.getTenantId(),
                row.getWorkItemId(),
                row.getEventType(),
                row.getActor(),
                readMap(row.getOldValueJson()),
                readMap(row.getNewValueJson()),
                row.getNote(),
                row.getCreatedAt());
    }

    private ConversationSessionView toSessionView(ConversationSessionDO row) {
        return new ConversationSessionView(
                row.getId(),
                row.getTenantId(),
                row.getCanvasId(),
                row.getVersionId(),
                row.getExecutionId(),
                row.getUserId(),
                row.getChannel(),
                row.getProvider(),
                row.getStatus(),
                row.getTurnCount() == null ? 0 : row.getTurnCount(),
                readMap(row.getContextJson()),
                row.getLastMessageAt(),
                row.getExpiresAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private ConversationMessageView toMessageView(ConversationMessageDO row) {
        return new ConversationMessageView(
                row.getId(),
                row.getTenantId(),
                row.getSessionId(),
                row.getDirection(),
                row.getMessageType(),
                row.getExternalMessageId(),
                row.getTextContent(),
                row.getIntent(),
                readMap(row.getContentJson()),
                row.getCreatedAt());
    }

    private String subject(ConversationSessionDO session) {
        return defaultString(session.getChannel(), "CONVERSATION") + " conversation with " + session.getUserId();
    }

    private String inboundPriority(String currentPriority, String intent, ConversationIngressReq req) {
        String normalized = normalizePriority(isBlank(currentPriority) ? "NORMAL" : currentPriority);
        if ("URGENT".equals(normalized) || "HIGH".equals(normalized)) {
            return normalized;
        }
        if (!isBlank(intent)) {
            String normalizedIntent = intent.toUpperCase(Locale.ROOT);
            if (normalizedIntent.contains("DEMO")
                    || normalizedIntent.contains("SALES")
                    || normalizedIntent.contains("HANDOFF")) {
                return "HIGH";
            }
        }
        Object priority = req == null || req.attributes() == null ? null : req.attributes().get("priority");
        if (priority instanceof String text && PRIORITIES.contains(text.trim().toUpperCase(Locale.ROOT))) {
            return text.trim().toUpperCase(Locale.ROOT);
        }
        return normalized;
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
            throw new IllegalArgumentException("conversation workspace JSON serialization failed", ex);
        }
    }

    private String normalizeStatus(String status) {
        String normalized = required(status, "status is required").toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported conversation work item status: " + status);
        }
        return normalized;
    }

    private String normalizePriority(String priority) {
        String normalized = required(priority, "priority is required").toUpperCase(Locale.ROOT);
        if (!PRIORITIES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported conversation work item priority: " + priority);
        }
        return normalized;
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

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value instanceof String text) {
            if (!text.isBlank()) {
                target.put(key, text.trim());
            }
        } else if (value != null) {
            target.put(key, value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
