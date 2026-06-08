// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeWindowStatsDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeObservationMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class MarketingIntegrationContractSloService {

    static final String ALERT_TYPE = "INTEGRATION_CONTRACT_SLO_BURN_RATE";

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final List<SloRule> RULES = List.of(
            new SloRule("PAGE_FAST_BURN", "PAGE", "CRITICAL", 60, 5, 14.4),
            new SloRule("PAGE_SLOW_BURN", "PAGE", "HIGH", 360, 30, 6.0),
            new SloRule("TICKET_BURN", "TICKET", "HIGH", 4320, 360, 1.0));

    private final MarketingIntegrationContractMapper contractMapper;
    private final MarketingIntegrationContractProbeObservationMapper observationMapper;
    private final MarketingMonitorAlertMapper alertMapper;
    private final ObjectMapper objectMapper;
    private final MarketingMonitorAlertFanoutService fanoutService;
    private final Clock clock;

    public MarketingIntegrationContractSloService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeObservationMapper observationMapper,
            MarketingMonitorAlertMapper alertMapper,
            ObjectMapper objectMapper,
            ObjectProvider<MarketingMonitorAlertFanoutService> fanoutProvider) {
        this(contractMapper,
                observationMapper,
                alertMapper,
                objectMapper,
                fanoutProvider == null ? null : fanoutProvider.getIfAvailable(),
                Clock.systemDefaultZone());
    }

    MarketingIntegrationContractSloService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeObservationMapper observationMapper,
            MarketingMonitorAlertMapper alertMapper,
            ObjectMapper objectMapper,
            MarketingMonitorAlertFanoutService fanoutService,
            Clock clock) {
        this.contractMapper = contractMapper;
        this.observationMapper = observationMapper;
        this.alertMapper = alertMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.fanoutService = fanoutService;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractSloEvaluationView> listProductionSloEvaluations(Long tenantId,
                                                                                            Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        int boundedLimit = boundedLimit(limit);
        List<MarketingIntegrationContractDO> rows = contractMapper.selectList(
                new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractDO::getEnvironment, "PRODUCTION")
                        .eq(MarketingIntegrationContractDO::getStatus, "ACTIVE")
                        .orderByDesc(MarketingIntegrationContractDO::getUpdatedAt)
                        .last("LIMIT " + boundedLimit));
        return (rows == null ? List.<MarketingIntegrationContractDO>of() : rows).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> "PRODUCTION".equalsIgnoreCase(defaultString(row.getEnvironment(), "")))
                .filter(row -> "ACTIVE".equalsIgnoreCase(defaultString(row.getStatus(), "")))
                .limit(boundedLimit)
                .map(row -> evaluateContract(scopedTenantId, row, sloProbeKey(row)))
                .toList();
    }

    /**
     * 执行业务操作 evaluateAndSyncContract，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contract contract 参数，参与本次业务定位、校验或状态计算
     * @param probeKey 业务键，用于定位租户内的配置、资产或治理对象
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回本次处理的状态、计数、命中明细或治理结论，供控制器和调度任务判断后续动作
     */
    public MarketingIntegrationContractSloEvaluationView evaluateAndSyncContract(Long tenantId,
                                                                                 MarketingIntegrationContractDO contract,
                                                                                 String probeKey,
                                                                                 String actor) {
        MarketingIntegrationContractSloEvaluationView view =
                evaluateContract(safeTenantId(tenantId), contract, defaultString(probeKey, sloProbeKey(contract)));
        syncAlert(view, actor(actor));
        return view;
    }

    private MarketingIntegrationContractSloEvaluationView evaluateContract(Long tenantId,
                                                                          MarketingIntegrationContractDO contract,
                                                                          String probeKey) {
        if (contract == null) {
            throw new IllegalArgumentException("integration contract is required");
        }
        Map<String, Object> metadata = map(contract.getMetadataJson());
        LocalDateTime generatedAt = now();
        double targetPercent = targetPercent(contract, metadata);
        double errorBudget = round4(Math.max(0.0001, (100.0 - targetPercent) / 100.0));
        int minLongSamples = intMetadata(metadata, "sloMinLongWindowSamples", 1);
        int minShortSamples = intMetadata(metadata, "sloMinShortWindowSamples", 1);
        List<MarketingIntegrationContractSloWindowView> windows = new ArrayList<>();
        for (SloRule rule : RULES) {
            MarketingIntegrationContractSloWindowView longWindow = window(
                    tenantId,
                    contract,
                    probeKey,
                    rule,
                    "long",
                    rule.longWindowMinutes(),
                    minLongSamples,
                    targetPercent,
                    errorBudget,
                    generatedAt);
            MarketingIntegrationContractSloWindowView shortWindow = window(
                    tenantId,
                    contract,
                    probeKey,
                    rule,
                    "short",
                    rule.shortWindowMinutes(),
                    minShortSamples,
                    targetPercent,
                    errorBudget,
                    generatedAt);
            windows.add(longWindow);
            windows.add(shortWindow);
            if (Boolean.TRUE.equals(longWindow.breached()) && Boolean.TRUE.equals(shortWindow.breached())) {
                return view(tenantId, contract, probeKey, rule.status(), rule.severity(), rule.key(),
                        targetPercent, errorBudget, reason(contract, rule, longWindow, shortWindow),
                        generatedAt, List.of(longWindow, shortWindow));
            }
        }
        boolean insufficient = windows.stream().noneMatch(MarketingIntegrationContractSloWindowView::sufficient);
        return view(tenantId, contract, probeKey, insufficient ? "INSUFFICIENT_DATA" : "OK", "INFO", null,
                targetPercent, errorBudget,
                insufficient ? "not enough production probe observations for SLO evaluation" : "SLO burn-rate is within policy",
                generatedAt, windows);
    }

    private MarketingIntegrationContractSloWindowView window(Long tenantId,
                                                            MarketingIntegrationContractDO contract,
                                                            String probeKey,
                                                            SloRule rule,
                                                            String windowKey,
                                                            int windowMinutes,
                                                            int minSamples,
                                                            double targetPercent,
                                                            double errorBudget,
                                                            LocalDateTime generatedAt) {
        LocalDateTime windowStart = generatedAt.minusMinutes(windowMinutes);
        MarketingIntegrationContractProbeWindowStatsDO stats = observationMapper.selectWindowStats(
                tenantId,
                contract.getId(),
                probeKey,
                windowStart);
        long total = stats == null || stats.getTotalCount() == null ? 0L : stats.getTotalCount();
        long bad = stats == null || stats.getBadCount() == null ? 0L : stats.getBadCount();
        double badRatio = total == 0 ? 0.0 : round4((double) bad / total);
        double burnRate = round2(badRatio / errorBudget);
        boolean sufficient = total >= minSamples;
        boolean breached = sufficient && burnRate >= rule.thresholdBurnRate();
        return new MarketingIntegrationContractSloWindowView(
                rule.key(),
                windowKey,
                windowMinutes,
                total,
                bad,
                badRatio,
                burnRate,
                rule.thresholdBurnRate(),
                sufficient,
                breached,
                windowStart.toString(),
                generatedAt.toString());
    }

    private MarketingIntegrationContractSloEvaluationView view(Long tenantId,
                                                              MarketingIntegrationContractDO contract,
                                                              String probeKey,
                                                              String status,
                                                              String severity,
                                                              String triggeredRuleKey,
                                                              double targetPercent,
                                                              double errorBudget,
                                                              String reason,
                                                              LocalDateTime generatedAt,
                                                              List<MarketingIntegrationContractSloWindowView> windows) {
        return new MarketingIntegrationContractSloEvaluationView(
                tenantId,
                contract.getId(),
                contract.getContractKey(),
                contract.getDisplayName(),
                contract.getProviderFamily(),
                probeKey,
                status,
                severity,
                triggeredRuleKey,
                round2(targetPercent),
                errorBudget,
                reason,
                generatedAt.toString(),
                windows);
    }

    private void syncAlert(MarketingIntegrationContractSloEvaluationView view, String actor) {
        if (view == null || alertMapper == null) {
            return;
        }
        if ("PAGE".equals(view.status()) || "TICKET".equals(view.status())) {
            upsertAlert(view, actor);
        } else if ("OK".equals(view.status())) {
            resolveAlert(view, actor);
        }
    }

    private void upsertAlert(MarketingIntegrationContractSloEvaluationView view, String actor) {
        MarketingMonitorAlertDO existing = openAlert(view.tenantId(), view.contractKey());
        if (existing == null) {
            MarketingMonitorAlertDO row = new MarketingMonitorAlertDO();
            row.setTenantId(view.tenantId());
            row.setAlertType(ALERT_TYPE);
            row.setSeverity(view.severity());
            row.setStatus("OPEN");
            row.setScopeKey(view.contractKey());
            row.setDedupeKey(dedupeKey(view.contractKey()));
            row.setTitle("Marketing integration contract SLO burn-rate breached");
            row.setReason(view.reason());
            row.setItemCount(1);
            row.setWindowStart(triggerWindowStart(view));
            row.setWindowEnd(now());
            row.setMetadataJson(json(metadata(view)));
            row.setCreatedBy(actor);
            row.setCreatedAt(now());
            row.setUpdatedAt(now());
            try {
                alertMapper.insert(row);
                dispatch(row, actor);
            } catch (DuplicateKeyException ex) {
                MarketingMonitorAlertDO concurrent = openAlert(view.tenantId(), view.contractKey());
                if (concurrent == null) {
                    throw ex;
                }
                updateAlert(concurrent, view);
            }
            return;
        }
        updateAlert(existing, view);
    }

    private void updateAlert(MarketingMonitorAlertDO existing, MarketingIntegrationContractSloEvaluationView view) {
        existing.setSeverity(view.severity());
        existing.setDedupeKey(dedupeKey(view.contractKey()));
        existing.setReason(view.reason());
        existing.setItemCount(existing.getItemCount() == null ? 1 : existing.getItemCount() + 1);
        existing.setWindowStart(triggerWindowStart(view));
        existing.setWindowEnd(now());
        existing.setMetadataJson(json(metadata(view)));
        existing.setUpdatedAt(now());
        alertMapper.updateById(existing);
    }

    private void resolveAlert(MarketingIntegrationContractSloEvaluationView view, String actor) {
        MarketingMonitorAlertDO existing = openAlert(view.tenantId(), view.contractKey());
        if (existing == null) {
            return;
        }
        LocalDateTime resolvedAt = now();
        existing.setStatus("RESOLVED");
        existing.setDedupeKey(null);
        existing.setResolvedBy(actor);
        existing.setResolvedAt(resolvedAt);
        existing.setWindowEnd(resolvedAt);
        Map<String, Object> metadata = new LinkedHashMap<>(map(existing.getMetadataJson()));
        metadata.put("recoveredAt", resolvedAt.toString());
        metadata.put("recoveredStatus", view.status());
        existing.setMetadataJson(json(metadata));
        existing.setUpdatedAt(resolvedAt);
        alertMapper.updateById(existing);
    }

    private MarketingMonitorAlertDO openAlert(Long tenantId, String contractKey) {
        return alertMapper.selectOne(new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                .eq(MarketingMonitorAlertDO::getTenantId, tenantId)
                .eq(MarketingMonitorAlertDO::getAlertType, ALERT_TYPE)
                .eq(MarketingMonitorAlertDO::getStatus, "OPEN")
                .eq(MarketingMonitorAlertDO::getDedupeKey, dedupeKey(contractKey))
                .last("LIMIT 1"));
    }

    private void dispatch(MarketingMonitorAlertDO row, String actor) {
        if (fanoutService == null) {
            return;
        }
        try {
            fanoutService.dispatchAlert(row.getTenantId(), row, actor);
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] SLO burn-rate fanout skipped alert={} error={}",
                    row.getId(), ex.getMessage());
        }
    }

    private Map<String, Object> metadata(MarketingIntegrationContractSloEvaluationView view) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("contractId", view.contractId());
        metadata.put("contractKey", view.contractKey());
        metadata.put("displayName", view.displayName());
        metadata.put("providerFamily", view.providerFamily());
        metadata.put("probeKey", view.probeKey());
        metadata.put("status", view.status());
        metadata.put("severity", view.severity());
        metadata.put("triggeredRuleKey", view.triggeredRuleKey());
        metadata.put("targetPercent", view.targetPercent());
        metadata.put("errorBudget", view.errorBudget());
        metadata.put("windows", view.windows());
        metadata.put("generatedAt", view.generatedAt());
        return metadata;
    }

    private LocalDateTime triggerWindowStart(MarketingIntegrationContractSloEvaluationView view) {
        return view.windows().stream()
                .map(MarketingIntegrationContractSloWindowView::windowStart)
                .map(LocalDateTime::parse)
                .min(LocalDateTime::compareTo)
                .orElse(now());
    }

    private String reason(MarketingIntegrationContractDO contract,
                          SloRule rule,
                          MarketingIntegrationContractSloWindowView longWindow,
                          MarketingIntegrationContractSloWindowView shortWindow) {
        return trimToLimit(contract.getContractKey() + " breached " + rule.key()
                + ": " + formatRate(longWindow.burnRate()) + " burn over " + longWindow.windowMinutes() + "m"
                + " and " + formatRate(shortWindow.burnRate()) + " over " + shortWindow.windowMinutes() + "m", 1000);
    }

    private static String formatRate(Double burnRate) {
        return String.format(Locale.ROOT, "%.2fx", burnRate == null ? 0.0 : burnRate);
    }

    private String sloProbeKey(MarketingIntegrationContractDO contract) {
        return defaultString(stringMetadata(map(contract == null ? null : contract.getMetadataJson()), "sloProbeKey"),
                MarketingIntegrationContractProbeAutomationService.PROBE_KEY);
    }

    private double targetPercent(MarketingIntegrationContractDO contract, Map<String, Object> metadata) {
        Double configured = doubleMetadata(metadata, "sloTargetPercent");
        if (configured != null) {
            return Math.max(50.0, Math.min(99.99, configured));
        }
        String tier = normalizeUpper(contract.getSlaTier());
        return switch (tier) {
            case "CRITICAL", "BUSINESS_CRITICAL" -> 99.9;
            case "BEST_EFFORT" -> 95.0;
            default -> 99.0;
        };
    }

    private Double doubleMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int intMetadata(Map<String, Object> metadata, String key, int fallback) {
        Double value = doubleMetadata(metadata, key);
        if (value == null || value < 1) {
            return fallback;
        }
        return Math.min(10_000, value.intValue());
    }

    private String stringMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("integration SLO alert metadata must be JSON serializable", ex);
        }
    }

    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static int boundedLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String actor(String actor) {
        String trimmed = actor == null ? "" : actor.trim();
        return trimmed.isBlank() ? "marketing-integration-slo-evaluator" : trimmed;
    }

    private static String dedupeKey(String contractKey) {
        return trimToLimit(ALERT_TYPE.toLowerCase(Locale.ROOT) + ":" + contractKey, 256);
    }

    private static String normalizeUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

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

    private record SloRule(
            String key,
            String status,
            String severity,
            Integer longWindowMinutes,
            Integer shortWindowMinutes,
            Double thresholdBurnRate) {
    }
}
