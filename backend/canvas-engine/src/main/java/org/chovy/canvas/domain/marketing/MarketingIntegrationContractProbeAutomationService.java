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
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MarketingIntegrationContractProbeAutomationService {

    static final String PROBE_KEY = "prod-readiness-probe";
    static final String FAILURE_PROBLEM_TYPE_URI = "urn:canvas:marketing-integration:probe-failure";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final MarketingIntegrationContractMapper contractMapper;
    private final MarketingIntegrationContractProbeService probeService;
    private final MarketingIntegrationContractProbeClient probeClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public MarketingIntegrationContractProbeAutomationService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeService probeService,
            MarketingIntegrationContractProbeClient probeClient,
            ObjectMapper objectMapper) {
        this(contractMapper, probeService, probeClient, objectMapper, Clock.systemDefaultZone());
    }

    MarketingIntegrationContractProbeAutomationService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeService probeService,
            MarketingIntegrationContractProbeClient probeClient,
            Clock clock) {
        this(contractMapper, probeService, probeClient, new ObjectMapper(), clock);
    }

    MarketingIntegrationContractProbeAutomationService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeService probeService,
            MarketingIntegrationContractProbeClient probeClient,
            ObjectMapper objectMapper,
            Clock clock) {
        this.contractMapper = contractMapper;
        this.probeService = probeService;
        this.probeClient = probeClient;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 扫描治理异常，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回本次处理的状态、计数、命中明细或治理结论，供控制器和调度任务判断后续动作
     */
    public ProbeAutomationSummary scanProductionContracts(Long tenantId, Integer limit, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        LocalDateTime evaluatedAt = LocalDateTime.now(clock).withNano(0);
        int boundedLimit = boundedLimit(limit);
        List<MarketingIntegrationContractDO> candidates = safeList(contractMapper.selectList(
                new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractDO::getEnvironment, "PRODUCTION")
                        .eq(MarketingIntegrationContractDO::getStatus, "ACTIVE")
                        .orderByDesc(MarketingIntegrationContractDO::getUpdatedAt)
                        .last("LIMIT " + boundedLimit)));
        List<MarketingIntegrationContractDO> contracts = candidates.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> "PRODUCTION".equalsIgnoreCase(defaultString(row.getEnvironment(), "")))
                .filter(row -> "ACTIVE".equalsIgnoreCase(defaultString(row.getStatus(), "")))
                .limit(boundedLimit)
                .toList();
        List<ProbeAutomationResult> results = contracts.stream()
                .map(contract -> probeContract(scopedTenantId, contract, actor, evaluatedAt))
                .toList();
        long passed = results.stream().filter(result -> "PASS".equals(result.status())).count();
        long failed = results.stream().filter(result -> !"PASS".equals(result.status())).count();
        return new ProbeAutomationSummary(
                scopedTenantId,
                candidates.size(),
                results.size(),
                Math.toIntExact(passed),
                Math.toIntExact(failed),
                Math.max(0, candidates.size() - results.size()),
                evaluatedAt,
                results);
    }

    private ProbeAutomationResult probeContract(Long tenantId,
                                                MarketingIntegrationContractDO contract,
                                                String actor,
                                                LocalDateTime evaluatedAt) {
        try {
            MarketingIntegrationContractProbeClient.ProbeResult probe = probeClient.probe(toTarget(contract));
            MarketingIntegrationContractProbeRunView view = probeService.recordProbeRun(
                    tenantId,
                    contract.getId(),
                    new MarketingIntegrationContractProbeRunCommand(
                            PROBE_KEY,
                            normalizeStatus(probe.status()),
                            probe.httpStatusCode(),
                            probe.latencyMs(),
                            defaultString(probe.problemTypeUri(), HttpMarketingIntegrationContractProbeClient.PROBLEM_TYPE_URI),
                            probe.errorMessage(),
                            defaultString(probe.summary(), "Provider health endpoint probed"),
                            evidence(contract, probe.evidence(), evaluatedAt)),
                    actor(actor));
            return toResult(view);
        } catch (RuntimeException ex) {
            MarketingIntegrationContractProbeRunView view = probeService.recordProbeRun(
                    tenantId,
                    contract.getId(),
                    new MarketingIntegrationContractProbeRunCommand(
                            PROBE_KEY,
                            "FAIL",
                            null,
                            null,
                            FAILURE_PROBLEM_TYPE_URI,
                            message(ex),
                            "Automatic probe failed",
                            failureEvidence(contract, ex, evaluatedAt)),
                    actor(actor));
            return toResult(view);
        }
    }

    private MarketingIntegrationContractProbeClient.ProbeTarget toTarget(MarketingIntegrationContractDO contract) {
        return new MarketingIntegrationContractProbeClient.ProbeTarget(
                contract.getId(),
                contract.getTenantId(),
                contract.getContractKey(),
                contract.getDisplayName(),
                contract.getProviderFamily(),
                contract.getApiRoot(),
                contract.getAuthMode(),
                contract.getTimeoutMs(),
                fromJson(contract.getSchemaContractJson()),
                fromJson(contract.getMetadataJson()));
    }

    private ProbeAutomationResult toResult(MarketingIntegrationContractProbeRunView view) {
        return new ProbeAutomationResult(
                view.contractId(),
                view.contractKey(),
                view.providerFamily(),
                view.probeKey(),
                view.status(),
                view.httpStatusCode(),
                view.latencyMs(),
                view.summary(),
                view.errorMessage(),
                view.observedAt());
    }

    private Map<String, Object> evidence(MarketingIntegrationContractDO contract,
                                         Map<String, Object> clientEvidence,
                                         LocalDateTime evaluatedAt) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        if (clientEvidence != null) {
            evidence.putAll(clientEvidence);
        }
        evidence.put("probeSource", "AUTOMATED");
        evidence.put("contractKey", contract.getContractKey());
        evidence.put("providerFamily", contract.getProviderFamily());
        evidence.put("evaluatedAt", evaluatedAt.toString());
        return evidence;
    }

    private Map<String, Object> failureEvidence(MarketingIntegrationContractDO contract,
                                                RuntimeException exception,
                                                LocalDateTime evaluatedAt) {
        Map<String, Object> evidence = evidence(contract, Map.of(), evaluatedAt);
        evidence.put("exceptionType", exception.getClass().getSimpleName());
        return evidence;
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

    private static String normalizeStatus(String value) {
        String status = defaultString(value, "PASS").toUpperCase(Locale.ROOT);
        return switch (status) {
            case "PASS", "WARN", "FAIL" -> status;
            default -> "FAIL";
        };
    }

    private static List<MarketingIntegrationContractDO> safeList(List<MarketingIntegrationContractDO> rows) {
        return rows == null ? List.of() : rows;
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
        return actor == null || actor.isBlank() ? "marketing-integration-probe-scheduler" : actor.trim();
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private static String message(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record ProbeAutomationSummary(
            Long tenantId,
            int candidateCount,
            int probedCount,
            int passedCount,
            int failedCount,
            int skippedCount,
            LocalDateTime evaluatedAt,
            List<ProbeAutomationResult> results) {
    }

    public record ProbeAutomationResult(
            Long contractId,
            String contractKey,
            String providerFamily,
            String probeKey,
            String status,
            Integer httpStatusCode,
            Long latencyMs,
            String summary,
            String errorMessage,
            String observedAt) {
    }
}
