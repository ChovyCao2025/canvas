package org.chovy.canvas.domain.bi.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiQueryCachePolicyDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiQueryCachePolicyMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * BiQueryCachePolicyService 编排 domain.bi.query 场景的领域业务规则。
 */
@Service
public class BiQueryCachePolicyService {

    private static final String AUDIT_ACTION = "BI_QUERY_CACHE_POLICY_UPDATE";
    private static final String AUDIT_RESOURCE_TYPE = "BI_QUERY_CACHE_POLICY";

    private final BiQueryCachePolicyMapper mapper;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final boolean defaultEnabled;
    private final long defaultTtlSeconds;
    private final BiQueryResultCache resultCache;

    /**
     * 创建 BiQueryCachePolicyService 实例并注入 domain.bi.query 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param defaultEnabled default enabled 参数，用于 BiQueryCachePolicyService 流程中的校验、计算或对象转换。
     * @param defaultTtlSeconds default ttl seconds 参数，用于 BiQueryCachePolicyService 流程中的校验、计算或对象转换。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public BiQueryCachePolicyService(BiQueryCachePolicyMapper mapper,
                                     BiAuditLogMapper auditLogMapper,
                                     ObjectMapper objectMapper,
                                     @Value("${canvas.bi.query.cache.enabled:true}") boolean defaultEnabled,
                                     @Value("${canvas.bi.query.cache.ttl-seconds:300}") long defaultTtlSeconds,
                                     BiQueryResultCache resultCache) {
        this.mapper = mapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.defaultEnabled = defaultEnabled;
        this.defaultTtlSeconds = Math.max(1, defaultTtlSeconds);
        this.resultCache = resultCache == null ? BiQueryResultCache.noop() : resultCache;
    }

    /**
     * 读取当前治理策略实体，供查询执行、缓存判断或管理配置使用。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @return 当前生效的治理策略
     */
    public BiQueryCachePolicy currentPolicy(Long tenantId) {
        return policyFromRows(policies(tenantId));
    }

    /**
     * 读取当前治理策略视图，用于管理端展示缓存、限流或查询规则配置。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiQueryCachePolicyView currentPolicyView(Long tenantId) {
        return view(currentPolicy(tenantId));
    }

    /**
     * 计算某个数据集或仪表盘查询实际应使用的缓存策略。
     *
     * <p>方法先读取租户当前策略，再优先按 dashboardKey 匹配仪表盘级配置；未提供仪表盘时按 datasetKey 匹配数据集级配置，
     * 资源未配置则回落到默认策略。该方法只读策略，不写审计日志，也不直接操作查询结果缓存。</p>
     *
     * @param tenantId 租户 ID，用于隔离策略配置
     * @param datasetKey 数据集标识，未命中仪表盘策略时用于匹配数据集策略
     * @param dashboardKey 仪表盘标识，非空时优先匹配仪表盘策略
     * @return 查询执行时应使用的资源级缓存开关、TTL 和缓存模式
     */
    public BiQueryCachePolicy.ResourcePolicy effectivePolicy(Long tenantId, String datasetKey, String dashboardKey) {
        BiQueryCachePolicy policy = currentPolicy(tenantId);
        if (dashboardKey != null && !dashboardKey.isBlank()) {
            return policy.effectiveForDashboard(dashboardKey);
        }
        return policy.effectiveForDataset(datasetKey);
    }

