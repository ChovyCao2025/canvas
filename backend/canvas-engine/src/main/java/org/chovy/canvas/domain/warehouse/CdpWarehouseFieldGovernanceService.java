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
/**
 * CdpWarehouseFieldGovernanceService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 CdpWarehouseFieldGovernanceService 实例。
     *
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseFieldGovernanceService(CdpWarehouseFieldPolicyMapper policyMapper,
                                              CdpWarehouseFieldAccessAuditMapper auditMapper) {
        this.policyMapper = policyMapper;
        this.auditMapper = auditMapper;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public FieldPolicyView upsertPolicy(Long tenantId, FieldPolicyCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        policyMapper.upsert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param lifecycleStatus 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<FieldPolicyView> listPolicies(Long tenantId, String datasetKey, String lifecycleStatus) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseFieldPolicyDO> query =
                new LambdaQueryWrapper<CdpWarehouseFieldPolicyDO>()
                        .in(CdpWarehouseFieldPolicyDO::getTenantId, tenantScope(scopedTenantId))
                        .orderByAsc(CdpWarehouseFieldPolicyDO::getTenantId)
                        .orderByAsc(CdpWarehouseFieldPolicyDO::getDatasetKey)
                        .orderByAsc(CdpWarehouseFieldPolicyDO::getFieldKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(datasetKey)) {
            query.eq(CdpWarehouseFieldPolicyDO::getDatasetKey, datasetKey.trim());
        }
        if (hasText(lifecycleStatus)) {
            query.eq(CdpWarehouseFieldPolicyDO::getLifecycleStatus,
                    lifecycleStatus.trim().toUpperCase(Locale.ROOT));
        }

        Map<String, FieldPolicyView> merged = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseFieldPolicyDO row : safeList(policyMapper.selectList(query))) {
            merged.put(policyKey(row.getDatasetKey(), row.getFieldKey()), toView(row));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ArrayList<>(merged.values());
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param dataset dataset 参数，用于 evaluateBiQuery 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param actionKey 业务键，用于在同一租户下定位资源。
     * @return 返回 evaluateBiQuery 流程生成的业务结果。
     */
    public BiPolicyEvaluation evaluateBiQuery(BiDatasetSpec dataset,
                                              BiQueryRequest request,
                                              BiQueryContext context,
                                              String actionKey) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return evaluation;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataset dataset 参数，用于 enforceBiQuery 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param actionKey 业务键，用于在同一租户下定位资源。
     */
    public void enforceBiQuery(BiDatasetSpec dataset,
                               BiQueryRequest request,
                               BiQueryContext context,
                               String actionKey) {
        BiPolicyEvaluation evaluation = evaluateBiQuery(dataset, request, context, actionKey);
        if (!evaluation.allowed()) {
            throw new FieldAccessDeniedException(evaluation.reason());
        }
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param usage usage 参数，用于 decide 流程中的校验、计算或对象转换。
     * @param policy policy 参数，用于 decide 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 decide 流程生成的业务结果。
     */
    private FieldUsageDecision decide(FieldUsage usage,
                                      CdpWarehouseFieldPolicyDO policy,
                                      BiQueryContext context) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new FieldUsageDecision(
                usage.fieldKey(),
                usage.usage(),
                DECISION_ALLOW,
                "field policy allowed",
                piiLevel,
                accessPolicy,
                minRole);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param usage usage 参数，用于 denied 流程中的校验、计算或对象转换。
     * @param policy policy 参数，用于 denied 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 denied 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 activePolicyMap 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataset dataset 参数，用于 collectFieldUsages 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 collect field usages 汇总后的集合、分页或映射视图。
     */
    private List<FieldUsage> collectFieldUsages(BiDatasetSpec dataset, BiQueryRequest request) {
        Map<String, FieldUsage> byKey = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String dimension : request.dimensions()) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            addUsage(byKey, dimension, USAGE_SELECT);
            addUsage(byKey, dimension, USAGE_GROUP);
        }
        for (BiFilter filter : request.filters()) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param byKey 业务键，用于在同一租户下定位资源。
     * @param dataset dataset 参数，用于 addMetricUsages 流程中的校验、计算或对象转换。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @param usage usage 参数，用于 addMetricUsages 流程中的校验、计算或对象转换。
     */
    private void addMetricUsages(Map<String, FieldUsage> byKey,
                                 BiDatasetSpec dataset,
                                 String metricKey,
                                 String usage) {
        addUsage(byKey, metricKey, usage);
        BiMetricSpec metric = dataset.metrics().get(metricKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (metric == null || !hasText(metric.expression())) {
            return;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String fieldKey : dataset.fields().keySet()) {
            if (referencesField(metric.expression(), fieldKey)) {
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                addUsage(byKey, fieldKey, USAGE_SELECT);
            }
        }
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param MapString map string 参数，用于 addUsage 流程中的校验、计算或对象转换。
     * @param byKey 业务键，用于在同一租户下定位资源。
     * @param fieldKey 业务键，用于在同一租户下定位资源。
     * @param usage usage 参数，用于 addUsage 流程中的校验、计算或对象转换。
     */
    private void addUsage(Map<String, FieldUsage> byKey, String fieldKey, String usage) {
        if (!hasText(fieldKey) || !hasText(usage)) {
            return;
        }
        String scopedUsage = usage.trim().toUpperCase(Locale.ROOT);
        byKey.putIfAbsent(fieldKey.trim() + "\u0000" + scopedUsage,
                new FieldUsage(fieldKey.trim(), scopedUsage));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evaluation evaluation 参数，用于 auditDenied 流程中的校验、计算或对象转换。
     * @param denied denied 参数，用于 auditDenied 流程中的校验、计算或对象转换。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 known access policy 的布尔判断结果。
     */
    private boolean knownAccessPolicy(String value) {
        return POLICY_ALLOW.equals(value) || POLICY_DENY.equals(value) || POLICY_MASK.equals(value);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 allowed usages 汇总后的集合、分页或映射视图。
     */
    private Set<String> allowedUsages(String value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(value)) {
            return defaultUsages();
        }
        Set<String> usages = new LinkedHashSet<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String part : value.split(",")) {
            if (hasText(part)) {
                usages.add(part.trim().toUpperCase(Locale.ROOT));
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return usages.isEmpty() ? defaultUsages() : usages;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @return 返回 default usages 汇总后的集合、分页或映射视图。
     */
    private Set<String> defaultUsages() {
        Set<String> usages = new LinkedHashSet<>();
        usages.add(USAGE_SELECT);
        usages.add(USAGE_FILTER);
        usages.add(USAGE_SORT);
        usages.add(USAGE_GROUP);
        return usages;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @return 返回 role rank 计算得到的数量、金额或指标值。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizedUsages(String value) {
        return String.join(",", allowedUsages(value));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param fieldKey 业务键，用于在同一租户下定位资源。
     * @return 返回 policy key 生成的文本或业务键。
     */
    private String policyKey(String datasetKey, String fieldKey) {
        return datasetKey + "\u0000" + fieldKey;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant scope 汇总后的集合、分页或映射视图。
     */
    private List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
    }

    /**
     * 校验输入、权限或业务前置条件。
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 upper required 生成的文本或业务键。
     */
    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 upper default 生成的文本或业务键。
     */
    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 upper blank to null 生成的文本或业务键。
     */
    private String upperBlankToNull(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param maxLength max length 参数，用于 truncate 流程中的校验、计算或对象转换。
     * @return 返回 truncate 生成的文本或业务键。
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * FieldAccessDeniedException 承载对应领域的业务规则、流程编排和结果转换。
     */
    public static class FieldAccessDeniedException extends RuntimeException {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param message 原因或消息文本，用于记录状态变化的业务依据。
         * @return 返回 FieldAccessDeniedException 流程生成的业务结果。
         */
        public FieldAccessDeniedException(String message) {
            super("Field access denied: " + message);
        }
    }

    /**
     * FieldUsage 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record FieldUsage(String fieldKey, String usage) {
    }

    /**
     * FieldPolicyCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * FieldPolicyView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * BiPolicyEvaluation 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * FieldUsageDecision 承载对应领域的业务规则、流程编排和结果转换。
     */
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
