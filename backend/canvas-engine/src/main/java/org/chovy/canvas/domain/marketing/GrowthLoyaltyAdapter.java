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

@Component
public class GrowthLoyaltyAdapter {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final LoyaltyService loyaltyService;
    private final ObjectMapper objectMapper;

    public GrowthLoyaltyAdapter(LoyaltyService loyaltyService, ObjectMapper objectMapper) {
        this.loyaltyService = loyaltyService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    GrowthLoyaltyAdapter(LoyaltyService loyaltyService) {
        this(loyaltyService, new ObjectMapper());
    }

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
                        "growth:grant:" + requiredId(grant.getId(), "grantId") + ":earn",
                        points,
                        defaultString(pool.getPointsType(), "BASE"),
                        "GROWTH_ACTIVITY",
                        String.valueOf(requiredId(activityId, "activityId")),
                        "growth activity reward grant " + grant.getId(),
                        null));
        return new GrowthLoyaltyResult("EARNED", accountPayload(account));
    }

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
                        "growth:grant:" + requiredId(grant.getId(), "grantId") + ":redeem",
                        required(pool.getLoyaltyRewardKey(), "loyaltyRewardKey"),
                        pointsCost,
                        "growth activity redemption " + grant.getId()));
        return new GrowthLoyaltyResult(redemption.status(), redemptionPayload(redemption));
    }

    public GrowthLoyaltyResult account(Long tenantId, String userId) {
        return new GrowthLoyaltyResult("ACCOUNT", accountPayload(
                loyaltyService.account(safeTenantId(tenantId), required(userId, "userId"))));
    }

    public List<LoyaltyService.BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId) {
        return loyaltyService.eligibleBenefits(safeTenantId(tenantId), required(userId, "userId"));
    }

    private void validatePoolAndGrant(Long tenantId,
                                      Long activityId,
                                      GrowthRewardPoolDO pool,
                                      GrowthRewardGrantDO grant,
                                      String expectedRewardType) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long scopedActivityId = requiredId(activityId, "activityId");
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

    private static Map<String, Object> redemptionPayload(LoyaltyService.RedemptionView redemption) {
        return Map.of(
                "redemptionId", redemption.redemptionId(),
                "redemptionKey", redemption.redemptionKey(),
                "rewardKey", redemption.rewardKey(),
                "pointsCost", redemption.pointsCost(),
                "status", redemption.status());
    }

    private static int positiveInt(Object value, String message) {
        int result;
        if (value instanceof Number number) {
            result = number.intValue();
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
