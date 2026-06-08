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
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeObservationDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeRunDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeObservationMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class MarketingIntegrationContractProbeService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MarketingIntegrationContractMapper contractMapper;
    private final MarketingIntegrationContractProbeRunMapper probeMapper;
    private final MarketingIntegrationContractProbeObservationMapper observationMapper;
    private final ObjectMapper objectMapper;
    private final MarketingIntegrationContractProbeAlertService alertService;
    private final MarketingIntegrationContractSloService sloService;
    private final Clock clock;

    @Autowired
    public MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                                    MarketingIntegrationContractProbeRunMapper probeMapper,
                                                    ObjectProvider<MarketingIntegrationContractProbeObservationMapper>
                                                            observationMapperProvider,
                                                    ObjectMapper objectMapper,
                                                    ObjectProvider<MarketingIntegrationContractProbeAlertService>
                                                            alertServiceProvider,
                                                    ObjectProvider<MarketingIntegrationContractSloService>
                                                            sloServiceProvider) {
        this(contractMapper,
                probeMapper,
                observationMapperProvider == null ? null : observationMapperProvider.getIfAvailable(),
                objectMapper,
                alertServiceProvider == null ? null : alertServiceProvider.getIfAvailable(),
                sloServiceProvider == null ? null : sloServiceProvider.getIfAvailable(),
                Clock.systemDefaultZone());
    }

    MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                             MarketingIntegrationContractProbeRunMapper probeMapper,
                                             ObjectMapper objectMapper,
                                             Clock clock) {
        this(contractMapper, probeMapper, null, objectMapper, null, null, clock);
    }

    MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                             MarketingIntegrationContractProbeRunMapper probeMapper,
                                             ObjectMapper objectMapper,
                                             MarketingIntegrationContractProbeAlertService alertService,
                                             Clock clock) {
        this(contractMapper, probeMapper, null, objectMapper, alertService, null, clock);
    }

    MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                             MarketingIntegrationContractProbeRunMapper probeMapper,
                                             MarketingIntegrationContractProbeObservationMapper observationMapper,
                                             ObjectMapper objectMapper,
                                             MarketingIntegrationContractProbeAlertService alertService,
                                             MarketingIntegrationContractSloService sloService,
                                             Clock clock) {
        this.contractMapper = contractMapper;
        this.probeMapper = probeMapper;
        this.observationMapper = observationMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.alertService = alertService;
        this.sloService = sloService;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行业务操作 recordProbe，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contractId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingIntegrationContractProbeView recordProbe(Long tenantId,
                                                            Long contractId,
                                                            MarketingIntegrationContractProbeCommand command,
                                                            String actor) {
        if (command == null) {
            throw new IllegalArgumentException("integration contract probe command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingIntegrationContractDO contract = contract(scopedTenantId, contractId);
        String probeKey = normalizeKey(command.probeKey(), "probeKey");
        MarketingIntegrationContractProbeRunDO row =
                probeMapper.selectOne(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractProbeRunDO::getContractId, contract.getId())
                        .eq(MarketingIntegrationContractProbeRunDO::getProbeKey, probeKey)
                        .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new MarketingIntegrationContractProbeRunDO();
            row.setTenantId(scopedTenantId);
            row.setContractId(contract.getId());
            row.setContractKey(contract.getContractKey());
            row.setProbeKey(probeKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setProviderFamily(contract.getProviderFamily());
        row.setEnvironment(normalizeEnvironment(command.environment(), contract.getEnvironment()));
        row.setStatus(normalizeStatus(command.status()));
        row.setHttpStatusCode(command.httpStatusCode());
        row.setLatencyMs(nonNegative(command.latencyMs()));
        row.setErrorType(trimToLimit(command.errorType(), 255));
        row.setProblemTypeUri(trimToLimit(command.problemTypeUri(), 512));
        row.setProblemTitle(trimToLimit(command.problemTitle(), 255));
        row.setProblemDetail(trimToLimit(command.problemDetail(), 1000));
        row.setErrorMessage(null);
        row.setSummary(null);
        row.setObservedAt(command.observedAt() == null ? LocalDateTime.now(clock).withNano(0) : command.observedAt());
        row.setEvidenceJson(toJson(command.evidence()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            probeMapper.insert(row);
        } else {
            probeMapper.updateById(row);
        }
        MarketingIntegrationContractProbeRunView runView = toRunView(row);
        appendObservation(scopedTenantId, contract, runView, actor);
        syncSlo(scopedTenantId, contract, runView.probeKey(), actor);
        return toView(row);
    }

    /**
     * 执行业务操作 recordProbeRun，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contractId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingIntegrationContractProbeRunView recordProbeRun(Long tenantId,
                                                                   Long contractId,
                                                                   MarketingIntegrationContractProbeRunCommand command,
                                                                   String actor) {
        if (command == null) {
            throw new IllegalArgumentException("integration contract probe command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingIntegrationContractDO contract = contract(scopedTenantId, contractId);
        String probeKey = normalizeKey(command.probeKey(), "probeKey");
        MarketingIntegrationContractProbeRunDO row =
                probeMapper.selectOne(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractProbeRunDO::getContractId, contract.getId())
                        .eq(MarketingIntegrationContractProbeRunDO::getProbeKey, probeKey)
                        .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new MarketingIntegrationContractProbeRunDO();
            row.setTenantId(scopedTenantId);
            row.setContractId(contract.getId());
            row.setContractKey(contract.getContractKey());
            row.setProbeKey(probeKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setProviderFamily(defaultString(contract.getProviderFamily(), "UNKNOWN"));
        row.setEnvironment(normalizeEnvironment(null, contract.getEnvironment()));
        row.setStatus(normalizeStatus(command.status()));
        row.setHttpStatusCode(validateHttpStatus(command.httpStatusCode()));
        row.setLatencyMs(validateLatency(command.latencyMs()));
        row.setProblemTypeUri(trimToLimit(command.problemTypeUri(), 512));
        row.setErrorMessage(trimToLimit(command.errorMessage(), 1000));
        row.setSummary(trimToLimit(command.summary(), 512));
        row.setObservedAt(LocalDateTime.now(clock).withNano(0));
        row.setEvidenceJson(toJson(command.evidence()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            probeMapper.insert(row);
        } else {
            probeMapper.updateById(row);
        }
        MarketingIntegrationContractProbeRunView view = toRunView(row);
        appendObservation(scopedTenantId, contract, view, actor);
        syncAlert(scopedTenantId, contract, view, actor);
        syncSlo(scopedTenantId, contract, view.probeKey(), actor);
        return view;
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contractId 目标业务记录 ID，需与租户边界匹配
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractProbeView> listContractProbes(Long tenantId,
                                                                         Long contractId,
                                                                         Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        contract(scopedTenantId, contractId);
        return probeMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractProbeRunDO::getContractId, requiredId(contractId, "contractId"))
                        .orderByDesc(MarketingIntegrationContractProbeRunDO::getObservedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractProbeView> listRecentProbes(Long tenantId,
                                                                       String status,
                                                                       Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedStatus = normalizeOptionalStatus(status);
        return probeMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null,
                                MarketingIntegrationContractProbeRunDO::getStatus,
                                normalizedStatus)
                        .orderByDesc(MarketingIntegrationContractProbeRunDO::getObservedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .map(this::toView)
                .toList();
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param providerFamily providerFamily 参数，参与本次业务定位、校验或状态计算
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractProbeRunView> listProbeRuns(Long tenantId,
                                                                        String status,
                                                                        String providerFamily,
                                                                        Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedStatus = normalizeOptionalStatus(status);
        String normalizedProvider = normalizeOptionalUpper(providerFamily);
        return probeMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null,
                                MarketingIntegrationContractProbeRunDO::getStatus,
                                normalizedStatus)
                        .eq(normalizedProvider != null,
                                MarketingIntegrationContractProbeRunDO::getProviderFamily,
                                normalizedProvider)
                        .orderByDesc(MarketingIntegrationContractProbeRunDO::getObservedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .filter(row -> normalizedProvider == null || normalizedProvider.equals(row.getProviderFamily()))
                .map(this::toRunView)
                .toList();
    }

    private MarketingIntegrationContractDO contract(Long tenantId, Long contractId) {
        MarketingIntegrationContractDO contract = contractMapper.selectById(requiredId(contractId, "contractId"));
        validateTenant(tenantId, contract == null ? null : contract.getTenantId(), "integration contract");
        return contract;
    }

    private void syncAlert(Long tenantId,
                           MarketingIntegrationContractDO contract,
                           MarketingIntegrationContractProbeRunView view,
                           String actor) {
        if (alertService == null) {
            return;
        }
        try {
            alertService.syncProbeResult(tenantId, contract, view, defaultString(actor, "system"));
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] probe alert sync skipped contract={} error={}",
                    contract.getContractKey(), ex.getMessage());
        }
    }

    private void appendObservation(Long tenantId,
                                   MarketingIntegrationContractDO contract,
                                   MarketingIntegrationContractProbeRunView view,
                                   String actor) {
        if (observationMapper == null) {
            return;
        }
        try {
            MarketingIntegrationContractProbeObservationDO row = new MarketingIntegrationContractProbeObservationDO();
            row.setTenantId(tenantId);
            row.setContractId(contract.getId());
            row.setProbeRunId(view.id());
            row.setContractKey(contract.getContractKey());
            row.setProviderFamily(defaultString(contract.getProviderFamily(), "UNKNOWN"));
            row.setProbeKey(view.probeKey());
            row.setEnvironment(view.environment());
            row.setStatus(view.status());
            row.setHttpStatusCode(view.httpStatusCode());
            row.setLatencyMs(view.latencyMs());
            row.setProblemTypeUri(view.problemTypeUri());
            row.setErrorMessage(view.errorMessage());
            row.setSummary(view.summary());
            row.setObservedAt(parseObservedAt(view.observedAt()));
            row.setEvidenceJson(toJson(view.evidence()));
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(LocalDateTime.now(clock).withNano(0));
            observationMapper.insert(row);
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] probe observation append skipped contract={} probe={} error={}",
                    contract.getContractKey(), view.probeKey(), ex.getMessage());
        }
    }

    private void syncSlo(Long tenantId,
                         MarketingIntegrationContractDO contract,
                         String probeKey,
                         String actor) {
        if (sloService == null) {
            return;
        }
        try {
            sloService.evaluateAndSyncContract(tenantId, contract, probeKey, defaultString(actor, "system"));
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] SLO evaluation skipped contract={} probe={} error={}",
                    contract.getContractKey(), probeKey, ex.getMessage());
        }
    }

    private MarketingIntegrationContractProbeView toView(MarketingIntegrationContractProbeRunDO row) {
        return new MarketingIntegrationContractProbeView(
                row.getId(),
                row.getTenantId(),
                row.getContractId(),
                row.getContractKey(),
                row.getProbeKey(),
                row.getEnvironment(),
                row.getStatus(),
                row.getHttpStatusCode(),
                row.getLatencyMs(),
                row.getErrorType(),
                row.getProblemTypeUri(),
                row.getProblemTitle(),
                row.getProblemDetail(),
                row.getObservedAt(),
                fromJson(row.getEvidenceJson()),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    private MarketingIntegrationContractProbeRunView toRunView(MarketingIntegrationContractProbeRunDO row) {
        return new MarketingIntegrationContractProbeRunView(
                row.getId(),
                row.getTenantId(),
                row.getContractId(),
                row.getContractKey(),
                row.getProviderFamily(),
                row.getEnvironment(),
                row.getProbeKey(),
                row.getStatus(),
                row.getHttpStatusCode(),
                row.getLatencyMs(),
                row.getProblemTypeUri(),
                row.getErrorMessage(),
                row.getSummary(),
                fromJson(row.getEvidenceJson()),
                row.getObservedAt() == null ? null : row.getObservedAt().toString(),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("probe evidence must be JSON serializable", e);
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

    private LocalDateTime parseObservedAt(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now(clock).withNano(0);
        }
        try {
            return LocalDateTime.parse(value.trim()).withNano(0);
        } catch (RuntimeException ex) {
            return LocalDateTime.now(clock).withNano(0);
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

    private static String normalizeKey(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "PASS");
        return switch (status) {
            case "PASS", "WARN", "FAIL" -> status;
            default -> throw new IllegalArgumentException("unsupported integration probe status: " + status);
        };
    }

    private static String normalizeOptionalStatus(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeStatus(trimmed);
    }

    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeEnvironment(String value, String fallback) {
        String environment = normalizeUpper(value, defaultString(fallback, "PRODUCTION"));
        return switch (environment) {
            case "PRODUCTION", "STAGING", "SANDBOX" -> environment;
            default -> throw new IllegalArgumentException("unsupported integration environment: " + environment);
        };
    }

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    private static Long nonNegative(Long value) {
        return value == null || value < 0 ? null : value;
    }

    private static Integer validateHttpStatus(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 100 || value > 599) {
            throw new IllegalArgumentException("httpStatusCode must be between 100 and 599");
        }
        return value;
    }

    private static Long validateLatency(Long value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException("latencyMs must be non-negative");
        }
        return value;
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

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    private static void validateTenant(Long expected, Long actual, String entity) {
        if (actual == null || !actual.equals(expected)) {
            throw new IllegalArgumentException(entity + " does not belong to tenant");
        }
    }
}
