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
import org.chovy.canvas.dal.dataobject.BiWorkspaceMemberDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiColumnPermissionMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiResourcePermissionMapper;
import org.chovy.canvas.dal.mapper.BiRowPermissionMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMemberMapper;
import org.chovy.canvas.domain.bi.portal.BiPortalMenuResource;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiSort;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * Applies workspace, resource, row, and column policies to BI queries and BI resource operations.
 *
 * <p>Tenant-specific policies are evaluated with tenant 0 defaults. Explicit DENY wins over MASK, MASK wins over
 * ALLOW, and the returned permission signature lets query caches isolate policy variants.</p>
 */
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
    private final BiWorkspaceMemberMapper workspaceMemberMapper;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 执行 BiPermissionService 流程，围绕 bi permission service 完成校验、计算或结果组装。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param resourcePermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param rowPermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param columnPermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiPermissionService(BiDatasetMapper datasetMapper,
                               BiResourcePermissionMapper resourcePermissionMapper,
                               BiRowPermissionMapper rowPermissionMapper,
                               BiColumnPermissionMapper columnPermissionMapper,
                               BiAuditLogMapper auditLogMapper,
                               ObjectMapper objectMapper) {
        this(datasetMapper, resourcePermissionMapper, rowPermissionMapper, columnPermissionMapper, null,
                auditLogMapper, objectMapper);
    }

    /**
     * 执行 BiPermissionService 流程，围绕 bi permission service 完成校验、计算或结果组装。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param resourcePermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param rowPermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param columnPermissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param workspaceMemberMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiPermissionService(BiDatasetMapper datasetMapper,
                               BiResourcePermissionMapper resourcePermissionMapper,
                               BiRowPermissionMapper rowPermissionMapper,
                               BiColumnPermissionMapper columnPermissionMapper,
                               BiWorkspaceMemberMapper workspaceMemberMapper,
                               BiAuditLogMapper auditLogMapper,
                               ObjectMapper objectMapper) {
        this.datasetMapper = datasetMapper;
        this.resourcePermissionMapper = resourcePermissionMapper;
        this.rowPermissionMapper = rowPermissionMapper;
        this.columnPermissionMapper = columnPermissionMapper;
        this.workspaceMemberMapper = workspaceMemberMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 prepareQuery 流程，围绕 prepare query 完成校验、计算或结果组装。
     *
     * @param dataset dataset 参数，用于 prepareQuery 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param actionKey 业务键，用于在同一租户下定位资源。
     * @return 返回 prepareQuery 流程生成的业务结果。
     */
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
            // Built-in datasets do not have persisted policy rows, but still need a stable cache signature.
            return new BiPreparedQuery(request, List.of(), DEFAULT_SIGNATURE + ":builtin");
        }
        BiQueryContext effectiveContext = effectiveWorkspaceContext(scopedContext, datasetRow.getWorkspaceId());

        enforceResourceAccess(
                effectiveContext.tenantId(),
                datasetRow.getWorkspaceId(),
                RESOURCE_DATASET,
                datasetRow.getId(),
                effectiveContext,
                scopedAction);

        List<String> rowRuleKeys = new ArrayList<>();
        List<BiFilter> filters = new ArrayList<>(request.filters());
        for (BiRowPermissionDO row : matchingRowPermissions(effectiveContext, datasetRow.getId())) {
            rowRuleKeys.add(row.getRuleKey());
            filters.addAll(parseRowFilters(row.getRuleKey(), row.getFilterJson()));
        }
        BiQueryRequest scopedRequest = new BiQueryRequest(
                request.datasetKey(),
                request.dashboardKey(),
                request.dimensions(),
                request.metrics(),
                filters,
                request.sorts(),
                request.limit(),
                request.offset(),
                request.sqlParameters());

        List<BiColumnMask> masks = evaluateColumnPolicies(dataset, scopedRequest, effectiveContext, datasetRow);
        return new BiPreparedQuery(
                scopedRequest,
                masks,
                signature(datasetRow.getId(), rowRuleKeys, masks));
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param String string 参数，用于 applyMasks 流程中的校验、计算或对象转换。
     * @param rows rows 参数，用于 applyMasks 流程中的校验、计算或对象转换。
     * @param masks masks 参数，用于 applyMasks 流程中的校验、计算或对象转换。
     * @return 返回 applyMasks 流程生成的业务结果。
     */
    public List<Map<String, Object>> applyMasks(List<Map<String, Object>> rows, List<BiColumnMask> masks) {
        if (rows == null || rows.isEmpty() || masks == null || masks.isEmpty()) {
            return rows == null ? List.of() : rows;
        }
        Map<String, BiColumnMask> byField = new LinkedHashMap<>();
        for (BiColumnMask mask : masks) {
            byField.putIfAbsent(mask.fieldKey(), mask);
        }
        // Copy each row before masking so cached or caller-owned result maps are not mutated in place.
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

    /**
     * 执行 enforceResourceAccess 流程，围绕 enforce resource access 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param actionKey 业务键，用于在同一租户下定位资源。
     */
    public void enforceResourceAccess(Long tenantId,
                                      Long workspaceId,
                                      String resourceType,
                                      Long resourceId,
                                      BiQueryContext context,
                                      String actionKey) {
        BiQueryContext scopedContext = effectiveWorkspaceContext(scopedContext(context), workspaceId);
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
        if (!allowed && systemSubscribeAccess(scopedContext, scopedAction)) {
            // Delivery scheduler system jobs may send subscriptions without per-resource grants.
            audit(scopedContext.tenantId(), workspaceId, scopedContext.username(), scopedAction,
                    scopedType, resourceId, Map.of(
                            "decision", EFFECT_ALLOW,
                            "reason", "BI scheduler system account is allowed to deliver subscriptions"));
            return;
        }
        if (!allowed && !defaultAllow(scopedType, scopedAction)) {
            String reason = "BI resource permission is required for " + scopedAction + " on " + scopedType;
            audit(scopedContext.tenantId(), workspaceId, scopedContext.username(), scopedAction,
                    scopedType, resourceId, Map.of("decision", EFFECT_DENY, "reason", reason));
            throw new BiPermissionDeniedException(reason);
        }
    }

    /**
     * 执行 visibleMenus 流程，围绕 visible menus 完成校验、计算或结果组装。
     *
     * @param menus menus 参数，用于 visibleMenus 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 visible menus 汇总后的集合、分页或映射视图。
     */
    public List<BiPortalMenuResource> visibleMenus(List<BiPortalMenuResource> menus, BiQueryContext context) {
        BiQueryContext scopedContext = scopedContext(context);
        return safeList(menus).stream()
                .filter(menu -> isMenuVisible(menu.visibility(), scopedContext))
                .toList();
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param String string 参数，用于 isMenuVisible 流程中的校验、计算或对象转换。
     * @param visibility visibility 参数，用于 isMenuVisible 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回布尔判断结果。
     */
    public boolean isMenuVisible(Map<String, Object> visibility, BiQueryContext context) {
        if (visibility == null || visibility.isEmpty()) {
            return true;
        }
        BiQueryContext scopedContext = scopedContext(context);
        if (containsAny(visibility.get("excludedUsers"), scopedContext.username())
                /**
                 * 判断业务条件是否成立。
                 *
                 * @return 返回 containsAny 流程生成的业务结果。
                 */
                || containsAny(visibility.get("denyUsers"), scopedContext.username())
                /**
                 * 判断业务条件是否成立。
                 *
                 * @return 返回 containsAny 流程生成的业务结果。
                 */
                || containsAny(visibility.get("excludedRoles"), scopedContext.role())
                /**
                 * 判断业务条件是否成立。
                 *
                 * @return 返回 containsAny 流程生成的业务结果。
                 */
                || containsAny(visibility.get("denyRoles"), scopedContext.role())) {
            return false;
        }
        boolean usersConfigured = hasValues(visibility.get("users"));
        boolean rolesConfigured = hasValues(visibility.get("roles"));
        if (!usersConfigured && !rolesConfigured) {
            return true;
        }
        return containsAny(visibility.get("users"), scopedContext.username())
                /**
                 * 判断业务条件是否成立。
                 *
                 * @return 返回 containsAny 流程生成的业务结果。
                 */
                || containsAny(visibility.get("roles"), scopedContext.role());
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param dataset dataset 参数，用于 evaluateColumnPolicies 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param datasetRow 持久化行数据，承载数据库记录内容。
     * @return 返回 evaluate column policies 汇总后的集合、分页或映射视图。
     */
    private List<BiColumnMask> evaluateColumnPolicies(BiDatasetSpec dataset,
                                                      BiQueryRequest request,
                                                      BiQueryContext context,
                                                      BiDatasetDO datasetRow) {
        Map<String, String> policies = effectiveColumnPolicies(context, datasetRow.getId());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (policies.isEmpty()) {
            return List.of();
        }
        List<FieldUsage> usages = collectFieldUsages(dataset, request);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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

    /**
     * 执行 effectiveColumnPolicies 流程，围绕 effective column policies 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @return 返回 effective column policies 生成的文本或业务键。
     */
    private Map<String, String> effectiveColumnPolicies(BiQueryContext context, Long datasetId) {
        Map<String, String> policies = new LinkedHashMap<>();
        for (BiColumnPermissionDO row : matchingColumnPolicyRows(context, datasetId).values()) {
            String fieldKey = trim(row.getFieldKey());
            String policy = upperDefault(row.getPolicy(), POLICY_ALLOW);
            String existing = policies.get(fieldKey);
            // Collapse multiple matching subjects to the strictest policy for each physical field.
            if (POLICY_DENY.equals(existing) || POLICY_DENY.equals(policy)) {
                policies.put(fieldKey, POLICY_DENY);
            // 根据前序判断结果进入后续条件分支。
            } else if (POLICY_MASK.equals(existing) || POLICY_MASK.equals(policy)) {
                policies.put(fieldKey, POLICY_MASK);
            } else {
                policies.put(fieldKey, POLICY_ALLOW);
            }
        }
        return policies;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @return 返回布尔判断结果。
     */
    private Map<String, BiColumnPermissionDO> matchingColumnPolicyRows(BiQueryContext context, Long datasetId) {
        Map<String, BiColumnPermissionDO> rows = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiColumnPermissionDO row : safeList(columnPermissionMapper.selectList(
                new LambdaQueryWrapper<BiColumnPermissionDO>()
                        .in(BiColumnPermissionDO::getTenantId, tenantScope(context.tenantId()))
                        .eq(BiColumnPermissionDO::getDatasetId, datasetId)
                        .eq(BiColumnPermissionDO::getEnabled, true)
                        .orderByAsc(BiColumnPermissionDO::getTenantId)
                        .orderByAsc(BiColumnPermissionDO::getFieldKey)))) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (!subjectMatches(row.getSubjectType(), row.getSubjectId(), context) || !hasText(row.getFieldKey())) {
                continue;
            }
            String fieldKey = row.getFieldKey().trim();
            BiColumnPermissionDO existing = rows.get(fieldKey);
            if (existing == null || policyRank(row.getPolicy()) > policyRank(existing.getPolicy())) {
                rows.put(fieldKey, row);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return rows;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiDatasetDO findDataset(Long tenantId, String datasetKey) {
        return datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .in(BiDatasetDO::getTenantId, tenantScope(tenantId))
                .eq(BiDatasetDO::getDatasetKey, required(datasetKey, "datasetKey"))
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                .orderByDesc(BiDatasetDO::getTenantId)
                .last("LIMIT 1"));
    }

    /**
     * 解析并校验输入数据。
     *
     * @param ruleKey 业务键，用于在同一租户下定位资源。
     * @param filterJson JSON 字符串，承载结构化配置或明细。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<BiFilter> parseRowFilters(String ruleKey, String filterJson) {
        if (!hasText(filterJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(filterJson);
            List<BiFilter> filters = new ArrayList<>();
            collectFilters(ruleKey, root, filters);
            return filters;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid BI row permission filter for rule " + ruleKey, e);
        }
    }

    /**
     * 执行 collectFilters 流程，围绕 collect filters 完成校验、计算或结果组装。
     *
     * @param ruleKey 业务键，用于在同一租户下定位资源。
     * @param node node 参数，用于 collectFilters 流程中的校验、计算或对象转换。
     * @param filters filters 参数，用于 collectFilters 流程中的校验、计算或对象转换。
     */
    private void collectFilters(String ruleKey, JsonNode node, List<BiFilter> filters) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        Object value = objectMapper.convertValue(valueNode, Object.class);
        BiFilter.Operator scopedOperator = BiFilter.Operator.valueOf(operator.trim().toUpperCase(Locale.ROOT));
        if (scopedOperator == BiFilter.Operator.IN && !(value instanceof Collection<?>)) {
            value = List.of(value);
        }
        filters.add(new BiFilter(field, scopedOperator, value));
    }

    /**
     * 执行 collectFieldUsages 流程，围绕 collect field usages 完成校验、计算或结果组装。
     *
     * @param dataset dataset 参数，用于 collectFieldUsages 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 collect field usages 汇总后的集合、分页或映射视图。
     */
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
            // A metric can expose source fields through its expression even when the output key differs.
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

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param byKey 业务键，用于在同一租户下定位资源。
     * @param dataset dataset 参数，用于 addMetricUsages 流程中的校验、计算或对象转换。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @param outputKey 业务键，用于在同一租户下定位资源。
     * @param usage usage 参数，用于 addMetricUsages 流程中的校验、计算或对象转换。
     */
    private void addMetricUsages(Map<String, FieldUsage> byKey,
                                 BiDatasetSpec dataset,
                                 String metricKey,
                                 String outputKey,
                                 String usage) {
        BiMetricSpec metric = dataset.metrics().get(metricKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (metric == null || !hasText(metric.expression())) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String fieldKey : dataset.fields().keySet()) {
            if (referencesField(metric.expression(), fieldKey)) {
                addUsage(byKey, fieldKey, outputKey, usage);
            }
        }
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 addUsage 流程中的校验、计算或对象转换。
     * @param byKey 业务键，用于在同一租户下定位资源。
     * @param fieldKey 业务键，用于在同一租户下定位资源。
     * @param outputKey 业务键，用于在同一租户下定位资源。
     * @param usage usage 参数，用于 addUsage 流程中的校验、计算或对象转换。
     */
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

    /**
     * 执行 referencesField 流程，围绕 references field 完成校验、计算或结果组装。
     *
     * @param expression expression 参数，用于 referencesField 流程中的校验、计算或对象转换。
     * @param fieldKey 业务键，用于在同一租户下定位资源。
     * @return 返回 references field 的布尔判断结果。
     */
    private boolean referencesField(String expression, String fieldKey) {
        if (!hasText(expression) || !hasText(fieldKey)) {
            return false;
        }
        return Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(fieldKey) + "(?![A-Za-z0-9_])")
                .matcher(expression)
                .find();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param mask mask 参数，用于 maskedValue 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Object maskedValue(Object value, BiColumnMask mask) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> config = map(mask.maskJson());
        String strategy = upperDefault(String.valueOf(config.getOrDefault("strategy", "PARTIAL")), "PARTIAL");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @param rowRuleKeys row rule keys 参数，用于 signature 流程中的校验、计算或对象转换。
     * @param masks masks 参数，用于 signature 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String signature(Long datasetId, List<String> rowRuleKeys, List<BiColumnMask> masks) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<String> rows = safeList(rowRuleKeys).stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        List<String> maskKeys = safeList(masks).stream()
                .map(mask -> mask.fieldKey() + ":" + mask.sourceFieldKey() + ":" + nullToBlank(mask.maskJson()))
                .sorted()
                .toList();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return DEFAULT_SIGNATURE
                + ":dataset=" + datasetId
                + ":rows=" + String.join(",", rows)
                + ":masks=" + String.join(",", maskKeys);
    }

    /**
     * 执行 actionMatches 流程，围绕 action matches 完成校验、计算或结果组装。
     *
     * @param policyAction policy action 参数，用于 actionMatches 流程中的校验、计算或对象转换。
     * @param requestedAction requested action 参数，用于 actionMatches 流程中的校验、计算或对象转换。
     * @return 返回 action matches 的布尔判断结果。
     */
    private boolean actionMatches(String policyAction, String requestedAction) {
        String scopedPolicy = upperDefault(policyAction, ACTION_VIEW);
        String scopedRequested = upperDefault(requestedAction, ACTION_VIEW);
        return "*".equals(scopedPolicy) || "ALL".equals(scopedPolicy) || scopedPolicy.equals(scopedRequested);
    }

    /**
     * 执行 subjectMatches 流程，围绕 subject matches 完成校验、计算或结果组装。
     *
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectId 业务对象 ID，用于定位具体记录。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 subject matches 的布尔判断结果。
     */
    private boolean subjectMatches(String subjectType, String subjectId, BiQueryContext context) {
        // 准备本次处理所需的上下文和中间变量。
        String scopedType = upperDefault(subjectType, SUBJECT_ALL);
        String scopedId = trim(subjectId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (SUBJECT_ALL.equals(scopedType) || "EVERYONE".equals(scopedType) || "PUBLIC".equals(scopedType)) {
            return true;
        }
        if (SUBJECT_USER.equals(scopedType) || "USER_ID".equals(scopedType) || "ACCOUNT".equals(scopedType)) {
            return "*".equals(scopedId) || context.username().equals(scopedId);
        }
        if (SUBJECT_ROLE.equals(scopedType) || "USER_GROUP".equals(scopedType) || "GROUP".equals(scopedType)) {
            return "*".equals(scopedId) || context.role().equalsIgnoreCase(scopedId);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return false;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param actionKey 业务键，用于在同一租户下定位资源。
     * @return 返回 default allow 的布尔判断结果。
     */
    private boolean defaultAllow(String resourceType, String actionKey) {
        if ("DATASOURCE".equals(upperDefault(resourceType, ""))) {
            return false;
        }
        String action = upperDefault(actionKey, ACTION_VIEW);
        return Set.of(ACTION_VIEW, ACTION_USE, ACTION_QUERY, "COMPILE", "EXECUTE").contains(action);
    }

    /**
     * 执行 systemSubscribeAccess 流程，围绕 system subscribe access 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param actionKey 业务键，用于在同一租户下定位资源。
     * @return 返回 system subscribe access 的布尔判断结果。
     */
    private boolean systemSubscribeAccess(BiQueryContext context, String actionKey) {
        return context != null
                && ACTION_SUBSCRIBE.equals(upperDefault(actionKey, ACTION_VIEW))
                && "SYSTEM".equalsIgnoreCase(context.role());
    }

    /**
     * 执行 policyRank 流程，围绕 policy rank 完成校验、计算或结果组装。
     *
     * @param policy policy 参数，用于 policyRank 流程中的校验、计算或对象转换。
     * @return 返回 policy rank 计算得到的数量、金额或指标值。
     */
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

    /**
     * 判断业务条件是否成立。
     *
     * @param configured configured 参数，用于 containsAny 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 contains any 的布尔判断结果。
     */
    private boolean containsAny(Object configured, String value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(value) || configured == null) {
            return false;
        }
        if (configured instanceof Collection<?> values) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return values.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .anyMatch(item -> "*".equals(item.trim()) || item.trim().equalsIgnoreCase(value));
        }
        return java.util.Arrays.stream(String.valueOf(configured).split(","))
                .map(String::trim)
                .anyMatch(item -> "*".equals(item) || item.equalsIgnoreCase(value));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param configured configured 参数，用于 hasValues 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasValues(Object configured) {
        if (configured instanceof Collection<?> values) {
            return !values.isEmpty();
        }
        return configured != null && hasText(String.valueOf(configured));
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param actorId 业务对象 ID，用于定位具体记录。
     * @param actionKey 业务键，用于在同一租户下定位资源。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param detail detail 参数，用于 audit 流程中的校验、计算或对象转换。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ignored) {
            // Permission decisions should remain deterministic even if audit storage is unavailable.
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * 执行 scopedContext 流程，围绕 scoped context 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 scopedContext 流程生成的业务结果。
     */
    private BiQueryContext scopedContext(BiQueryContext context) {
        return context == null ? new BiQueryContext(0L, "system", RoleNames.OPERATOR) : context;
    }

    /**
     * 执行 effectiveWorkspaceContext 流程，围绕 effective workspace context 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @return 返回 effectiveWorkspaceContext 流程生成的业务结果。
     */
    private BiQueryContext effectiveWorkspaceContext(BiQueryContext context, Long workspaceId) {
        BiQueryContext scoped = scopedContext(context);
        if (workspaceMemberMapper == null || workspaceId == null || workspaceId <= 0 || !hasText(scoped.username())) {
            return scoped;
        }
        BiWorkspaceMemberDO member = workspaceMemberMapper.selectOne(new LambdaQueryWrapper<BiWorkspaceMemberDO>()
                .in(BiWorkspaceMemberDO::getTenantId, tenantScope(scoped.tenantId()))
                .eq(BiWorkspaceMemberDO::getWorkspaceId, workspaceId)
                .eq(BiWorkspaceMemberDO::getUserId, scoped.username())
                .orderByDesc(BiWorkspaceMemberDO::getTenantId)
                .last("LIMIT 1"));
        if (member == null || !hasText(member.getRoleKey())) {
            return scoped;
        }
        return new BiQueryContext(scoped.tenantId(), scoped.username(), member.getRoleKey().trim());
    }

    /**
     * 执行 tenantScope 流程，围绕 tenant scope 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant scope 汇总后的集合、分页或映射视图。
     */
    private List<Long> tenantScope(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (scopedTenantId == 0L) {
            return List.of(0L);
        }
        // Include tenant 0 defaults with scoped tenant rows; each query chooses the ordering it needs.
        return List.of(0L, scopedTenantId);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 执行 upperRequired 流程，围绕 upper required 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 upper required 生成的文本或业务键。
     */
    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 upperDefault 流程，围绕 upper default 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 upper default 生成的文本或业务键。
     */
    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to blank 生成的文本或业务键。
     */
    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * BiPreparedQuery 数据记录。
     */
    public record BiPreparedQuery(
            BiQueryRequest request,
            List<BiColumnMask> columnMasks,
            String permissionSignature) {
        public BiPreparedQuery {
            columnMasks = columnMasks == null ? List.of() : List.copyOf(columnMasks);
            permissionSignature = permissionSignature == null ? DEFAULT_SIGNATURE : permissionSignature;
        }
    }

    /**
     * BiColumnMask 数据记录。
     */
    public record BiColumnMask(
            String fieldKey,
            String sourceFieldKey,
            String maskJson) {
    }

    /**
     * FieldUsage 数据记录。
     */
    private record FieldUsage(
            String fieldKey,
            String outputKey,
            String usage) {
    }

    /**
     * BiPermissionDeniedException 承载对应领域的业务规则、流程编排和结果转换。
     */
    public static class BiPermissionDeniedException extends RuntimeException {
        /**
         * 执行 BiPermissionDeniedException 流程，围绕 bi permission denied exception 完成校验、计算或结果组装。
         *
         * @param message 原因或消息文本，用于记录状态变化的业务依据。
         * @return 返回 BiPermissionDeniedException 流程生成的业务结果。
         */
        public BiPermissionDeniedException(String message) {
            super("BI permission denied: " + message);
        }
    }
}
