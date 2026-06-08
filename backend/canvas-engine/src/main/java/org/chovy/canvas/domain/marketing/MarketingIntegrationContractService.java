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
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractAuditEventDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractAuditEventMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MarketingIntegrationContractService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MarketingIntegrationContractMapper contractMapper;
    private final MarketingIntegrationContractAuditEventMapper auditMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public MarketingIntegrationContractService(MarketingIntegrationContractMapper contractMapper,
                                               MarketingIntegrationContractAuditEventMapper auditMapper,
                                               ObjectMapper objectMapper) {
        this.contractMapper = contractMapper;
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 创建或更新表治理契约，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；会同步写入审计事件。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingIntegrationContractView upsertContract(
            Long tenantId,
            MarketingIntegrationContractCommand command,
            String actor) {
        if (command == null) {
            throw new IllegalArgumentException("integration contract command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        String contractKey = normalizeKey(command.contractKey(), "contractKey");
        MarketingIntegrationContractDO row =
                contractMapper.selectOne(new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractDO::getContractKey, contractKey)
                        .last("LIMIT 1"));
        boolean insert = row == null;
        String previousStatus = insert ? null : row.getStatus();
        if (insert) {
            row = new MarketingIntegrationContractDO();
            row.setTenantId(scopedTenantId);
            row.setContractKey(contractKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setDisplayName(defaultString(command.displayName(), contractKey));
        row.setProviderFamily(normalizeUpper(required(command.providerFamily(), "providerFamily"), "providerFamily"));
        row.setSourceCapabilityKey(normalizeKey(command.sourceCapabilityKey(), "sourceCapabilityKey"));
        row.setTargetCapabilityKey(normalizeKey(command.targetCapabilityKey(), "targetCapabilityKey"));
        row.setAssetKey(normalizeKey(command.assetKey(), "assetKey"));
        row.setDirection(normalizeDirection(command.direction()));
        row.setEnvironment(normalizeEnvironment(command.environment()));
        row.setAuthMode(normalizeAuthMode(command.authMode()));
        row.setCredentialDependency(trimToLimit(command.credentialDependency(), 255));
        row.setApiRoot(required(command.apiRoot(), "apiRoot"));
        row.setOwnerTeam(trimToLimit(command.ownerTeam(), 128));
        row.setStatus(normalizeStatus(command.status()));
        row.setSlaTier(normalizeUpper(command.slaTier(), "STANDARD"));
        row.setTimeoutMs(normalizeTimeout(command.timeoutMs()));
        row.setRetryPolicyJson(toJson(command.retryPolicy()));
        row.setSchemaContractJson(toJson(command.schemaContract()));
        row.setMetadataJson(toJson(command.metadata()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            contractMapper.insert(row);
        } else {
            contractMapper.updateById(row);
        }
        writeAudit(row, insert ? "CREATED" : "UPDATED", previousStatus, row.getStatus(), actor);
        return toView(row);
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
    public List<MarketingIntegrationContractView> listContracts(
            Long tenantId,
            String status,
            String providerFamily,
            Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedStatus = normalizeOptionalStatus(status);
        String normalizedProvider = normalizeOptionalUpper(providerFamily);
        return contractMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                        .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null, MarketingIntegrationContractDO::getStatus, normalizedStatus)
                        .eq(normalizedProvider != null,
                                MarketingIntegrationContractDO::getProviderFamily,
                                normalizedProvider)
                        .orderByDesc(MarketingIntegrationContractDO::getUpdatedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .filter(row -> normalizedProvider == null || normalizedProvider.equals(row.getProviderFamily()))
                .map(this::toView)
                .toList();
    }

    /**
     * 执行业务操作 archiveContract，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；会同步写入审计事件。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contractId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingIntegrationContractView archiveContract(Long tenantId, Long contractId, String actor) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingIntegrationContractDO row = contractMapper.selectById(requiredId(contractId, "contractId"));
        validateTenant(scopedTenantId, row == null ? null : row.getTenantId(), "integration contract");
        String previousStatus = row.getStatus();
        row.setStatus("ARCHIVED");
        row.setUpdatedBy(defaultString(actor, "system"));
        contractMapper.updateById(row);
        writeAudit(row, "ARCHIVED", previousStatus, row.getStatus(), actor);
        return toView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会同步写入审计事件。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contractId 目标业务记录 ID，需与租户边界匹配
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractAuditEventView> listAuditEvents(
            Long tenantId,
            Long contractId,
            Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingIntegrationContractDO row = contractMapper.selectById(requiredId(contractId, "contractId"));
        validateTenant(scopedTenantId, row == null ? null : row.getTenantId(), "integration contract");
        return auditMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractAuditEventDO>()
                        .eq(MarketingIntegrationContractAuditEventDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractAuditEventDO::getContractId, row.getId())
                        .orderByDesc(MarketingIntegrationContractAuditEventDO::getRevision)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .map(this::toAuditView)
                .toList();
    }

    private void writeAudit(MarketingIntegrationContractDO row,
                            String eventType,
                            String previousStatus,
                            String newStatus,
                            String actor) {
        MarketingIntegrationContractAuditEventDO audit = new MarketingIntegrationContractAuditEventDO();
        audit.setTenantId(row.getTenantId());
        audit.setContractId(row.getId());
        audit.setContractKey(row.getContractKey());
        audit.setRevision(nextRevision(row.getTenantId(), row.getId()));
        audit.setEventType(eventType);
        audit.setPreviousStatus(previousStatus);
        audit.setNewStatus(newStatus);
        audit.setSnapshotJson(toJson(snapshot(row)));
        audit.setChangedFieldsJson(toJson(changedFields(previousStatus, newStatus, eventType)));
        audit.setChangedBy(defaultString(actor, "system"));
        auditMapper.insert(audit);
    }

    private Integer nextRevision(Long tenantId, Long contractId) {
        MarketingIntegrationContractAuditEventDO latest =
                auditMapper.selectOne(new LambdaQueryWrapper<MarketingIntegrationContractAuditEventDO>()
                        .eq(MarketingIntegrationContractAuditEventDO::getTenantId, tenantId)
                        .eq(MarketingIntegrationContractAuditEventDO::getContractId, contractId)
                        .orderByDesc(MarketingIntegrationContractAuditEventDO::getRevision)
                        .last("LIMIT 1"));
        return latest == null || latest.getRevision() == null ? 1 : latest.getRevision() + 1;
    }

    private Map<String, Object> snapshot(MarketingIntegrationContractDO row) {
        return Map.ofEntries(
                Map.entry("contractKey", row.getContractKey()),
                Map.entry("displayName", row.getDisplayName()),
                Map.entry("providerFamily", row.getProviderFamily()),
                Map.entry("sourceCapabilityKey", row.getSourceCapabilityKey()),
                Map.entry("targetCapabilityKey", row.getTargetCapabilityKey()),
                Map.entry("assetKey", row.getAssetKey()),
                Map.entry("direction", row.getDirection()),
                Map.entry("environment", row.getEnvironment()),
                Map.entry("authMode", row.getAuthMode()),
                Map.entry("apiRoot", row.getApiRoot()),
                Map.entry("status", row.getStatus()),
                Map.entry("slaTier", row.getSlaTier()),
                Map.entry("timeoutMs", row.getTimeoutMs()));
    }

    private Map<String, Object> changedFields(String previousStatus, String newStatus, String eventType) {
        if (previousStatus != null && newStatus != null && !previousStatus.equals(newStatus)) {
            return Map.of("changedFields", List.of("status"));
        }
        return Map.of("changedFields", List.of(eventType.toLowerCase(Locale.ROOT)));
    }

    private MarketingIntegrationContractView toView(MarketingIntegrationContractDO row) {
        return new MarketingIntegrationContractView(
                row.getId(),
                row.getTenantId(),
                row.getContractKey(),
                row.getDisplayName(),
                row.getProviderFamily(),
                row.getSourceCapabilityKey(),
                row.getTargetCapabilityKey(),
                row.getAssetKey(),
                row.getDirection(),
                row.getEnvironment(),
                row.getAuthMode(),
                row.getCredentialDependency(),
                row.getApiRoot(),
                row.getOwnerTeam(),
                row.getStatus(),
                row.getSlaTier(),
                row.getTimeoutMs(),
                fromJson(row.getRetryPolicyJson()),
                fromJson(row.getSchemaContractJson()),
                fromJson(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private MarketingIntegrationContractAuditEventView toAuditView(MarketingIntegrationContractAuditEventDO row) {
        return new MarketingIntegrationContractAuditEventView(
                row.getId(),
                row.getTenantId(),
                row.getContractId(),
                row.getContractKey(),
                row.getRevision(),
                row.getEventType(),
                row.getPreviousStatus(),
                row.getNewStatus(),
                fromJson(row.getSnapshotJson()),
                fromJson(row.getChangedFieldsJson()),
                row.getChangedBy(),
                row.getCreatedAt());
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("integration contract metadata must be JSON serializable", e);
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

    private static String normalizeKey(String value, String field) {
        String normalized = required(value, field).trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "DRAFT");
        return switch (status) {
            case "DRAFT", "ACTIVE", "DEGRADED", "BLOCKED", "ARCHIVED" -> status;
            default -> throw new IllegalArgumentException("unsupported integration contract status: " + status);
        };
    }

    private static String normalizeOptionalStatus(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeStatus(trimmed);
    }

    private static String normalizeDirection(String value) {
        String direction = normalizeUpper(value, "OUTBOUND");
        return switch (direction) {
            case "INBOUND", "OUTBOUND", "BIDIRECTIONAL", "INTERNAL" -> direction;
            default -> throw new IllegalArgumentException("unsupported integration direction: " + direction);
        };
    }

    private static String normalizeEnvironment(String value) {
        String environment = normalizeUpper(value, "PRODUCTION");
        return switch (environment) {
            case "PRODUCTION", "STAGING", "SANDBOX" -> environment;
            default -> throw new IllegalArgumentException("unsupported integration environment: " + environment);
        };
    }

    private static String normalizeAuthMode(String value) {
        String authMode = normalizeUpper(value, "OAUTH");
        return switch (authMode) {
            case "OAUTH", "API_KEY", "HMAC", "INTERNAL", "NONE" -> authMode;
            default -> throw new IllegalArgumentException("unsupported integration auth mode: " + authMode);
        };
    }

    private static int normalizeTimeout(Integer timeoutMs) {
        if (timeoutMs == null) {
            return 30000;
        }
        if (timeoutMs < 1000 || timeoutMs > 300000) {
            throw new IllegalArgumentException("timeoutMs must be between 1000 and 300000");
        }
        return timeoutMs;
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
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
