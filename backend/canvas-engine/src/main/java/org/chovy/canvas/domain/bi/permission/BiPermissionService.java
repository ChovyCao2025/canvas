package org.chovy.canvas.domain.bi.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiColumnPermissionDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiResourcePermissionDO;
import org.chovy.canvas.dal.dataobject.BiRowPermissionDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiColumnPermissionMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiResourcePermissionMapper;
import org.chovy.canvas.dal.mapper.BiRowPermissionMapper;
import org.chovy.canvas.domain.bi.portal.BiPortalMenuResource;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiSort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class BiPermissionService {

    public static final String ACTION_VIEW = "VIEW";
    public static final String ACTION_USE = "USE";
    public static final String ACTION_QUERY = "QUERY";
    public static final String ACTION_EDIT = "EDIT";
    public static final String ACTION_PUBLISH = "PUBLISH";
    public static final String ACTION_EXPORT = "EXPORT";
    public static final String ACTION_EMBED = "EMBED";
    public static final String ACTION_SUBSCRIBE = "SUBSCRIBE";

    private static final String RESOURCE_DATASET = "DATASET";
    private static final String EFFECT_ALLOW = "ALLOW";
    private static final String EFFECT_DENY = "DENY";
    private static final String POLICY_ALLOW = "ALLOW";
    private static final String POLICY_DENY = "DENY";
    private static final String POLICY_MASK = "MASK";
    private static final String SUBJECT_USER = "USER";
    private static final String SUBJECT_ROLE = "ROLE";
    private static final String SUBJECT_ALL = "ALL";
    private static final String USAGE_SELECT = "SELECT";
    private static final String USAGE_FILTER = "FILTER";
    private static final String USAGE_SORT = "SORT";
    private static final String USAGE_GROUP = "GROUP";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String DEFAULT_SIGNATURE = "bi-permission:v1";

    private final BiDatasetMapper datasetMapper;
    private final BiResourcePermissionMapper resourcePermissionMapper;
    private final BiRowPermissionMapper rowPermissionMapper;
    private final BiColumnPermissionMapper columnPermissionMapper;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public BiPermissionService(BiDatasetMapper datasetMapper,
                               BiResourcePermissionMapper resourcePermissionMapper,
                               BiRowPermissionMapper rowPermissionMapper,
                               BiColumnPermissionMapper columnPermissionMapper,
                               BiAuditLogMapper auditLogMapper,
                               ObjectMapper objectMapper) {
        this.datasetMapper = datasetMapper;
        this.resourcePermissionMapper = resourcePermissionMapper;
        this.rowPermissionMapper = rowPermissionMapper;
        this.columnPermissionMapper = columnPermissionMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public BiPreparedQuery prepareQuery(BiDatasetSpec dataset,
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
        BiQueryContext scopedContext = scopedContext(context);
        String scopedAction = upperDefault(actionKey, ACTION_USE);
        BiDatasetDO datasetRow = findDataset(scopedContext.tenantId(), dataset.datasetKey());
        if (datasetRow == null || datasetRow.getId() == null) {
            return new BiPreparedQuery(request, List.of(), DEFAULT_SIGNATURE + ":builtin");
        }

        enforceResourceAccess(
                scopedContext.tenantId(),
                datasetRow.getWorkspaceId(),
                RESOURCE_DATASET,
                datasetRow.getId(),
                scopedContext,
                scopedAction);

        List<String> rowRuleKeys = new ArrayList<>();
        List<BiFilter> filters = new ArrayList<>(request.filters());
        for (BiRowPermissionDO row : matchingRowPermissions(scopedContext, datasetRow.getId())) {
            rowRuleKeys.add(row.getRuleKey());
            filters.addAll(parseRowFilters(row.getRuleKey(), row.getFilterJson()));
        }
        BiQueryRequest scopedRequest = new BiQueryRequest(
                request.datasetKey(),
                request.dimensions(),
                request.metrics(),
                filters,
                request.sorts(),
                request.limit());

        List<BiColumnMask> masks = evaluateColumnPolicies(dataset, scopedRequest, scopedContext, datasetRow);
        return new BiPreparedQuery(
                scopedRequest,
                masks,
                signature(datasetRow.getId(), rowRuleKeys, masks));
    }

    public List<Map<String, Object>> applyMasks(List<Map<String, Object>> rows, List<BiColumnMask> masks) {
        if (rows == null || rows.isEmpty() || masks == null || masks.isEmpty()) {
            return rows == null ? List.of() : rows;
        }
        Map<String, BiColumnMask> byField = new LinkedHashMap<>();
        for (BiColumnMask mask : masks) {
            byField.putIfAbsent(mask.fieldKey(), mask);
        }
        return rows.stream()
                .map(row -> {
                    Map<String, Object> masked = new LinkedHashMap<>(row);
                    for (BiColumnMask mask : byField.values()) {
                        if (masked.containsKey(mask.fieldKey())) {
                            masked.put(mask.fieldKey(), maskedValue(masked.get(mask.fieldKey()), mask));
                        }
                    }
                    return masked;
                })
                .toList();
    }

    public void enforceResourceAccess(Long tenantId,
                                      Long workspaceId,
                                      String resourceType,
                                      Long resourceId,
                                      BiQueryContext context,
                                      String actionKey) {
        BiQueryContext scopedContext = scopedContext(context);
        String scopedAction = upperDefault(actionKey, ACTION_VIEW);
        String scopedType = upperRequired(resourceType, "resourceType");
        if (resourceId == null) {
            throw new IllegalArgumentException("resourceId is required");
        }
        List<BiResourcePermissionDO> rows = safeList(resourcePermissionMapper.selectList(
                new LambdaQueryWrapper<BiResourcePermissionDO>()
                        .in(BiResourcePermissionDO::getTenantId, tenantScope(tenantId))
                        .eq(BiResourcePermissionDO::getResourceType, scopedType)
                        .eq(BiResourcePermissionDO::getResourceId, resourceId)
                        .orderByDesc(BiResourcePermissionDO::getTenantId)));
        List<BiResourcePermissionDO> matching = rows.stream()
                .filter(row -> actionMatches(row.getActionKey(), scopedAction))
                .filter(row -> subjectMatches(row.getSubjectType(), row.getSubjectId(), scopedContext))
                .toList();
        boolean denied = matching.stream()
                .anyMatch(row -> EFFECT_DENY.equals(upperDefault(row.getEffect(), EFFECT_ALLOW)));
        if (denied) {
            String reason = "BI resource permission DENY blocks " + scopedAction + " on " + scopedType;
            audit(scopedContext.tenantId(), workspaceId, scopedContext.username(), scopedAction,
                    scopedType, resourceId, Map.of("decision", EFFECT_DENY, "reason", reason));
            throw new BiPermissionDeniedException(reason);
        }
        boolean allowed = matching.stream()
                .anyMatch(row -> EFFECT_ALLOW.equals(upperDefault(row.getEffect(), EFFECT_ALLOW)));
        if (!allowed && !defaultAllow(scopedAction)) {
            String reason = "BI resource permission is required for " + scopedAction + " on " + scopedType;
            audit(scopedContext.tenantId(), workspaceId, scopedContext.username(), scopedAction,
                    scopedType, resourceId, Map.of("decision", EFFECT_DENY, "reason", reason));
            throw new BiPermissionDeniedException(reason);
        }
    }

    public List<BiPortalMenuResource> visibleMenus(List<BiPortalMenuResource> menus, BiQueryContext context) {
        BiQueryContext scopedContext = scopedContext(context);
        return safeList(menus).stream()
                .filter(menu -> isMenuVisible(menu.visibility(), scopedContext))
                .toList();
    }

    public boolean isMenuVisible(Map<String, Object> visibility, BiQueryContext context) {
        if (visibility == null || visibility.isEmpty()) {
            return true;
        }
        BiQueryContext scopedContext = scopedContext(context);
        if (containsAny(visibility.get("excludedUsers"), scopedContext.username())
                || containsAny(visibility.get("denyUsers"), scopedContext.username())
                || containsAny(visibility.get("excludedRoles"), scopedContext.role())
                || containsAny(visibility.get("denyRoles"), scopedContext.role())) {
            return false;
        }
        boolean usersConfigured = hasValues(visibility.get("users"));
        boolean rolesConfigured = hasValues(visibility.get("roles"));
        if (!usersConfigured && !rolesConfigured) {
            return true;
        }
        return containsAny(visibility.get("users"), scopedContext.username())
                || containsAny(visibility.get("roles"), scopedContext.role());
    }

    private List<BiColumnMask> evaluateColumnPolicies(BiDatasetSpec dataset,
                                                      BiQueryRequest request,
                                                      BiQueryContext context,
                                                      BiDatasetDO datasetRow) {
        Map<String, String> policies = effectiveColumnPolicies(context, datasetRow.getId());
        if (policies.isEmpty()) {
            return List.of();
        }
        List<FieldUsage> usages = collectFieldUsages(dataset, request);
        List<FieldUsage> denied = usages.stream()
                .filter(usage -> POLICY_DENY.equals(policies.get(usage.fieldKey())))
                .toList();
        if (!denied.isEmpty()) {
            FieldUsage first = denied.get(0);
            String reason = "BI column permission DENY blocks " + first.usage() + " on " + first.fieldKey();
            audit(context.tenantId(), datasetRow.getWorkspaceId(), context.username(), ACTION_QUERY,
                    RESOURCE_DATASET, datasetRow.getId(), Map.of(
                            "decision", EFFECT_DENY,
                            "fieldKey", first.fieldKey(),
                            "usage", first.usage(),
                            "reason", reason));
            throw new BiPermissionDeniedException(reason);
        }
        Map<String, BiColumnMask> masks = new LinkedHashMap<>();
        Map<String, BiColumnPermissionDO> policyRows = matchingColumnPolicyRows(context, datasetRow.getId());
        for (FieldUsage usage : usages) {
            if (!USAGE_SELECT.equals(usage.usage())) {
                continue;
            }
            if (POLICY_MASK.equals(policies.get(usage.fieldKey()))) {
                BiColumnPermissionDO row = policyRows.get(usage.fieldKey());
                masks.putIfAbsent(usage.outputKey(), new BiColumnMask(
                        usage.outputKey(),
                        usage.fieldKey(),
                        row == null ? null : row.getMaskJson()));
            }
        }
        return List.copyOf(masks.values());
    }

    private Map<String, String> effectiveColumnPolicies(BiQueryContext context, Long datasetId) {
        Map<String, String> policies = new LinkedHashMap<>();
        for (BiColumnPermissionDO row : matchingColumnPolicyRows(context, datasetId).values()) {
            String fieldKey = trim(row.getFieldKey());
            String policy = upperDefault(row.getPolicy(), POLICY_ALLOW);
            String existing = policies.get(fieldKey);
            if (POLICY_DENY.equals(existing) || POLICY_DENY.equals(policy)) {
                policies.put(fieldKey, POLICY_DENY);
            } else if (POLICY_MASK.equals(existing) || POLICY_MASK.equals(policy)) {
                policies.put(fieldKey, POLICY_MASK);
            } else {
                policies.put(fieldKey, POLICY_ALLOW);
            }
        }
        return policies;
    }

    private Map<String, BiColumnPermissionDO> matchingColumnPolicyRows(BiQueryContext context, Long datasetId) {
        Map<String, BiColumnPermissionDO> rows = new LinkedHashMap<>();
        for (BiColumnPermissionDO row : safeList(columnPermissionMapper.selectList(
                new LambdaQueryWrapper<BiColumnPermissionDO>()
                        .in(BiColumnPermissionDO::getTenantId, tenantScope(context.tenantId()))
                        .eq(BiColumnPermissionDO::getDatasetId, datasetId)
                        .eq(BiColumnPermissionDO::getEnabled, true)
                        .orderByAsc(BiColumnPermissionDO::getTenantId)
                        .orderByAsc(BiColumnPermissionDO::getFieldKey)))) {
            if (!subjectMatches(row.getSubjectType(), row.getSubjectId(), context) || !hasText(row.getFieldKey())) {
                continue;
            }
            String fieldKey = row.getFieldKey().trim();
            BiColumnPermissionDO existing = rows.get(fieldKey);
            if (existing == null || policyRank(row.getPolicy()) > policyRank(existing.getPolicy())) {
                rows.put(fieldKey, row);
            }
        }
        return rows;
    }

    private List<BiRowPermissionDO> matchingRowPermissions(BiQueryContext context, Long datasetId) {
        return safeList(rowPermissionMapper.selectList(new LambdaQueryWrapper<BiRowPermissionDO>()
                        .in(BiRowPermissionDO::getTenantId, tenantScope(context.tenantId()))
                        .eq(BiRowPermissionDO::getDatasetId, datasetId)
                        .eq(BiRowPermissionDO::getEnabled, true)
                        .orderByAsc(BiRowPermissionDO::getTenantId)
                        .orderByAsc(BiRowPermissionDO::getRuleKey)))
                .stream()
                .filter(row -> subjectMatches(row.getSubjectType(), row.getSubjectId(), context))
                .toList();
    }

    private BiDatasetDO findDataset(Long tenantId, String datasetKey) {
        return datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .in(BiDatasetDO::getTenantId, tenantScope(tenantId))
                .eq(BiDatasetDO::getDatasetKey, required(datasetKey, "datasetKey"))
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                .orderByDesc(BiDatasetDO::getTenantId)
                .last("LIMIT 1"));
    }

    private List<BiFilter> parseRowFilters(String ruleKey, String filterJson) {
        if (!hasText(filterJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(filterJson);
            List<BiFilter> filters = new ArrayList<>();
            collectFilters(ruleKey, root, filters);
            return filters;
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid BI row permission filter for rule " + ruleKey, e);
        }
    }

    private void collectFilters(String ruleKey, JsonNode node, List<BiFilter> filters) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectFilters(ruleKey, item, filters);
            }
            return;
        }
        if (node.has("filters")) {
            collectFilters(ruleKey, node.get("filters"), filters);
            return;
        }
        if (!node.hasNonNull("field")) {
            throw new IllegalArgumentException("row permission rule " + ruleKey + " is missing field");
        }
        String field = required(node.get("field").asText(), "field");
        String operator = node.hasNonNull("operator")
                ? node.get("operator").asText()
                : node.path("op").asText("EQ");
        JsonNode valueNode = node.has("value") ? node.get("value") : node.get("values");
        Object value = objectMapper.convertValue(valueNode, Object.class);
        BiFilter.Operator scopedOperator = BiFilter.Operator.valueOf(operator.trim().toUpperCase(Locale.ROOT));
        if (scopedOperator == BiFilter.Operator.IN && !(value instanceof Collection<?>)) {
            value = List.of(value);
        }
        filters.add(new BiFilter(field, scopedOperator, value));
    }

    private List<FieldUsage> collectFieldUsages(BiDatasetSpec dataset, BiQueryRequest request) {
        Map<String, FieldUsage> byKey = new LinkedHashMap<>();
        for (String dimension : request.dimensions()) {
            addUsage(byKey, dimension, dimension, USAGE_SELECT);
            addUsage(byKey, dimension, dimension, USAGE_GROUP);
        }
        for (BiFilter filter : request.filters()) {
            if (filter != null) {
                addUsage(byKey, filter.field(), filter.field(), USAGE_FILTER);
            }
        }
        for (String metricKey : request.metrics()) {
            addUsage(byKey, metricKey, metricKey, USAGE_SELECT);
            addMetricUsages(byKey, dataset, metricKey, metricKey, USAGE_SELECT);
        }
        for (BiSort sort : request.sorts()) {
            if (sort == null) {
                continue;
            }
            if (dataset.metrics().containsKey(sort.field())) {
                addUsage(byKey, sort.field(), sort.field(), USAGE_SORT);
                addMetricUsages(byKey, dataset, sort.field(), sort.field(), USAGE_SORT);
            } else {
                addUsage(byKey, sort.field(), sort.field(), USAGE_SORT);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private void addMetricUsages(Map<String, FieldUsage> byKey,
                                 BiDatasetSpec dataset,
                                 String metricKey,
                                 String outputKey,
                                 String usage) {
        BiMetricSpec metric = dataset.metrics().get(metricKey);
        if (metric == null || !hasText(metric.expression())) {
            return;
        }
        for (String fieldKey : dataset.fields().keySet()) {
            if (referencesField(metric.expression(), fieldKey)) {
                addUsage(byKey, fieldKey, outputKey, usage);
            }
        }
    }

    private void addUsage(Map<String, FieldUsage> byKey, String fieldKey, String outputKey, String usage) {
        if (!hasText(fieldKey) || !hasText(usage)) {
            return;
        }
        String scopedField = fieldKey.trim();
        String scopedOutput = hasText(outputKey) ? outputKey.trim() : scopedField;
        String scopedUsage = usage.trim().toUpperCase(Locale.ROOT);
        byKey.putIfAbsent(scopedField + "\u0000" + scopedOutput + "\u0000" + scopedUsage,
                new FieldUsage(scopedField, scopedOutput, scopedUsage));
    }

    private boolean referencesField(String expression, String fieldKey) {
        if (!hasText(expression) || !hasText(fieldKey)) {
            return false;
        }
        return Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(fieldKey) + "(?![A-Za-z0-9_])")
                .matcher(expression)
                .find();
    }

    private Object maskedValue(Object value, BiColumnMask mask) {
        Map<String, Object> config = map(mask.maskJson());
        String strategy = upperDefault(String.valueOf(config.getOrDefault("strategy", "PARTIAL")), "PARTIAL");
        if ("NULL".equals(strategy)) {
            return null;
        }
        if ("FIXED".equals(strategy)) {
            return config.getOrDefault("replacement", "***");
        }
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            if (text.length() <= 4) {
                return "***";
            }
            return text.substring(0, 2) + "***" + text.substring(text.length() - 2);
        }
        return null;
    }

    private String signature(Long datasetId, List<String> rowRuleKeys, List<BiColumnMask> masks) {
        List<String> rows = safeList(rowRuleKeys).stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        List<String> maskKeys = safeList(masks).stream()
                .map(mask -> mask.fieldKey() + ":" + mask.sourceFieldKey() + ":" + nullToBlank(mask.maskJson()))
                .sorted()
                .toList();
        return DEFAULT_SIGNATURE
                + ":dataset=" + datasetId
                + ":rows=" + String.join(",", rows)
                + ":masks=" + String.join(",", maskKeys);
    }

    private boolean actionMatches(String policyAction, String requestedAction) {
        String scopedPolicy = upperDefault(policyAction, ACTION_VIEW);
        String scopedRequested = upperDefault(requestedAction, ACTION_VIEW);
        return "*".equals(scopedPolicy) || "ALL".equals(scopedPolicy) || scopedPolicy.equals(scopedRequested);
    }

    private boolean subjectMatches(String subjectType, String subjectId, BiQueryContext context) {
        String scopedType = upperDefault(subjectType, SUBJECT_ALL);
        String scopedId = trim(subjectId);
        if (SUBJECT_ALL.equals(scopedType) || "EVERYONE".equals(scopedType) || "PUBLIC".equals(scopedType)) {
            return true;
        }
        if (SUBJECT_USER.equals(scopedType) || "USER_ID".equals(scopedType) || "ACCOUNT".equals(scopedType)) {
            return "*".equals(scopedId) || context.username().equals(scopedId);
        }
        if (SUBJECT_ROLE.equals(scopedType) || "USER_GROUP".equals(scopedType) || "GROUP".equals(scopedType)) {
            return "*".equals(scopedId) || context.role().equalsIgnoreCase(scopedId);
        }
        return false;
    }

    private boolean defaultAllow(String actionKey) {
        String action = upperDefault(actionKey, ACTION_VIEW);
        return Set.of(ACTION_VIEW, ACTION_USE, ACTION_QUERY, "COMPILE", "EXECUTE").contains(action);
    }

    private int policyRank(String policy) {
        String normalized = upperDefault(policy, POLICY_ALLOW);
        if (POLICY_DENY.equals(normalized)) {
            return 30;
        }
        if (POLICY_MASK.equals(normalized)) {
            return 20;
        }
        return 10;
    }

    private boolean containsAny(Object configured, String value) {
        if (!hasText(value) || configured == null) {
            return false;
        }
        if (configured instanceof Collection<?> values) {
            return values.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .anyMatch(item -> "*".equals(item.trim()) || item.trim().equalsIgnoreCase(value));
        }
        return java.util.Arrays.stream(String.valueOf(configured).split(","))
                .map(String::trim)
                .anyMatch(item -> "*".equals(item) || item.equalsIgnoreCase(value));
    }

    private boolean hasValues(Object configured) {
        if (configured instanceof Collection<?> values) {
            return !values.isEmpty();
        }
        return configured != null && hasText(String.valueOf(configured));
    }

    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private void audit(Long tenantId,
                       Long workspaceId,
                       String actorId,
                       String actionKey,
                       String resourceType,
                       Long resourceId,
                       Map<String, Object> detail) {
        BiAuditLogDO row = new BiAuditLogDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setWorkspaceId(workspaceId);
        row.setActorId(actorId);
        row.setActionKey(actionKey);
        row.setResourceType(resourceType);
        row.setResourceId(resourceId);
        row.setDetailJson(json(detail));
        row.setCreatedAt(LocalDateTime.now());
        try {
            auditLogMapper.insert(row);
        } catch (RuntimeException ignored) {
            // Permission decisions should remain deterministic even if audit storage is unavailable.
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private BiQueryContext scopedContext(BiQueryContext context) {
        return context == null ? new BiQueryContext(0L, "system", RoleNames.OPERATOR) : context;
    }

    private List<Long> tenantScope(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (scopedTenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, scopedTenantId);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
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

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    public record BiPreparedQuery(
            BiQueryRequest request,
            List<BiColumnMask> columnMasks,
            String permissionSignature) {
        public BiPreparedQuery {
            columnMasks = columnMasks == null ? List.of() : List.copyOf(columnMasks);
            permissionSignature = permissionSignature == null ? DEFAULT_SIGNATURE : permissionSignature;
        }
    }

    public record BiColumnMask(
            String fieldKey,
            String sourceFieldKey,
            String maskJson) {
    }

    private record FieldUsage(
            String fieldKey,
            String outputKey,
            String usage) {
    }

    public static class BiPermissionDeniedException extends RuntimeException {
        public BiPermissionDeniedException(String message) {
            super("BI permission denied: " + message);
        }
    }
}
