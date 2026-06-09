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

/**
 * ConversationWorkspaceService 编排 domain.conversation 场景的领域业务规则。
 */
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

    /**
     * 创建 ConversationWorkspaceService 实例并注入 domain.conversation 场景依赖。
     * @param sessionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param messageMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param contactProfileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param workItemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param taskMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 执行 ConversationWorkspaceService 流程，围绕 conversation workspace service 完成校验、计算或结果组装。
     *
     * @param sessionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param messageMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param contactProfileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param workItemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param taskMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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
    /**
     * 确保会话存在对应客服工单。
     * 已存在工单时直接返回；不存在时会创建联系人画像、OPEN 工单和 CREATED 审计。
     */
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
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @return 返回 defaultString 流程生成的业务结果。
                 */
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

    /**
     * 查询租户内客服工单收件箱。
     * 支持按状态、分配人和渠道过滤，并按优先级和最近客户消息时间排序。
     */
    public List<ConversationWorkItemView> inbox(Long tenantId, ConversationInboxQuery query) {
        Long scopedTenantId = tenantId(tenantId);
        ConversationInboxQuery scopedQuery = query == null ? new ConversationInboxQuery(null, null, null, 50) : query;
        LambdaQueryWrapper<ConversationWorkItemDO> wrapper = new LambdaQueryWrapper<ConversationWorkItemDO>()
                .eq(ConversationWorkItemDO::getTenantId, scopedTenantId)
                .orderByDesc(ConversationWorkItemDO::getPriority)
                .orderByDesc(ConversationWorkItemDO::getLastCustomerMessageAt)
                .last("LIMIT " + boundedLimit(scopedQuery.limit()));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!isBlank(scopedQuery.status())) {
            wrapper.eq(ConversationWorkItemDO::getStatus, normalizeStatus(scopedQuery.status()));
        }
        if (!isBlank(scopedQuery.assignedTo())) {
            wrapper.eq(ConversationWorkItemDO::getAssignedTo, scopedQuery.assignedTo().trim());
        }
        if (!isBlank(scopedQuery.channel())) {
            wrapper.eq(ConversationWorkItemDO::getChannel, scopedQuery.channel().trim().toUpperCase(Locale.ROOT));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return workItemMapper.selectList(wrapper).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .map(this::toWorkItemView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 人工分配会话工单。
     * 方法更新负责人、团队和最后操作时间，并记录 ASSIGNED 审计，返回更新后的工单视图。
     */
    public ConversationWorkItemView assign(Long tenantId,
                                           Long workItemId,
                                           ConversationAssignmentCommand command,
                                           String actor) {
        // 准备本次处理所需的上下文和中间变量。
        ConversationWorkItemDO row = requireWorkItem(tenantId(tenantId), workItemId);
        String assignedTo = required(command == null ? null : command.assignedTo(), "assignedTo is required");
        String assignedTeam = blankToNull(command.assignedTeam());
        Map<String, Object> oldValues = values(
                "assignedTo", row.getAssignedTo(),
                "assignedTeam", row.getAssignedTeam());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toWorkItemView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新会话工单状态、优先级和下次跟进时间。
     * 方法限定工单租户归属，写入操作时间和 STATUS_CHANGED 审计。
     */
    public ConversationWorkItemView updateStatus(Long tenantId,
                                                 Long workItemId,
                                                 ConversationWorkItemStatusCommand command,
                                                 String actor) {
        // 准备本次处理所需的上下文和中间变量。
        ConversationWorkItemDO row = requireWorkItem(tenantId(tenantId), workItemId);
        String status = normalizeStatus(command == null ? null : command.status());
        String priority = normalizePriority(command.priority() == null ? row.getPriority() : command.priority());
        Map<String, Object> oldValues = values(
                "status", row.getStatus(),
                "priority", row.getPriority(),
                "nextFollowUpAt", row.getNextFollowUpAt());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toWorkItemView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 为会话工单创建 SOP 任务。
     * 任务会绑定租户和工单，保存任务 key、标题、负责人、到期时间和元数据，并追加 TASK_CREATED 审计。
     */
    public ConversationSopTaskView createTask(Long tenantId,
                                              Long workItemId,
                                              ConversationSopTaskCommand command,
                                              String actor) {
        ConversationWorkItemDO workItem = requireWorkItem(tenantId(tenantId), workItemId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(now());
        taskMapper.insert(row);
        audit(workItem.getTenantId(), workItem.getId(), "TASK_CREATED", actor, Map.of(),
                values("taskId", row.getId(), "taskKey", row.getTaskKey(), "title", row.getTitle()),
                null);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toTaskView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 将 SOP 任务标记为完成。
     * 会校验任务和工单同属租户，记录完成人与完成时间，并写入 TASK_COMPLETED 审计。
     */
    public ConversationSopTaskView completeTask(Long tenantId,
                                                Long taskId,
                                                ConversationSopTaskCompletionCommand command,
                                                String actor) {
        Long scopedTenantId = tenantId(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationSopTaskDO row = taskMapper.selectById(taskId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toTaskView(row);
    }

    /**
     * 汇总会话工单时间线。
     * 返回工单、联系人、会话、消息、SOP 任务和工单审计，消息与审计数量分别受 limit 控制。
     */
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
                /**
                 * 转换为接口返回或领域视图。
                 *
                 * @param session session 参数，用于 toSessionView 流程中的校验、计算或对象转换。
                 * @return 返回组装或转换后的结果对象。
                 */
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
    /**
     * 在收到入站消息后同步更新客服工单。
     * 如果会话尚无工单会先创建，随后打开工单、更新最近客户消息时间、属性和优先级，并记录 INBOUND_MESSAGE 审计。
     */
    public void recordInboundMessage(Long tenantId,
                                     ConversationSessionDO session,
                                     ConversationMessageDO message,
                                     ConversationIngressReq req,
                                     LocalDateTime occurredAt) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        Map<String, Object> attributes = new LinkedHashMap<>(readMap(row.getAttributesJson()));
        attributes.put("lastMessageId", message.getId());
        putIfPresent(attributes, "lastText", message.getTextContent());
        putIfPresent(attributes, "lastIntent", message.getIntent());
        if (req != null && req.attributes() != null && !req.attributes().isEmpty()) {
            attributes.put("lastAttributes", req.attributes());
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param session session 参数，用于 ensureContactProfile 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 ensureContactProfile 流程生成的业务结果。
     */
    private ConversationContactProfileDO ensureContactProfile(Long tenantId, ConversationSessionDO session, String actor) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationContactProfileDO existing = contactProfileMapper.selectOne(new LambdaQueryWrapper<ConversationContactProfileDO>()
                .eq(ConversationContactProfileDO::getTenantId, tenantId)
                .eq(ConversationContactProfileDO::getUserId, session.getUserId())
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param sessionId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireSession 流程生成的业务结果。
     */
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

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workItemId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireWorkItem 流程生成的业务结果。
     */
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

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param sessionId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    private ConversationWorkItemDO findBySession(Long tenantId, Long sessionId) {
        return workItemMapper.selectOne(new LambdaQueryWrapper<ConversationWorkItemDO>()
                .eq(ConversationWorkItemDO::getTenantId, tenantId)
                .eq(ConversationWorkItemDO::getSessionId, sessionId)
                .last("LIMIT 1"));
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
    private ConversationWorkItemView toWorkItemView(ConversationWorkItemDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 执行 subject 流程，围绕 subject 完成校验、计算或结果组装。
     *
     * @param session session 参数，用于 subject 流程中的校验、计算或对象转换。
     * @return 返回 subject 生成的文本或业务键。
     */
    private String subject(ConversationSessionDO session) {
        return defaultString(session.getChannel(), "CONVERSATION") + " conversation with " + session.getUserId();
    }

    /**
     * 执行 inboundPriority 流程，围绕 inbound priority 完成校验、计算或结果组装。
     *
     * @param currentPriority current priority 参数，用于 inboundPriority 流程中的校验、计算或对象转换。
     * @param intent intent 参数，用于 inboundPriority 流程中的校验、计算或对象转换。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 inbound priority 生成的文本或业务键。
     */
    private String inboundPriority(String currentPriority, String intent, ConversationIngressReq req) {
        // 准备本次处理所需的上下文和中间变量。
        String normalized = normalizePriority(isBlank(currentPriority) ? "NORMAL" : currentPriority);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return normalized;
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
            throw new IllegalArgumentException("conversation workspace JSON serialization failed", ex);
        }
    }

    /**
     * 规范化输入值。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        String normalized = required(status, "status is required").toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported conversation work item status: " + status);
        }
        return normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param priority priority 参数，用于 normalizePriority 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizePriority(String priority) {
        String normalized = required(priority, "priority is required").toUpperCase(Locale.ROOT);
        if (!PRIORITIES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported conversation work item priority: " + priority);
        }
        return normalized;
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
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     */
    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value instanceof String text) {
            if (!text.isBlank()) {
                target.put(key, text.trim());
            }
        // 根据前序判断结果进入后续条件分支。
        } else if (value != null) {
            target.put(key, value);
        }
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