    /**
     * 创建或更新治理策略，并记录操作者以支撑后续审计和运行时生效。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @param actor 触发策略变更、调度或刷新动作的操作者标识
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiQueryCachePolicyView upsertPolicy(Long tenantId,
                                               BiQueryCachePolicyUpdateCommand command,
                                               String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQueryCachePolicy before = currentPolicy(scopedTenantId);
        // 遍历候选记录并转换为前端或服务层需要的视图。
        Map<String, BiQueryCachePolicyDO> existing = policies(scopedTenantId).stream()
                .collect(Collectors.toMap(this::resourceId, Function.identity(), (left, right) -> right));
        // 访问持久化数据，读取现有配置或写入本次变更。
        BiQueryCachePolicyUpdateCommand safeCommand = command == null
                ? new BiQueryCachePolicyUpdateCommand(null, null, null, List.of())
                : command;
        upsert(existing.get(resourceId(BiQueryCachePolicy.TYPE_DEFAULT, BiQueryCachePolicy.DEFAULT_RESOURCE_KEY)),
                scopedTenantId,
                BiQueryCachePolicy.TYPE_DEFAULT,
                BiQueryCachePolicy.DEFAULT_RESOURCE_KEY,
                value(safeCommand.defaultEnabled(), before.defaultEnabled()),
                value(safeCommand.defaultTtlSeconds(), before.defaultTtlSeconds()),
                normalizeCacheMode(value(safeCommand.defaultCacheMode(), before.defaultCacheMode())),
                actor);
        for (BiQueryCachePolicyUpdateCommand.ResourcePolicyCommand resource : safeCommand.resources()) {
            // 校验策略输入和默认值，避免无效配置进入持久化或查询流程。
            if (resource == null || isBlank(resource.resourceType()) || isBlank(resource.resourceKey())) {
                continue;
            }
            String type = resource.resourceType().trim().toUpperCase(Locale.ROOT);
            String key = resource.resourceKey().trim();
            BiQueryCachePolicy.ResourcePolicy fallback = BiQueryCachePolicy.TYPE_DASHBOARD.equals(type)
                    ? before.effectiveForDashboard(key)
                    : before.effectiveForDataset(key);
            upsert(existing.get(resourceId(type, key)),
                    scopedTenantId,
                    type,
                    key,
                    value(resource.enabled(), fallback.enabled()),
                    value(resource.ttlSeconds(), fallback.ttlSeconds()),
                    normalizeCacheMode(value(resource.cacheMode(), fallback.cacheMode())),
                    actor);
        }
        BiQueryCachePolicy after = currentPolicy(scopedTenantId);
        auditUpdate(scopedTenantId, actor, before, after);
        return view(after);
    }

    /**
     * 按规则清理 BI 查询结果缓存，使数据集、租户或全局缓存立即失效。
     *
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 缓存失效范围和清理数量结果
     */
    public BiQueryCacheInvalidationResult invalidate(BiQueryCacheInvalidationCommand command) {
        String scope = command == null || isBlank(command.scope())
                ? "ALL"
                : command.scope().trim().toUpperCase(Locale.ROOT);
        return switch (scope) {
            case "SQL_HASH" -> {
                String sqlHash = command == null ? null : command.sqlHash();
                int deleted = resultCache.evict(sqlHash) ? 1 : 0;
                /**
                 * 执行 BiQueryCacheInvalidationResult 流程，围绕 bi query cache invalidation result 完成校验、计算或结果组装。
                 *
                 * @param scope scope 参数，用于 BiQueryCacheInvalidationResult 流程中的校验、计算或对象转换。
                 * @param deleted deleted 参数，用于 BiQueryCacheInvalidationResult 流程中的校验、计算或对象转换。
                 * @param sqlHash sql hash 参数，用于 BiQueryCacheInvalidationResult 流程中的校验、计算或对象转换。
                 * @return 返回 BiQueryCacheInvalidationResult 流程生成的业务结果。
                 */
                yield new BiQueryCacheInvalidationResult(scope, deleted,
                        deleted == 0 ? "no cache entry matched " + sqlHash : "cleared sql hash " + sqlHash);
            }
            case "DATASET" -> {
                String datasetKey = command == null ? null : command.datasetKey();
                int deleted = resultCache.evictDataset(datasetKey);
                /**
                 * 执行 BiQueryCacheInvalidationResult 流程，围绕 bi query cache invalidation result 完成校验、计算或结果组装。
                 *
                 * @param scope scope 参数，用于 BiQueryCacheInvalidationResult 流程中的校验、计算或对象转换。
                 * @param deleted deleted 参数，用于 BiQueryCacheInvalidationResult 流程中的校验、计算或对象转换。
                 * @param datasetKey 业务键，用于在同一租户下定位资源。
                 * @return 返回 BiQueryCacheInvalidationResult 流程生成的业务结果。
                 */
                yield new BiQueryCacheInvalidationResult(scope, deleted,
                        "cleared dataset " + datasetKey);
            }
            case "ALL" -> {
                int deleted = resultCache.clear();
                /**
                 * 执行 BiQueryCacheInvalidationResult 流程，围绕 bi query cache invalidation result 完成校验、计算或结果组装。
                 *
                 * @param scope scope 参数，用于 BiQueryCacheInvalidationResult 流程中的校验、计算或对象转换。
                 * @param deleted deleted 参数，用于 BiQueryCacheInvalidationResult 流程中的校验、计算或对象转换。
                 * @return 返回 BiQueryCacheInvalidationResult 流程生成的业务结果。
                 */
                yield new BiQueryCacheInvalidationResult(scope, deleted, "cleared all BI query cache entries");
            }
            default -> throw new IllegalArgumentException("unsupported cache invalidation scope: " + scope);
        };
    }

