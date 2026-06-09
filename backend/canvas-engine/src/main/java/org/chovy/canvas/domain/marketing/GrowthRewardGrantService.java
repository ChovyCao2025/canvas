package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardGrantMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardPoolMapper;
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
 * GrowthRewardGrantService 编排 domain.marketing 场景的领域业务规则。
 */
@Service
public class GrowthRewardGrantService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthRewardPoolMapper poolMapper;
    private final GrowthRewardGrantMapper grantMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 GrowthRewardGrantService 实例并注入 domain.marketing 场景依赖。
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param poolMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param grantMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public GrowthRewardGrantService(GrowthActivityMapper activityMapper,
                                    GrowthRewardPoolMapper poolMapper,
                                    GrowthRewardGrantMapper grantMapper,
                                    ObjectMapper objectMapper) {
        this(activityMapper, poolMapper, grantMapper, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 GrowthRewardGrantService 流程，围绕 growth reward grant service 完成校验、计算或结果组装。
     *
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param poolMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param grantMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    GrowthRewardGrantService(GrowthActivityMapper activityMapper,
                             GrowthRewardPoolMapper poolMapper,
                             GrowthRewardGrantMapper grantMapper,
                             ObjectMapper objectMapper,
                             Clock clock) {
        this.activityMapper = activityMapper;
        this.poolMapper = poolMapper;
        this.grantMapper = grantMapper;
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
    public List<GrowthRewardGrantView> listGrants(Long tenantId, Long activityId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        return grantMapper.selectList(new LambdaQueryWrapper<GrowthRewardGrantDO>()
                        .eq(GrowthRewardGrantDO::getTenantId, scopedTenantId)
                        .eq(GrowthRewardGrantDO::getActivityId, scopedActivityId)
                        .orderByDesc(GrowthRewardGrantDO::getUpdatedAt))
                .stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 创建奖励发放并预留奖池库存，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView createGrant(Long tenantId, Long activityId, GrowthRewardGrantCommand command, String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("growth reward grant command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        validateActivity(scopedTenantId, scopedActivityId);
        GrowthRewardPoolDO pool = validatePool(scopedTenantId, scopedActivityId, command.poolId());
        if (!"ACTIVE".equals(pool.getStatus())) {
            throw new IllegalArgumentException("reward pool is not active");
        }
        String idempotencyKey = required(command.idempotencyKey(), "idempotencyKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        GrowthRewardGrantDO existing = grantMapper.selectOne(new LambdaQueryWrapper<GrowthRewardGrantDO>()
                .eq(GrowthRewardGrantDO::getTenantId, scopedTenantId)
                .eq(GrowthRewardGrantDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
        if (existing != null) {
            return toView(existing);
        }
        BigDecimal costAmount = defaultAmount(command.costAmount());
        ensureInventoryAvailable(pool);
        reservePool(pool, costAmount, actor);
        GrowthRewardGrantDO row = new GrowthRewardGrantDO();
        row.setTenantId(scopedTenantId);
        row.setActivityId(scopedActivityId);
        row.setPoolId(pool.getId());
        row.setParticipantId(command.participantId());
        row.setReferralRelationId(command.referralRelationId());
        row.setTaskProgressId(command.taskProgressId());
        row.setGrantReason(normalizeUpper(command.grantReason(), "MANUAL"));
        row.setStatus("RESERVED");
        row.setIdempotencyKey(idempotencyKey);
        row.setProviderRequestJson(toJson(command.providerRequest()));
        row.setProviderResponseJson("{}");
        row.setCostAmount(costAmount);
        row.setCreatedBy(defaultString(actor, "system"));
        row.setUpdatedBy(defaultString(actor, "system"));
        grantMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 把奖励发放标记为成功，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param grantId 目标业务记录 ID，需与租户边界匹配
     * @param providerResponse 外部供应商或下游系统返回的响应内容，会写入对账或审计字段
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView markSuccess(Long tenantId, Long grantId, Map<String, Object> providerResponse, String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if (!"RESERVED".equals(row.getStatus()) && !"FAILED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot mark reward grant success from status " + row.getStatus());
        }
        return transition(row, "SUCCESS", providerResponse, actor, true);
    }

    /**
     * 把奖励发放标记为失败，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param grantId 目标业务记录 ID，需与租户边界匹配
     * @param providerResponse 外部供应商或下游系统返回的响应内容，会写入对账或审计字段
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView markFailure(Long tenantId, Long grantId, Map<String, Object> providerResponse, String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if (!"RESERVED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot mark reward grant failure from status " + row.getStatus());
        }
        return transition(row, "FAILED", providerResponse, actor, true);
    }

    /**
     * 重试失败或取消的奖励发放，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param grantId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView retryGrant(Long tenantId, Long grantId, String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!"FAILED".equals(row.getStatus()) && !"CANCELED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot retry reward grant from status " + row.getStatus());
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        GrowthRewardPoolDO pool = poolMapper.selectById(row.getPoolId());
        if (pool != null && row.getTenantId().equals(pool.getTenantId())) {
            ensureInventoryAvailable(pool);
            reservePool(pool, defaultAmount(row.getCostAmount()), actor);
        }
        row.setStatus("RESERVED");
        row.setProviderResponseJson(toJson(Map.of("retry", true)));
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        grantMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 取消奖励发放并释放相关库存，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param grantId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView cancelGrant(Long tenantId, Long grantId, String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if ("SUCCESS".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot cancel successful reward grant");
        }
        return transition(row, "CANCELED", fromJson(row.getProviderResponseJson()), actor, true);
    }

    /**
     * 把奖励标记为已核销，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param grantId 目标业务记录 ID，需与租户边界匹配
     * @param providerResponse 外部供应商或下游系统返回的响应内容，会写入对账或审计字段
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView markRedeemed(Long tenantId,
                                              Long grantId,
                                              Map<String, Object> providerResponse,
                                              String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if (!"SUCCESS".equals(row.getStatus()) && !"REDEEMED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot redeem reward grant from status " + row.getStatus());
        }
        return transition(row, "REDEEMED", providerResponse, actor, false);
    }

    /**
     * 把奖励标记为已过期，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param grantId 目标业务记录 ID，需与租户边界匹配
     * @param providerResponse 外部供应商或下游系统返回的响应内容，会写入对账或审计字段
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView markExpired(Long tenantId,
                                             Long grantId,
                                             Map<String, Object> providerResponse,
                                             String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        if (!"SUCCESS".equals(row.getStatus()) && !"EXPIRED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot expire reward grant from status " + row.getStatus());
        }
        return transition(row, "EXPIRED", providerResponse, actor, false);
    }

    /**
     * 按供应商状态对账奖励发放，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param grantId 目标业务记录 ID，需与租户边界匹配
     * @param providerStatus 状态值，用于筛选记录或驱动目标状态流转
     * @param providerResponse 外部供应商或下游系统返回的响应内容，会写入对账或审计字段
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public GrowthRewardGrantView reconcileGrant(Long tenantId,
                                                Long grantId,
                                                String providerStatus,
                                                Map<String, Object> providerResponse,
                                                String actor) {
        GrowthRewardGrantDO row = grant(safeTenantId(tenantId), grantId);
        String status = normalizeProviderStatus(providerStatus);
        if ("REDEEMED".equals(row.getStatus()) || "EXPIRED".equals(row.getStatus())) {
            throw new IllegalArgumentException("cannot reconcile terminal reward grant from status " + row.getStatus());
        }
        return transition(row, status, providerResponse, actor, true);
    }

    /**
     * 执行 transition 流程，围绕 transition 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param providerResponse provider response 参数，用于 transition 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param updateCounters 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 transition 流程生成的业务结果。
     */
    private GrowthRewardGrantView transition(GrowthRewardGrantDO row,
                                             String status,
                                             Map<String, Object> providerResponse,
                                             String actor,
                                             boolean updateCounters) {
        // 准备本次处理所需的上下文和中间变量。
        String previousStatus = row.getStatus();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (updateCounters) {
            updatePoolCounters(row, previousStatus, status, actor);
        }
        row.setStatus(status);
        row.setProviderResponseJson(toJson(providerResponse));
        row.setUpdatedBy(defaultString(actor, "system"));
        row.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        grantMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 执行 grant 流程，围绕 grant 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param grantId 业务对象 ID，用于定位具体记录。
     * @return 返回 grant 流程生成的业务结果。
     */
    private GrowthRewardGrantDO grant(Long tenantId, Long grantId) {
        GrowthRewardGrantDO row = grantMapper.selectById(requiredId(grantId, "grantId"));
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("reward grant does not belong to tenant");
        }
        return row;
    }

    /**
     * 执行 reservePool 流程，围绕 reserve pool 完成校验、计算或结果组装。
     *
     * @param pool pool 参数，用于 reservePool 流程中的校验、计算或对象转换。
     * @param costAmount cost amount 参数，用于 reservePool 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void reservePool(GrowthRewardPoolDO pool, BigDecimal costAmount, String actor) {
        // 准备本次处理所需的上下文和中间变量。
        pool.setReservedInventory(defaultLong(pool.getReservedInventory()) + 1L);
        pool.setReservedAmount(defaultAmount(pool.getReservedAmount()).add(costAmount));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        pool.setUpdatedBy(defaultString(actor, "system"));
        pool.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        poolMapper.updateById(pool);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param grant grant 参数，用于 updatePoolCounters 流程中的校验、计算或对象转换。
     * @param previousStatus 业务状态，用于筛选或推进状态流转。
     * @param nextStatus 业务状态，用于筛选或推进状态流转。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void updatePoolCounters(GrowthRewardGrantDO grant,
                                    String previousStatus,
                                    String nextStatus,
                                    String actor) {
        GrowthRewardPoolDO pool = poolMapper.selectById(grant.getPoolId());
        if (pool == null || !grant.getTenantId().equals(pool.getTenantId())) {
            return;
        }
        BigDecimal costAmount = defaultAmount(grant.getCostAmount());
        if ("RESERVED".equals(previousStatus) && "SUCCESS".equals(nextStatus)) {
            pool.setReservedInventory(decrement(defaultLong(pool.getReservedInventory())));
            pool.setGrantedInventory(defaultLong(pool.getGrantedInventory()) + 1L);
            pool.setReservedAmount(decrement(defaultAmount(pool.getReservedAmount()), costAmount));
            pool.setGrantedAmount(defaultAmount(pool.getGrantedAmount()).add(costAmount));
        // 根据前序判断结果进入后续条件分支。
        } else if ("RESERVED".equals(previousStatus)
                && ("FAILED".equals(nextStatus) || "CANCELED".equals(nextStatus))) {
            pool.setReservedInventory(decrement(defaultLong(pool.getReservedInventory())));
            pool.setReservedAmount(decrement(defaultAmount(pool.getReservedAmount()), costAmount));
        // 根据前序判断结果进入后续条件分支。
        } else if ("FAILED".equals(previousStatus) && "SUCCESS".equals(nextStatus)) {
            pool.setGrantedInventory(defaultLong(pool.getGrantedInventory()) + 1L);
            pool.setGrantedAmount(defaultAmount(pool.getGrantedAmount()).add(costAmount));
        } else {
            return;
        }
        pool.setUpdatedBy(defaultString(actor, "system"));
        pool.setUpdatedAt(LocalDateTime.now(clock).withNano(0));
        poolMapper.updateById(pool);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param pool pool 参数，用于 ensureInventoryAvailable 流程中的校验、计算或对象转换。
     */
    private void ensureInventoryAvailable(GrowthRewardPoolDO pool) {
        if (!"LIMITED".equals(pool.getInventoryMode())) {
            return;
        }
        long total = defaultLong(pool.getTotalInventory());
        long used = defaultLong(pool.getReservedInventory()) + defaultLong(pool.getGrantedInventory());
        if (total <= 0 || used >= total) {
            throw new IllegalArgumentException("reward pool inventory exhausted");
        }
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
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param poolId 业务对象 ID，用于定位具体记录。
     * @return 返回布尔判断结果。
     */
    private GrowthRewardPoolDO validatePool(Long tenantId, Long activityId, Long poolId) {
        GrowthRewardPoolDO row = poolMapper.selectById(requiredId(poolId, "poolId"));
        if (row == null || !tenantId.equals(row.getTenantId()) || !activityId.equals(row.getActivityId())) {
            throw new IllegalArgumentException("reward pool does not belong to activity");
        }
        return row;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private GrowthRewardGrantView toView(GrowthRewardGrantDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new GrowthRewardGrantView(
                row.getId(),
                row.getTenantId(),
                row.getActivityId(),
                row.getPoolId(),
                row.getParticipantId(),
                row.getReferralRelationId(),
                row.getTaskProgressId(),
                row.getGrantReason(),
                row.getStatus(),
                row.getIdempotencyKey(),
                fromJson(row.getProviderRequestJson()),
                fromJson(row.getProviderResponseJson()),
                defaultAmount(row.getCostAmount()),
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
            throw new IllegalArgumentException("provider payload must be JSON serializable", e);
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
    private static String normalizeProviderStatus(String value) {
        String status = normalizeUpper(value, "FAILED");
        return switch (status) {
            case "RESERVED", "PENDING" -> "RESERVED";
            case "SUCCESS", "SUCCEEDED", "GRANTED" -> "SUCCESS";
            case "FAILED", "FAILURE" -> "FAILED";
            case "CANCELED", "CANCELLED" -> "CANCELED";
            case "REDEEMED" -> "REDEEMED";
            case "EXPIRED" -> "EXPIRED";
            default -> throw new IllegalArgumentException("unsupported provider reward grant status: " + status);
        };
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
     * @return 返回 default long 计算得到的数量、金额或指标值。
     */
    private static Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 执行 decrement 流程，围绕 decrement 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 decrement 计算得到的数量、金额或指标值。
     */
    private static Long decrement(Long value) {
        return Math.max(0L, defaultLong(value) - 1L);
    }

    /**
     * 执行 decrement 流程，围绕 decrement 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param amount amount 参数，用于 decrement 流程中的校验、计算或对象转换。
     * @return 返回 decrement 计算得到的数量、金额或指标值。
     */
    private static BigDecimal decrement(BigDecimal value, BigDecimal amount) {
        BigDecimal next = defaultAmount(value).subtract(defaultAmount(amount));
        return next.signum() < 0 ? BigDecimal.ZERO : next;
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
