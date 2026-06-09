package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.ApprovalAuditEventDO;
import org.chovy.canvas.dal.dataobject.ApprovalDefinitionDO;
import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;
import org.chovy.canvas.dal.dataobject.ApprovalTaskDO;
import org.chovy.canvas.dal.mapper.ApprovalAuditEventMapper;
import org.chovy.canvas.dal.mapper.ApprovalDefinitionMapper;
import org.chovy.canvas.dal.mapper.ApprovalInstanceMapper;
import org.chovy.canvas.dal.mapper.ApprovalTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * ApprovalWorkflowService 编排 domain.approval 场景的领域业务规则。
 */
@Service
public class ApprovalWorkflowService {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    public static final String AUTO_PENDING = "PENDING";
    public static final String AUTO_RUNNING = "RUNNING";
    public static final String AUTO_SUCCESS = "SUCCESS";
    public static final String AUTO_FAILED = "FAILED";

    private static final String MODE_ANY_ONE = "ANY_ONE";
    private static final String MODE_PARALLEL = "PARALLEL";
    private static final String ROLE_TENANT_ADMIN = "tenant_admin";

    private final ApprovalDefinitionMapper definitionMapper;
    private final ApprovalInstanceMapper instanceMapper;
    private final ApprovalTaskMapper taskMapper;
    private final ApprovalAuditEventMapper auditEventMapper;
    private final List<ApprovalAutoActionHandler> autoActionHandlers;
    private final List<ApprovalExternalProvider> externalProviders;
    private final Clock clock;

    /**
     * 创建 ApprovalWorkflowService 实例并注入 domain.approval 场景依赖。
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param instanceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param taskMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditEventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param autoActionHandlers auto action handlers 参数，用于 ApprovalWorkflowService 流程中的校验、计算或对象转换。
     * @param externalProviders external providers 参数，用于 ApprovalWorkflowService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public ApprovalWorkflowService(ApprovalDefinitionMapper definitionMapper,
                                   ApprovalInstanceMapper instanceMapper,
                                   ApprovalTaskMapper taskMapper,
                                   ApprovalAuditEventMapper auditEventMapper,
                                   List<ApprovalAutoActionHandler> autoActionHandlers,
                                   List<ApprovalExternalProvider> externalProviders) {
        this(definitionMapper, instanceMapper, taskMapper, auditEventMapper,
                autoActionHandlers, externalProviders, Clock.systemUTC());
    }

    /**
     * 执行 ApprovalWorkflowService 流程，围绕 approval workflow service 完成校验、计算或结果组装。
     *
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param instanceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param taskMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditEventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param autoActionHandlers auto action handlers 参数，用于 ApprovalWorkflowService 流程中的校验、计算或对象转换。
     * @param externalProviders external providers 参数，用于 ApprovalWorkflowService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    ApprovalWorkflowService(ApprovalDefinitionMapper definitionMapper,
                            ApprovalInstanceMapper instanceMapper,
                            ApprovalTaskMapper taskMapper,
                            ApprovalAuditEventMapper auditEventMapper,
                            List<ApprovalAutoActionHandler> autoActionHandlers,
                            List<ApprovalExternalProvider> externalProviders,
                            Clock clock) {
        this.definitionMapper = definitionMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.auditEventMapper = auditEventMapper;
        this.autoActionHandlers = autoActionHandlers == null ? List.of() : List.copyOf(autoActionHandlers);
        this.externalProviders = externalProviders == null ? List.of() : List.copyOf(externalProviders);
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 提交一个新的审批实例并创建对应审批任务。
     * 方法会按租户和 definitionKey 读取审批定义，规范化审批人、写入实例与任务、记录提交审计，并在配置外部审批时发起外部提交流程。
     */
    public ApprovalInstanceView submit(ApprovalSubmitCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("approval submit command is required");
        }
        ApprovalDefinitionDO definition = definition(command.tenantId(), command.definitionKey());
        List<String> approvers = normalizeApprovers(command.approvers());
        LocalDateTime now = now();

        ApprovalInstanceDO instance = new ApprovalInstanceDO();
        instance.setTenantId(normalizeTenant(command.tenantId()));
        instance.setDefinitionKey(requireText(command.definitionKey(), "definitionKey"));
        instance.setDomain(requireText(command.domain(), "domain").toUpperCase(Locale.ROOT));
        instance.setTargetType(requireText(command.targetType(), "targetType").toUpperCase(Locale.ROOT));
        instance.setTargetId(requireText(command.targetId(), "targetId"));
        instance.setTargetVersionId(command.targetVersionId());
        instance.setStatus(STATUS_PENDING);
        instance.setSubmitter(defaultActor(command.submitter()));
        instance.setSubmitReason(trimToNull(command.submitReason()));
        instance.setRiskLevel(trimToNull(command.riskLevel()));
        instance.setRiskReasonsJson(trimToNull(command.riskReasonsJson()));
        instance.setSnapshotJson(trimToNull(command.snapshotJson()));
        instance.setRequestedAt(now);
        instance.setAutoAction(trimToNull(command.autoAction()));
        instance.setAutoActionStatus(instance.getAutoAction() == null ? null : AUTO_PENDING);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        instanceMapper.insert(instance);

