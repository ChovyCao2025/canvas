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
