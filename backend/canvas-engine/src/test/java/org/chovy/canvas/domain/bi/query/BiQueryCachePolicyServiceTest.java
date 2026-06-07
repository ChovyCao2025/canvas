package org.chovy.canvas.domain.bi.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiQueryCachePolicyDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiQueryCachePolicyMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiQueryCachePolicyServiceTest {

    @Test
    void readsTenantCachePolicyWithDatasetAndDashboardOverrides() {
        BiQueryCachePolicyMapper mapper = mock(BiQueryCachePolicyMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                row(7L, "DEFAULT", "__DEFAULT__", true, 300L, "CACHE", "ops"),
                row(7L, "DATASET", "canvas_daily_stats", false, 60L, "DIRECT_QUERY", "ops"),
                row(7L, "DASHBOARD", "canvas-effect", true, 120L, "CACHE", "ops")
        ));
        BiQueryCachePolicyService service = new BiQueryCachePolicyService(
                mapper,
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                true,
                300L,
                BiQueryResultCache.noop());

        BiQueryCachePolicy policy = service.currentPolicy(7L);

        assertThat(policy.defaultEnabled()).isTrue();
        assertThat(policy.defaultTtlSeconds()).isEqualTo(300L);
        assertThat(policy.effectiveForDataset("canvas_daily_stats").enabled()).isFalse();
        assertThat(policy.effectiveForDataset("canvas_daily_stats").cacheMode()).isEqualTo("DIRECT_QUERY");
        assertThat(policy.effectiveForDashboard("canvas-effect").ttlSeconds()).isEqualTo(120L);
    }

    @Test
    void upsertsDefaultAndScopedCachePoliciesForTenant() {
        BiQueryCachePolicyMapper mapper = mock(BiQueryCachePolicyMapper.class);
        BiQueryCachePolicyDO existingDefault = row(7L, "DEFAULT", "__DEFAULT__", true, 300L, "CACHE", "system");
        existingDefault.setId(11L);
        when(mapper.selectList(any())).thenReturn(List.of(existingDefault));
        BiQueryCachePolicyService service = new BiQueryCachePolicyService(
                mapper,
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                true,
                300L,
                BiQueryResultCache.noop());

        service.upsertPolicy(7L, new BiQueryCachePolicyUpdateCommand(
                false,
                45L,
                "DIRECT_QUERY",
                List.of(new BiQueryCachePolicyUpdateCommand.ResourcePolicyCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        true,
                        180L,
                        "CACHE"))),
                "admin");

        ArgumentCaptor<BiQueryCachePolicyDO> updated = ArgumentCaptor.forClass(BiQueryCachePolicyDO.class);
        ArgumentCaptor<BiQueryCachePolicyDO> inserted = ArgumentCaptor.forClass(BiQueryCachePolicyDO.class);
        verify(mapper).updateById(updated.capture());
        verify(mapper).insert(inserted.capture());
        assertThat(updated.getValue().getId()).isEqualTo(11L);
        assertThat(updated.getValue().getTenantId()).isEqualTo(7L);
        assertThat(updated.getValue().getResourceType()).isEqualTo("DEFAULT");
        assertThat(updated.getValue().getResourceKey()).isEqualTo("__DEFAULT__");
        assertThat(updated.getValue().getEnabled()).isFalse();
        assertThat(updated.getValue().getTtlSeconds()).isEqualTo(45L);
        assertThat(updated.getValue().getCacheMode()).isEqualTo("DIRECT_QUERY");
        assertThat(updated.getValue().getUpdatedBy()).isEqualTo("admin");
        assertThat(inserted.getValue().getTenantId()).isEqualTo(7L);
        assertThat(inserted.getValue().getResourceType()).isEqualTo("DATASET");
        assertThat(inserted.getValue().getResourceKey()).isEqualTo("canvas_daily_stats");
        assertThat(inserted.getValue().getEnabled()).isTrue();
        assertThat(inserted.getValue().getTtlSeconds()).isEqualTo(180L);
        assertThat(inserted.getValue().getCacheMode()).isEqualTo("CACHE");
    }

    @Test
    void auditsCachePolicyUpdateWithBeforeAndAfterSnapshots() throws Exception {
        BiQueryCachePolicyMapper mapper = mock(BiQueryCachePolicyMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        BiQueryCachePolicyDO existingDefault = row(7L, "DEFAULT", "__DEFAULT__", true, 300L, "CACHE", "system");
        existingDefault.setId(11L);
        when(mapper.selectList(any())).thenReturn(
                List.of(existingDefault),
                List.of(row(7L, "DEFAULT", "__DEFAULT__", false, 45L, "DIRECT_QUERY", "admin")));
        ObjectMapper objectMapper = new ObjectMapper();
        BiQueryCachePolicyService service = new BiQueryCachePolicyService(
                mapper,
                auditLogMapper,
                objectMapper,
                true,
                300L,
                BiQueryResultCache.noop());

        service.upsertPolicy(7L, new BiQueryCachePolicyUpdateCommand(
                false,
                45L,
                "DIRECT_QUERY",
                List.of()),
                "admin");

        ArgumentCaptor<BiAuditLogDO> audit = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper).insert(audit.capture());
        assertThat(audit.getValue().getTenantId()).isEqualTo(7L);
        assertThat(audit.getValue().getActorId()).isEqualTo("admin");
        assertThat(audit.getValue().getActionKey()).isEqualTo("BI_QUERY_CACHE_POLICY_UPDATE");
        assertThat(audit.getValue().getResourceType()).isEqualTo("BI_QUERY_CACHE_POLICY");
        assertThat(audit.getValue().getCreatedAt()).isNotNull();
        JsonNode detail = objectMapper.readTree(audit.getValue().getDetailJson());
        assertThat(detail.path("before").path("defaultEnabled").asBoolean()).isTrue();
        assertThat(detail.path("after").path("defaultEnabled").asBoolean()).isFalse();
        assertThat(detail.path("after").path("defaultTtlSeconds").asLong()).isEqualTo(45L);
    }

    @Test
    void appliesCachePolicyUpdateWhenAuditStorageFails() {
        BiQueryCachePolicyMapper mapper = mock(BiQueryCachePolicyMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(), List.of(
                row(7L, "DEFAULT", "__DEFAULT__", false, 45L, "DIRECT_QUERY", "admin")
        ));
        when(auditLogMapper.insert(any(BiAuditLogDO.class))).thenThrow(new IllegalStateException("audit unavailable"));
        BiQueryCachePolicyService service = new BiQueryCachePolicyService(
                mapper,
                auditLogMapper,
                new ObjectMapper(),
                true,
                300L,
                BiQueryResultCache.noop());

        assertThatCode(() -> service.upsertPolicy(7L, new BiQueryCachePolicyUpdateCommand(
                false,
                45L,
                "DIRECT_QUERY",
                List.of()),
                "admin")).doesNotThrowAnyException();
    }

    @Test
    void invalidatesCacheByHashDatasetAndAllEntries() {
        BiQueryCachePolicyMapper mapper = mock(BiQueryCachePolicyMapper.class);
        TrackableResultCache cache = new TrackableResultCache();
        cache.put("hash-one", result("hash-one", "canvas_daily_stats"), Duration.ofSeconds(300));
        cache.put("hash-two", result("hash-two", "channel_daily_stats"), Duration.ofSeconds(300));
        BiQueryCachePolicyService service = new BiQueryCachePolicyService(
                mapper,
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                true,
                300L,
                cache);

        BiQueryCacheInvalidationResult byHash = service.invalidate(
                new BiQueryCacheInvalidationCommand("SQL_HASH", "hash-one", null));
        BiQueryCacheInvalidationResult byDataset = service.invalidate(
                new BiQueryCacheInvalidationCommand("DATASET", null, "channel_daily_stats"));
        BiQueryCacheInvalidationResult all = service.invalidate(
                new BiQueryCacheInvalidationCommand("ALL", null, null));

        assertThat(byHash.deletedEntries()).isEqualTo(1);
        assertThat(byHash.message()).contains("hash-one");
        assertThat(byDataset.deletedEntries()).isEqualTo(1);
        assertThat(byDataset.message()).contains("channel_daily_stats");
        assertThat(all.deletedEntries()).isZero();
        assertThat(cache.size()).isZero();
    }

    @Test
    void exposesQueryCacheStatsFromProvider() {
        TrackableResultCache cache = new TrackableResultCache();
        cache.put("hash-one", result("hash-one", "canvas_daily_stats"), Duration.ofSeconds(300));
        BiQueryCachePolicyService service = new BiQueryCachePolicyService(
                mock(BiQueryCachePolicyMapper.class),
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                true,
                300L,
                cache);

        BiQueryCacheStats stats = service.cacheStats();

        assertThat(stats.provider()).isEqualTo("trackable");
        assertThat(stats.enabled()).isTrue();
        assertThat(stats.entryCount()).isEqualTo(1);
        assertThat(stats.maxEntries()).isEqualTo(10);
        assertThat(stats.ttlSeconds()).isEqualTo(300);
    }

    private BiQueryCachePolicyDO row(Long tenantId,
                                     String resourceType,
                                     String resourceKey,
                                     Boolean enabled,
                                     Long ttlSeconds,
                                     String cacheMode,
                                     String updatedBy) {
        BiQueryCachePolicyDO row = new BiQueryCachePolicyDO();
        row.setTenantId(tenantId);
        row.setResourceType(resourceType);
        row.setResourceKey(resourceKey);
        row.setEnabled(enabled);
        row.setTtlSeconds(ttlSeconds);
        row.setCacheMode(cacheMode);
        row.setUpdatedBy(updatedBy);
        return row;
    }

    private BiQueryResult result(String sqlHash, String datasetKey) {
        return new BiQueryResult(datasetKey, List.of(), List.of(), 0, 1L, sqlHash);
    }

    private static final class TrackableResultCache implements BiQueryResultCache {
        private final java.util.Map<String, BiQueryResult> values = new java.util.LinkedHashMap<>();

        @Override
        public Optional<BiQueryResult> get(String sqlHash) {
            return Optional.ofNullable(values.get(sqlHash));
        }

        @Override
        public void put(String sqlHash, BiQueryResult result) {
            values.put(sqlHash, result);
        }

        @Override
        public void put(String sqlHash, BiQueryResult result, Duration ttl) {
            values.put(sqlHash, result);
        }

        @Override
        public boolean evict(String sqlHash) {
            return values.remove(sqlHash) != null;
        }

        @Override
        public int evictDataset(String datasetKey) {
            int before = values.size();
            values.entrySet().removeIf(entry -> datasetKey.equals(entry.getValue().datasetKey()));
            return before - values.size();
        }

        @Override
        public int clear() {
            int count = values.size();
            values.clear();
            return count;
        }

        @Override
        public BiQueryCacheStats stats() {
            return new BiQueryCacheStats("trackable", true, values.size(), 10, 300, 0, 0, values.size(), 0);
        }

        int size() {
            return values.size();
        }
    }
}
