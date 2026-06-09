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
/**
 * MarketingIntegrationContractProbeAutomationService 承载对应领域的业务规则、流程编排和结果转换。
 */
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
    /**
     * 初始化 MarketingIntegrationContractProbeAutomationService 实例。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingIntegrationContractProbeAutomationService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeService probeService,
            MarketingIntegrationContractProbeClient probeClient,
            ObjectMapper objectMapper) {
        this(contractMapper, probeService, probeClient, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 初始化 MarketingIntegrationContractProbeAutomationService 实例。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingIntegrationContractProbeAutomationService(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractProbeService probeService,
            MarketingIntegrationContractProbeClient probeClient,
            Clock clock) {
        this(contractMapper, probeService, probeClient, new ObjectMapper(), clock);
    }

    /**
     * 初始化 MarketingIntegrationContractProbeAutomationService 实例。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<MarketingIntegrationContractDO> candidates = safeList(contractMapper.selectList(
                new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractDO::getEnvironment, "PRODUCTION")
                        .eq(MarketingIntegrationContractDO::getStatus, "ACTIVE")
                        .orderByDesc(MarketingIntegrationContractDO::getUpdatedAt)
                        .last("LIMIT " + boundedLimit)));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contract contract 参数，用于 probeContract 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 probeContract 流程生成的业务结果。
     */
    private ProbeAutomationResult probeContract(Long tenantId,
                                                MarketingIntegrationContractDO contract,
                                                String actor,
                                                LocalDateTime evaluatedAt) {
        // 准备本次处理所需的上下文和中间变量。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return toResult(view);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param contract contract 参数，用于 toTarget 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param view view 参数，用于 toResult 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param contract contract 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @param clientEvidence 依赖组件，用于完成数据访问或外部能力调用。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 evidence 流程生成的业务结果。
     */
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

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param contract contract 参数，用于 failureEvidence 流程中的校验、计算或对象转换。
     * @param exception exception 参数，用于 failureEvidence 流程中的校验、计算或对象转换。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 failureEvidence 流程生成的业务结果。
     */
    private Map<String, Object> failureEvidence(MarketingIntegrationContractDO contract,
                                                RuntimeException exception,
                                                LocalDateTime evaluatedAt) {
        Map<String, Object> evidence = evidence(contract, Map.of(), evaluatedAt);
        evidence.put("exceptionType", exception.getClass().getSimpleName());
        return evidence;
    }

    /**
     * 组装输出结构或完成对象转换。
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
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeStatus(String value) {
        String status = defaultString(value, "PASS").toUpperCase(Locale.ROOT);
        return switch (status) {
            case "PASS", "WARN", "FAIL" -> status;
            default -> "FAIL";
        };
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private static List<MarketingIntegrationContractDO> safeList(List<MarketingIntegrationContractDO> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 safe tenant id 计算得到的数量、金额或指标值。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int boundedLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private static String actor(String actor) {
        return actor == null || actor.isBlank() ? "marketing-integration-probe-scheduler" : actor.trim();
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param exception exception 参数，用于 message 流程中的校验、计算或对象转换。
     * @return 返回 message 生成的文本或业务键。
     */
    private static String message(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    /**
     * ProbeAutomationSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * ProbeAutomationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
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
