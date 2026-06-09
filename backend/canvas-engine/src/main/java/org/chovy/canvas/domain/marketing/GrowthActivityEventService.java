package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthActivityEventDO;
import org.chovy.canvas.dal.mapper.GrowthActivityEventMapper;
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
 * GrowthActivityEventService 编排 domain.marketing 场景的领域业务规则。
 */
@Service
public class GrowthActivityEventService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthActivityEventMapper eventMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 GrowthActivityEventService 实例并注入 domain.marketing 场景依赖。
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public GrowthActivityEventService(GrowthActivityMapper activityMapper,
                                      GrowthActivityEventMapper eventMapper,
                                      ObjectMapper objectMapper) {
        this(activityMapper, eventMapper, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 GrowthActivityEventService 流程，围绕 growth activity event service 完成校验、计算或结果组装。
     *
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    GrowthActivityEventService(GrowthActivityMapper activityMapper,
                               GrowthActivityEventMapper eventMapper,
                               ObjectMapper objectMapper,
                               Clock clock) {
        this.activityMapper = activityMapper;
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 记录增长活动事件，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthActivityEventView recordEvent(Long tenantId,
                                               Long activityId,
                                               GrowthActivityEventCommand command,
                                               String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("growth activity event command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);
        String eventKey = required(command.eventKey(), "eventKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        GrowthActivityEventDO existing = eventMapper.selectOne(new LambdaQueryWrapper<GrowthActivityEventDO>()
                .eq(GrowthActivityEventDO::getTenantId, scopedTenantId)
                .eq(GrowthActivityEventDO::getEventKey, eventKey)
                .last("LIMIT 1"));
        if (existing != null) {
            return toView(existing);
        }
        GrowthActivityEventDO row = new GrowthActivityEventDO();
        row.setTenantId(scopedTenantId);
        row.setActivityId(scopedActivityId);
        row.setParticipantId(command.participantId());
        row.setEventType(normalizeUpper(command.eventType(), "CUSTOM"));
        row.setEventKey(eventKey);
        row.setSourceType(normalizeUpper(command.sourceType(), "GROWTH_ACTIVITY"));
        row.setSourceId(command.sourceId());
        row.setPayloadJson(toJson(command.payload()));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setOccurredAt(now());
        eventMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<GrowthActivityEventView> listEvents(Long tenantId, Long activityId, String eventType, Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);
        String normalizedType = optionalUpper(eventType);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return eventMapper.selectList(new LambdaQueryWrapper<GrowthActivityEventDO>()
                        .eq(GrowthActivityEventDO::getTenantId, scopedTenantId)
                        .eq(GrowthActivityEventDO::getActivityId, scopedActivityId)
                        .eq(normalizedType != null, GrowthActivityEventDO::getEventType, normalizedType)
                        .orderByDesc(GrowthActivityEventDO::getOccurredAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> scopedActivityId.equals(row.getActivityId()))
                .filter(row -> normalizedType == null || normalizedType.equals(row.getEventType()))
                .map(this::toView)
                .toList();
    }

    /**
     * 记录增长活动生命周期事件，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public GrowthActivityEventView logLifecycle(Long tenantId, Long activityId, String status, String actor) {
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                null,
                "ACTIVITY_LIFECYCLE",
                /**
                 * 规范化输入值。
                 *
                 * @param status 业务状态，用于筛选或推进状态流转。
                 * @param activityId 业务对象 ID，用于定位具体记录。
                 * @param actor 操作人标识，用于审计和权限判断。
                 * @return 返回解析、归一化或安全处理后的值。
                 */
                "activity:" + activityId + ":lifecycle:" + normalizeUpper(status, "UNKNOWN"),
                "GROWTH_ACTIVITY",
                activityId,
                Map.of("status", normalizeUpper(status, "UNKNOWN"))), actor);
    }

    /**
     * 记录参与人入场事件，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param participantId 目标业务记录 ID，需与租户边界匹配
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public GrowthActivityEventView logParticipantEntry(Long tenantId,
                                                       Long activityId,
                                                       Long participantId,
                                                       String status,
                                                       Map<String, Object> payload) {
        String normalizedStatus = normalizeUpper(status, "JOINED");
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                participantId,
                "PARTICIPANT_ENTRY",
                "activity:" + activityId + ":participant:" + participantId + ":" + normalizedStatus,
                "PARTICIPANT",
                participantId,
                merge(payload, Map.of("status", normalizedStatus))), "system");
    }

    /**
     * 记录邀请关系达标事件，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param relationId 目标业务记录 ID，需与租户边界匹配
     * @param participantId 目标业务记录 ID，需与租户边界匹配
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public GrowthActivityEventView logReferralQualification(Long tenantId,
                                                            Long activityId,
                                                            Long relationId,
                                                            Long participantId,
                                                            Map<String, Object> payload) {
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                participantId,
                "REFERRAL_QUALIFICATION",
                "referral:" + relationId + ":qualified",
                "REFERRAL_RELATION",
                relationId,
                payload), "system");
    }

    /**
     * 记录任务进度事件，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param progressId 目标业务记录 ID，需与租户边界匹配
     * @param participantId 目标业务记录 ID，需与租户边界匹配
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public GrowthActivityEventView logTaskProgress(Long tenantId,
                                                   Long activityId,
                                                   Long progressId,
                                                   Long participantId,
                                                   Map<String, Object> payload) {
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                participantId,
                "TASK_PROGRESS",
                "task-progress:" + progressId,
                "TASK_PROGRESS",
                progressId,
                payload), "system");
    }

    /**
     * 记录奖励发放状态流转事件，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param grantId 目标业务记录 ID，需与租户边界匹配
     * @param participantId 目标业务记录 ID，需与租户边界匹配
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public GrowthActivityEventView logGrantTransition(Long tenantId,
                                                      Long activityId,
                                                      Long grantId,
                                                      Long participantId,
                                                      String status,
                                                      Map<String, Object> payload) {
        String normalizedStatus = normalizeUpper(status, "UNKNOWN");
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                participantId,
                "GRANT_TRANSITION",
                "grant:" + grantId + ":" + normalizedStatus,
                "REWARD_GRANT",
                grantId,
                merge(payload, Map.of("status", normalizedStatus))), "system");
    }

    /**
     * 记录转化证据事件，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param conversionKey 业务键，用于定位租户内的配置、资产或治理对象
     * @param participantId 目标业务记录 ID，需与租户边界匹配
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public GrowthActivityEventView logConversionEvidence(Long tenantId,
                                                         Long activityId,
                                                         String conversionKey,
                                                         Long participantId,
                                                         Map<String, Object> payload) {
        return recordEvent(tenantId, activityId, new GrowthActivityEventCommand(
                participantId,
                "CONVERSION_EVIDENCE",
                "conversion:" + required(conversionKey, "conversionKey"),
                "CONVERSION",
                null,
                payload), "system");
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     */
    private void validateActivity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(activityId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private GrowthActivityEventView toView(GrowthActivityEventDO row) {
        return new GrowthActivityEventView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getParticipantId(),
                row.getEventType(),
                row.getEventKey(),
                row.getSourceType(),
                row.getSourceId(),
                fromJson(row.getPayloadJson()),
                row.getCreatedBy(),
                row.getOccurredAt());
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
            throw new IllegalArgumentException("event payload must be JSON serializable", e);
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
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 merge 流程中的校验、计算或对象转换。
     * @param base base 参数，用于 merge 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 merge 流程中的校验、计算或对象转换。
     * @param additions additions 参数，用于 merge 流程中的校验、计算或对象转换。
     * @return 返回 merge 流程生成的业务结果。
     */
    private static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> additions) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ((base == null || base.isEmpty()) && (additions == null || additions.isEmpty())) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        if (additions != null) {
            merged.putAll(additions);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Map.copyOf(merged);
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
     * @param fallback fallback 参数，用于 normalizeUpper 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 optionalUpper 流程，围绕 optional upper 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 optional upper 生成的文本或业务键。
     */
    private static String optionalUpper(String value) {
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
}
