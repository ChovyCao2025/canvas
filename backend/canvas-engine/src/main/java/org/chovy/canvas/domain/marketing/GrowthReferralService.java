package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthReferralCodeDO;
import org.chovy.canvas.dal.dataobject.GrowthReferralRelationDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthReferralCodeMapper;
import org.chovy.canvas.dal.mapper.GrowthReferralRelationMapper;
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
 * GrowthReferralService 编排 domain.marketing 场景的领域业务规则。
 */
@Service
public class GrowthReferralService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthReferralCodeMapper codeMapper;
    private final GrowthReferralRelationMapper relationMapper;
    private final GrowthRewardGrantService rewardGrantService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 GrowthReferralService 实例并注入 domain.marketing 场景依赖。
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param codeMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param relationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardGrantService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public GrowthReferralService(GrowthActivityMapper activityMapper,
                                 GrowthReferralCodeMapper codeMapper,
                                 GrowthReferralRelationMapper relationMapper,
                                 GrowthRewardGrantService rewardGrantService,
                                 ObjectMapper objectMapper) {
        this(activityMapper, codeMapper, relationMapper, rewardGrantService, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 GrowthReferralService 流程，围绕 growth referral service 完成校验、计算或结果组装。
     *
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param codeMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param relationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardGrantService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    GrowthReferralService(GrowthActivityMapper activityMapper,
                          GrowthReferralCodeMapper codeMapper,
                          GrowthReferralRelationMapper relationMapper,
                          GrowthRewardGrantService rewardGrantService,
                          ObjectMapper objectMapper,
                          Clock clock) {
        this.activityMapper = activityMapper;
        this.codeMapper = codeMapper;
        this.relationMapper = relationMapper;
        this.rewardGrantService = rewardGrantService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 生成邀请推荐码，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param participantId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthReferralCodeView generateCode(Long tenantId, Long activityId, Long participantId, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        Long scopedParticipantId = requiredId(participantId, "participantId");
        validateReferralActivity(scopedTenantId, scopedActivityId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        GrowthReferralCodeDO existing = codeMapper.selectOne(new LambdaQueryWrapper<GrowthReferralCodeDO>()
                .eq(GrowthReferralCodeDO::getTenantId, scopedTenantId)
                .eq(GrowthReferralCodeDO::getActivityId, scopedActivityId)
                .eq(GrowthReferralCodeDO::getParticipantId, scopedParticipantId)
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (existing != null) {
            return toCodeView(existing);
        }
        GrowthReferralCodeDO row = new GrowthReferralCodeDO();
        row.setTenantId(scopedTenantId);
        row.setActivityId(scopedActivityId);
        row.setParticipantId(scopedParticipantId);
        row.setCode("G" + scopedActivityId + "P" + scopedParticipantId);
        row.setStatus("ACTIVE");
        row.setCreatedBy(defaultString(actor, "system"));
        row.setCreatedAt(LocalDateTime.now(clock).withNano(0));
        codeMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toCodeView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<GrowthReferralCodeView> listCodes(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateReferralActivity(scopedTenantId, scopedActivityId);
        return codeMapper.selectList(new LambdaQueryWrapper<GrowthReferralCodeDO>()
                        .eq(GrowthReferralCodeDO::getTenantId, scopedTenantId)
                        .eq(GrowthReferralCodeDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthReferralCodeDO::getCreatedAt))
                .stream()
                .map(this::toCodeView)
                .toList();
    }

    /**
     * 创建或更新邀请关系，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthReferralRelationView upsertRelation(Long tenantId,
                                                     Long activityId,
                                                     GrowthReferralRelationCommand command,
                                                     String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("growth referral relation command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateReferralActivity(scopedTenantId, scopedActivityId);
        String referralCode = normalizeCode(command.referralCode());
        String inviteeUserId = required(command.inviteeUserId(), "inviteeUserId");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        GrowthReferralCodeDO code = codeMapper.selectOne(new LambdaQueryWrapper<GrowthReferralCodeDO>()
                .eq(GrowthReferralCodeDO::getTenantId, scopedTenantId)
                .eq(GrowthReferralCodeDO::getCode, referralCode)
                .last("LIMIT 1"));
        if (code == null || !scopedActivityId.equals(code.getActivityId())) {
            throw new IllegalArgumentException("referral code does not belong to activity");
        }
        if (!"ACTIVE".equals(code.getStatus())) {
            throw new IllegalArgumentException("referral code is not active");
        }
        GrowthReferralRelationDO row = relationMapper.selectOne(new LambdaQueryWrapper<GrowthReferralRelationDO>()
                .eq(GrowthReferralRelationDO::getTenantId, scopedTenantId)
                .eq(GrowthReferralRelationDO::getActivityId, scopedActivityId)
                .eq(GrowthReferralRelationDO::getInviteeUserId, inviteeUserId)
                .last("LIMIT 1"));
        if (row != null) {
            return toRelationView(row);
        }
        row = new GrowthReferralRelationDO();
        row.setTenantId(scopedTenantId);
        row.setActivityId(scopedActivityId);
        row.setReferralCodeId(code.getId());
        row.setReferrerParticipantId(code.getParticipantId());
        row.setInviteeUserId(inviteeUserId);
        row.setStatus("PENDING");
        row.setRiskEvidenceJson(toJson(command.riskEvidence()));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setCreatedAt(LocalDateTime.now(clock).withNano(0));
        row.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        relationMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toRelationView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<GrowthReferralRelationView> listRelations(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateReferralActivity(scopedTenantId, scopedActivityId);
        return relationMapper.selectList(new LambdaQueryWrapper<GrowthReferralRelationDO>()
                        .eq(GrowthReferralRelationDO::getTenantId, scopedTenantId)
                        .eq(GrowthReferralRelationDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthReferralRelationDO::getUpdatedAt))
                .stream()
                .map(this::toRelationView)
                .toList();
    }

    /**
     * 标记邀请关系达标，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param relationId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthReferralRelationView qualifyRelation(Long tenantId,
                                                      Long relationId,
                                                      GrowthReferralQualificationCommand command,
                                                      String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("growth referral qualification command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        GrowthReferralRelationDO row = relationMapper.selectById(requiredId(relationId, "relationId"));
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("referral relation does not belong to tenant");
        }
        if ("QUALIFIED".equals(row.getStatus())) {
            return toRelationView(row);
        }
        if (!"PENDING".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot qualify referral relation from status " + row.getStatus());
        }
        GrowthRewardGrantView inviterGrant = rewardGrantService.createGrant(scopedTenantId, row.getActivityId(),
                new GrowthRewardGrantCommand(
                        requiredId(command.inviterRewardPoolId(), "inviterRewardPoolId"),
                        row.getReferrerParticipantId(),
                        row.getId(),
                        null,
                        "REFERRAL_INVITER",
                        "referral:" + row.getId() + ":inviter",
                        Map.of("role", "INVITER", "inviteeUserId", row.getInviteeUserId()),
                        BigDecimal.ZERO),
                actor);
        GrowthRewardGrantView inviteeGrant = rewardGrantService.createGrant(scopedTenantId, row.getActivityId(),
                new GrowthRewardGrantCommand(
                        requiredId(command.inviteeRewardPoolId(), "inviteeRewardPoolId"),
                        null,
                        row.getId(),
                        null,
                        "REFERRAL_INVITEE",
                        "referral:" + row.getId() + ":invitee",
                        Map.of("role", "INVITEE", "inviteeUserId", row.getInviteeUserId()),
                        BigDecimal.ZERO),
                actor);
        row.setStatus("QUALIFIED");
        row.setRiskEvidenceJson(toJson(command.riskEvidence()));
        row.setInviterRewardGrantId(inviterGrant.id());
        row.setInviteeRewardGrantId(inviteeGrant.id());
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        relationMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toRelationView(row);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     */
    private void validateReferralActivity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(activityId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
        if (!"REFERRAL_INVITE".equals(row.getActivityType())) {
            throw new IllegalArgumentException("growth activity is not a referral invite activity");
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private GrowthReferralCodeView toCodeView(GrowthReferralCodeDO row) {
        return new GrowthReferralCodeView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getParticipantId(),
                row.getCode(),
                row.getStatus(),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private GrowthReferralRelationView toRelationView(GrowthReferralRelationDO row) {
        return new GrowthReferralRelationView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getReferralCodeId(),
                row.getReferrerParticipantId(),
                row.getInviteeUserId(),
                row.getStatus(),
                fromJson(row.getRiskEvidenceJson()),
                row.getInviterRewardGrantId(),
                row.getInviteeRewardGrantId(),
                row.getCreatedBy(),
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
            throw new IllegalArgumentException("risk evidence must be JSON serializable", e);
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
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeCode(String value) {
        return required(value, "referralCode").toUpperCase(Locale.ROOT);
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
}
