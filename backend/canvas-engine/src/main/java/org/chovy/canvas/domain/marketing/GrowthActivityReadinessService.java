package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardGrantMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardPoolMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * GrowthActivityReadinessService 编排 domain.marketing 场景的领域业务规则。
 */
@Service
public class GrowthActivityReadinessService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthRewardPoolMapper poolMapper;
    private final GrowthRewardGrantMapper grantMapper;
    private final MarketingIntegrationContractMapper contractMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 GrowthActivityReadinessService 实例并注入 domain.marketing 场景依赖。
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param poolMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param grantMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public GrowthActivityReadinessService(GrowthActivityMapper activityMapper,
                                          GrowthRewardPoolMapper poolMapper,
                                          GrowthRewardGrantMapper grantMapper,
                                          MarketingIntegrationContractMapper contractMapper,
                                          ObjectMapper objectMapper) {
        this(activityMapper, poolMapper, grantMapper, contractMapper, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 GrowthActivityReadinessService 流程，围绕 growth activity readiness service 完成校验、计算或结果组装。
     *
     * @param activityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param poolMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param grantMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    GrowthActivityReadinessService(GrowthActivityMapper activityMapper,
                                   GrowthRewardPoolMapper poolMapper,
                                   GrowthRewardGrantMapper grantMapper,
                                   MarketingIntegrationContractMapper contractMapper,
                                   ObjectMapper objectMapper,
                                   Clock clock) {
        this.activityMapper = activityMapper;
        this.poolMapper = poolMapper;
        this.grantMapper = grantMapper;
        this.contractMapper = contractMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 评估当前业务对象的治理状态，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param activityId 目标业务记录 ID，需与租户边界匹配
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public GrowthActivityReadinessView evaluate(Long tenantId, Long activityId) {
        // 准备本次流程的上下文、默认值和中间结果。
        Long scopedTenantId = safeTenantId(tenantId);
        GrowthActivityDO activity = activity(scopedTenantId, activityId);
        Map<String, Object> metadata = map(activity.getMetadataJson());
        Map<String, Object> audienceRefs = map(activity.getAudienceRefsJson());
        List<GrowthRewardPoolDO> pools = pools(scopedTenantId, activity.getId());
        List<GrowthRewardGrantDO> grants = grants(scopedTenantId, activity.getId());
        List<MarketingIntegrationContractDO> contracts = contracts(scopedTenantId, providerContractKeys(pools));

        List<GrowthActivityReadinessCheckView> blockers = new ArrayList<>();
        List<GrowthActivityReadinessCheckView> warnings = new ArrayList<>();
        List<GrowthActivityReadinessCheckView> checks = new ArrayList<>();

        addCheck(checks, blockers, activity.getCampaignId() != null,
                "CAMPAIGN_MASTER", "campaign-master", "Campaign master is linked",
                "activity campaignId is required before launch", null);

        addCheck(checks, blockers, hasText(stringValue(metadata.get("journeyRef")))
                        || hasText(stringValue(metadata.get("journeyId")))
                        || hasText(stringValue(metadata.get("canvasId"))),
                "JOURNEY_LINK", "journey-link", "Journey link is configured",
                "metadata.journeyRef, journeyId, or canvasId is required before launch", null);

        addCheck(checks, blockers, !activePools(pools).isEmpty(),
                "REWARD_POOL", "reward-pool", "Active reward pool exists",
                "at least one ACTIVE reward pool is required before launch", null);

        // 遍历候选记录并转换为前端或服务层需要的视图。
        BigDecimal totalBudget = activePools(pools).stream()
                .map(GrowthRewardPoolDO::getBudgetAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        addCheck(checks, blockers, totalBudget.compareTo(BigDecimal.ZERO) > 0,
                "REWARD_BUDGET", "reward-budget", "Reward budget is configured",
                "active reward pools must have positive budgetAmount before launch", null);

        Set<String> providerKeys = providerContractKeys(activePools(pools));
        boolean providerContractsReady = providerKeys.isEmpty() || providerKeys.stream()
                .allMatch(key -> contracts.stream().anyMatch(contract -> contractReady(contract, key)));
        addCheck(checks, blockers, providerContractsReady,
                "PROVIDER_CONTRACT", "provider-contract", "Provider contracts are production-ready",
                "all reward pool externalContractKey values must point to ACTIVE PRODUCTION integration contracts", null);

        boolean contentRequired = Set.of("CONTENT_PRIVATE_DOMAIN_ACTIVITY", "RETENTION_WINBACK")
                .contains(normalize(activity.getActivityType()));
        addCheck(checks, blockers, !contentRequired || hasText(stringValue(metadata.get("contentReleaseRef"))),
                "CONTENT_RELEASE", "content-release", "Content release dependency is configured",
                "content/private-domain and retention activities require metadata.contentReleaseRef", null);

        boolean riskRequired = Set.of("REFERRAL_INVITE", "TASK_INCENTIVE")
                .contains(normalize(activity.getActivityType()));
        addCheck(checks, blockers, !riskRequired || hasText(activity.getRiskPolicyRef()),
                "RISK_POLICY", "risk-policy", "Risk policy dependency is configured",
                "referral and task incentive activities require riskPolicyRef", null);

        addCheck(checks, blockers, !audienceRefs.isEmpty(),
                "AUDIENCE_AVAILABILITY", "audience-availability", "Audience dependency is configured",
                "audienceRefs must include at least one target audience reference", null);

        addCheck(checks, blockers, hasText(activity.getDashboardRef()) || hasText(stringValue(metadata.get("analyticsRef"))),
                "ANALYTICS_LINK", "analytics-link", "Analytics link is configured",
                "dashboardRef or metadata.analyticsRef is required before launch", null);

        long failedGrants = grants.stream()
                .filter(row -> "FAILED".equals(normalize(row.getStatus())))
                .count();
        long threshold = longValue(metadata.get("failedGrantThreshold"), 10L);
        boolean underThreshold = threshold <= 0 || failedGrants < threshold;
        GrowthActivityReadinessCheckView failedGrantCheck = check(
                underThreshold ? "PASS" : "WARNING",
                "FAILED_GRANT_THRESHOLD",
                "failed-grant-threshold",
                "Failed reward grants are below threshold",
                "failedGrants=" + failedGrants + ", threshold=" + threshold,
                null);
        checks.add(failedGrantCheck);
        // 校验策略输入和默认值，避免无效配置进入持久化或查询流程。
        if (!underThreshold) {
            warnings.add(failedGrantCheck);
        }

        String status = blockers.isEmpty() ? (warnings.isEmpty() ? "READY" : "DEGRADED") : "BLOCKED";
        return new GrowthActivityReadinessView(
                activity.getTenantId(),
                activity.getId(),
                activity.getActivityKey(),
                activity.getActivityType(),
                LocalDateTime.now(clock).withNano(0).toString(),
                status,
                blockers.isEmpty(),
                blockers.size(),
                warnings.size(),
                List.copyOf(blockers),
                List.copyOf(warnings),
                List.copyOf(checks));
    }

    /**
     * 执行 activity 流程，围绕 activity 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回 activity 流程生成的业务结果。
     */
    private GrowthActivityDO activity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(requiredId(activityId, "activityId"));
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
        return row;
    }

    /**
     * 执行 pools 流程，围绕 pools 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回 pools 汇总后的集合、分页或映射视图。
     */
    private List<GrowthRewardPoolDO> pools(Long tenantId, Long activityId) {
        return poolMapper.selectList(new LambdaQueryWrapper<GrowthRewardPoolDO>()
                        .eq(GrowthRewardPoolDO::getTenantId, tenantId)
                        .eq(GrowthRewardPoolDO::getActivityId, activityId))
                .stream()
                .filter(row -> tenantId.equals(row.getTenantId()) && activityId.equals(row.getActivityId()))
                .toList();
    }

    /**
     * 执行 grants 流程，围绕 grants 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回 grants 汇总后的集合、分页或映射视图。
     */
    private List<GrowthRewardGrantDO> grants(Long tenantId, Long activityId) {
        return grantMapper.selectList(new LambdaQueryWrapper<GrowthRewardGrantDO>()
                        .eq(GrowthRewardGrantDO::getTenantId, tenantId)
                        .eq(GrowthRewardGrantDO::getActivityId, activityId))
                .stream()
                .filter(row -> tenantId.equals(row.getTenantId()) && activityId.equals(row.getActivityId()))
                .toList();
    }

    /**
     * 执行 contracts 流程，围绕 contracts 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contractKeys contract keys 参数，用于 contracts 流程中的校验、计算或对象转换。
     * @return 返回 contracts 汇总后的集合、分页或映射视图。
     */
    private List<MarketingIntegrationContractDO> contracts(Long tenantId, Set<String> contractKeys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (contractKeys.isEmpty()) {
            return List.of();
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return contractMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, tenantId)
                        .in(MarketingIntegrationContractDO::getContractKey, contractKeys))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> contractKeys.contains(row.getContractKey()))
                .toList();
    }

    /**
     * 执行 activePools 流程，围绕 active pools 完成校验、计算或结果组装。
     *
     * @param pools pools 参数，用于 activePools 流程中的校验、计算或对象转换。
     * @return 返回 active pools 汇总后的集合、分页或映射视图。
     */
    private static List<GrowthRewardPoolDO> activePools(List<GrowthRewardPoolDO> pools) {
        return pools.stream()
                .filter(row -> "ACTIVE".equals(normalize(row.getStatus())))
                .toList();
    }

    /**
     * 执行 providerContractKeys 流程，围绕 provider contract keys 完成校验、计算或结果组装。
     *
     * @param pools pools 参数，用于 providerContractKeys 流程中的校验、计算或对象转换。
     * @return 返回 provider contract keys 汇总后的集合、分页或映射视图。
     */
    private static Set<String> providerContractKeys(List<GrowthRewardPoolDO> pools) {
        // 准备本次处理所需的上下文和中间变量。
        Set<String> keys = new LinkedHashSet<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        pools.stream()
                .map(GrowthRewardPoolDO::getExternalContractKey)
                .filter(GrowthActivityReadinessService::hasText)
                .map(String::trim)
                .forEach(keys::add);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return keys;
    }

    /**
     * 执行 contractReady 流程，围绕 contract ready 完成校验、计算或结果组装。
     *
     * @param contract contract 参数，用于 contractReady 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 contract ready 的布尔判断结果。
     */
    private static boolean contractReady(MarketingIntegrationContractDO contract, String key) {
        return contract != null
                && key.equals(contract.getContractKey())
                && "ACTIVE".equals(normalize(contract.getStatus()))
                && "PRODUCTION".equals(normalize(contract.getEnvironment()));
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param checks checks 参数，用于 addCheck 流程中的校验、计算或对象转换。
     * @param blockers blockers 参数，用于 addCheck 流程中的校验、计算或对象转换。
     * @param passed passed 参数，用于 addCheck 流程中的校验、计算或对象转换。
     * @param itemType 类型标识，用于选择对应处理分支。
     * @param itemKey 业务键，用于在同一租户下定位资源。
     * @param title title 参数，用于 addCheck 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param route route 参数，用于 addCheck 流程中的校验、计算或对象转换。
     */
    private void addCheck(List<GrowthActivityReadinessCheckView> checks,
                          List<GrowthActivityReadinessCheckView> blockers,
                          boolean passed,
                          String itemType,
                          String itemKey,
                          String title,
                          String reason,
                          String route) {
        GrowthActivityReadinessCheckView row = check(passed ? "PASS" : "BLOCKER", itemType, itemKey, title, reason, route);
        checks.add(row);
        if (!passed) {
            blockers.add(row);
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param severity severity 参数，用于 check 流程中的校验、计算或对象转换。
     * @param itemType 类型标识，用于选择对应处理分支。
     * @param itemKey 业务键，用于在同一租户下定位资源。
     * @param title title 参数，用于 check 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param route route 参数，用于 check 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private static GrowthActivityReadinessCheckView check(String severity,
                                                          String itemType,
                                                          String itemKey,
                                                          String title,
                                                          String reason,
                                                          String route) {
        return new GrowthActivityReadinessCheckView(severity, itemType, itemKey, title, reason, route);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String value) {
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
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 执行 stringValue 流程，围绕 string value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string value 生成的文本或业务键。
     */
    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 执行 longValue 流程，围绕 long value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 longValue 流程中的校验、计算或对象转换。
     * @return 返回 long value 计算得到的数量、金额或指标值。
     */
    private static long longValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