        List<ApprovalTaskDO> tasks = new ArrayList<>();
        int dueHours = positiveOrDefault(command.dueHours(),
                positiveOrDefault(definition.getDefaultDueHours(), 24));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int i = 0; i < approvers.size(); i++) {
            ApprovalTaskDO task = new ApprovalTaskDO();
            task.setTenantId(instance.getTenantId());
            task.setInstanceId(instance.getId());
            task.setStepNo(i + 1);
            task.setApprover(approvers.get(i));
            task.setStatus(STATUS_PENDING);
            task.setDueAt(now.plusHours(dueHours));
            taskMapper.insert(task);
            tasks.add(task);
        }

        recordAudit(instance.getTenantId(), instance.getId(), null, "SUBMITTED",
                instance.getSubmitter(), null, null, STATUS_PENDING, detail(command.submitReason()));
        submitExternalIfNeeded(definition, instance, tasks, command);
        return toView(instance, tasks);
    }

    /**
     * 审批通过指定任务。
     * 会校验任务租户、待处理状态和操作者权限，并在实例满足通过条件时完成实例、记录审计和触发自动动作。
     */
    public ApprovalInstanceView approveTask(ApprovalDecisionCommand command) {
        return decide(command, true);
    }

    /**
     * 驳回指定审批任务。
     * 方法复用统一决策流程，完成任务和实例状态流转，记录审批意见，并按实例配置执行失败后的自动动作。
     */
    public ApprovalInstanceView rejectTask(ApprovalDecisionCommand command) {
        return decide(command, false);
    }

    /**
     * 针对业务目标查找最新待处理审批并代当前操作者完成决策。
     * {@code actorRole} 支持租户管理员越权审批；无待处理实例或任务已结束时返回 {@code null}，有待处理任务但操作者无权时抛出拒绝访问异常。
     */
    public ApprovalInstanceView decideTargetTask(Long tenantId,
                                                 String targetType,
                                                 String targetId,
                                                 String actor,
                                                 String actorRole,
                                                 String comment,
                                                 boolean approve) {
        ApprovalInstanceDO instance = latestPendingTargetInstance(tenantId, targetType, targetId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (instance == null) {
            return null;
        }
        String normalizedActor = defaultActor(actor);
        List<ApprovalTaskDO> tasks = tasks(instance.getTenantId(), instance.getId());
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        ApprovalTaskDO task = tasks.stream()
                .filter(candidate -> STATUS_PENDING.equalsIgnoreCase(candidate.getStatus()))
                .filter(candidate -> canAct(candidate, normalizedActor, actorRole))
                .findFirst()
                .orElse(null);
        if (task == null) {
            boolean hasPendingTask = tasks.stream()
                    .anyMatch(candidate -> STATUS_PENDING.equalsIgnoreCase(candidate.getStatus()));
            if (hasPendingTask) {
                throw new AccessDeniedException("approval task is not assigned to actor: " + normalizedActor);
            }
            return null;
        }
        ApprovalDecisionCommand command = new ApprovalDecisionCommand(
                instance.getTenantId(), task.getId(), normalizedActor, actorRole, comment);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return approve ? approveTask(command) : rejectTask(command);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 扫描并处理已到期的待审批任务。
     * 根据审批定义的超时策略延长等待或将实例置为过期，过程中会取消剩余任务、记录审计并触发到期自动动作；返回实际过期的实例数。
     */
    public int expireDueTasks(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 1000));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<ApprovalTaskDO> dueTasks = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getStatus, STATUS_PENDING)
                .isNotNull(ApprovalTaskDO::getDueAt)
                .le(ApprovalTaskDO::getDueAt, now())
                .orderByAsc(ApprovalTaskDO::getDueAt)
                .orderByAsc(ApprovalTaskDO::getId)
                .last("LIMIT " + boundedLimit));
        int expired = 0;
        Set<Long> handledInstances = new LinkedHashSet<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (ApprovalTaskDO dueTask : safeList(dueTasks)) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (dueTask.getInstanceId() == null || !handledInstances.add(dueTask.getInstanceId())) {
                continue;
            }
            ApprovalInstanceDO instance = instanceMapper.selectById(dueTask.getInstanceId());
            if (instance == null || !STATUS_PENDING.equalsIgnoreCase(instance.getStatus())) {
                continue;
            }
            if (keepWaitingOnTimeout(instance)) {
                extendPendingTasks(instance);
                continue;
            }
            expirePendingTasks(instance);
            completeInstance(instance, STATUS_EXPIRED, "system", "approval task due time reached");
            runAutoActionIfNeeded(instance, "system", null, STATUS_EXPIRED);
            expired++;
        }
        return expired;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 取消指定业务目标下仍处于待处理状态的审批实例。
     * 方法限定租户和目标类型/ID，更新实例完成信息、取消未完成任务并写入取消审计，常用于重新提交评审前关闭旧流程。
     */
    public void cancelOpen(Long tenantId, String targetType, String targetId, String actor, String reason) {
        List<ApprovalInstanceDO> open = instanceMapper.selectList(new LambdaQueryWrapper<ApprovalInstanceDO>()
                .eq(ApprovalInstanceDO::getTenantId, normalizeTenant(tenantId))
                .eq(ApprovalInstanceDO::getTargetType, requireText(targetType, "targetType").toUpperCase(Locale.ROOT))
                .eq(ApprovalInstanceDO::getTargetId, requireText(targetId, "targetId"))
                .eq(ApprovalInstanceDO::getStatus, STATUS_PENDING));
        for (ApprovalInstanceDO instance : safeList(open)) {
            instance.setStatus(STATUS_CANCELLED);
            instance.setCompletedAt(now());
            instance.setCompletedBy(defaultActor(actor));
            instance.setResultComment(trimToNull(reason));
            instanceMapper.updateById(instance);
            cancelPendingTasks(instance.getTenantId(), instance.getId(), null);
            recordAudit(instance.getTenantId(), instance.getId(), null, "CANCELLED",
                    defaultActor(actor), null, STATUS_PENDING, STATUS_CANCELLED, detail(reason));
        }
    }

    /**
     * 查询指定业务目标最近一次已通过审批。
     * 可按 definitionKey 和目标版本进一步限定，返回 {@code null} 表示当前目标或版本尚无有效通过记录。
     */
    public ApprovalInstanceView latestApproved(Long tenantId,
                                               String definitionKey,
                                               String targetType,
                                               String targetId,
                                               Long targetVersionId) {
        LambdaQueryWrapper<ApprovalInstanceDO> query = new LambdaQueryWrapper<ApprovalInstanceDO>()
                .eq(ApprovalInstanceDO::getTenantId, normalizeTenant(tenantId))
                .eq(definitionKey != null && !definitionKey.isBlank(), ApprovalInstanceDO::getDefinitionKey, definitionKey)
                .eq(ApprovalInstanceDO::getTargetType, requireText(targetType, "targetType").toUpperCase(Locale.ROOT))
                .eq(ApprovalInstanceDO::getTargetId, requireText(targetId, "targetId"))
                .eq(targetVersionId != null, ApprovalInstanceDO::getTargetVersionId, targetVersionId)
                .eq(ApprovalInstanceDO::getStatus, STATUS_APPROVED)
                .orderByDesc(ApprovalInstanceDO::getCompletedAt)
                .orderByDesc(ApprovalInstanceDO::getRequestedAt)
                .orderByDesc(ApprovalInstanceDO::getId)
                .last("LIMIT 1");
        ApprovalInstanceDO instance = instanceMapper.selectOne(query);
        return instance == null ? null : toView(instance, tasks(instance.getTenantId(), instance.getId()));
    }

    /**
     * 查询操作者在租户内可处理或可查看的审批任务。
     * 任务先按租户和状态过滤，再按审批人或管理员角色做权限过滤，避免普通用户看到无关任务。
     */
    public List<ApprovalTaskView> listTasks(Long tenantId, String actor, String actorRole, String status) {
        // 准备本次处理所需的上下文和中间变量。
        LambdaQueryWrapper<ApprovalTaskDO> query = new LambdaQueryWrapper<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getTenantId, normalizeTenant(tenantId))
                .eq(status != null && !status.isBlank(), ApprovalTaskDO::getStatus, normalizeStatus(status))
                .orderByAsc(ApprovalTaskDO::getDueAt)
                .orderByDesc(ApprovalTaskDO::getId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(taskMapper.selectList(query)).stream()
                .filter(task -> canAct(task, defaultActor(actor), actorRole))
                .map(this::toTaskView)
                .toList();
    }

    /**
     * 查询租户内审批实例列表。
     * 支持按目标类型、目标 ID 和实例状态过滤，返回值包含实例下的任务快照，便于审批中心展示完整流程状态。
     */
    public List<ApprovalInstanceView> listInstances(Long tenantId, String targetType, String targetId, String status) {
        LambdaQueryWrapper<ApprovalInstanceDO> query = new LambdaQueryWrapper<ApprovalInstanceDO>()
                .eq(ApprovalInstanceDO::getTenantId, normalizeTenant(tenantId))
                .eq(targetType != null && !targetType.isBlank(), ApprovalInstanceDO::getTargetType,
                        targetType == null ? null : targetType.trim().toUpperCase(Locale.ROOT))
                .eq(targetId != null && !targetId.isBlank(), ApprovalInstanceDO::getTargetId, targetId)
                .eq(status != null && !status.isBlank(), ApprovalInstanceDO::getStatus, normalizeStatus(status))
                .orderByDesc(ApprovalInstanceDO::getRequestedAt)
                .orderByDesc(ApprovalInstanceDO::getId);
        return safeList(instanceMapper.selectList(query)).stream()
                .map(instance -> toView(instance, tasks(instance.getTenantId(), instance.getId())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 手动同步单个外部审批实例的最新状态。
     * 会先校验实例归属租户，再调用配置的外部审批 Provider，同步任务与实例状态并返回同步后的视图。
     */
    public ApprovalInstanceView syncExternalInstance(Long tenantId, Long instanceId) {
        if (instanceId == null || instanceId <= 0) {
            throw new IllegalArgumentException("instanceId is required");
        }
        ApprovalInstanceDO instance = instanceMapper.selectById(instanceId);
        if (instance == null || !Objects.equals(instance.getTenantId(), normalizeTenant(tenantId))) {
            throw new IllegalArgumentException("approval instance not found: " + instanceId);
        }
        return syncExternalInstance(instance);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 批量同步租户内仍待处理且绑定外部实例的审批流程。
     * 返回状态发生变化的实例数量，调用外部 Provider 可能产生网络请求或第三方限流影响。
     */
    public int syncPendingExternalInstances(Long tenantId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 1000));
        List<ApprovalInstanceDO> instances = instanceMapper.selectList(new LambdaQueryWrapper<ApprovalInstanceDO>()
                .eq(ApprovalInstanceDO::getTenantId, normalizeTenant(tenantId))
                .eq(ApprovalInstanceDO::getStatus, STATUS_PENDING)
                .isNotNull(ApprovalInstanceDO::getExternalInstanceId)
                .orderByAsc(ApprovalInstanceDO::getRequestedAt)
                .orderByAsc(ApprovalInstanceDO::getId)
                .last("LIMIT " + boundedLimit));
        int synced = 0;
        for (ApprovalInstanceDO instance : safeList(instances)) {
            String before = instance.getStatus();
            syncExternalInstance(instance);
            if (!Objects.equals(before, instance.getStatus())) {
                synced++;
            }
        }
        return synced;
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param instance instance 参数，用于 syncExternalInstance 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private ApprovalInstanceView syncExternalInstance(ApprovalInstanceDO instance) {
        ApprovalDefinitionDO definition = definition(instance.getTenantId(), instance.getDefinitionKey());
        ApprovalExternalProvider provider = externalProvider(definition);
        if (provider == null) {
            return toView(instance, tasks(instance.getTenantId(), instance.getId()));
        }
        ApprovalExternalSyncResult result = provider.sync(definition, instance);
        if (result == null) {
            return toView(instance, tasks(instance.getTenantId(), instance.getId()));
        }
        List<ApprovalTaskDO> tasks = tasks(instance.getTenantId(), instance.getId());
        syncTaskStatuses(instance, tasks, result.taskStatusesByExternalTaskId());
        syncInstanceStatus(instance, result.instanceStatus());
        return toView(instance, tasks(instance.getTenantId(), instance.getId()));
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param approve approve 参数，用于 decide 流程中的校验、计算或对象转换。
     * @return 返回 decide 流程生成的业务结果。
     */
    private ApprovalInstanceView decide(ApprovalDecisionCommand command, boolean approve) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null || command.taskId() == null || command.taskId() <= 0) {
            throw new IllegalArgumentException("taskId is required");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ApprovalTaskDO task = taskMapper.selectById(command.taskId());
        if (task == null || !Objects.equals(task.getTenantId(), normalizeTenant(command.tenantId()))) {
            throw new IllegalArgumentException("approval task not found: " + command.taskId());
        }
        if (!STATUS_PENDING.equalsIgnoreCase(task.getStatus())) {
            throw new IllegalStateException("approval task is not pending: " + command.taskId());
        }
        String actor = defaultActor(command.actor());
        if (!canAct(task, actor, command.actorRole())) {
            throw new AccessDeniedException("approval task is not assigned to actor: " + actor);
        }
        ApprovalInstanceDO instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null || !Objects.equals(instance.getTenantId(), task.getTenantId())) {
            throw new IllegalArgumentException("approval instance not found: " + task.getInstanceId());
        }
        if (!STATUS_PENDING.equalsIgnoreCase(instance.getStatus())) {
            throw new IllegalStateException("approval instance is not pending: " + instance.getId());
        }
        ApprovalDefinitionDO definition = definition(instance.getTenantId(), instance.getDefinitionKey());
        decideExternalIfNeeded(definition, instance, task, command, approve);

        String oldTaskStatus = task.getStatus();
        task.setStatus(approve ? STATUS_APPROVED : STATUS_REJECTED);
        task.setActedAt(now());
        task.setActionComment(trimToNull(command.comment()));
        taskMapper.updateById(task);
        recordAudit(task.getTenantId(), instance.getId(), task.getId(), approve ? "TASK_APPROVED" : "TASK_REJECTED",
                actor, command.actorRole(), oldTaskStatus, task.getStatus(), detail(command.comment()));

        List<ApprovalTaskDO> tasks = tasks(instance.getTenantId(), instance.getId());
        if (approve) {
            if (isFinalApproval(definition, tasks)) {
                completeInstance(instance, STATUS_APPROVED, actor, command.comment());
                cancelPendingTasks(instance.getTenantId(), instance.getId(), task.getId());
                runAutoActionIfNeeded(instance, actor, command.actorRole(), STATUS_APPROVED);
            }
        } else {
            completeInstance(instance, STATUS_REJECTED, actor, command.comment());
            cancelPendingTasks(instance.getTenantId(), instance.getId(), task.getId());
            runAutoActionIfNeeded(instance, actor, command.actorRole(), STATUS_REJECTED);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(instance, tasks(instance.getTenantId(), instance.getId()));
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param instance instance 参数，用于 completeInstance 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param comment comment 参数，用于 completeInstance 流程中的校验、计算或对象转换。
     */
    private void completeInstance(ApprovalInstanceDO instance, String status, String actor, String comment) {
        String oldStatus = instance.getStatus();
        instance.setStatus(status);
        instance.setCompletedAt(now());
        instance.setCompletedBy(actor);
        instance.setResultComment(trimToNull(comment));
        instanceMapper.updateById(instance);
        recordAudit(instance.getTenantId(), instance.getId(), null, "INSTANCE_" + status,
                /**
                 * 查询并组装符合条件的业务数据。
                 *
                 * @return 返回 detail 流程生成的业务结果。
                 */
                actor, null, oldStatus, status, detail(comment));
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param instance instance 参数，用于 runAutoActionIfNeeded 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param actorRole actor role 参数，用于 runAutoActionIfNeeded 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     */
    private void runAutoActionIfNeeded(ApprovalInstanceDO instance, String actor, String actorRole, String status) {
        String autoAction = trimToNull(instance.getAutoAction());
        if (autoAction == null) {
            return;
        }
        ApprovalAutoActionHandler handler = autoActionHandlers.stream()
                .filter(candidate -> candidate.supports(autoAction))
                .filter(candidate -> candidate.supportsStatus(autoAction, status))
                .findFirst()
                .orElse(null);
        if (handler == null) {
            if (!STATUS_APPROVED.equalsIgnoreCase(status)) {
                return;
            }
            instance.setAutoActionStatus(AUTO_FAILED);
            instance.setAutoActionError("No auto action handler for " + autoAction);
            instanceMapper.updateById(instance);
            recordAudit(instance.getTenantId(), instance.getId(), null, "AUTO_ACTION_FAILED",
                    actor, actorRole, AUTO_PENDING, AUTO_FAILED, instance.getAutoActionError());
            return;
        }
        instance.setAutoActionStatus(AUTO_RUNNING);
        instance.setAutoActionError(null);
        instanceMapper.updateById(instance);
        try {
            handler.execute(instance, actor);
            instance.setAutoActionStatus(AUTO_SUCCESS);
            instance.setAutoActionError(null);
            instanceMapper.updateById(instance);
            recordAudit(instance.getTenantId(), instance.getId(), null, "AUTO_ACTION_SUCCEEDED",
                    actor, actorRole, AUTO_RUNNING, AUTO_SUCCESS, autoAction);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            instance.setAutoActionStatus(AUTO_FAILED);
            instance.setAutoActionError(ex.getMessage());
            instanceMapper.updateById(instance);
            recordAudit(instance.getTenantId(), instance.getId(), null, "AUTO_ACTION_FAILED",
                    actor, actorRole, AUTO_RUNNING, AUTO_FAILED, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param definition definition 参数，用于 submitExternalIfNeeded 流程中的校验、计算或对象转换。
     * @param instance instance 参数，用于 submitExternalIfNeeded 流程中的校验、计算或对象转换。
     * @param tasks tasks 参数，用于 submitExternalIfNeeded 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     */
    private void submitExternalIfNeeded(ApprovalDefinitionDO definition,
                                        ApprovalInstanceDO instance,
                                        List<ApprovalTaskDO> tasks,
                                        ApprovalSubmitCommand command) {
        ApprovalExternalProvider provider = externalProvider(definition);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (provider == null) {
            return;
        }
        ApprovalExternalSubmissionResult result = provider.submit(definition, instance, tasks, command);
        if (result == null) {
            return;
        }
        String externalInstanceId = trimToNull(result.externalInstanceId());
        if (externalInstanceId != null) {
            instance.setExternalInstanceId(externalInstanceId);
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            instanceMapper.updateById(instance);
            recordAudit(instance.getTenantId(), instance.getId(), null, "EXTERNAL_INSTANCE_BOUND",
                    instance.getSubmitter(), null, null, null, externalInstanceId);
        }
        if (result.externalTaskIdsByLocalTaskId() == null || result.externalTaskIdsByLocalTaskId().isEmpty()) {
            return;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (ApprovalTaskDO task : safeList(tasks)) {
            String externalTaskId = trimToNull(result.externalTaskIdsByLocalTaskId().get(task.getId()));
            if (externalTaskId == null) {
                continue;
            }
            task.setExternalTaskId(externalTaskId);
            taskMapper.updateById(task);
            recordAudit(task.getTenantId(), instance.getId(), task.getId(), "EXTERNAL_TASK_BOUND",
                    instance.getSubmitter(), null, null, null, externalTaskId);
        }
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param definition definition 参数，用于 decideExternalIfNeeded 流程中的校验、计算或对象转换。
     * @param instance instance 参数，用于 decideExternalIfNeeded 流程中的校验、计算或对象转换。
     * @param task task 参数，用于 decideExternalIfNeeded 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param approve approve 参数，用于 decideExternalIfNeeded 流程中的校验、计算或对象转换。
     */
    private void decideExternalIfNeeded(ApprovalDefinitionDO definition,
                                        ApprovalInstanceDO instance,
                                        ApprovalTaskDO task,
                                        ApprovalDecisionCommand command,
                                        boolean approve) {
        ApprovalExternalProvider provider = externalProvider(definition);
        if (provider == null) {
            return;
        }
        provider.decide(definition, instance, task, command, approve);
        recordAudit(instance.getTenantId(), instance.getId(), task.getId(),
                approve ? "EXTERNAL_TASK_APPROVED" : "EXTERNAL_TASK_REJECTED",
                command.actor(), command.actorRole(), null, null, task.getExternalTaskId());
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param instance instance 参数，用于 syncTaskStatuses 流程中的校验、计算或对象转换。
     * @param tasks tasks 参数，用于 syncTaskStatuses 流程中的校验、计算或对象转换。
     * @param externalTaskStatuses external task statuses 参数，用于 syncTaskStatuses 流程中的校验、计算或对象转换。
     */
    private void syncTaskStatuses(ApprovalInstanceDO instance,
                                  List<ApprovalTaskDO> tasks,
                                  Map<String, String> externalTaskStatuses) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (externalTaskStatuses == null || externalTaskStatuses.isEmpty()) {
            return;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (ApprovalTaskDO task : safeList(tasks)) {
            String externalTaskId = trimToNull(task.getExternalTaskId());
            String status = externalTaskId == null ? null : trimToNull(externalTaskStatuses.get(externalTaskId));
            if (status == null || status.equalsIgnoreCase(task.getStatus())) {
                continue;
            }
            String oldStatus = task.getStatus();
            task.setStatus(normalizeStatus(status));
            if (!STATUS_PENDING.equalsIgnoreCase(task.getStatus())) {
                task.setActedAt(now());
            }
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            taskMapper.updateById(task);
            recordAudit(task.getTenantId(), instance.getId(), task.getId(),
                    "EXTERNAL_SYNC_TASK_" + task.getStatus(),
                    "lark", null, oldStatus, task.getStatus(), externalTaskId);
        }
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param instance instance 参数，用于 syncInstanceStatus 流程中的校验、计算或对象转换。
     * @param externalStatus 业务状态，用于筛选或推进状态流转。
     */
    private void syncInstanceStatus(ApprovalInstanceDO instance, String externalStatus) {
        String status = trimToNull(externalStatus);
        if (status == null || status.equalsIgnoreCase(instance.getStatus())) {
            return;
        }
        String normalized = normalizeStatus(status);
        String oldStatus = instance.getStatus();
        instance.setStatus(normalized);
        if (isTerminalStatus(normalized)) {
            instance.setCompletedAt(now());
            instance.setCompletedBy("lark");
        }
        instanceMapper.updateById(instance);
        recordAudit(instance.getTenantId(), instance.getId(), null,
                "EXTERNAL_SYNC_INSTANCE_" + normalized,
                /**
                 * 解析、归一化或保护输入值，生成安全可用的中间结果。
                 *
                 * @return 返回解析、归一化或安全处理后的值。
                 */
                "lark", null, oldStatus, normalized, trimToNull(instance.getExternalInstanceId()));
        if (isTerminalStatus(normalized)) {
            runAutoActionIfNeeded(instance, "lark", null, normalized);
        }
    }

    /**
     * 执行 externalProvider 流程，围绕 external provider 完成校验、计算或结果组装。
     *
     * @param definition definition 参数，用于 externalProvider 流程中的校验、计算或对象转换。
     * @return 返回 externalProvider 流程生成的业务结果。
     */
    private ApprovalExternalProvider externalProvider(ApprovalDefinitionDO definition) {
        String provider = definition == null ? null : trimToNull(definition.getExternalProvider());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (provider == null || "LOCAL".equalsIgnoreCase(provider)) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return externalProviders.stream()
                .filter(candidate -> candidate.supports(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("approval external provider is not configured: " + provider));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param definition definition 参数，用于 isFinalApproval 流程中的校验、计算或对象转换。
     * @param tasks tasks 参数，用于 isFinalApproval 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isFinalApproval(ApprovalDefinitionDO definition, List<ApprovalTaskDO> tasks) {
        String mode = definition.getMode() == null ? MODE_ANY_ONE : definition.getMode().trim().toUpperCase(Locale.ROOT);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        int approved = (int) tasks.stream().filter(task -> STATUS_APPROVED.equalsIgnoreCase(task.getStatus())).count();
        int pending = (int) tasks.stream().filter(task -> STATUS_PENDING.equalsIgnoreCase(task.getStatus())).count();
        int minApprovals = positiveOrDefault(definition.getMinApprovals(), 1);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (MODE_ANY_ONE.equals(mode)) {
            return approved >= 1;
        }
        if (MODE_PARALLEL.equals(mode)) {
            return approved >= minApprovals || pending == 0;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return approved >= minApprovals && pending == 0;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回布尔判断结果。
     */
    private boolean isTerminalStatus(String status) {
        return STATUS_APPROVED.equalsIgnoreCase(status)
                || STATUS_REJECTED.equalsIgnoreCase(status)
                || STATUS_CANCELLED.equalsIgnoreCase(status)
                || STATUS_EXPIRED.equalsIgnoreCase(status);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param targetType 类型标识，用于选择对应处理分支。
     * @param targetId 业务对象 ID，用于定位具体记录。
     * @return 返回 latestPendingTargetInstance 流程生成的业务结果。
     */
    private ApprovalInstanceDO latestPendingTargetInstance(Long tenantId, String targetType, String targetId) {
        return instanceMapper.selectOne(new LambdaQueryWrapper<ApprovalInstanceDO>()
                .eq(ApprovalInstanceDO::getTenantId, normalizeTenant(tenantId))
                .eq(ApprovalInstanceDO::getTargetType, requireText(targetType, "targetType").toUpperCase(Locale.ROOT))
                .eq(ApprovalInstanceDO::getTargetId, requireText(targetId, "targetId"))
                .eq(ApprovalInstanceDO::getStatus, STATUS_PENDING)
                .orderByDesc(ApprovalInstanceDO::getRequestedAt)
                .orderByDesc(ApprovalInstanceDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param instance instance 参数，用于 expirePendingTasks 流程中的校验、计算或对象转换。
     */
    private void expirePendingTasks(ApprovalInstanceDO instance) {
        List<ApprovalTaskDO> pendingTasks = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getTenantId, instance.getTenantId())
                .eq(ApprovalTaskDO::getInstanceId, instance.getId())
                .eq(ApprovalTaskDO::getStatus, STATUS_PENDING));
        for (ApprovalTaskDO task : safeList(pendingTasks)) {
            String oldStatus = task.getStatus();
            task.setStatus(STATUS_EXPIRED);
            task.setActedAt(now());
            task.setActionComment("approval task due time reached");
            int updated = taskMapper.update(task, new LambdaUpdateWrapper<ApprovalTaskDO>()
                    .eq(ApprovalTaskDO::getId, task.getId())
                    .eq(ApprovalTaskDO::getStatus, STATUS_PENDING));
            if (updated > 0) {
                recordAudit(task.getTenantId(), instance.getId(), task.getId(), "TASK_EXPIRED",
                        /**
                         * 查询并组装符合条件的业务数据。
                         *
                         * @return 返回 detail 流程生成的业务结果。
                         */
                        "system", null, oldStatus, STATUS_EXPIRED, detail(task.getActionComment()));
            }
        }
    }

    /**
     * 执行 extendPendingTasks 流程，围绕 extend pending tasks 完成校验、计算或结果组装。
     *
     * @param instance instance 参数，用于 extendPendingTasks 流程中的校验、计算或对象转换。
     */
    private void extendPendingTasks(ApprovalInstanceDO instance) {
        LocalDateTime nextDueAt = now().plusHours(24);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<ApprovalTaskDO> pendingTasks = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getTenantId, instance.getTenantId())
                .eq(ApprovalTaskDO::getInstanceId, instance.getId())
                .eq(ApprovalTaskDO::getStatus, STATUS_PENDING));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (ApprovalTaskDO task : safeList(pendingTasks)) {
            task.setDueAt(nextDueAt);
            int updated = taskMapper.update(task, new LambdaUpdateWrapper<ApprovalTaskDO>()
                    .eq(ApprovalTaskDO::getId, task.getId())
                    .eq(ApprovalTaskDO::getStatus, STATUS_PENDING));
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (updated > 0) {
                recordAudit(task.getTenantId(), instance.getId(), task.getId(), "TASK_DUE_EXTENDED",
                        "system", null, STATUS_PENDING, STATUS_PENDING, "KEEP_WAITING");
            }
        }
    }

    /**
     * 执行 keepWaitingOnTimeout 流程，围绕 keep waiting on timeout 完成校验、计算或结果组装。
     *
     * @param instance instance 参数，用于 keepWaitingOnTimeout 流程中的校验、计算或对象转换。
     * @return 返回 keep waiting on timeout 的布尔判断结果。
     */
    private boolean keepWaitingOnTimeout(ApprovalInstanceDO instance) {
        String snapshot = instance.getSnapshotJson();
        if (snapshot == null || snapshot.isBlank()) {
            return false;
        }
        try {
            JsonNode node = JSON.readTree(snapshot);
            return node.hasNonNull("onTimeout")
                    && "KEEP_WAITING".equalsIgnoreCase(node.get("onTimeout").asText());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 执行 definition 流程，围绕 definition 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param definitionKey 业务键，用于在同一租户下定位资源。
     * @return 返回 definition 流程生成的业务结果。
     */
    private ApprovalDefinitionDO definition(Long tenantId, String definitionKey) {
        String key = requireText(definitionKey, "definitionKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ApprovalDefinitionDO definition = definitionMapper.selectOne(new LambdaQueryWrapper<ApprovalDefinitionDO>()
                .in(ApprovalDefinitionDO::getTenantId, List.of(normalizeTenant(tenantId), 0L))
                .eq(ApprovalDefinitionDO::getDefinitionKey, key)
                .eq(ApprovalDefinitionDO::getEnabled, 1)
                .orderByDesc(ApprovalDefinitionDO::getTenantId)
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (definition != null) {
            return definition;
        }
        ApprovalDefinitionDO fallback = new ApprovalDefinitionDO();
        fallback.setTenantId(0L);
        fallback.setDefinitionKey(key);
        fallback.setName(key);
        fallback.setDomain("GENERAL");
        fallback.setTargetType("GENERAL");
        fallback.setEnabled(1);
        fallback.setMode(MODE_ANY_ONE);
        fallback.setMinApprovals(1);
        fallback.setDefaultDueHours(24);
        fallback.setExternalProvider("LOCAL");
        // 汇总前面计算出的状态和明细，返回给调用方。
        return fallback;
    }

    /**
     * 执行 tasks 流程，围绕 tasks 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param instanceId 业务对象 ID，用于定位具体记录。
     * @return 返回 tasks 汇总后的集合、分页或映射视图。
     */
    private List<ApprovalTaskDO> tasks(Long tenantId, Long instanceId) {
        if (instanceId == null) {
            return List.of();
        }
        return safeList(taskMapper.selectList(new LambdaQueryWrapper<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getTenantId, tenantId)
                .eq(ApprovalTaskDO::getInstanceId, instanceId)
                .orderByAsc(ApprovalTaskDO::getStepNo)
                .orderByAsc(ApprovalTaskDO::getId)));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param instanceId 业务对象 ID，用于定位具体记录。
     * @param exceptTaskId 业务对象 ID，用于定位具体记录。
     */
    private void cancelPendingTasks(Long tenantId, Long instanceId, Long exceptTaskId) {
        List<ApprovalTaskDO> pendingTasks = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getTenantId, tenantId)
                .eq(ApprovalTaskDO::getInstanceId, instanceId)
                .eq(ApprovalTaskDO::getStatus, STATUS_PENDING));
        for (ApprovalTaskDO task : safeList(pendingTasks)) {
            if (exceptTaskId != null && Objects.equals(exceptTaskId, task.getId())) {
                continue;
            }
            task.setStatus(STATUS_CANCELLED);
            taskMapper.update(task, new LambdaUpdateWrapper<ApprovalTaskDO>()
                    .eq(ApprovalTaskDO::getId, task.getId())
                    .eq(ApprovalTaskDO::getStatus, STATUS_PENDING));
        }
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param task task 参数，用于 canAct 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param actorRole actor role 参数，用于 canAct 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean canAct(ApprovalTaskDO task, String actor, String actorRole) {
        // 准备本次处理所需的上下文和中间变量。
        String approver = task.getApprover();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (approver == null || actor == null) {
            return false;
        }
        if (approver.equalsIgnoreCase(actor)) {
            return true;
        }
        if (ROLE_TENANT_ADMIN.equalsIgnoreCase(approver)) {
            return RoleNames.TENANT_ADMIN.equalsIgnoreCase(actorRole)
                    || RoleNames.ADMIN.equalsIgnoreCase(actorRole)
                    || RoleNames.SUPER_ADMIN.equalsIgnoreCase(actorRole);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "*".equals(approver);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param instance instance 参数，用于 toView 流程中的校验、计算或对象转换。
     * @param tasks tasks 参数，用于 toView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private ApprovalInstanceView toView(ApprovalInstanceDO instance, List<ApprovalTaskDO> tasks) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ApprovalInstanceView(
                instance.getId(),
                instance.getTenantId(),
                instance.getDefinitionKey(),
                instance.getDomain(),
                instance.getTargetType(),
                instance.getTargetId(),
                instance.getTargetVersionId(),
                instance.getStatus(),
                instance.getSubmitter(),
                instance.getSubmitReason(),
                instance.getRiskLevel(),
                instance.getRiskReasonsJson(),
                instance.getSnapshotJson(),
                instance.getExternalInstanceId(),
                instance.getRequestedAt(),
                instance.getCompletedAt(),
                instance.getCompletedBy(),
                instance.getResultComment(),
                instance.getAutoAction(),
                instance.getAutoActionStatus(),
                instance.getAutoActionError(),
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                safeList(tasks).stream()
                        .filter(task -> STATUS_PENDING.equalsIgnoreCase(task.getStatus()))
                        .map(this::toTaskView)
                        .toList());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param task task 参数，用于 toTaskView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private ApprovalTaskView toTaskView(ApprovalTaskDO task) {
        return new ApprovalTaskView(
                task.getId(),
                task.getTenantId(),
                task.getInstanceId(),
                task.getStepNo(),
                task.getApprover(),
                task.getStatus(),
                task.getExternalTaskId(),
                task.getDueAt(),
                task.getActedAt(),
                task.getActionComment());
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param instanceId 业务对象 ID，用于定位具体记录。
     * @param taskId 业务对象 ID，用于定位具体记录。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param actorRole actor role 参数，用于 recordAudit 流程中的校验、计算或对象转换。
     * @param oldStatus 业务状态，用于筛选或推进状态流转。
     * @param newStatus 业务状态，用于筛选或推进状态流转。
     * @param detailJson JSON 字符串，承载结构化配置或明细。
     */
    private void recordAudit(Long tenantId,
                             Long instanceId,
                             Long taskId,
                             String eventType,
                             String actor,
                             String actorRole,
                             String oldStatus,
                             String newStatus,
                             String detailJson) {
        if (instanceId == null) {
            return;
        }
        ApprovalAuditEventDO row = new ApprovalAuditEventDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setInstanceId(instanceId);
        row.setTaskId(taskId);
        row.setEventType(eventType);
        row.setActor(defaultActor(actor));
        row.setActorRole(trimToNull(actorRole));
        row.setOldStatus(trimToNull(oldStatus));
        row.setNewStatus(trimToNull(newStatus));
        row.setDetailJson(trimToNull(detailJson));
        row.setCreatedAt(now());
        auditEventMapper.insert(row);
    }

    /**
     * 规范化输入值。
     *
     * @param raw raw 参数，用于 normalizeApprovers 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> normalizeApprovers(List<String> raw) {
        Set<String> approvers = new LinkedHashSet<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (raw != null) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            raw.stream()
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .forEach(approvers::add);
        }
        if (approvers.isEmpty()) {
            approvers.add(ROLE_TENANT_ADMIN);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(approvers);
    }

    /**
     * 规范化输入值。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        return requireText(status, "status").toUpperCase(Locale.ROOT);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    /**
     * 执行 positiveOrDefault 流程，围绕 positive or default 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 positiveOrDefault 流程中的校验、计算或对象转换。
     * @return 返回 positive or default 计算得到的数量、金额或指标值。
     */
    private int positiveOrDefault(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    /**
     * 解析操作人标识。
     *
     * @param raw raw 参数，用于 defaultActor 流程中的校验、计算或对象转换。
     * @return 返回 default actor 生成的文本或业务键。
     */
    private String defaultActor(String raw) {
        String value = trimToNull(raw);
        return value == null ? "system" : value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param raw raw 参数，用于 requireText 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String raw, String field) {
        String value = trimToNull(raw);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param raw raw 参数，用于 trimToNull 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param comment comment 参数，用于 detail 流程中的校验、计算或对象转换。
     * @return 返回 detail 生成的文本或业务键。
     */
    private String detail(String comment) {
        return trimToNull(comment);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
