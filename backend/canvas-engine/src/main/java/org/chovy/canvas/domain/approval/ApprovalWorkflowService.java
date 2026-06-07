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
    public ApprovalInstanceView submit(ApprovalSubmitCommand command) {
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
        instanceMapper.insert(instance);

        List<ApprovalTaskDO> tasks = new ArrayList<>();
        int dueHours = positiveOrDefault(command.dueHours(),
                positiveOrDefault(definition.getDefaultDueHours(), 24));
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

    public ApprovalInstanceView approveTask(ApprovalDecisionCommand command) {
        return decide(command, true);
    }

    public ApprovalInstanceView rejectTask(ApprovalDecisionCommand command) {
        return decide(command, false);
    }

    public ApprovalInstanceView decideTargetTask(Long tenantId,
                                                 String targetType,
                                                 String targetId,
                                                 String actor,
                                                 String actorRole,
                                                 String comment,
                                                 boolean approve) {
        ApprovalInstanceDO instance = latestPendingTargetInstance(tenantId, targetType, targetId);
        if (instance == null) {
            return null;
        }
        String normalizedActor = defaultActor(actor);
        List<ApprovalTaskDO> tasks = tasks(instance.getTenantId(), instance.getId());
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
        return approve ? approveTask(command) : rejectTask(command);
    }

    @Transactional(rollbackFor = Exception.class)
    public int expireDueTasks(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 1000));
        List<ApprovalTaskDO> dueTasks = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getStatus, STATUS_PENDING)
                .isNotNull(ApprovalTaskDO::getDueAt)
                .le(ApprovalTaskDO::getDueAt, now())
                .orderByAsc(ApprovalTaskDO::getDueAt)
                .orderByAsc(ApprovalTaskDO::getId)
                .last("LIMIT " + boundedLimit));
        int expired = 0;
        Set<Long> handledInstances = new LinkedHashSet<>();
        for (ApprovalTaskDO dueTask : safeList(dueTasks)) {
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

    public List<ApprovalTaskView> listTasks(Long tenantId, String actor, String actorRole, String status) {
        LambdaQueryWrapper<ApprovalTaskDO> query = new LambdaQueryWrapper<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getTenantId, normalizeTenant(tenantId))
                .eq(status != null && !status.isBlank(), ApprovalTaskDO::getStatus, normalizeStatus(status))
                .orderByAsc(ApprovalTaskDO::getDueAt)
                .orderByDesc(ApprovalTaskDO::getId);
        return safeList(taskMapper.selectList(query)).stream()
                .filter(task -> canAct(task, defaultActor(actor), actorRole))
                .map(this::toTaskView)
                .toList();
    }

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

    private ApprovalInstanceView decide(ApprovalDecisionCommand command, boolean approve) {
        if (command == null || command.taskId() == null || command.taskId() <= 0) {
            throw new IllegalArgumentException("taskId is required");
        }
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
        return toView(instance, tasks(instance.getTenantId(), instance.getId()));
    }

    private void completeInstance(ApprovalInstanceDO instance, String status, String actor, String comment) {
        String oldStatus = instance.getStatus();
        instance.setStatus(status);
        instance.setCompletedAt(now());
        instance.setCompletedBy(actor);
        instance.setResultComment(trimToNull(comment));
        instanceMapper.updateById(instance);
        recordAudit(instance.getTenantId(), instance.getId(), null, "INSTANCE_" + status,
                actor, null, oldStatus, status, detail(comment));
    }

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
        } catch (RuntimeException ex) {
            instance.setAutoActionStatus(AUTO_FAILED);
            instance.setAutoActionError(ex.getMessage());
            instanceMapper.updateById(instance);
            recordAudit(instance.getTenantId(), instance.getId(), null, "AUTO_ACTION_FAILED",
                    actor, actorRole, AUTO_RUNNING, AUTO_FAILED, ex.getMessage());
            throw ex;
        }
    }

    private void submitExternalIfNeeded(ApprovalDefinitionDO definition,
                                        ApprovalInstanceDO instance,
                                        List<ApprovalTaskDO> tasks,
                                        ApprovalSubmitCommand command) {
        ApprovalExternalProvider provider = externalProvider(definition);
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
            instanceMapper.updateById(instance);
            recordAudit(instance.getTenantId(), instance.getId(), null, "EXTERNAL_INSTANCE_BOUND",
                    instance.getSubmitter(), null, null, null, externalInstanceId);
        }
        if (result.externalTaskIdsByLocalTaskId() == null || result.externalTaskIdsByLocalTaskId().isEmpty()) {
            return;
        }
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

    private void syncTaskStatuses(ApprovalInstanceDO instance,
                                  List<ApprovalTaskDO> tasks,
                                  Map<String, String> externalTaskStatuses) {
        if (externalTaskStatuses == null || externalTaskStatuses.isEmpty()) {
            return;
        }
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
            taskMapper.updateById(task);
            recordAudit(task.getTenantId(), instance.getId(), task.getId(),
                    "EXTERNAL_SYNC_TASK_" + task.getStatus(),
                    "lark", null, oldStatus, task.getStatus(), externalTaskId);
        }
    }

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
                "lark", null, oldStatus, normalized, trimToNull(instance.getExternalInstanceId()));
        if (isTerminalStatus(normalized)) {
            runAutoActionIfNeeded(instance, "lark", null, normalized);
        }
    }

    private ApprovalExternalProvider externalProvider(ApprovalDefinitionDO definition) {
        String provider = definition == null ? null : trimToNull(definition.getExternalProvider());
        if (provider == null || "LOCAL".equalsIgnoreCase(provider)) {
            return null;
        }
        return externalProviders.stream()
                .filter(candidate -> candidate.supports(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("approval external provider is not configured: " + provider));
    }

    private boolean isFinalApproval(ApprovalDefinitionDO definition, List<ApprovalTaskDO> tasks) {
        String mode = definition.getMode() == null ? MODE_ANY_ONE : definition.getMode().trim().toUpperCase(Locale.ROOT);
        int approved = (int) tasks.stream().filter(task -> STATUS_APPROVED.equalsIgnoreCase(task.getStatus())).count();
        int pending = (int) tasks.stream().filter(task -> STATUS_PENDING.equalsIgnoreCase(task.getStatus())).count();
        int minApprovals = positiveOrDefault(definition.getMinApprovals(), 1);
        if (MODE_ANY_ONE.equals(mode)) {
            return approved >= 1;
        }
        if (MODE_PARALLEL.equals(mode)) {
            return approved >= minApprovals || pending == 0;
        }
        return approved >= minApprovals && pending == 0;
    }

    private boolean isTerminalStatus(String status) {
        return STATUS_APPROVED.equalsIgnoreCase(status)
                || STATUS_REJECTED.equalsIgnoreCase(status)
                || STATUS_CANCELLED.equalsIgnoreCase(status)
                || STATUS_EXPIRED.equalsIgnoreCase(status);
    }

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
                        "system", null, oldStatus, STATUS_EXPIRED, detail(task.getActionComment()));
            }
        }
    }

    private void extendPendingTasks(ApprovalInstanceDO instance) {
        LocalDateTime nextDueAt = now().plusHours(24);
        List<ApprovalTaskDO> pendingTasks = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getTenantId, instance.getTenantId())
                .eq(ApprovalTaskDO::getInstanceId, instance.getId())
                .eq(ApprovalTaskDO::getStatus, STATUS_PENDING));
        for (ApprovalTaskDO task : safeList(pendingTasks)) {
            task.setDueAt(nextDueAt);
            int updated = taskMapper.update(task, new LambdaUpdateWrapper<ApprovalTaskDO>()
                    .eq(ApprovalTaskDO::getId, task.getId())
                    .eq(ApprovalTaskDO::getStatus, STATUS_PENDING));
            if (updated > 0) {
                recordAudit(task.getTenantId(), instance.getId(), task.getId(), "TASK_DUE_EXTENDED",
                        "system", null, STATUS_PENDING, STATUS_PENDING, "KEEP_WAITING");
            }
        }
    }

    private boolean keepWaitingOnTimeout(ApprovalInstanceDO instance) {
        String snapshot = instance.getSnapshotJson();
        if (snapshot == null || snapshot.isBlank()) {
            return false;
        }
        try {
            JsonNode node = JSON.readTree(snapshot);
            return node.hasNonNull("onTimeout")
                    && "KEEP_WAITING".equalsIgnoreCase(node.get("onTimeout").asText());
        } catch (Exception ignored) {
            return false;
        }
    }

    private ApprovalDefinitionDO definition(Long tenantId, String definitionKey) {
        String key = requireText(definitionKey, "definitionKey");
        ApprovalDefinitionDO definition = definitionMapper.selectOne(new LambdaQueryWrapper<ApprovalDefinitionDO>()
                .in(ApprovalDefinitionDO::getTenantId, List.of(normalizeTenant(tenantId), 0L))
                .eq(ApprovalDefinitionDO::getDefinitionKey, key)
                .eq(ApprovalDefinitionDO::getEnabled, 1)
                .orderByDesc(ApprovalDefinitionDO::getTenantId)
                .last("LIMIT 1"));
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
        return fallback;
    }

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

    private boolean canAct(ApprovalTaskDO task, String actor, String actorRole) {
        String approver = task.getApprover();
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
        return "*".equals(approver);
    }

    private ApprovalInstanceView toView(ApprovalInstanceDO instance, List<ApprovalTaskDO> tasks) {
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
                safeList(tasks).stream()
                        .filter(task -> STATUS_PENDING.equalsIgnoreCase(task.getStatus()))
                        .map(this::toTaskView)
                        .toList());
    }

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

    private List<String> normalizeApprovers(List<String> raw) {
        Set<String> approvers = new LinkedHashSet<>();
        if (raw != null) {
            raw.stream()
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .forEach(approvers::add);
        }
        if (approvers.isEmpty()) {
            approvers.add(ROLE_TENANT_ADMIN);
        }
        return List.copyOf(approvers);
    }

    private String normalizeStatus(String status) {
        return requireText(status, "status").toUpperCase(Locale.ROOT);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private int positiveOrDefault(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private String defaultActor(String raw) {
        String value = trimToNull(raw);
        return value == null ? "system" : value;
    }

    private String requireText(String raw, String field) {
        String value = trimToNull(raw);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private String detail(String comment) {
        return trimToNull(comment);
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
