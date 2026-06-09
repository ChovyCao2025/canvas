package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthTaskDefinitionDO;
import org.chovy.canvas.dal.dataobject.GrowthTaskProgressDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthTaskDefinitionMapper;
import org.chovy.canvas.dal.mapper.GrowthTaskProgressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GrowthTaskService 编排 domain.marketing 场景的领域业务规则。
 */
@Service
public class GrowthTaskService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthTaskDefinitionMapper definitionMapper;
    private final GrowthTaskProgressMapper progressMapper;
    private final GrowthRewardGrantService rewardGrantService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 GrowthTaskService 实例并注入 domain.marketing 场景依赖。
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param progressMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardGrantService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public GrowthTaskService(GrowthActivityMapper activityMapper,
                             GrowthTaskDefinitionMapper definitionMapper,
                             GrowthTaskProgressMapper progressMapper,
                             GrowthRewardGrantService rewardGrantService,
                             ObjectMapper objectMapper) {
        this(activityMapper, definitionMapper, progressMapper, rewardGrantService, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 GrowthTaskService 流程，围绕 growth task service 完成校验、计算或结果组装。
     *
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param progressMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardGrantService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    GrowthTaskService(GrowthActivityMapper activityMapper,
                      GrowthTaskDefinitionMapper definitionMapper,
                      GrowthTaskProgressMapper progressMapper,
                      GrowthRewardGrantService rewardGrantService,
                      ObjectMapper objectMapper,
                      Clock clock) {
        this.activityMapper = activityMapper;
        this.definitionMapper = definitionMapper;
        this.progressMapper = progressMapper;
        this.rewardGrantService = rewardGrantService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<GrowthTaskDefinitionView> listTaskDefinitions(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        return definitionMapper.selectList(new LambdaQueryWrapper<GrowthTaskDefinitionDO>()
                        .eq(GrowthTaskDefinitionDO::getTenantId, scopedTenantId)
                        .eq(GrowthTaskDefinitionDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthTaskDefinitionDO::getUpdatedAt))
                .stream()
                .map(this::toDefinitionView)
                .toList();
    }

    /**
     * 创建或更新业务记录，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthTaskDefinitionView upsertTaskDefinition(Long tenantId,
                                                         Long activityId,
                                                         GrowthTaskDefinitionCommand command,
                                                         String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("growth task definition command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateTaskActivity(scopedTenantId, scopedActivityId);
        String taskKey = normalizeKey(command.taskKey(), "taskKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        GrowthTaskDefinitionDO row = definitionMapper.selectOne(new LambdaQueryWrapper<GrowthTaskDefinitionDO>()
                .eq(GrowthTaskDefinitionDO::getTenantId, scopedTenantId)
                .eq(GrowthTaskDefinitionDO::getActivityId, scopedActivityId)
                .eq(GrowthTaskDefinitionDO::getTaskKey, taskKey)
                .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new GrowthTaskDefinitionDO();
            row.setTenantId(scopedTenantId);
            row.setActivityId(scopedActivityId);
            row.setTaskKey(taskKey);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(now());
        }
        row.setTaskType(normalizeUpper(command.taskType(), "EVENT_COUNT"));
        row.setCompletionPolicy(normalizeCompletionPolicy(command.completionPolicy()));
        row.setResetPolicy(normalizeResetPolicy(command.resetPolicy()));
        row.setRewardPoolId(command.rewardPoolId());
        row.setTargetValue(positive(command.targetValue(), BigDecimal.ONE));
        row.setStatus(normalizeStatus(command.status()));
        row.setRuleJson(toJson(command.rule()));
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(now());
        if (insert) {
            definitionMapper.insert(row);
        } else {
            definitionMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toDefinitionView(row);
    }

    /**
     * 执行业务操作 recordProgress，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthTaskProgressView recordProgress(Long tenantId,
                                                 Long activityId,
                                                 GrowthTaskProgressCommand command,
                                                 String actor) {
        if (command == null) {
            throw new IllegalArgumentException("growth task progress command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        GrowthTaskDefinitionDO task = task(scopedTenantId, scopedActivityId, command.taskId());
        if (!"ACTIVE".equals(task.getStatus())) {
            throw new IllegalArgumentException("growth task definition is not active");
        }
        Long participantId = requiredId(command.participantId(), "participantId");
        GrowthTaskProgressDO row = progressMapper.selectOne(new LambdaQueryWrapper<GrowthTaskProgressDO>()
                .eq(GrowthTaskProgressDO::getTenantId, scopedTenantId)
                .eq(GrowthTaskProgressDO::getActivityId, scopedActivityId)
                .eq(GrowthTaskProgressDO::getParticipantId, participantId)
                .eq(GrowthTaskProgressDO::getTaskId, task.getId())
                .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new GrowthTaskProgressDO();
            row.setTenantId(scopedTenantId);
            row.setActivityId(scopedActivityId);
            row.setParticipantId(participantId);
            row.setTaskId(task.getId());
            row.setProgressValue(BigDecimal.ZERO);
            row.setTargetValue(task.getTargetValue());
            row.setStatus("IN_PROGRESS");
        // 根据前序判断结果进入后续条件分支。
        } else if ("COMPLETED".equals(row.getStatus()) && "ONCE".equals(task.getResetPolicy())) {
            return toProgressView(row);
        }
        row.setProgressValue(defaultAmount(row.getProgressValue()).add(positive(command.deltaValue(), BigDecimal.ONE)));
        row.setTargetValue(task.getTargetValue());
        row.setLastEventKey(trimToLimit(command.eventKey(), 191));
        row.setEvidenceJson(toJson(command.evidence()));
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(now());
        if (row.getProgressValue().compareTo(task.getTargetValue()) >= 0 && !"COMPLETED".equals(row.getStatus())) {
            row.setStatus("COMPLETED");
            row.setCompletedAt(now());
            if (task.getRewardPoolId() != null && row.getRewardGrantId() == null) {
                if (insert) {
                    progressMapper.insert(row);
                    insert = false;
                }
                GrowthRewardGrantView grant = rewardGrantService.createGrant(scopedTenantId, scopedActivityId,
                        new GrowthRewardGrantCommand(
                                task.getRewardPoolId(),
                                participantId,
                                null,
                                row.getId(),
                                "TASK_COMPLETION",
                                "task:" + row.getId() + ":completion",
                                Map.of("taskKey", task.getTaskKey(), "progressValue", row.getProgressValue()),
                                BigDecimal.ZERO),
                        actor);
                row.setRewardGrantId(grant.id());
            }
        }
        if (insert) {
            progressMapper.insert(row);
        } else {
            progressMapper.updateById(row);
        }
        return toProgressView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<GrowthTaskProgressView> listTaskProgress(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        return progressMapper.selectList(new LambdaQueryWrapper<GrowthTaskProgressDO>()
                        .eq(GrowthTaskProgressDO::getTenantId, scopedTenantId)
                        .eq(GrowthTaskProgressDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthTaskProgressDO::getUpdatedAt))
                .stream()
                .map(this::toProgressView)
                .toList();
    }

    /**
     * 执行业务操作 resetProgress，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param progressId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthTaskProgressView resetProgress(Long tenantId, Long progressId, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        GrowthTaskProgressDO row = progressMapper.selectById(requiredId(progressId, "progressId"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth task progress does not belong to tenant");
        }
        GrowthTaskDefinitionDO task = task(scopedTenantId, row.getActivityId(), row.getTaskId());
        if ("ONCE".equals(task.getResetPolicy())) {
            throw new IllegalArgumentException("growth task progress cannot reset once-only task");
        }
        row.setProgressValue(BigDecimal.ZERO);
        row.setStatus("IN_PROGRESS");
        row.setRewardGrantId(null);
        row.setCompletedAt(null);
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(now());
        progressMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toProgressView(row);
    }

    /**
     * 执行 task 流程，围绕 task 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param taskId 业务对象 ID，用于定位具体记录。
     * @return 返回 task 流程生成的业务结果。
     */
    private GrowthTaskDefinitionDO task(Long tenantId, Long activityId, Long taskId) {
        GrowthTaskDefinitionDO row = definitionMapper.selectById(requiredId(taskId, "taskId"));
        if (row == null || !tenantId.equals(row.getTenantId()) || !activityId.equals(row.getActivityId())) {
            throw new IllegalArgumentException("growth task definition does not belong to activity");
        }
        return row;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     */
    private void validateTaskActivity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(activityId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
        if (!"TASK_INCENTIVE".equals(row.getActivityType())) {
            throw new IllegalArgumentException("growth activity is not a task incentive activity");
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private GrowthTaskDefinitionView toDefinitionView(GrowthTaskDefinitionDO row) {
        return new GrowthTaskDefinitionView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getTaskKey(),
                row.getTaskType(),
                row.getCompletionPolicy(),
                row.getResetPolicy(),
                row.getRewardPoolId(),
                row.getTargetValue(),
                row.getStatus(),
                fromJson(row.getRuleJson()),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private GrowthTaskProgressView toProgressView(GrowthTaskProgressDO row) {
        return new GrowthTaskProgressView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getParticipantId(),
                row.getTaskId(),
                row.getProgressValue(),
                row.getTargetValue(),
                row.getStatus(),
                row.getLastEventKey(),
                fromJson(row.getEvidenceJson()),
                row.getRewardGrantId(),
                row.getUpdatedBy(),
                row.getCompletedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param String string 参数，用于 toJson 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("task payload must be JSON serializable", e);
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 safe tenant id 计算得到的数量、金额或指标值。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeKey(String value, String field) {
        String normalized = required(value, field).trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeUpper 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeCompletionPolicy(String value) {
        String policy = normalizeUpper(value, "EVENT");
        return switch (policy) {
            case "EVENT", "MANUAL" -> policy;
            default -> throw new IllegalArgumentException("unsupported task completion policy: " + policy);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeResetPolicy(String value) {
        String policy = normalizeUpper(value, "ONCE");
        return switch (policy) {
            case "ONCE", "DAILY", "WEEKLY", "MANUAL_RESET" -> policy;
            default -> throw new IllegalArgumentException("unsupported task reset policy: " + policy);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "ACTIVE");
        return switch (status) {
            case "ACTIVE", "PAUSED", "CLOSED" -> status;
            default -> throw new IllegalArgumentException("unsupported task status: " + status);
        };
    }

    /**
     * 执行 positive 流程，围绕 positive 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 positive 流程中的校验、计算或对象转换。
     * @return 返回 positive 计算得到的数量、金额或指标值。
     */
    private static BigDecimal positive(BigDecimal value, BigDecimal fallback) {
        BigDecimal actual = value == null ? fallback : value;
        return actual.signum() <= 0 ? fallback : actual;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default amount 计算得到的数量、金额或指标值。
     */
    private static BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String trimToLimit(String value, int limit) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit);
    }
}
