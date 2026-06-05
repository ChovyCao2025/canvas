package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldAccessAuditDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldPolicyDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseFieldAccessAuditMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseFieldPolicyMapper;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiSort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CdpWarehouseFieldGovernanceService {

    public static final String ACTION_BI_COMPILE = "BI_COMPILE";
    public static final String ACTION_BI_EXECUTE = "BI_EXECUTE";
    public static final String ACTION_BI_EVALUATE = "BI_EVALUATE";

    private static final int MAX_REASON_LENGTH = 1000;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String DECISION_ALLOW = "ALLOW";
    private static final String DECISION_DENY = "DENY";
    private static final String POLICY_ALLOW = "ALLOW";
    private static final String POLICY_DENY = "DENY";
    private static final String POLICY_MASK = "MASK";
    private static final String USAGE_SELECT = "SELECT";
    private static final String USAGE_FILTER = "FILTER";
    private static final String USAGE_SORT = "SORT";
    private static final String USAGE_GROUP = "GROUP";

    private final CdpWarehouseFieldPolicyMapper policyMapper;
    private final CdpWarehouseFieldAccessAuditMapper auditMapper;

    public CdpWarehouseFieldGovernanceService(CdpWarehouseFieldPolicyMapper policyMapper,
                                              CdpWarehouseFieldAccessAuditMapper auditMapper) {
        this.policyMapper = policyMapper;
        this.auditMapper = auditMapper;
    }

    public FieldPolicyView upsertPolicy(Long tenantId, FieldPolicyCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("field policy command is required");
        }
        CdpWarehouseFieldPolicyDO row = new CdpWarehouseFieldPolicyDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setDatasetKey(required(command.datasetKey(), "datasetKey"));
        row.setFieldKey(required(command.fieldKey(), "fieldKey"));
        row.setPhysicalName(required(command.physicalName(), "physicalName"));
        row.setColumnName(required(command.columnName(), "columnName"));
        row.setValueType(upperRequired(command.valueType(), "valueType"));
        row.setSemanticType(upperBlankToNull(command.semanticType()));
        row.setPiiLevel(upperDefault(command.piiLevel(), "NORMAL"));
        row.setAccessPolicy(upperDefault(command.accessPolicy(), POLICY_ALLOW));
        row.setMinRole(upperDefault(command.minRole(), RoleNames.OPERATOR));
        row.setAllowedUsages(normalizedUsages(command.allowedUsages()));
        row.setMaskStrategy(upperBlankToNull(command.maskStrategy()));
        row.setLifecycleStatus(upperDefault(command.lifecycleStatus(), STATUS_ACTIVE));
        row.setOwnerName(blankToNull(command.ownerName()));
        row.setDescription(blankToNull(command.description()));
        policyMapper.upsert(row);
        return toView(row);
    }

    public List<FieldPolicyView> listPolicies(Long tenantId, String datasetKey, String lifecycleStatus) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseFieldPolicyDO> query =
                new LambdaQueryWrapper<CdpWarehouseFieldPolicyDO>()
                        .in(CdpWarehouseFieldPolicyDO::getTenantId, tenantScope(scopedTenantId))
                        .orderByAsc(CdpWarehouseFieldPolicyDO::getTenantId)
                        .orderByAsc(CdpWarehouseFieldPolicyDO::getDatasetKey)
                        .orderByAsc(CdpWarehouseFieldPolicyDO::getFieldKey);
        if (hasText(datasetKey)) {
            query.eq(CdpWarehouseFieldPolicyDO::getDatasetKey, datasetKey.trim());
        }
        if (hasText(lifecycleStatus)) {
            query.eq(CdpWarehouseFieldPolicyDO::getLifecycleStatus,
                    lifecycleStatus.trim().toUpperCase(Locale.ROOT));
        }

        Map<String, FieldPolicyView> merged = new LinkedHashMap<>();
        for (CdpWarehouseFieldPolicyDO row : safeList(policyMapper.selectList(query))) {
            merged.put(policyKey(row.getDatasetKey(), row.getFieldKey()), toView(row));
        }
        return new ArrayList<>(merged.values());
    }

    public BiPolicyEvaluation evaluateBiQuery(BiDatasetSpec dataset,
                                              BiQueryRequest request,
                                              BiQueryContext context,
                                              String actionKey) {
        if (dataset == null) {
            throw new IllegalArgumentException("dataset is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!dataset.datasetKey().equals(request.datasetKey())) {
            throw new IllegalArgumentException("dataset does not match request");
        }
        BiQueryContext scopedContext = context == null
                ? new BiQueryContext(0L, "system", RoleNames.OPERATOR)
                : context;
        String scopedAction = upperDefault(actionKey, ACTION_BI_EVALUATE);
        Map<String, CdpWarehouseFieldPolicyDO> policies =
                activePolicyMap(scopedContext.tenantId(), dataset.datasetKey());
        List<FieldUsageDecision> decisions = collectFieldUsages(dataset, request).stream()
                .map(usage -> decide(usage, policies.get(usage.fieldKey()), scopedContext))
                .toList();
        List<FieldUsageDecision> denied = decisions.stream()
                .filter(decision -> DECISION_DENY.equals(decision.decision()))
                .toList();
        BiPolicyEvaluation evaluation = new BiPolicyEvaluation(
                scopedContext.tenantId(),
                dataset.datasetKey(),
                scopedContext.username(),
                scopedContext.role(),
                scopedAction,
                denied.isEmpty(),
                decisions,
                denied.isEmpty() ? "allowed" : denied.get(0).reason());
        auditDenied(evaluation, denied);
        return evaluation;
    }

    public void enforceBiQuery(BiDatasetSpec dataset,
                               BiQueryRequest request,
                               BiQueryContext context,
                               String actionKey) {
        BiPolicyEvaluation evaluation = evaluateBiQuery(dataset, request, context, actionKey);
        if (!evaluation.allowed()) {
            throw new FieldAccessDeniedException(evaluation.reason());
        }
    }

    private FieldUsageDecision decide(FieldUsage usage,
                                      CdpWarehouseFieldPolicyDO policy,
                                      BiQueryContext context) {
        if (policy == null) {
            return new FieldUsageDecision(
                    usage.fieldKey(),
                    usage.usage(),
                    DECISION_ALLOW,
                    "no active field policy",
                    "UNKNOWN",
                    POLICY_ALLOW,
                    RoleNames.OPERATOR);
        }
        String accessPolicy = upperDefault(policy.getAccessPolicy(), POLICY_ALLOW);
        String minRole = upperDefault(policy.getMinRole(), RoleNames.OPERATOR);
        String piiLevel = upperDefault(policy.getPiiLevel(), "NORMAL");
        if (POLICY_DENY.equals(accessPolicy)) {
            return denied(usage, policy, "field policy DENY blocks BI usage");
        }
        if (!allowedUsages(policy.getAllowedUsages()).contains(usage.usage())) {
            return denied(usage, policy, "usage " + usage.usage() + " is not allowed for field");
        }
        if (!knownAccessPolicy(accessPolicy)) {
            return denied(usage, policy, "unknown access policy " + accessPolicy);
        }
        if (roleRank(context.role()) < roleRank(minRole)) {
            return denied(usage, policy, "role " + context.role() + " is below required " + minRole);
        }
        if (POLICY_MASK.equals(accessPolicy)) {
            return new FieldUsageDecision(
                    usage.fieldKey(),
                    usage.usage(),
                    DECISION_ALLOW,
                    "MASK policy allowed for role " + context.role(),
                    piiLevel,
                    accessPolicy,
                    minRole);
        }
        return new FieldUsageDecision(
                usage.fieldKey(),
                usage.usage(),
                DECISION_ALLOW,
                "field policy allowed",
                piiLevel,
                accessPolicy,
                minRole);
    }

    private FieldUsageDecision denied(FieldUsage usage, CdpWarehouseFieldPolicyDO policy, String reason) {
        return new FieldUsageDecision(
                usage.fieldKey(),
                usage.usage(),
                DECISION_DENY,
                reason,
                upperDefault(policy.getPiiLevel(), "NORMAL"),
                upperDefault(policy.getAccessPolicy(), POLICY_ALLOW),
                upperDefault(policy.getMinRole(), RoleNames.OPERATOR));
    }

    private Map<String, CdpWarehouseFieldPolicyDO> activePolicyMap(Long tenantId, String datasetKey) {
        LambdaQueryWrapper<CdpWarehouseFieldPolicyDO> query =
                new LambdaQueryWrapper<CdpWarehouseFieldPolicyDO>()
                        .in(CdpWarehouseFieldPolicyDO::getTenantId, tenantScope(normalizeTenant(tenantId)))
                        .eq(CdpWarehouseFieldPolicyDO::getDatasetKey, datasetKey)
                        .eq(CdpWarehouseFieldPolicyDO::getLifecycleStatus, STATUS_ACTIVE)
                        .orderByAsc(CdpWarehouseFieldPolicyDO::getTenantId)
                        .orderByAsc(CdpWarehouseFieldPolicyDO::getFieldKey);
        Map<String, CdpWarehouseFieldPolicyDO> byField = new LinkedHashMap<>();
        for (CdpWarehouseFieldPolicyDO row : safeList(policyMapper.selectList(query))) {
            byField.put(row.getFieldKey(), row);
        }
        return byField;
    }

    private List<FieldUsage> collectFieldUsages(BiDatasetSpec dataset, BiQueryRequest request) {
        Map<String, FieldUsage> byKey = new LinkedHashMap<>();
        for (String dimension : request.dimensions()) {
            addUsage(byKey, dimension, USAGE_SELECT);
            addUsage(byKey, dimension, USAGE_GROUP);
        }
        for (BiFilter filter : request.filters()) {
            if (filter != null) {
                addUsage(byKey, filter.field(), USAGE_FILTER);
            }
        }
        for (String metricKey : request.metrics()) {
            addMetricUsages(byKey, dataset, metricKey, USAGE_SELECT);
        }
        for (BiSort sort : request.sorts()) {
            if (sort == null) {
                continue;
            }
            if (dataset.metrics().containsKey(sort.field())) {
                addMetricUsages(byKey, dataset, sort.field(), USAGE_SORT);
            } else {
                addUsage(byKey, sort.field(), USAGE_SORT);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private void addMetricUsages(Map<String, FieldUsage> byKey,
                                 BiDatasetSpec dataset,
                                 String metricKey,
                                 String usage) {
        addUsage(byKey, metricKey, usage);
        BiMetricSpec metric = dataset.metrics().get(metricKey);
        if (metric == null || !hasText(metric.expression())) {
            return;
        }
        for (String fieldKey : dataset.fields().keySet()) {
            if (referencesField(metric.expression(), fieldKey)) {
                addUsage(byKey, fieldKey, USAGE_SELECT);
            }
        }
    }

    private void addUsage(Map<String, FieldUsage> byKey, String fieldKey, String usage) {
        if (!hasText(fieldKey) || !hasText(usage)) {
            return;
        }
        String scopedUsage = usage.trim().toUpperCase(Locale.ROOT);
        byKey.putIfAbsent(fieldKey.trim() + "\u0000" + scopedUsage,
                new FieldUsage(fieldKey.trim(), scopedUsage));
    }

    private boolean referencesField(String expression, String fieldKey) {
        if (!hasText(expression) || !hasText(fieldKey)) {
            return false;
        }
        return Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(fieldKey) + "(?![A-Za-z0-9_])")
                .matcher(expression)
                .find();
    }

    private void auditDenied(BiPolicyEvaluation evaluation, List<FieldUsageDecision> denied) {
        for (FieldUsageDecision decision : denied) {
            CdpWarehouseFieldAccessAuditDO row = new CdpWarehouseFieldAccessAuditDO();
            row.setTenantId(evaluation.tenantId());
            row.setDatasetKey(evaluation.datasetKey());
            row.setFieldKey(decision.fieldKey());
            row.setActorId(evaluation.actorId());
            row.setActorRole(evaluation.actorRole());
            row.setActionKey(evaluation.actionKey());
            row.setDecision(DECISION_DENY);
            row.setReason(truncate(decision.usage() + ": " + decision.reason(), MAX_REASON_LENGTH));
            row.setCreatedAt(LocalDateTime.now());
            try {
                auditMapper.insert(row);
            } catch (RuntimeException ignored) {
                // The BI query still must be denied even when audit persistence is temporarily unavailable.
            }
        }
    }

    private FieldPolicyView toView(CdpWarehouseFieldPolicyDO row) {
        return new FieldPolicyView(
                row.getId(),
                row.getTenantId(),
                row.getDatasetKey(),
                row.getFieldKey(),
                row.getPhysicalName(),
                row.getColumnName(),
                row.getValueType(),
                row.getSemanticType(),
                row.getPiiLevel(),
                row.getAccessPolicy(),
                row.getMinRole(),
                row.getAllowedUsages(),
                row.getMaskStrategy(),
                row.getLifecycleStatus(),
                row.getOwnerName(),
                row.getDescription());
    }

    private boolean knownAccessPolicy(String value) {
        return POLICY_ALLOW.equals(value) || POLICY_DENY.equals(value) || POLICY_MASK.equals(value);
    }

    private Set<String> allowedUsages(String value) {
        if (!hasText(value)) {
            return defaultUsages();
        }
        Set<String> usages = new LinkedHashSet<>();
        for (String part : value.split(",")) {
            if (hasText(part)) {
                usages.add(part.trim().toUpperCase(Locale.ROOT));
            }
        }
        return usages.isEmpty() ? defaultUsages() : usages;
    }

    private Set<String> defaultUsages() {
        Set<String> usages = new LinkedHashSet<>();
        usages.add(USAGE_SELECT);
        usages.add(USAGE_FILTER);
        usages.add(USAGE_SORT);
        usages.add(USAGE_GROUP);
        return usages;
    }

    private int roleRank(String role) {
        String normalized = upperDefault(role, RoleNames.OPERATOR);
        if (RoleNames.ADMIN.equals(normalized) || RoleNames.SUPER_ADMIN.equals(normalized)) {
            return 30;
        }
        if (RoleNames.TENANT_ADMIN.equals(normalized)) {
            return 20;
        }
        return 10;
    }

    private String normalizedUsages(String value) {
        return String.join(",", allowedUsages(value));
    }

    private String policyKey(String datasetKey, String fieldKey) {
        return datasetKey + "\u0000" + fieldKey;
    }

    private List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
    }

    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    private String upperBlankToNull(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static class FieldAccessDeniedException extends RuntimeException {
        public FieldAccessDeniedException(String message) {
            super("Field access denied: " + message);
        }
    }

    private record FieldUsage(String fieldKey, String usage) {
    }

    public record FieldPolicyCommand(
            String datasetKey,
            String fieldKey,
            String physicalName,
            String columnName,
            String valueType,
            String semanticType,
            String piiLevel,
            String accessPolicy,
            String minRole,
            String allowedUsages,
            String maskStrategy,
            String lifecycleStatus,
            String ownerName,
            String description) {
    }

    public record FieldPolicyView(
            Long id,
            Long tenantId,
            String datasetKey,
            String fieldKey,
            String physicalName,
            String columnName,
            String valueType,
            String semanticType,
            String piiLevel,
            String accessPolicy,
            String minRole,
            String allowedUsages,
            String maskStrategy,
            String lifecycleStatus,
            String ownerName,
            String description) {
    }

    public record BiPolicyEvaluation(
            Long tenantId,
            String datasetKey,
            String actorId,
            String actorRole,
            String actionKey,
            boolean allowed,
            List<FieldUsageDecision> decisions,
            String reason) {
        public BiPolicyEvaluation {
            decisions = decisions == null ? List.of() : List.copyOf(decisions);
        }
    }

    public record FieldUsageDecision(
            String fieldKey,
            String usage,
            String decision,
            String reason,
            String piiLevel,
            String accessPolicy,
            String minRole) {
    }
}
