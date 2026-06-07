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

@Component
public class GrowthBenefitPromotionGrantAdapter {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final NodeHandler commitActionHandler;
    private final GrowthRewardGrantService grantService;
    private final ObjectMapper objectMapper;

    public GrowthBenefitPromotionGrantAdapter(@Qualifier("commitActionHandler") NodeHandler commitActionHandler,
                                              GrowthRewardGrantService grantService,
                                              ObjectMapper objectMapper) {
        this.commitActionHandler = commitActionHandler;
        this.grantService = grantService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    GrowthBenefitPromotionGrantAdapter(NodeHandler commitActionHandler,
                                       GrowthRewardGrantService grantService) {
        this(commitActionHandler, grantService, new ObjectMapper());
    }

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

    private Map<String, Object> configFor(Long activityId, GrowthRewardPoolDO pool, GrowthRewardGrantDO grant) {
        String rewardType = normalizeUpper(pool.getRewardType(), "COUPON");
        Map<String, Object> config = new HashMap<>();
        config.put(MapFieldKeys.IDEMPOTENCY_KEY, required(grant.getIdempotencyKey(), "idempotencyKey"));
        config.put(MapFieldKeys.NODE_ID_INTERNAL, "growth-grant-" + requiredId(grant.getId(), "grantId"));
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
            return config;
        }
        throw new IllegalArgumentException("unsupported benefit promotion reward type: " + rewardType);
    }

    private ExecutionContext context(Long tenantId, Long activityId, GrowthRewardGrantDO grant, String userId) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(safeTenantId(tenantId));
        ctx.setCanvasId(activityId);
        ctx.setExecutionId("growth-activity-" + activityId + ":grant-" + grant.getId());
        ctx.setUserId(required(userId, "userId"));
        return ctx;
    }

    private void validateTenantAndActivity(Long tenantId,
                                           Long activityId,
                                           GrowthRewardPoolDO pool,
                                           GrowthRewardGrantDO grant) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
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

    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

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

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }
}
