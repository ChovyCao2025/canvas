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

/**
 * BiQueryGovernancePolicyService 编排 domain.bi.query 场景的领域业务规则。
 */
@Service
public class BiQueryGovernancePolicyService {

    public static final String DEFAULT_DATASET_KEY = "__DEFAULT__";
    private static final String AUDIT_ACTION = "BI_QUERY_GOVERNANCE_POLICY_UPDATE";
    private static final String AUDIT_RESOURCE_TYPE = "BI_QUERY_GOVERNANCE_POLICY";

    private final BiQueryGovernancePolicyMapper mapper;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建 BiQueryGovernancePolicyService 实例并注入 domain.bi.query 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryGovernancePolicyService(BiQueryGovernancePolicyMapper mapper) {
        this(mapper, null, new ObjectMapper());
    }

    /**
     * 创建 BiQueryGovernancePolicyService 实例并注入 domain.bi.query 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiQueryGovernancePolicyService(BiQueryGovernancePolicyMapper mapper,
                                          BiAuditLogMapper auditLogMapper,
                                          ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 读取当前治理策略实体，供查询执行、缓存判断或管理配置使用。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @return 当前生效的治理策略
     */
    public BiQueryGovernancePolicy currentPolicy(Long tenantId) {
        return policyFromRows(policies(tenantId));
    }

    /**
     * 读取当前治理策略视图，用于管理端展示缓存、限流或查询规则配置。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiQueryGovernancePolicyView currentPolicyView(Long tenantId) {
        return view(currentPolicy(tenantId));
    }

    /**
     * 查询最近治理审计记录，用于追踪权限、策略和配置变更。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param limit 本次读取、处理或领取的最大数量
     * @return 符合条件的业务列表
     */
    public List<BiQueryGovernanceAuditEntry> recentAudit(Long tenantId, int limit) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (auditLogMapper == null) {
            return List.of();
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<BiAuditLogDO> rows = auditLogMapper.selectList(
                new LambdaQueryWrapper<BiAuditLogDO>()
                        .eq(BiAuditLogDO::getTenantId, scopedTenantId)
                        .eq(BiAuditLogDO::getActionKey, AUDIT_ACTION)
                        .eq(BiAuditLogDO::getResourceType, AUDIT_RESOURCE_TYPE)
                        .orderByDesc(BiAuditLogDO::getCreatedAt)
                        .last("LIMIT " + boundedLimit));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 创建或更新治理策略，并记录操作者以支撑后续审计和运行时生效。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @param actor 触发策略变更、调度或刷新动作的操作者标识
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiQueryGovernancePolicyView upsertPolicy(
            Long tenantId,
            BiQueryGovernancePolicyUpdateCommand command,
            String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化数据，读取现有配置或写入本次变更。
        BiQueryGovernancePolicyUpdateCommand scopedCommand = command == null
                ? new BiQueryGovernancePolicyUpdateCommand(null, null, List.of())
                : command;
        List<BiQueryGovernancePolicyDO> beforeRows = policies(scopedTenantId);
        BiQueryGovernancePolicy before = policyFromRows(beforeRows);
        // 遍历候选记录并转换为前端或服务层需要的视图。
        Map<String, BiQueryGovernancePolicyDO> existing = beforeRows.stream()
                .filter(row -> row.getDatasetKey() != null)
                .collect(Collectors.toMap(BiQueryGovernancePolicyDO::getDatasetKey, Function.identity(),
                        (left, right) -> right));
        upsert(existing.get(DEFAULT_DATASET_KEY), scopedTenantId, DEFAULT_DATASET_KEY,
                scopedCommand.defaultTimeoutMs(), scopedCommand.defaultQuotaRows(), actor);
        for (BiQueryGovernancePolicyUpdateCommand.DatasetPolicyCommand dataset : scopedCommand.datasets()) {
            // 校验策略输入和默认值，避免无效配置进入持久化或查询流程。
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

    /**
     * 执行数据写入或状态变更。
     *
     * @param existing existing 参数，用于 upsert 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param timeoutMs 时间参数，用于计算窗口、过期或审计时间。
     * @param quotaRows quota rows 参数，用于 upsert 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedBy(actor == null || actor.isBlank() ? "system" : actor.trim());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row.getId() == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
    }

    /**
     * 执行 policies 流程，围绕 policies 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 policies 汇总后的集合、分页或映射视图。
     */
    private List<BiQueryGovernancePolicyDO> policies(Long tenantId) {
        List<BiQueryGovernancePolicyDO> rows = mapper.selectList(
                new LambdaQueryWrapper<BiQueryGovernancePolicyDO>()
                        .eq(BiQueryGovernancePolicyDO::getTenantId, normalizeTenant(tenantId)));
        return rows == null ? List.of() : rows;
    }

    /**
     * 执行 policyFromRows 流程，围绕 policy from rows 完成校验、计算或结果组装。
     *
     * @param rows rows 参数，用于 policyFromRows 流程中的校验、计算或对象转换。
     * @return 返回 policyFromRows 流程生成的业务结果。
     */
    private BiQueryGovernancePolicy policyFromRows(List<BiQueryGovernancePolicyDO> rows) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiQueryGovernancePolicy(defaultTimeoutMs, defaultQuotaRows, datasets);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param policy policy 参数，用于 view 流程中的校验、计算或对象转换。
     * @return 返回 view 流程生成的业务结果。
     */
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

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param before before 参数，用于 auditUpdate 流程中的校验、计算或对象转换。
     * @param after after 参数，用于 auditUpdate 流程中的校验、计算或对象转换。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ignored) {
            // Policy changes should still apply if audit storage is temporarily unavailable.
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param detail detail 参数，用于 toJson 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Object detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return "{}";
        }
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
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private long value(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