    /**
     * 读取查询缓存统计信息，用于观察缓存命中、容量和失效治理效果。
     *
     * @return 查询缓存命中、容量和失效统计
     */
    public BiQueryCacheStats cacheStats() {
        return resultCache.stats();
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param existing existing 参数，用于 upsert 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param enabled enabled 参数，用于 upsert 流程中的校验、计算或对象转换。
     * @param ttlSeconds ttl seconds 参数，用于 upsert 流程中的校验、计算或对象转换。
     * @param cacheMode 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void upsert(BiQueryCachePolicyDO existing,
                        Long tenantId,
                        String resourceType,
                        String resourceKey,
                        boolean enabled,
                        long ttlSeconds,
                        String cacheMode,
                        String actor) {
        BiQueryCachePolicyDO row = existing == null ? new BiQueryCachePolicyDO() : existing;
        row.setTenantId(tenantId);
        row.setResourceType(resourceType);
        row.setResourceKey(resourceKey);
        row.setEnabled(enabled);
        row.setTtlSeconds(Math.max(1, ttlSeconds));
        row.setCacheMode(normalizeCacheMode(cacheMode));
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
    private List<BiQueryCachePolicyDO> policies(Long tenantId) {
        List<BiQueryCachePolicyDO> rows = mapper.selectList(
                new LambdaQueryWrapper<BiQueryCachePolicyDO>()
                        .eq(BiQueryCachePolicyDO::getTenantId, normalizeTenant(tenantId)));
        return rows == null ? List.of() : rows;
    }

    /**
     * 执行 policyFromRows 流程，围绕 policy from rows 完成校验、计算或结果组装。
     *
     * @param rows rows 参数，用于 policyFromRows 流程中的校验、计算或对象转换。
     * @return 返回 policyFromRows 流程生成的业务结果。
     */
    private BiQueryCachePolicy policyFromRows(List<BiQueryCachePolicyDO> rows) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        BiQueryCachePolicyDO defaultRow = rows.stream()
                .filter(row -> BiQueryCachePolicy.TYPE_DEFAULT.equals(normalizeType(row.getResourceType())))
                .findFirst()
                .orElse(null);
        boolean enabled = defaultRow == null || defaultRow.getEnabled() == null
                ? defaultEnabled
                : defaultRow.getEnabled();
        long ttlSeconds = defaultRow == null || defaultRow.getTtlSeconds() == null
                ? defaultTtlSeconds
                : defaultRow.getTtlSeconds();
        String cacheMode = defaultRow == null ? "CACHE" : normalizeCacheMode(defaultRow.getCacheMode());
        List<BiQueryCachePolicy.ResourcePolicy> resources = rows.stream()
                .filter(row -> !BiQueryCachePolicy.TYPE_DEFAULT.equals(normalizeType(row.getResourceType())))
                .sorted(Comparator.comparing(row -> resourceId(row.getResourceType(), row.getResourceKey())))
                .map(row -> new BiQueryCachePolicy.ResourcePolicy(
                        normalizeType(row.getResourceType()),
                        row.getResourceKey(),
                        row.getEnabled() == null ? enabled : row.getEnabled(),
                        row.getTtlSeconds() == null ? ttlSeconds : row.getTtlSeconds(),
                        row.getCacheMode() == null ? cacheMode : row.getCacheMode()))
                .toList();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiQueryCachePolicy(enabled, ttlSeconds, cacheMode, resources);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param policy policy 参数，用于 view 流程中的校验、计算或对象转换。
     * @return 返回 view 流程生成的业务结果。
     */
    private BiQueryCachePolicyView view(BiQueryCachePolicy policy) {
        return new BiQueryCachePolicyView(
                policy.defaultEnabled(),
                policy.defaultTtlSeconds(),
                policy.defaultCacheMode(),
                policy.resources().stream()
                        .map(resource -> new BiQueryCachePolicyView.ResourcePolicyView(
                                resource.resourceType(),
                                resource.resourceKey(),
                                resource.enabled(),
                                resource.ttlSeconds(),
                                resource.cacheMode()))
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
                             BiQueryCachePolicy before,
                             BiQueryCachePolicy after) {
        if (auditLogMapper == null) {
            return;
        }
        BiAuditLogDO row = new BiAuditLogDO();
        row.setTenantId(tenantId);
        row.setActorId(actor == null || actor.isBlank() ? "system" : actor.trim());
        row.setActionKey(AUDIT_ACTION);
        row.setResourceType(AUDIT_RESOURCE_TYPE);
        row.setDetailJson(toJson(Map.of("before", before, "after", after)));
        row.setCreatedAt(LocalDateTime.now());
        try {
            auditLogMapper.insert(row);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ignored) {
            // Cache policy updates must still apply if audit persistence is unavailable.
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
     * 执行 resourceId 流程，围绕 resource id 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 resource id 生成的文本或业务键。
     */
    private String resourceId(BiQueryCachePolicyDO row) {
        return resourceId(row.getResourceType(), row.getResourceKey());
    }

    /**
     * 执行 resourceId 流程，围绕 resource id 完成校验、计算或结果组装。
     *
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @return 返回 resource id 生成的文本或业务键。
     */
    private String resourceId(String resourceType, String resourceKey) {
        return normalizeType(resourceType) + ":" + (resourceKey == null ? "" : resourceKey.trim());
    }

    /**
     * 规范化输入值。
     *
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return BiQueryCachePolicy.TYPE_DATASET;
        }
        return resourceType.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 归一化管理端提交的缓存模式。
     *
     * <p>当前服务层只负责大小写和空值归一化，最终是否允许该模式由查询执行链路解释；
     * 空值统一回退到 CACHE，避免历史配置缺字段时改变默认缓存口径。</p>
     */
    private String normalizeCacheMode(String cacheMode) {
        if (cacheMode == null || cacheMode.isBlank()) {
            return "CACHE";
        }
        return cacheMode.trim().toUpperCase(Locale.ROOT);
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
     * @return 返回 value 的布尔判断结果。
     */
    private boolean value(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private long value(Long value, long fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    /**
     * 执行 value 流程，围绕 value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 生成的文本或业务键。
     */
    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
