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

@Service
public class GrowthActivityReadinessService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GrowthActivityMapper activityMapper;
    private final GrowthRewardPoolMapper poolMapper;
    private final GrowthRewardGrantMapper grantMapper;
    private final MarketingIntegrationContractMapper contractMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public GrowthActivityReadinessService(GrowthActivityMapper activityMapper,
                                          GrowthRewardPoolMapper poolMapper,
                                          GrowthRewardGrantMapper grantMapper,
                                          MarketingIntegrationContractMapper contractMapper,
                                          ObjectMapper objectMapper) {
        this(activityMapper, poolMapper, grantMapper, contractMapper, objectMapper, Clock.systemDefaultZone());
    }

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

    public GrowthActivityReadinessView evaluate(Long tenantId, Long activityId) {
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

    private GrowthActivityDO activity(Long tenantId, Long activityId) {
        GrowthActivityDO row = activityMapper.selectById(requiredId(activityId, "activityId"));
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("growth activity does not belong to tenant");
        }
        return row;
    }

    private List<GrowthRewardPoolDO> pools(Long tenantId, Long activityId) {
        return poolMapper.selectList(new LambdaQueryWrapper<GrowthRewardPoolDO>()
                        .eq(GrowthRewardPoolDO::getTenantId, tenantId)
                        .eq(GrowthRewardPoolDO::getActivityId, activityId))
                .stream()
                .filter(row -> tenantId.equals(row.getTenantId()) && activityId.equals(row.getActivityId()))
                .toList();
    }

    private List<GrowthRewardGrantDO> grants(Long tenantId, Long activityId) {
        return grantMapper.selectList(new LambdaQueryWrapper<GrowthRewardGrantDO>()
                        .eq(GrowthRewardGrantDO::getTenantId, tenantId)
                        .eq(GrowthRewardGrantDO::getActivityId, activityId))
                .stream()
                .filter(row -> tenantId.equals(row.getTenantId()) && activityId.equals(row.getActivityId()))
                .toList();
    }

    private List<MarketingIntegrationContractDO> contracts(Long tenantId, Set<String> contractKeys) {
        if (contractKeys.isEmpty()) {
            return List.of();
        }
        return contractMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, tenantId)
                        .in(MarketingIntegrationContractDO::getContractKey, contractKeys))
                .stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> contractKeys.contains(row.getContractKey()))
                .toList();
    }

    private static List<GrowthRewardPoolDO> activePools(List<GrowthRewardPoolDO> pools) {
        return pools.stream()
                .filter(row -> "ACTIVE".equals(normalize(row.getStatus())))
                .toList();
    }

    private static Set<String> providerContractKeys(List<GrowthRewardPoolDO> pools) {
        Set<String> keys = new LinkedHashSet<>();
        pools.stream()
                .map(GrowthRewardPoolDO::getExternalContractKey)
                .filter(GrowthActivityReadinessService::hasText)
                .map(String::trim)
                .forEach(keys::add);
        return keys;
    }

    private static boolean contractReady(MarketingIntegrationContractDO contract, String key) {
        return contract != null
                && key.equals(contract.getContractKey())
                && "ACTIVE".equals(normalize(contract.getStatus()))
                && "PRODUCTION".equals(normalize(contract.getEnvironment()));
    }

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

    private static GrowthActivityReadinessCheckView check(String severity,
                                                          String itemType,
                                                          String itemKey,
                                                          String title,
                                                          String reason,
                                                          String route) {
        return new GrowthActivityReadinessCheckView(severity, itemType, itemKey, title, reason, route);
    }

    private Map<String, Object> map(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
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

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long longValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
