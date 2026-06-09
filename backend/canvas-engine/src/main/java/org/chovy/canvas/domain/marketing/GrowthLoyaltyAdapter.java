package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.domain.loyalty.LoyaltyService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GrowthLoyaltyAdapter 编排 domain.marketing 场景的领域业务规则。
 */
@Component
public class GrowthLoyaltyAdapter {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final LoyaltyService loyaltyService;
    private final ObjectMapper objectMapper;

    /**
     * 创建 GrowthLoyaltyAdapter 实例并注入 domain.marketing 场景依赖。
     * @param loyaltyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public GrowthLoyaltyAdapter(LoyaltyService loyaltyService, ObjectMapper objectMapper) {
        this.loyaltyService = loyaltyService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 执行 GrowthLoyaltyAdapter 流程，围绕 growth loyalty adapter 完成校验、计算或结果组装。
     *
     * @param loyaltyService 依赖组件，用于完成数据访问或外部能力调用。
     */
    GrowthLoyaltyAdapter(LoyaltyService loyaltyService) {
        this(loyaltyService, new ObjectMapper());
    }

    /**
     * earnPoints 处理 domain.marketing 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param pool pool 参数，用于 earnPoints 流程中的校验、计算或对象转换。
     * @param grant grant 参数，用于 earnPoints 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 earnPoints 流程生成的业务结果。
     */
    public GrowthLoyaltyResult earnPoints(Long tenantId,
                                          Long activityId,
                                          GrowthRewardPoolDO pool,
                                          GrowthRewardGrantDO grant,
                                          String userId) {
        validatePoolAndGrant(tenantId, activityId, pool, grant, "POINTS");
        int points = positiveInt(fromJson(pool.getMetadataJson()).get("points"), "loyalty earn points must be positive");
        LoyaltyService.LoyaltyAccountView account = loyaltyService.earn(
                safeTenantId(tenantId),
                required(userId, "userId"),
                new LoyaltyService.EarnCommand(
                        // Grant ID makes loyalty writes idempotent across retrying growth reward jobs.
                        "growth:grant:" + requiredId(grant.getId(), "grantId") + ":earn",
                        points,
                        defaultString(pool.getPointsType(), "BASE"),
                        "GROWTH_ACTIVITY",
                        String.valueOf(requiredId(activityId, "activityId")),
                        "growth activity reward grant " + grant.getId(),
                        null));
        return new GrowthLoyaltyResult("EARNED", accountPayload(account));
    }

    /**
     * redeemBenefit 处理 domain.marketing 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pool pool 参数，用于 redeemBenefit 流程中的校验、计算或对象转换。
     * @param grant grant 参数，用于 redeemBenefit 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 redeemBenefit 流程生成的业务结果。
     */
    public GrowthLoyaltyResult redeemBenefit(Long tenantId,
                                             GrowthRewardPoolDO pool,
                                             GrowthRewardGrantDO grant,
                                             String userId) {
        validatePoolAndGrant(tenantId, pool == null ? null : pool.getActivityId(), pool, grant, "LOYALTY");
        int pointsCost = positiveInt(fromJson(pool.getMetadataJson()).get("pointsCost"),
                "loyalty redemption points cost must be positive");
        LoyaltyService.RedemptionView redemption = loyaltyService.redeem(
                safeTenantId(tenantId),
                required(userId, "userId"),
                new LoyaltyService.RedemptionCommand(
                        // Redemption keys share the grant namespace so earn/redeem retries remain independently deduplicated.
                        "growth:grant:" + requiredId(grant.getId(), "grantId") + ":redeem",
                        required(pool.getLoyaltyRewardKey(), "loyaltyRewardKey"),
                        pointsCost,
                        "growth activity redemption " + grant.getId()));
        return new GrowthLoyaltyResult(redemption.status(), redemptionPayload(redemption));
    }

