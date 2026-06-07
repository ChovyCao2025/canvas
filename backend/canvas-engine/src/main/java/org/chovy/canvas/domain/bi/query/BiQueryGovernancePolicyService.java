package org.chovy.canvas.domain.bi.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiQueryGovernancePolicyDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiQueryGovernancePolicyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BiQueryGovernancePolicyService {

    public static final String DEFAULT_DATASET_KEY = "__DEFAULT__";
    private static final String AUDIT_ACTION = "BI_QUERY_GOVERNANCE_POLICY_UPDATE";
    private static final String AUDIT_RESOURCE_TYPE = "BI_QUERY_GOVERNANCE_POLICY";

    private final BiQueryGovernancePolicyMapper mapper;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public BiQueryGovernancePolicyService(BiQueryGovernancePolicyMapper mapper) {
        this(mapper, null, new ObjectMapper());
    }

    @Autowired
    public BiQueryGovernancePolicyService(BiQueryGovernancePolicyMapper mapper,
                                          BiAuditLogMapper auditLogMapper,
                                          ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public BiQueryGovernancePolicy currentPolicy(Long tenantId) {
        return policyFromRows(policies(tenantId));
    }

    public BiQueryGovernancePolicyView currentPolicyView(Long tenantId) {
        return view(currentPolicy(tenantId));
    }

    public List<BiQueryGovernanceAuditEntry> recentAudit(Long tenantId, int limit) {
        if (auditLogMapper == null) {
            return List.of();
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        List<BiAuditLogDO> rows = auditLogMapper.selectList(
                new LambdaQueryWrapper<BiAuditLogDO>()
                        .eq(BiAuditLogDO::getTenantId, scopedTenantId)
                        .eq(BiAuditLogDO::getActionKey, AUDIT_ACTION)
                        .eq(BiAuditLogDO::getResourceType, AUDIT_RESOURCE_TYPE)
                        .orderByDesc(BiAuditLogDO::getCreatedAt)
                        .last("LIMIT " + boundedLimit));
        return (rows == null ? List.<BiAuditLogDO>of() : rows).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> AUDIT_ACTION.equals(row.getActionKey()))
                .filter(row -> AUDIT_RESOURCE_TYPE.equals(row.getResourceType()))
                .sorted(Comparator.comparing(BiAuditLogDO::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(boundedLimit)
                .map(row -> new BiQueryGovernanceAuditEntry(
                        row.getId(),
                        row.getActorId(),
                        row.getActionKey(),
                        row.getResourceType(),
                        row.getDetailJson(),
                        row.getCreatedAt()))
                .toList();
    }

    public BiQueryGovernancePolicyView upsertPolicy(
            Long tenantId,
            BiQueryGovernancePolicyUpdateCommand command,
            String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQueryGovernancePolicyUpdateCommand scopedCommand = command == null
                ? new BiQueryGovernancePolicyUpdateCommand(null, null, List.of())
                : command;
        List<BiQueryGovernancePolicyDO> beforeRows = policies(scopedTenantId);
        BiQueryGovernancePolicy before = policyFromRows(beforeRows);
        Map<String, BiQueryGovernancePolicyDO> existing = beforeRows.stream()
                .filter(row -> row.getDatasetKey() != null)
                .collect(Collectors.toMap(BiQueryGovernancePolicyDO::getDatasetKey, Function.identity(),
                        (left, right) -> right));
        upsert(existing.get(DEFAULT_DATASET_KEY), scopedTenantId, DEFAULT_DATASET_KEY,
                scopedCommand.defaultTimeoutMs(), scopedCommand.defaultQuotaRows(), actor);
        for (BiQueryGovernancePolicyUpdateCommand.DatasetPolicyCommand dataset : scopedCommand.datasets()) {
            if (dataset == null || dataset.datasetKey() == null || dataset.datasetKey().isBlank()) {
                continue;
            }
            String datasetKey = dataset.datasetKey().trim();
            upsert(existing.get(datasetKey), scopedTenantId, datasetKey,
                    dataset.timeoutMs(), dataset.quotaRows(), actor);
        }
        BiQueryGovernancePolicy after = currentPolicy(scopedTenantId);
        auditUpdate(scopedTenantId, actor, before, after);
        return view(after);
    }

    private void upsert(BiQueryGovernancePolicyDO existing,
                        Long tenantId,
                        String datasetKey,
                        Long timeoutMs,
                        Integer quotaRows,
                        String actor) {
        BiQueryGovernancePolicyDO row = existing == null ? new BiQueryGovernancePolicyDO() : existing;
        row.setTenantId(tenantId);
        row.setDatasetKey(datasetKey);
        row.setTimeoutMs(timeoutMs == null || timeoutMs <= 0
                ? BiQueryGovernancePolicy.DEFAULT_TIMEOUT_MS
                : timeoutMs);
        row.setQuotaRows(quotaRows == null || quotaRows <= 0
                ? BiQueryGovernancePolicy.DEFAULT_QUOTA_ROWS
                : quotaRows);
        row.setUpdatedBy(actor == null || actor.isBlank() ? "system" : actor.trim());
        if (row.getId() == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
    }

    private List<BiQueryGovernancePolicyDO> policies(Long tenantId) {
        List<BiQueryGovernancePolicyDO> rows = mapper.selectList(
                new LambdaQueryWrapper<BiQueryGovernancePolicyDO>()
                        .eq(BiQueryGovernancePolicyDO::getTenantId, normalizeTenant(tenantId)));
        return rows == null ? List.of() : rows;
    }

    private BiQueryGovernancePolicy policyFromRows(List<BiQueryGovernancePolicyDO> rows) {
        BiQueryGovernancePolicyDO defaultRow = rows.stream()
                .filter(row -> DEFAULT_DATASET_KEY.equals(row.getDatasetKey()))
                .findFirst()
                .orElse(null);
        long defaultTimeoutMs = defaultRow == null || defaultRow.getTimeoutMs() == null
                ? BiQueryGovernancePolicy.DEFAULT_TIMEOUT_MS
                : defaultRow.getTimeoutMs();
        int defaultQuotaRows = defaultRow == null || defaultRow.getQuotaRows() == null
                ? BiQueryGovernancePolicy.DEFAULT_QUOTA_ROWS
                : defaultRow.getQuotaRows();
        Map<String, BiQueryGovernancePolicy.DatasetPolicy> datasets = rows.stream()
                .filter(row -> row.getDatasetKey() != null)
                .filter(row -> !DEFAULT_DATASET_KEY.equals(row.getDatasetKey()))
                .collect(Collectors.toMap(
                        BiQueryGovernancePolicyDO::getDatasetKey,
                        row -> new BiQueryGovernancePolicy.DatasetPolicy(
                                value(row.getTimeoutMs(), defaultTimeoutMs),
                                value(row.getQuotaRows(), defaultQuotaRows)),
                        (left, right) -> right,
                        LinkedHashMap::new));
        return new BiQueryGovernancePolicy(defaultTimeoutMs, defaultQuotaRows, datasets);
    }

    private BiQueryGovernancePolicyView view(BiQueryGovernancePolicy policy) {
        return new BiQueryGovernancePolicyView(
                policy.defaultTimeoutMs(),
                policy.defaultQuotaRows(),
                policy.datasets().entrySet().stream()
                        .map(entry -> new BiQueryGovernancePolicyView.DatasetPolicyView(
                                entry.getKey(),
                                entry.getValue().timeoutMs(),
                                entry.getValue().quotaRows()))
                        .toList());
    }

    private void auditUpdate(Long tenantId,
                             String actor,
                             BiQueryGovernancePolicy before,
                             BiQueryGovernancePolicy after) {
        if (auditLogMapper == null) {
            return;
        }
        BiAuditLogDO row = new BiAuditLogDO();
        row.setTenantId(tenantId);
        row.setActorId(actor == null || actor.isBlank() ? "system" : actor.trim());
        row.setActionKey(AUDIT_ACTION);
        row.setResourceType(AUDIT_RESOURCE_TYPE);
        row.setDetailJson(toJson(Map.of(
                "before", before,
                "after", after)));
        row.setCreatedAt(LocalDateTime.now());
        try {
            auditLogMapper.insert(row);
        } catch (RuntimeException ignored) {
            // Policy changes should still apply if audit storage is temporarily unavailable.
        }
    }

    private String toJson(Object detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private long value(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
