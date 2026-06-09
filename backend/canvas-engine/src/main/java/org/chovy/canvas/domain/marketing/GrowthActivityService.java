package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GrowthActivityService 编排 domain.marketing 场景的领域业务规则。
 */
@Service
public class GrowthActivityService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper mapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 GrowthActivityService 实例并注入 domain.marketing 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public GrowthActivityService(GrowthActivityMapper mapper, ObjectMapper objectMapper) {
        this(mapper, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 GrowthActivityService 流程，围绕 growth activity service 完成校验、计算或结果组装。
     *
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    GrowthActivityService(GrowthActivityMapper mapper, ObjectMapper objectMapper, Clock clock) {
        this.mapper = mapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建或更新增长活动，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthActivityView upsertActivity(Long tenantId, GrowthActivityCommand command, String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("growth activity command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        String activityKey = normalizeKey(command.activityKey(), "activityKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        GrowthActivityDO row = mapper.selectOne(new LambdaQueryWrapper<GrowthActivityDO>()
                .eq(GrowthActivityDO::getTenantId, scopedTenantId)
                .eq(GrowthActivityDO::getActivityKey, activityKey)
                .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new GrowthActivityDO();
            row.setTenantId(scopedTenantId);
            row.setActivityKey(activityKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setActivityName(defaultString(command.activityName(), activityKey));
        row.setActivityType(normalizeActivityType(command.activityType()));
        row.setStatus(normalizeStatus(command.status()));
        row.setCampaignId(command.campaignId());
        row.setObjective(normalizeUpper(command.objective(), "UNSPECIFIED"));
        row.setOwnerTeam(trimToLimit(command.ownerTeam(), 128));
        row.setStartAt(command.startAt());
        row.setEndAt(command.endAt());
        if (row.getStartAt() != null && row.getEndAt() != null && row.getEndAt().isBefore(row.getStartAt())) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        row.setChannelScope(normalizeOptionalUpper(command.channelScope()));
        row.setAudienceRefsJson(toJson(command.audienceRefs()));
        row.setRiskPolicyRef(trimToLimit(command.riskPolicyRef(), 128));
        row.setExperimentRef(trimToLimit(command.experimentRef(), 128));
        row.setDashboardRef(trimToLimit(command.dashboardRef(), 128));
        row.setMetadataJson(toJson(command.metadata()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityType 类型标识，用于选择对应处理分支。
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<GrowthActivityView> listActivities(Long tenantId, String activityType, String status, Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedType = normalizeOptionalActivityType(activityType);
        String normalizedStatus = normalizeOptionalStatus(status);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return mapper.selectList(new LambdaQueryWrapper<GrowthActivityDO>()
                        .eq(GrowthActivityDO::getTenantId, scopedTenantId)
                        .eq(normalizedType != null, GrowthActivityDO::getActivityType, normalizedType)
                        .eq(normalizedStatus != null, GrowthActivityDO::getStatus, normalizedStatus)
                        .orderByDesc(GrowthActivityDO::getUpdatedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> normalizedType == null || normalizedType.equals(row.getActivityType()))
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .map(this::toView)
                .toList();
    }

    /**
     * 读取单个业务对象详情，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public GrowthActivityView getActivity(Long tenantId, Long activityId) {
        return toView(activity(safeTenantId(tenantId), activityId));
    }

    /**
     * 发布增长活动，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthActivityView publishActivity(Long tenantId, Long activityId, String actor) {
        GrowthActivityDO row = activity(safeTenantId(tenantId), activityId);
        if (!"DRAFT".equals(row.getStatus()) && !"PAUSED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot publish activity from status " + row.getStatus());
        }
        return transition(row, "ACTIVE", actor);
    }

    /**
     * 暂停增长活动，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthActivityView pauseActivity(Long tenantId, Long activityId, String actor) {
        GrowthActivityDO row = activity(safeTenantId(tenantId), activityId);
        if (!"ACTIVE".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot pause activity from status " + row.getStatus());
        }
        return transition(row, "PAUSED", actor);
    }

    /**
     * 关闭增长活动，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthActivityView closeActivity(Long tenantId, Long activityId, String actor) {
        GrowthActivityDO row = activity(safeTenantId(tenantId), activityId);
        if ("CLOSED".equals(row.getStatus())) {
            return toView(row);
        }
        if ("ARCHIVED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot close activity from status " + row.getStatus());
        }
        return transition(row, "CLOSED", actor);
    }

    /**
     * 执行 transition 流程，围绕 transition 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 transition 流程生成的业务结果。
     */
    private GrowthActivityView transition(GrowthActivityDO row, String status, String actor) {
        // 准备本次处理所需的上下文和中间变量。
        row.setStatus(status);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        mapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 执行 activity 流程，围绕 activity 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回 activity 流程生成的业务结果。
     */
    private GrowthActivityDO activity(Long tenantId, Long activityId) {
        GrowthActivityDO row = mapper.selectById(requiredId(activityId, "activityId"));
        validateTenant(tenantId, row == null ? null : row.getTenantId(), "growth activity");
        return row;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private GrowthActivityView toView(GrowthActivityDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new GrowthActivityView(
                row.getId(),
                row.getTenantId(),
                row.getActivityKey(),
                row.getActivityName(),
                row.getActivityType(),
                row.getStatus(),
                row.getCampaignId(),
                row.getObjective(),
                row.getOwnerTeam(),
                row.getStartAt(),
                row.getEndAt(),
                row.getChannelScope(),
                fromJson(row.getAudienceRefsJson()),
                row.getRiskPolicyRef(),
                row.getExperimentRef(),
                row.getDashboardRef(),
                fromJson(row.getMetadataJson()),
                row.getCreatedBy(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedBy(),
                row.getCreatedAt(),
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
            throw new IllegalArgumentException("metadata must be JSON serializable", e);
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
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeActivityType(String value) {
        String type = normalizeUpper(value, "BENEFIT_PROMOTION");
        return switch (type) {
            case "BENEFIT_PROMOTION", "REFERRAL_INVITE", "TASK_INCENTIVE",
                    "LOYALTY_MEMBER_ACTIVITY", "RETENTION_WINBACK",
                    "CONTENT_PRIVATE_DOMAIN_ACTIVITY" -> type;
            default -> throw new IllegalArgumentException("unsupported activity type: " + type);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptionalActivityType(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeActivityType(trimmed);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "DRAFT");
        return switch (status) {
            case "DRAFT", "ACTIVE", "PAUSED", "CLOSED", "ARCHIVED" -> status;
            default -> throw new IllegalArgumentException("unsupported activity status: " + status);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptionalStatus(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeStatus(trimmed);
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
    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
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

    /**
     * 规范化输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param expected 待处理业务值，用于规则计算、转换或外部调用。
     * @param actual actual 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     * @param entity entity 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     */
    private static void validateTenant(Long expected, Long actual, String entity) {
        if (actual == null || !actual.equals(expected)) {
            throw new IllegalArgumentException(entity + " does not belong to tenant");
        }
    }
}
