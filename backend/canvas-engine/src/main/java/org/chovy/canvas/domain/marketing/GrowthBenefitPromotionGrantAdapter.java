package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * GrowthBenefitPromotionGrantAdapter 编排 domain.marketing 场景的领域业务规则。
 */
@Component
public class GrowthBenefitPromotionGrantAdapter {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final NodeHandler commitActionHandler;
    private final GrowthRewardGrantService grantService;
    private final ObjectMapper objectMapper;

    /**
     * 创建 GrowthBenefitPromotionGrantAdapter 实例并注入 domain.marketing 场景依赖。
     * @param commitActionHandler commit action handler 参数，用于 GrowthBenefitPromotionGrantAdapter 流程中的校验、计算或对象转换。
     * @param grantService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public GrowthBenefitPromotionGrantAdapter(@Qualifier("commitActionHandler") NodeHandler commitActionHandler,
                                              GrowthRewardGrantService grantService,
                                              ObjectMapper objectMapper) {
        this.commitActionHandler = commitActionHandler;
        this.grantService = grantService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 执行 GrowthBenefitPromotionGrantAdapter 流程，围绕 growth benefit promotion grant adapter 完成校验、计算或结果组装。
     *
     * @param commitActionHandler commit action handler 参数，用于 GrowthBenefitPromotionGrantAdapter 流程中的校验、计算或对象转换。
     * @param grantService 依赖组件，用于完成数据访问或外部能力调用。
     */
    GrowthBenefitPromotionGrantAdapter(NodeHandler commitActionHandler,
                                       GrowthRewardGrantService grantService) {
        this(commitActionHandler, grantService, new ObjectMapper());
    }

    /**
     * grantBenefit 处理 domain.marketing 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param pool pool 参数，用于 grantBenefit 流程中的校验、计算或对象转换。
     * @param grant grant 参数，用于 grantBenefit 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 grantBenefit 流程生成的业务结果。
     */
    public GrowthBenefitGrantResult grantBenefit(Long tenantId,
                                                 Long activityId,
                                                 GrowthRewardPoolDO pool,
                                                 GrowthRewardGrantDO grant,
                                                 String userId,
                                                 String actor) {
        validateTenantAndActivity(tenantId, activityId, pool, grant);
        Map<String, Object> config = configFor(activityId, pool, grant);
        ExecutionContext ctx = context(tenantId, activityId, grant, userId);
        NodeResult result = commitActionHandler.executeAsync(config, ctx).block();
        Map<String, Object> providerResponse = result == null ? Map.of() : result.output();
        if (result != null && result.success()) {
            GrowthRewardGrantView view = grantService.markSuccess(tenantId, grant.getId(), providerResponse, actor);
            return new GrowthBenefitGrantResult(view.id(), view.status(), view.providerResponse());
        }
        Map<String, Object> failureResponse = providerResponse == null || providerResponse.isEmpty()
                ? Map.of("message", result == null ? "provider returned no result" : result.errorMessage())
                : providerResponse;
        GrowthRewardGrantView view = grantService.markFailure(tenantId, grant.getId(), failureResponse, actor);
        return new GrowthBenefitGrantResult(view.id(), view.status(), view.providerResponse());
    }

    /**
     * 执行 configFor 流程，围绕 config for 完成校验、计算或结果组装。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param pool pool 参数，用于 configFor 流程中的校验、计算或对象转换。
     * @param grant grant 参数，用于 configFor 流程中的校验、计算或对象转换。
     * @return 返回 configFor 流程生成的业务结果。
     */
    private Map<String, Object> configFor(Long activityId, GrowthRewardPoolDO pool, GrowthRewardGrantDO grant) {
        // 准备本次处理所需的上下文和中间变量。
        String rewardType = normalizeUpper(pool.getRewardType(), "COUPON");
        Map<String, Object> config = new HashMap<>();
        config.put(MapFieldKeys.IDEMPOTENCY_KEY, required(grant.getIdempotencyKey(), "idempotencyKey"));
        config.put(MapFieldKeys.NODE_ID_INTERNAL, "growth-grant-" + requiredId(grant.getId(), "grantId"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("COUPON".equals(rewardType)) {
            config.put(MapFieldKeys.ACTION_TYPE, "ISSUE_COUPON");
            config.put(MapFieldKeys.COUPON_TYPE_KEY, required(pool.getCouponTypeKey(), "couponTypeKey"));
            config.put(MapFieldKeys.PARAMS, Map.of(
                    "activityId", activityId,
                    "rewardPoolId", pool.getId(),
                    "rewardGrantId", grant.getId()));
            return config;
        }
        if ("POINTS".equals(rewardType)) {
            Map<String, Object> metadata = fromJson(pool.getMetadataJson());
            config.put(MapFieldKeys.ACTION_TYPE, "POINTS");
            config.put("operation", "GRANT");
            config.put("points", points(metadata));
            config.put("pointsType", defaultString(pool.getPointsType(), "MARKETING"));
            config.put("reason", "growth activity " + activityId + " reward grant " + grant.getId());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return config;
        }
        throw new IllegalArgumentException("unsupported benefit promotion reward type: " + rewardType);
    }

    /**
     * 执行 context 流程，围绕 context 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param grant grant 参数，用于 context 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 context 流程生成的业务结果。
     */
    private ExecutionContext context(Long tenantId, Long activityId, GrowthRewardGrantDO grant, String userId) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(safeTenantId(tenantId));
        ctx.setCanvasId(activityId);
        ctx.setExecutionId("growth-activity-" + activityId + ":grant-" + grant.getId());
        ctx.setUserId(required(userId, "userId"));
        return ctx;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param pool pool 参数，用于 validateTenantAndActivity 流程中的校验、计算或对象转换。
     * @param grant grant 参数，用于 validateTenantAndActivity 流程中的校验、计算或对象转换。
     */
    private void validateTenantAndActivity(Long tenantId,
                                           Long activityId,
                                           GrowthRewardPoolDO pool,
                                           GrowthRewardGrantDO grant) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (pool == null || !scopedTenantId.equals(pool.getTenantId()) || !scopedActivityId.equals(pool.getActivityId())) {
            throw new IllegalArgumentException("reward pool does not belong to activity");
        }
        if (grant == null || !scopedTenantId.equals(grant.getTenantId()) || !scopedActivityId.equals(grant.getActivityId())) {
            throw new IllegalArgumentException("reward grant does not belong to activity");
        }
        if (!pool.getId().equals(grant.getPoolId())) {
            throw new IllegalArgumentException("reward grant does not belong to pool");
        }
        if (!"COMMIT_ACTION".equals(normalizeUpper(pool.getGrantChannel(), "COMMIT_ACTION"))) {
            throw new IllegalArgumentException("benefit promotion grant requires COMMIT_ACTION channel");
        }
        if (!"RESERVED".equals(normalizeUpper(grant.getStatus(), "RESERVED"))) {
            throw new IllegalArgumentException("benefit promotion grant requires RESERVED status");
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
     * 执行 points 流程，围绕 points 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 points 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 points 流程中的校验、计算或对象转换。
     * @return 返回 points 计算得到的数量、金额或指标值。
     */
    private static int points(Map<String, Object> metadata) {
        Object value = metadata == null ? null : metadata.get("points");
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value instanceof String string && !string.isBlank()) {
            return Math.max(0, Integer.parseInt(string));
        }
        return 0;
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
