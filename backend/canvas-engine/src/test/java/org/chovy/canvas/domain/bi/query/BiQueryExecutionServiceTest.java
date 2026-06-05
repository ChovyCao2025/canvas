package org.chovy.canvas.domain.bi.query;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldAccessAuditDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldPolicyDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseFieldAccessAuditMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseFieldPolicyMapper;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiQueryExecutionServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-05T03:00:00Z"), ZoneOffset.UTC);

    @Test
    void executesCompiledQueryAndRecordsHistory() {
        List<BiCompiledQuery> compiledQueries = new ArrayList<>();
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        BiQueryExecutionService service = new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> {
                    compiledQueries.add(query);
                    return List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L));
                },
                history::add,
                CLOCK);
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(new BiFilter("canvas_id", BiFilter.Operator.EQ, 12L)),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100
        );

        BiQueryResult result = service.execute(request, new BiQueryContext(7L, "alice"));

        assertThat(compiledQueries).hasSize(1);
        assertThat(compiledQueries.get(0).parameters()).containsExactly(7L, 12L);
        assertThat(result.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(result.columns()).extracting(BiQueryColumn::key)
                .containsExactly("stat_date", "total_executions");
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0)).containsEntry("total_executions", 42L);
        assertThat(result.sqlHash()).hasSize(64);
        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.tenantId()).isEqualTo(7L);
            assertThat(entry.username()).isEqualTo("alice");
            assertThat(entry.status()).isEqualTo("SUCCESS");
            assertThat(entry.rowCount()).isEqualTo(1);
            assertThat(entry.sqlHash()).isEqualTo(result.sqlHash());
        });
    }

    @Test
    void recordsFailureWhenDatasourceExecutionFails() {
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        BiQueryExecutionService service = new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> {
                    throw new IllegalStateException("BI datasource is not available");
                },
                history::add,
                CLOCK);
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(),
                100
        );

        assertThatThrownBy(() -> service.execute(request, new BiQueryContext(7L, "alice")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("datasource");

        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.status()).isEqualTo("FAILED");
            assertThat(entry.rowCount()).isZero();
            assertThat(entry.errorMessage()).contains("datasource");
        });
    }

    @Test
    void returnsCachedResultForRepeatedCompiledQuery() {
        List<BiCompiledQuery> compiledQueries = new ArrayList<>();
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        MapBackedResultCache cache = new MapBackedResultCache();
        BiQueryExecutionService service = new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> {
                    compiledQueries.add(query);
                    return List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L));
                },
                history::add,
                cache,
                CLOCK);
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(new BiFilter("canvas_id", BiFilter.Operator.EQ, 12L)),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100
        );

        BiQueryResult first = service.execute(request, new BiQueryContext(7L, "alice"));
        BiQueryResult second = service.execute(request, new BiQueryContext(7L, "alice"));

        assertThat(first.cached()).isFalse();
        assertThat(second.cached()).isTrue();
        assertThat(compiledQueries).hasSize(1);
        assertThat(history).extracting(BiQueryHistoryEntry::status)
                .containsExactly("SUCCESS", "CACHE_HIT");
    }

    @Test
    void deniesFieldPolicyBeforeDatasourceExecutionAndRecordsFailureHistory() {
        CdpWarehouseFieldPolicyMapper policyMapper = mock(CdpWarehouseFieldPolicyMapper.class);
        CdpWarehouseFieldAccessAuditMapper auditMapper = mock(CdpWarehouseFieldAccessAuditMapper.class);
        when(policyMapper.selectList(any())).thenReturn(List.of(
                policy("canvas_id", "DENY", RoleNames.OPERATOR, "SELECT,FILTER,SORT,GROUP")));
        CdpWarehouseFieldGovernanceService governanceService =
                new CdpWarehouseFieldGovernanceService(policyMapper, auditMapper);
        List<BiCompiledQuery> compiledQueries = new ArrayList<>();
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        BiQueryExecutionService service = new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> {
                    compiledQueries.add(query);
                    return List.of(Map.of("canvas_id", 12L));
                },
                history::add,
                BiQueryResultCache.noop(),
                BiDatasetSpecResolver.builtIn(),
                governanceService,
                CLOCK);
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of(),
                List.of("total_executions"),
                List.of(new BiFilter("canvas_id", BiFilter.Operator.EQ, 12L)),
                List.of(),
                100
        );

        assertThatThrownBy(() -> service.execute(request,
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR)))
                .isInstanceOf(CdpWarehouseFieldGovernanceService.FieldAccessDeniedException.class)
                .hasMessageContaining("DENY");

        assertThat(compiledQueries).isEmpty();
        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.status()).isEqualTo("FAILED");
            assertThat(entry.rowCount()).isZero();
            assertThat(entry.sqlHash()).hasSize(64);
            assertThat(entry.errorMessage()).contains("Field access denied");
        });
        verify(auditMapper).insert(any(CdpWarehouseFieldAccessAuditDO.class));
    }

    @Test
    void gatedQueryExecutesWhenAvailabilityPasses() {
        LocalDateTime from = LocalDateTime.parse("2026-06-05T01:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T02:00:00");
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        when(availabilityService.evaluate(7L, from, to, "HYBRID"))
                .thenReturn(availability("PASS", from, to));
        List<BiCompiledQuery> compiledQueries = new ArrayList<>();
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        BiQueryExecutionService service = gatedService(
                availabilityService,
                (query, dataset) -> {
                    compiledQueries.add(query);
                    return List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L));
                },
                history::add);

        BiQueryExecutionService.GatedBiQueryResult result = service.executeWithAvailabilityGate(
                defaultRequest(),
                new BiQueryContext(7L, "alice"),
                from,
                to,
                "HYBRID",
                false);

        assertThat(result.status()).isEqualTo("EXECUTED");
        assertThat(result.reason()).isEqualTo("warehouse availability PASS");
        assertThat(result.availability().status()).isEqualTo("PASS");
        assertThat(result.queryResult()).isNotNull();
        assertThat(result.queryResult().rowCount()).isEqualTo(1);
        assertThat(compiledQueries).hasSize(1);
        assertThat(history).extracting(BiQueryHistoryEntry::status).containsExactly("SUCCESS");
    }

    @Test
    void gatedQueryBlocksFailBeforeDatasourceAndRecordsHistory() {
        LocalDateTime from = LocalDateTime.parse("2026-06-05T01:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T02:00:00");
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        when(availabilityService.evaluate(7L, from, to, "OFFLINE"))
                .thenReturn(availability("FAIL", from, to));
        List<BiCompiledQuery> compiledQueries = new ArrayList<>();
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        BiQueryExecutionService service = gatedService(
                availabilityService,
                (query, dataset) -> {
                    compiledQueries.add(query);
                    return List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L));
                },
                history::add);

        BiQueryExecutionService.GatedBiQueryResult result = service.executeWithAvailabilityGate(
                defaultRequest(),
                new BiQueryContext(7L, "alice"),
                from,
                to,
                "OFFLINE",
                true);

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.reason()).isEqualTo("warehouse availability FAIL");
        assertThat(result.queryResult()).isNull();
        assertThat(compiledQueries).isEmpty();
        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.status()).isEqualTo("BLOCKED");
            assertThat(entry.rowCount()).isZero();
            assertThat(entry.sqlHash()).hasSize(64);
            assertThat(entry.errorMessage()).isEqualTo("warehouse availability FAIL");
        });
    }

    @Test
    void gatedQueryBlocksWarnUnlessAllowed() {
        LocalDateTime from = LocalDateTime.parse("2026-06-05T01:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T02:00:00");
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        when(availabilityService.evaluate(7L, from, to, "REALTIME"))
                .thenReturn(availability("WARN", from, to));
        List<BiCompiledQuery> compiledQueries = new ArrayList<>();
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        BiQueryExecutionService service = gatedService(
                availabilityService,
                (query, dataset) -> {
                    compiledQueries.add(query);
                    return List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L));
                },
                history::add);

        BiQueryExecutionService.GatedBiQueryResult blocked = service.executeWithAvailabilityGate(
                defaultRequest(),
                new BiQueryContext(7L, "alice"),
                from,
                to,
                "REALTIME",
                false);
        BiQueryExecutionService.GatedBiQueryResult executed = service.executeWithAvailabilityGate(
                defaultRequest(),
                new BiQueryContext(7L, "alice"),
                from,
                to,
                "REALTIME",
                true);

        assertThat(blocked.status()).isEqualTo("BLOCKED");
        assertThat(blocked.reason()).contains("allowWarn=true");
        assertThat(executed.status()).isEqualTo("EXECUTED");
        assertThat(executed.reason()).isEqualTo("warehouse availability WARN accepted by operator");
        assertThat(compiledQueries).hasSize(1);
        assertThat(history).extracting(BiQueryHistoryEntry::status)
                .containsExactly("BLOCKED", "SUCCESS");
    }

    @Test
    void contractGatedQueryBlocksBeforeDatasourceAndRecordsHistory() {
        LocalDateTime from = LocalDateTime.parse("2026-06-05T01:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T02:00:00");
        CdpWarehouseConsumerAvailabilityService consumerAvailabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        when(consumerAvailabilityService.evaluateContract(7L, "bi_daily_active_users", from, to))
                .thenReturn(consumerAvailability("bi_daily_active_users", "FAIL", false, from, to));
        List<BiCompiledQuery> compiledQueries = new ArrayList<>();
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        BiQueryExecutionService service = contractGatedService(
                consumerAvailabilityService,
                (query, dataset) -> {
                    compiledQueries.add(query);
                    return List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L));
                },
                history::add);

        BiQueryExecutionService.ContractGatedBiQueryResult result =
                service.executeWithConsumerAvailabilityContract(
                        defaultRequest(),
                        new BiQueryContext(7L, "alice"),
                        "bi_daily_active_users",
                        from,
                        to);

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.queryResult()).isNull();
        assertThat(result.consumerAvailability().status()).isEqualTo("FAIL");
        assertThat(compiledQueries).isEmpty();
        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.status()).isEqualTo("BLOCKED");
            assertThat(entry.errorMessage()).contains("blocked");
            assertThat(entry.sqlHash()).hasSize(64);
        });
    }

    @Test
    void contractGatedQueryExecutesWhenContractAllows() {
        LocalDateTime from = LocalDateTime.parse("2026-06-05T01:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T02:00:00");
        CdpWarehouseConsumerAvailabilityService consumerAvailabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        when(consumerAvailabilityService.evaluateContract(7L, "bi_daily_active_users", from, to))
                .thenReturn(consumerAvailability("bi_daily_active_users", "WARN", true, from, to));
        List<BiCompiledQuery> compiledQueries = new ArrayList<>();
        List<BiQueryHistoryEntry> history = new ArrayList<>();
        BiQueryExecutionService service = contractGatedService(
                consumerAvailabilityService,
                (query, dataset) -> {
                    compiledQueries.add(query);
                    return List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L));
                },
                history::add);

        BiQueryExecutionService.ContractGatedBiQueryResult result =
                service.executeWithConsumerAvailabilityContract(
                        defaultRequest(),
                        new BiQueryContext(7L, "alice"),
                        "bi_daily_active_users",
                        from,
                        to);

        assertThat(result.status()).isEqualTo("EXECUTED");
        assertThat(result.consumerAvailability().allowed()).isTrue();
        assertThat(result.queryResult()).isNotNull();
        assertThat(result.queryResult().rowCount()).isEqualTo(1);
        assertThat(compiledQueries).hasSize(1);
        assertThat(history).extracting(BiQueryHistoryEntry::status).containsExactly("SUCCESS");
    }

    private static final class MapBackedResultCache implements BiQueryResultCache {
        private final java.util.Map<String, BiQueryResult> values = new java.util.HashMap<>();

        @Override
        public Optional<BiQueryResult> get(String sqlHash) {
            return Optional.ofNullable(values.get(sqlHash));
        }

        @Override
        public void put(String sqlHash, BiQueryResult result) {
            values.put(sqlHash, result);
        }
    }

    private CdpWarehouseFieldPolicyDO policy(String fieldKey,
                                             String accessPolicy,
                                             String minRole,
                                             String allowedUsages) {
        CdpWarehouseFieldPolicyDO row = new CdpWarehouseFieldPolicyDO();
        row.setId(1L);
        row.setTenantId(0L);
        row.setDatasetKey("canvas_daily_stats");
        row.setFieldKey(fieldKey);
        row.setPhysicalName("canvas_dws.canvas_daily_stats");
        row.setColumnName(fieldKey);
        row.setValueType("NUMBER");
        row.setSemanticType("ID");
        row.setPiiLevel("PII_RELATED");
        row.setAccessPolicy(accessPolicy);
        row.setMinRole(minRole);
        row.setAllowedUsages(allowedUsages);
        row.setLifecycleStatus("ACTIVE");
        return row;
    }

    private BiQueryExecutionService gatedService(CdpWarehouseAvailabilityService availabilityService,
                                                 BiQueryExecutor executor,
                                                 BiQueryHistoryRecorder historyRecorder) {
        return new BiQueryExecutionService(
                new BiQueryCompiler(),
                executor,
                historyRecorder,
                BiQueryResultCache.noop(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                availabilityService,
                CLOCK);
    }

    private BiQueryExecutionService contractGatedService(
            CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
            BiQueryExecutor executor,
            BiQueryHistoryRecorder historyRecorder) {
        return new BiQueryExecutionService(
                new BiQueryCompiler(),
                executor,
                historyRecorder,
                BiQueryResultCache.noop(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                null,
                consumerAvailabilityService,
                CLOCK);
    }

    private BiQueryRequest defaultRequest() {
        return new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100
        );
    }

    private CdpWarehouseAvailabilityService.AvailabilityDecision availability(String status,
                                                                             LocalDateTime from,
                                                                             LocalDateTime to) {
        return new CdpWarehouseAvailabilityService.AvailabilityDecision(
                7L,
                "HYBRID",
                from,
                to,
                to,
                status,
                List.of(new CdpWarehouseAvailabilityService.AvailabilityGate(
                        "offline_aggregate",
                        status,
                        "test availability " + status,
                        to,
                        0L,
                        1)));
    }

    private CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation consumerAvailability(
            String contractKey,
            String status,
            boolean allowed,
            LocalDateTime from,
            LocalDateTime to) {
        return new CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation(
                7L,
                contractKey,
                "BI_METRIC",
                "daily_active_users",
                "OFFLINE",
                from,
                to,
                to,
                status,
                allowed,
                allowed ? "BLOCK_ON_FAIL" : "BLOCK_ON_WARN",
                availability(status, from, to),
                List.of(),
                "consumer availability " + status + (allowed ? " allowed" : " blocked"));
    }
}
