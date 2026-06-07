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

    public BiQueryCachePolicy currentPolicy(Long tenantId) {
        return policyFromRows(policies(tenantId));
    }

    public BiQueryCachePolicyView currentPolicyView(Long tenantId) {
        return view(currentPolicy(tenantId));
    }

    public BiQueryCachePolicy.ResourcePolicy effectivePolicy(Long tenantId, String datasetKey, String dashboardKey) {
        BiQueryCachePolicy policy = currentPolicy(tenantId);
        if (dashboardKey != null && !dashboardKey.isBlank()) {
            return policy.effectiveForDashboard(dashboardKey);
        }
        return policy.effectiveForDataset(datasetKey);
    }

    public BiQueryCachePolicyView upsertPolicy(Long tenantId,
                                               BiQueryCachePolicyUpdateCommand command,
                                               String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQueryCachePolicy before = currentPolicy(scopedTenantId);
        Map<String, BiQueryCachePolicyDO> existing = policies(scopedTenantId).stream()
                .collect(Collectors.toMap(this::resourceId, Function.identity(), (left, right) -> right));
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

    public BiQueryCacheInvalidationResult invalidate(BiQueryCacheInvalidationCommand command) {
        String scope = command == null || isBlank(command.scope())
                ? "ALL"
                : command.scope().trim().toUpperCase(Locale.ROOT);
        return switch (scope) {
            case "SQL_HASH" -> {
                String sqlHash = command == null ? null : command.sqlHash();
                int deleted = resultCache.evict(sqlHash) ? 1 : 0;
                yield new BiQueryCacheInvalidationResult(scope, deleted,
                        deleted == 0 ? "no cache entry matched " + sqlHash : "cleared sql hash " + sqlHash);
            }
            case "DATASET" -> {
                String datasetKey = command == null ? null : command.datasetKey();
                int deleted = resultCache.evictDataset(datasetKey);
                yield new BiQueryCacheInvalidationResult(scope, deleted,
                        "cleared dataset " + datasetKey);
            }
            case "ALL" -> {
                int deleted = resultCache.clear();
                yield new BiQueryCacheInvalidationResult(scope, deleted, "cleared all BI query cache entries");
            }
            default -> throw new IllegalArgumentException("unsupported cache invalidation scope: " + scope);
        };
    }

    public BiQueryCacheStats cacheStats() {
        return resultCache.stats();
    }

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
        row.setUpdatedBy(actor == null || actor.isBlank() ? "system" : actor.trim());
        if (row.getId() == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
    }

    private List<BiQueryCachePolicyDO> policies(Long tenantId) {
        List<BiQueryCachePolicyDO> rows = mapper.selectList(
                new LambdaQueryWrapper<BiQueryCachePolicyDO>()
                        .eq(BiQueryCachePolicyDO::getTenantId, normalizeTenant(tenantId)));
        return rows == null ? List.of() : rows;
    }

    private BiQueryCachePolicy policyFromRows(List<BiQueryCachePolicyDO> rows) {
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
        return new BiQueryCachePolicy(enabled, ttlSeconds, cacheMode, resources);
    }

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
        } catch (RuntimeException ignored) {
            // Cache policy updates must still apply if audit persistence is unavailable.
        }
    }

    private String toJson(Object detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String resourceId(BiQueryCachePolicyDO row) {
        return resourceId(row.getResourceType(), row.getResourceKey());
    }

    private String resourceId(String resourceType, String resourceKey) {
        return normalizeType(resourceType) + ":" + (resourceKey == null ? "" : resourceKey.trim());
    }

    private String normalizeType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return BiQueryCachePolicy.TYPE_DATASET;
        }
        return resourceType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCacheMode(String cacheMode) {
        if (cacheMode == null || cacheMode.isBlank()) {
            return "CACHE";
        }
        return cacheMode.trim().toUpperCase(Locale.ROOT);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private boolean value(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private long value(Long value, long fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