    /**
     * account 处理 domain.marketing 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 account 流程生成的业务结果。
     */
    public GrowthLoyaltyResult account(Long tenantId, String userId) {
        return new GrowthLoyaltyResult("ACCOUNT", accountPayload(
                loyaltyService.account(safeTenantId(tenantId), required(userId, "userId"))));
    }

    /**
     * eligibleBenefits 处理 domain.marketing 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 eligible benefits 汇总后的集合、分页或映射视图。
     */
    public List<LoyaltyService.BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId) {
        return loyaltyService.eligibleBenefits(safeTenantId(tenantId), required(userId, "userId"));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param pool pool 参数，用于 validatePoolAndGrant 流程中的校验、计算或对象转换。
     * @param grant grant 参数，用于 validatePoolAndGrant 流程中的校验、计算或对象转换。
     * @param expectedRewardType 类型标识，用于选择对应处理分支。
     */
    private void validatePoolAndGrant(Long tenantId,
                                      Long activityId,
                                      GrowthRewardPoolDO pool,
                                      GrowthRewardGrantDO grant,
                                      String expectedRewardType) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        // Pool and grant must both belong to the same tenant/activity to avoid cross-campaign reward leakage.
        if (pool == null || !scopedTenantId.equals(pool.getTenantId()) || !scopedActivityId.equals(pool.getActivityId())) {
            throw new IllegalArgumentException("growth loyalty adapter reward pool does not belong to activity");
        }
        if (!expectedRewardType.equals(normalizeUpper(pool.getRewardType(), expectedRewardType))) {
            throw new IllegalArgumentException("growth loyalty adapter requires " + expectedRewardType + " reward type");
        }
        if (grant == null || !scopedTenantId.equals(grant.getTenantId()) || !scopedActivityId.equals(grant.getActivityId())) {
            throw new IllegalArgumentException("growth loyalty adapter reward grant does not belong to activity");
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
            // Malformed optional metadata behaves like missing metadata; required fields are validated after parsing.
            return Map.of();
        }
    }

    /**
     * 执行 accountPayload 流程，围绕 account payload 完成校验、计算或结果组装。
     *
     * @param account account 参数，用于 accountPayload 流程中的校验、计算或对象转换。
     * @return 返回 accountPayload 流程生成的业务结果。
     */
    private static Map<String, Object> accountPayload(LoyaltyService.LoyaltyAccountView account) {
        return Map.of(
                "accountId", account.accountId(),
                "tenantId", account.tenantId(),
                "userId", account.userId(),
                "memberNo", account.memberNo(),
                "tierCode", account.tierCode(),
                "pointsBalance", account.pointsBalance(),
                "lifetimePoints", account.lifetimePoints(),
                "status", account.status());
    }

    /**
     * 执行 redemptionPayload 流程，围绕 redemption payload 完成校验、计算或结果组装。
     *
     * @param redemption redemption 参数，用于 redemptionPayload 流程中的校验、计算或对象转换。
     * @return 返回 redemptionPayload 流程生成的业务结果。
     */
    private static Map<String, Object> redemptionPayload(LoyaltyService.RedemptionView redemption) {
        return Map.of(
                "redemptionId", redemption.redemptionId(),
                "redemptionKey", redemption.redemptionKey(),
                "rewardKey", redemption.rewardKey(),
                "pointsCost", redemption.pointsCost(),
                "status", redemption.status());
    }

    /**
     * 执行 positiveInt 流程，围绕 positive int 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 positive int 计算得到的数量、金额或指标值。
     */
    private static int positiveInt(Object value, String message) {
        int result;
        if (value instanceof Number number) {
            result = number.intValue();
        // 根据前序判断结果进入后续条件分支。
        } else if (value instanceof String text && !text.isBlank()) {
            result = Integer.parseInt(text);
        } else {
            result = 0;
        }
        if (result <= 0) {
            throw new IllegalArgumentException(message);
        }
        return result;
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
