package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldAccessAuditDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldPolicyDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseFieldAccessAuditMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseFieldPolicyMapper;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.embed.BiEmbedTokenCleanupResult;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketRequest;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketService;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketVerifyRequest;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealth;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthProvider;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthSloSummary;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthSnapshot;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiQueryCompiler;
import org.chovy.canvas.domain.bi.query.BiQueryCancellationResult;
import org.chovy.canvas.domain.bi.query.BiQueryCacheInvalidationCommand;
import org.chovy.canvas.domain.bi.query.BiQueryCacheInvalidationResult;
import org.chovy.canvas.domain.bi.query.BiQueryCacheStats;
import org.chovy.canvas.domain.bi.query.BiQueryCachePolicyService;
import org.chovy.canvas.domain.bi.query.BiQueryCachePolicyUpdateCommand;
import org.chovy.canvas.domain.bi.query.BiQueryCachePolicyView;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryDetail;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryItem;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryReader;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryRecorder;
import org.chovy.canvas.domain.bi.query.BiQueryGovernanceSummary;
import org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicy;
import org.chovy.canvas.domain.bi.query.BiQueryGovernanceAuditEntry;
import org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicyService;
import org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicyUpdateCommand;
import org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicyView;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.bi.query.BiSort;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiQueryControllerTest {

    private static final String EMBED_SECRET = "bi-embed-test-secret-with-at-least-32-bytes";
    private static final Clock EMBED_CLOCK = Clock.fixed(Instant.parse("2026-06-05T01:50:00Z"), ZoneOffset.UTC);

    @Test
    void compileInjectsCurrentTenantIntoSafeQuery() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQueryController controller = new BiQueryController(resolver);
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(new BiFilter("stat_date", BiFilter.Operator.BETWEEN, List.of("2026-06-01", "2026-06-05"))),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100
        );

        StepVerifier.create(controller.compile(request))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().sql()).contains("tenant_id = ?");
                    assertThat(response.getData().parameters()).containsExactly(7L, "2026-06-01", "2026-06-05");
                })
                .verifyComplete();
    }

    @Test
    void compileUsesDefaultTenantWhenResolverIsAbsent() {
        BiQueryController controller = new BiQueryController();
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("trigger_type"),
                List.of("success_rate"),
                List.of(),
                List.of(),
                50
        );

        StepVerifier.create(controller.compile(request).map(response -> response.getData().parameters()))
                .assertNext(parameters -> assertThat(parameters).containsExactly(0L))
                .verifyComplete();
    }

    @Test
    void compileRejectsInvalidFieldsBeforeSqlGeneration() {
        BiQueryController controller = new BiQueryController();
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("unsafe_metric"),
                List.of(),
                List.of(),
                50
        );

        StepVerifier.create(controller.compile(request))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("Unknown metric"))
                .verify();
    }

    @Test
    void compileRejectsFieldPolicyBeforeReturningSql() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "alice")));
        CdpWarehouseFieldPolicyMapper policyMapper = mock(CdpWarehouseFieldPolicyMapper.class);
        CdpWarehouseFieldAccessAuditMapper auditMapper = mock(CdpWarehouseFieldAccessAuditMapper.class);
        when(policyMapper.selectList(any())).thenReturn(List.of(
                policy("stat_date", "DENY", RoleNames.OPERATOR, "SELECT,FILTER,SORT,GROUP")));
        CdpWarehouseFieldGovernanceService governanceService =
                new CdpWarehouseFieldGovernanceService(policyMapper, auditMapper);
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> List.of(),
                () -> List.of(),
                BiDatasetSpecResolver.builtIn(),
                governanceService);
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(),
                100
        );

        StepVerifier.create(controller.compile(request))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(CdpWarehouseFieldGovernanceService.FieldAccessDeniedException.class)
                        .hasMessageContaining("Field access denied"))
                .verify();
        verify(auditMapper, atLeastOnce()).insert(any(CdpWarehouseFieldAccessAuditDO.class));
    }

    @Test
    void executesQueryForCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQueryExecutionService queryExecutionService = new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L)),
                BiQueryHistoryRecorder.noop(),
                EMBED_CLOCK);
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                queryExecutionService);
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(new BiFilter("canvas_id", BiFilter.Operator.EQ, 12L)),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100
        );

        StepVerifier.create(controller.execute(request))
                .assertNext(response -> {
                    assertThat(response.getData().columns()).extracting(column -> column.key())
                            .containsExactly("stat_date", "total_executions");
                    assertThat(response.getData().rows()).singleElement()
                            .satisfies(row -> assertThat(row).containsEntry("total_executions", 42L));
                    assertThat(response.getData().rowCount()).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    void executeGatedUsesCurrentTenantWindowAndMode() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        LocalDateTime from = LocalDateTime.parse("2026-06-05T01:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T02:00:00");
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        when(availabilityService.evaluate(7L, from, to, "HYBRID"))
                .thenReturn(availability("PASS", from, to));
        BiQueryExecutionService queryExecutionService = new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L)),
                BiQueryHistoryRecorder.noop(),
                org.chovy.canvas.domain.bi.query.BiQueryResultCache.noop(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                availabilityService,
                EMBED_CLOCK);
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                queryExecutionService);
        BiQueryRequest query = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100
        );

        StepVerifier.create(controller.executeGated(
                        new BiQueryController.GatedQueryRequest(query, from, to, "HYBRID", true)))
                .assertNext(response -> {
                    assertThat(response.getData().tenantId()).isEqualTo(7L);
                    assertThat(response.getData().datasetKey()).isEqualTo("canvas_daily_stats");
                    assertThat(response.getData().status()).isEqualTo("EXECUTED");
                    assertThat(response.getData().queryResult().rowCount()).isEqualTo(1);
                })
                .verifyComplete();
        verify(availabilityService).evaluate(7L, from, to, "HYBRID");
    }

    @Test
    void executeContractGatedUsesCurrentTenantAndContract() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        LocalDateTime from = LocalDateTime.parse("2026-06-05T01:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T02:00:00");
        CdpWarehouseConsumerAvailabilityService consumerAvailabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        when(consumerAvailabilityService.evaluateContract(7L, "bi_daily_active_users", from, to))
                .thenReturn(consumerAvailability("bi_daily_active_users", "PASS", true, from, to));
        BiQueryExecutionService queryExecutionService = new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L)),
                BiQueryHistoryRecorder.noop(),
                org.chovy.canvas.domain.bi.query.BiQueryResultCache.noop(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                null,
                consumerAvailabilityService,
                EMBED_CLOCK);
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                queryExecutionService);
        BiQueryRequest query = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100
        );

        StepVerifier.create(controller.executeContractGated(
                        new BiQueryController.ContractGatedQueryRequest(
                                query, "bi_daily_active_users", from, to)))
                .assertNext(response -> {
                    assertThat(response.getData().tenantId()).isEqualTo(7L);
                    assertThat(response.getData().contractKey()).isEqualTo("bi_daily_active_users");
                    assertThat(response.getData().status()).isEqualTo("EXECUTED");
                    assertThat(response.getData().queryResult().rowCount()).isEqualTo(1);
                })
                .verifyComplete();
        verify(consumerAvailabilityService).evaluateContract(7L, "bi_daily_active_users", from, to);
    }

    @Test
    void listsQueryHistoryForCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        AtomicReference<Long> tenantSeen = new AtomicReference<>();
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> {
                    tenantSeen.set(tenantId);
                    return List.of(new BiQueryHistoryItem(
                            99L,
                            "canvas_daily_stats",
                            "alice",
                            3,
                            12L,
                            "SUCCESS",
                            "abcdef",
                            null,
                            LocalDateTime.parse("2026-06-05T02:30:00")));
                },
                () -> List.of());

        StepVerifier.create(controller.queryHistory(20))
                .assertNext(response -> {
                    assertThat(tenantSeen.get()).isEqualTo(7L);
                    assertThat(response.getData()).singleElement().satisfies(item -> {
                        assertThat(item.datasetKey()).isEqualTo("canvas_daily_stats");
                        assertThat(item.status()).isEqualTo("SUCCESS");
                        assertThat(item.rowCount()).isEqualTo(3);
                    });
                })
                .verifyComplete();
    }

    @Test
    void getsQueryHistoryDetailForCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        AtomicReference<Long> tenantSeen = new AtomicReference<>();
        AtomicReference<Long> historyIdSeen = new AtomicReference<>();
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(new BiFilter("canvas_id", BiFilter.Operator.EQ, 12L)),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100);
        BiQueryHistoryReader reader = new BiQueryHistoryReader() {
            @Override
            public List<BiQueryHistoryItem> recent(Long tenantId, int limit) {
                return List.of();
            }

            @Override
            public Optional<BiQueryHistoryDetail> detail(Long tenantId, Long historyId) {
                tenantSeen.set(tenantId);
                historyIdSeen.set(historyId);
                return Optional.of(new BiQueryHistoryDetail(
                        historyId,
                        "canvas_daily_stats",
                        "alice",
                        request,
                        3,
                        1250L,
                        "SUCCESS",
                        "abcdef",
                        null,
                        LocalDateTime.parse("2026-06-05T02:30:00")));
            }
        };
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                reader,
                () -> List.of());

        StepVerifier.create(controller.queryHistoryDetail(99L))
                .assertNext(response -> {
                    assertThat(tenantSeen.get()).isEqualTo(7L);
                    assertThat(historyIdSeen.get()).isEqualTo(99L);
                    assertThat(response.getData().datasetKey()).isEqualTo("canvas_daily_stats");
                    assertThat(response.getData().request().filters()).singleElement()
                            .satisfies(filter -> assertThat(filter.field()).isEqualTo("canvas_id"));
                    assertThat(response.getData().durationMs()).isEqualTo(1250L);
                })
                .verifyComplete();
    }

    @Test
    void exposesTenantScopedQueryGovernanceSummary() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        AtomicReference<Long> tenantSeen = new AtomicReference<>();
        AtomicReference<Integer> limitSeen = new AtomicReference<>();
        BiQueryHistoryReader reader = new BiQueryHistoryReader() {
            @Override
            public List<BiQueryHistoryItem> recent(Long tenantId, int limit) {
                return List.of();
            }

            @Override
            public BiQueryGovernanceSummary governanceSummary(Long tenantId,
                                                              int limit,
                                                              BiQueryGovernancePolicy policy) {
                tenantSeen.set(tenantId);
                limitSeen.set(limit);
                return new BiQueryGovernanceSummary(
                        20,
                        2,
                        3,
                        4,
                        1250L,
                        30_000L,
                        1_000_000,
                        List.of(new BiQueryGovernanceSummary.DatasetQueryStats(
                                "canvas_daily_stats",
                                12,
                                2,
                                3,
                                1,
                                1850L,
                                44_000L,
                                30_000L,
                                1_000_000)));
            }
        };
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                reader,
                () -> List.of());

        StepVerifier.create(controller.queryGovernanceSummary(50))
                .assertNext(response -> {
                    assertThat(tenantSeen.get()).isEqualTo(7L);
                    assertThat(limitSeen.get()).isEqualTo(50);
                    assertThat(response.getData().totalQueries()).isEqualTo(20);
                    assertThat(response.getData().slowQueries()).isEqualTo(2);
                    assertThat(response.getData().failedQueries()).isEqualTo(3);
                    assertThat(response.getData().cacheHits()).isEqualTo(4);
                    assertThat(response.getData().timeoutPolicyMs()).isEqualTo(30_000L);
                    assertThat(response.getData().datasetQuotaRows()).isEqualTo(1_000_000);
                    assertThat(response.getData().datasets()).singleElement().satisfies(dataset -> {
                        assertThat(dataset.datasetKey()).isEqualTo("canvas_daily_stats");
                        assertThat(dataset.maxDurationMs()).isEqualTo(44_000L);
                    });
                })
                .verifyComplete();
    }

    @Test
    void updatesTenantQueryGovernancePolicy() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQueryGovernancePolicyService policyService = mock(BiQueryGovernancePolicyService.class);
        BiQueryGovernancePolicyView view = new BiQueryGovernancePolicyView(
                20_000L,
                500_000,
                List.of(new BiQueryGovernancePolicyView.DatasetPolicyView("canvas_daily_stats", 8_000L, 100_000)));
        BiQueryGovernancePolicyUpdateCommand command = new BiQueryGovernancePolicyUpdateCommand(
                20_000L,
                500_000,
                List.of(new BiQueryGovernancePolicyUpdateCommand.DatasetPolicyCommand("canvas_daily_stats", 8_000L, 100_000)));
        when(policyService.upsertPolicy(7L, command, "alice")).thenReturn(view);
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> List.of(),
                BiDatasourceHealthProvider.empty(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                BiQueryGovernancePolicy.defaults(),
                policyService);

        StepVerifier.create(controller.upsertQueryGovernancePolicy(command))
                .assertNext(response -> {
                    assertThat(response.getData().defaultTimeoutMs()).isEqualTo(20_000L);
                    assertThat(response.getData().defaultQuotaRows()).isEqualTo(500_000);
                    assertThat(response.getData().datasets()).extracting(BiQueryGovernancePolicyView.DatasetPolicyView::datasetKey)
                            .containsExactly("canvas_daily_stats");
                })
                .verifyComplete();
    }

    @Test
    void exposesTenantScopedQueryGovernanceAudit() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQueryGovernancePolicyService policyService = mock(BiQueryGovernancePolicyService.class);
        when(policyService.recentAudit(7L, 3)).thenReturn(List.of(new BiQueryGovernanceAuditEntry(
                101L,
                "alice",
                "BI_QUERY_GOVERNANCE_POLICY_UPDATE",
                "BI_QUERY_GOVERNANCE_POLICY",
                "{\"after\":{\"defaultTimeoutMs\":20000}}",
                LocalDateTime.parse("2026-06-05T08:20:00"))));
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> List.of(),
                BiDatasourceHealthProvider.empty(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                BiQueryGovernancePolicy.defaults(),
                policyService);

        StepVerifier.create(controller.queryGovernanceAudit(3))
                .assertNext(response -> {
                    assertThat(response.getData()).singleElement().satisfies(entry -> {
                        assertThat(entry.id()).isEqualTo(101L);
                        assertThat(entry.actorId()).isEqualTo("alice");
                        assertThat(entry.actionKey()).isEqualTo("BI_QUERY_GOVERNANCE_POLICY_UPDATE");
                        assertThat(entry.detailJson()).contains("defaultTimeoutMs");
                    });
                })
                .verifyComplete();
    }

    @Test
    void exposesTenantScopedQueryCachePolicy() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQueryCachePolicyService cachePolicyService = mock(BiQueryCachePolicyService.class);
        when(cachePolicyService.currentPolicyView(7L)).thenReturn(new BiQueryCachePolicyView(
                true,
                300L,
                "CACHE",
                List.of(new BiQueryCachePolicyView.ResourcePolicyView(
                        "DATASET",
                        "canvas_daily_stats",
                        false,
                        60L,
                        "DIRECT_QUERY"))));
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> List.of(),
                BiDatasourceHealthProvider.empty(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                BiQueryGovernancePolicy.defaults(),
                null,
                cachePolicyService);

        StepVerifier.create(controller.queryCachePolicy())
                .assertNext(response -> {
                    assertThat(response.getData().defaultEnabled()).isTrue();
                    assertThat(response.getData().defaultTtlSeconds()).isEqualTo(300L);
                    assertThat(response.getData().resources()).singleElement().satisfies(resource -> {
                        assertThat(resource.resourceType()).isEqualTo("DATASET");
                        assertThat(resource.resourceKey()).isEqualTo("canvas_daily_stats");
                        assertThat(resource.enabled()).isFalse();
                        assertThat(resource.cacheMode()).isEqualTo("DIRECT_QUERY");
                    });
                })
                .verifyComplete();
    }

    @Test
    void updatesTenantQueryCachePolicy() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQueryCachePolicyService cachePolicyService = mock(BiQueryCachePolicyService.class);
        BiQueryCachePolicyUpdateCommand command = new BiQueryCachePolicyUpdateCommand(
                false,
                45L,
                "DIRECT_QUERY",
                List.of(new BiQueryCachePolicyUpdateCommand.ResourcePolicyCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        true,
                        180L,
                        "CACHE")));
        BiQueryCachePolicyView view = new BiQueryCachePolicyView(
                false,
                45L,
                "DIRECT_QUERY",
                List.of(new BiQueryCachePolicyView.ResourcePolicyView(
                        "DATASET",
                        "canvas_daily_stats",
                        true,
                        180L,
                        "CACHE")));
        when(cachePolicyService.upsertPolicy(7L, command, "alice")).thenReturn(view);
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> List.of(),
                BiDatasourceHealthProvider.empty(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                BiQueryGovernancePolicy.defaults(),
                null,
                cachePolicyService);

        StepVerifier.create(controller.upsertQueryCachePolicy(command))
                .assertNext(response -> {
                    assertThat(response.getData().defaultEnabled()).isFalse();
                    assertThat(response.getData().defaultTtlSeconds()).isEqualTo(45L);
                    assertThat(response.getData().resources()).singleElement()
                            .satisfies(resource -> assertThat(resource.ttlSeconds()).isEqualTo(180L));
                })
                .verifyComplete();
    }

    @Test
    void invalidatesTenantQueryCache() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQueryCachePolicyService cachePolicyService = mock(BiQueryCachePolicyService.class);
        BiQueryCacheInvalidationCommand command = new BiQueryCacheInvalidationCommand(
                "DATASET",
                null,
                "canvas_daily_stats");
        when(cachePolicyService.invalidate(command)).thenReturn(new BiQueryCacheInvalidationResult(
                "DATASET",
                3,
                "cleared dataset canvas_daily_stats"));
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> List.of(),
                BiDatasourceHealthProvider.empty(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                BiQueryGovernancePolicy.defaults(),
                null,
                cachePolicyService);

        StepVerifier.create(controller.invalidateQueryCache(command))
                .assertNext(response -> {
                    assertThat(response.getData().scope()).isEqualTo("DATASET");
                    assertThat(response.getData().deletedEntries()).isEqualTo(3);
                    assertThat(response.getData().message()).contains("canvas_daily_stats");
                })
                .verifyComplete();
    }

    @Test
    void readsQueryCacheStatsForTenantAdmin() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQueryCachePolicyService cachePolicyService = mock(BiQueryCachePolicyService.class);
        when(cachePolicyService.cacheStats()).thenReturn(new BiQueryCacheStats(
                "memory",
                true,
                2,
                500,
                300,
                8,
                2,
                5,
                1));
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> List.of(),
                BiDatasourceHealthProvider.empty(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                BiQueryGovernancePolicy.defaults(),
                null,
                cachePolicyService);

        StepVerifier.create(controller.queryCacheStats())
                .assertNext(response -> {
                    assertThat(response.getData().provider()).isEqualTo("memory");
                    assertThat(response.getData().entryCount()).isEqualTo(2);
                    assertThat(response.getData().hitCount()).isEqualTo(8);
                    assertThat(response.getData().evictionCount()).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    void queryGovernanceSummaryUsesConfiguredTimeoutAndQuotaPolicy() {
        BiQueryHistoryReader reader = (tenantId, limit) -> List.of(
                new BiQueryHistoryItem(
                        1L,
                        "canvas_daily_stats",
                        "alice",
                        10,
                        16_000L,
                        "SUCCESS",
                        "slow",
                        null,
                        LocalDateTime.parse("2026-06-05T02:00:00")),
                new BiQueryHistoryItem(
                        2L,
                        "node_daily_stats",
                        "bob",
                        8,
                        5_000L,
                        "CACHE_HIT",
                        "cache",
                        null,
                        LocalDateTime.parse("2026-06-05T02:02:00")));
        BiQueryGovernancePolicy policy = new BiQueryGovernancePolicy(
                12_000L,
                500_000,
                Map.of(
                        "canvas_daily_stats", new BiQueryGovernancePolicy.DatasetPolicy(15_000L, 120_000),
                        "node_daily_stats", new BiQueryGovernancePolicy.DatasetPolicy(7_500L, 80_000)));

        BiQueryGovernanceSummary summary = reader.governanceSummary(7L, 100, policy);

        assertThat(summary.timeoutPolicyMs()).isEqualTo(12_000L);
        assertThat(summary.datasetQuotaRows()).isEqualTo(500_000);
        assertThat(summary.slowQueries()).isEqualTo(1);
        assertThat(summary.datasets()).extracting(BiQueryGovernanceSummary.DatasetQueryStats::datasetKey)
                .containsExactly("canvas_daily_stats", "node_daily_stats");
        assertThat(summary.datasets().get(0).timeoutPolicyMs()).isEqualTo(15_000L);
        assertThat(summary.datasets().get(0).quotaRows()).isEqualTo(120_000);
        assertThat(summary.datasets().get(1).timeoutPolicyMs()).isEqualTo(7_500L);
        assertThat(summary.datasets().get(1).quotaRows()).isEqualTo(80_000);
    }

    @Test
    void queryGovernanceSummaryAttributesSlowQueriesByDatasetPolicyBreach() {
        BiQueryHistoryReader reader = (tenantId, limit) -> List.of(
                new BiQueryHistoryItem(
                        1L,
                        "canvas_daily_stats",
                        "alice",
                        10,
                        18_000L,
                        "SUCCESS",
                        "slow-canvas",
                        null,
                        LocalDateTime.parse("2026-06-05T02:00:00")),
                new BiQueryHistoryItem(
                        2L,
                        "node_daily_stats",
                        "bob",
                        8,
                        9_000L,
                        "SUCCESS",
                        "slow-node",
                        null,
                        LocalDateTime.parse("2026-06-05T02:02:00")),
                new BiQueryHistoryItem(
                        3L,
                        "node_daily_stats",
                        "bob",
                        8,
                        3_000L,
                        "SUCCESS",
                        "fast-node",
                        null,
                        LocalDateTime.parse("2026-06-05T02:04:00")));
        BiQueryGovernancePolicy policy = new BiQueryGovernancePolicy(
                12_000L,
                500_000,
                Map.of(
                        "canvas_daily_stats", new BiQueryGovernancePolicy.DatasetPolicy(15_000L, 120_000),
                        "node_daily_stats", new BiQueryGovernancePolicy.DatasetPolicy(7_500L, 80_000)));

        BiQueryGovernanceSummary summary = reader.governanceSummary(7L, 100, policy);

        assertThat(summary.slowAttributions())
                .extracting(BiQueryGovernanceSummary.SlowQueryAttribution::datasetKey)
                .containsExactly("canvas_daily_stats", "node_daily_stats");
        assertThat(summary.slowAttributions().get(0).slowQueries()).isEqualTo(1);
        assertThat(summary.slowAttributions().get(0).maxDurationMs()).isEqualTo(18_000L);
        assertThat(summary.slowAttributions().get(0).maxOverPolicyMs()).isEqualTo(3_000L);
        assertThat(summary.slowAttributions().get(1).timeoutPolicyMs()).isEqualTo(7_500L);
        assertThat(summary.slowAttributions().get(1).maxOverPolicyMs()).isEqualTo(1_500L);
    }

    @Test
    void explainsQueryForCurrentTenantWithoutExecutingRows() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        AtomicReference<String> explainedSql = new AtomicReference<>();
        AtomicReference<List<Object>> explainedParameters = new AtomicReference<>();
        BiQueryExecutionService queryExecutionService = new BiQueryExecutionService(
                new BiQueryCompiler(),
                new org.chovy.canvas.domain.bi.query.BiQueryExecutor() {
                    @Override
                    public List<Map<String, Object>> execute(org.chovy.canvas.domain.bi.query.BiCompiledQuery query,
                                                             org.chovy.canvas.domain.bi.query.BiDatasetSpec dataset) {
                        throw new AssertionError("explain must not execute the data query");
                    }

                    @Override
                    public List<String> explain(org.chovy.canvas.domain.bi.query.BiCompiledQuery query,
                                                org.chovy.canvas.domain.bi.query.BiDatasetSpec dataset) {
                        explainedSql.set(query.sql());
                        explainedParameters.set(query.parameters());
                        return List.of("SCAN canvas_daily_stats tenant=7", "LIMIT 100");
                    }
                },
                BiQueryHistoryRecorder.noop(),
                EMBED_CLOCK);
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                queryExecutionService);
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100);

        StepVerifier.create(controller.explain(request))
                .assertNext(response -> {
                    assertThat(response.getData().datasetKey()).isEqualTo("canvas_daily_stats");
                    assertThat(response.getData().steps()).containsExactly(
                            "SCAN canvas_daily_stats tenant=7",
                            "LIMIT 100");
                    assertThat(response.getData().sqlHash()).isNotBlank();
                    assertThat(explainedSql.get()).contains("tenant_id = ?");
                    assertThat(explainedParameters.get()).containsExactly(7L);
                })
                .verifyComplete();
    }

    @Test
    void cancelsQueryBySqlHash() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        when(queryExecutionService.cancel(any(String.class), any(BiQueryContext.class)))
                .thenReturn(new BiQueryCancellationResult(
                        "abcdef123456",
                        true,
                        "cancellation requested"));
        BiQueryController controller = new BiQueryController(
                resolver,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                queryExecutionService);

        StepVerifier.create(controller.cancelQuery("abcdef123456"))
                .assertNext(response -> {
                    assertThat(response.getData().sqlHash()).isEqualTo("abcdef123456");
                    assertThat(response.getData().cancelled()).isTrue();
                    assertThat(response.getData().message()).isEqualTo("cancellation requested");
                })
                .verifyComplete();
        verify(queryExecutionService).cancel(
                "abcdef123456",
                new BiQueryContext(7L, "alice", "TENANT_ADMIN"));
    }

    @Test
    void exposesDatasourceHealth() {
        BiQueryController controller = new BiQueryController(
                null,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> List.of(),
                () -> List.of(
                        new BiDatasourceHealth("primary", "MYSQL", true, "available"),
                        new BiDatasourceHealth("doris", "DORIS", false, "disabled")));

        StepVerifier.create(controller.datasourceHealth())
                .assertNext(response -> assertThat(response.getData())
                        .extracting(BiDatasourceHealth::sourceKey)
                        .containsExactly("primary", "doris"))
                .verifyComplete();
    }

    @Test
    void exposesDatasourceHealthHistoryWithLimit() {
        BiQueryController controller = new BiQueryController(
                null,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> List.of(),
                new org.chovy.canvas.domain.bi.query.BiDatasourceHealthProvider() {
                    @Override
                    public List<BiDatasourceHealth> health() {
                        return List.of();
                    }

                    @Override
                    public List<BiDatasourceHealthSnapshot> healthHistory(int limit) {
                        return List.of(
                                new BiDatasourceHealthSnapshot("doris", "DORIS", false, "timeout",
                                        LocalDateTime.parse("2026-06-05T08:10:00")),
                                new BiDatasourceHealthSnapshot("primary", "MYSQL", true, "available",
                                        LocalDateTime.parse("2026-06-05T08:09:00"))
                        ).subList(0, Math.min(limit, 2));
                    }
                });

        StepVerifier.create(controller.datasourceHealthHistory(1))
                .assertNext(response -> assertThat(response.getData())
                        .extracting(BiDatasourceHealthSnapshot::sourceKey)
                        .containsExactly("doris"))
                .verifyComplete();
    }

    @Test
    void exposesDatasourceHealthSloSummary() {
        BiQueryController controller = new BiQueryController(
                null,
                new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK),
                BiQueryExecutionService.testService(),
                (tenantId, limit) -> List.of(),
                new org.chovy.canvas.domain.bi.query.BiDatasourceHealthProvider() {
                    @Override
                    public List<BiDatasourceHealth> health() {
                        return List.of();
                    }

                    @Override
                    public List<BiDatasourceHealthSnapshot> healthHistory(int limit) {
                        return List.of(
                                new BiDatasourceHealthSnapshot("doris", "DORIS", false, "timeout",
                                        LocalDateTime.parse("2026-06-05T08:10:00")),
                                new BiDatasourceHealthSnapshot("doris", "DORIS", true, "available",
                                        LocalDateTime.parse("2026-06-05T08:09:00")),
                                new BiDatasourceHealthSnapshot("primary", "MYSQL", true, "available",
                                        LocalDateTime.parse("2026-06-05T08:08:00")),
                                new BiDatasourceHealthSnapshot("primary", "MYSQL", true, "available",
                                        LocalDateTime.parse("2026-06-05T08:07:00"))
                        ).subList(0, Math.min(limit, 4));
                    }
                });

        StepVerifier.create(controller.datasourceHealthSlo(4))
                .assertNext(response -> {
                    BiDatasourceHealthSloSummary summary = response.getData();
                    assertThat(summary.totalChecks()).isEqualTo(4);
                    assertThat(summary.availableChecks()).isEqualTo(3);
                    assertThat(summary.unavailableChecks()).isEqualTo(1);
                    assertThat(summary.availabilityRate()).isEqualTo(75.0);
                    assertThat(summary.sources()).extracting(BiDatasourceHealthSloSummary.SourceSlo::sourceKey)
                            .containsExactly("doris", "primary");
                    assertThat(summary.sources().get(0).availabilityRate()).isEqualTo(50.0);
                    assertThat(summary.sources().get(0).lastMessage()).isEqualTo("timeout");
                })
                .verifyComplete();
    }

    @Test
    void listsBuiltInMarketingDatasetsWithoutInternalSqlDetails() {
        BiQueryController controller = new BiQueryController();

        StepVerifier.create(controller.listDatasets().map(response -> response.getData()))
                .assertNext(datasets -> {
                    assertThat(datasets).extracting(BiQueryController.DatasetView::datasetKey)
                            .contains("canvas_daily_stats");
                    BiQueryController.DatasetView dataset = datasets.get(0);
                    assertThat(dataset.fields()).extracting(BiQueryController.FieldView::fieldKey)
                            .contains("stat_date", "canvas_name", "trigger_type");
                    assertThat(dataset.metrics()).extracting(BiQueryController.MetricView::metricKey)
                            .contains("total_executions", "success_rate");
                })
                .verifyComplete();
    }

    @Test
    void exposesDashboardPresetsForCanvasEmbedding() {
        BiQueryController controller = new BiQueryController();

        StepVerifier.create(controller.getDashboardPreset("canvas-effect").map(response -> response.getData()))
                .assertNext(preset -> {
                    assertThat(preset.dashboardKey()).isEqualTo("canvas-effect");
                    assertThat(preset.widgets()).extracting(widget -> widget.chartType())
                            .contains("KPI_CARD", "LINE", "BAR", "TABLE");
                    assertThat(preset.embedScopes()).contains("INTERNAL_CANVAS", "EXTERNAL_TICKET");
                })
                .verifyComplete();
    }

    @Test
    void createsEmbedTicketForCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        BiQueryController controller = new BiQueryController(resolver, embedTicketService);
        BiEmbedTicketRequest request = new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "INTERNAL_CANVAS",
                Map.of("canvasId", "12"),
                300
        );

        StepVerifier.create(controller.createEmbedTicket(request))
                .assertNext(response -> {
                    assertThat(response.getData().embedUrl()).startsWith("/bi/embed/DASHBOARD/canvas-effect?ticket=");
                    assertThat(embedTicketService.verify(response.getData().ticket()).tenantId()).isEqualTo(7L);
                    assertThat(embedTicketService.verify(response.getData().ticket()).filters()).containsEntry("canvasId", "12");
                })
                .verifyComplete();
    }

    @Test
    void verifiesEmbedTicketForAnonymousRender() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        BiQueryController controller = new BiQueryController(null, embedTicketService);
        String ticket = embedTicketService.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of("canvasId", "12"),
                300,
                List.of("reports.example.com")
        )).ticket();

        StepVerifier.create(controller.verifyEmbedTicket(
                        new BiEmbedTicketVerifyRequest(ticket),
                        "https://reports.example.com",
                        null))
                .assertNext(response -> {
                    assertThat(response.getData().resourceType()).isEqualTo("DASHBOARD");
                    assertThat(response.getData().resourceKey()).isEqualTo("canvas-effect");
                    assertThat(response.getData().scope()).isEqualTo("EXTERNAL_TICKET");
                    assertThat(response.getData().filters()).containsEntry("canvasId", "12");
                })
                .verifyComplete();
        StepVerifier.create(controller.verifyEmbedTicket(
                        new BiEmbedTicketVerifyRequest(ticket),
                        "https://reports.example.com",
                        null))
                .expectError(SecurityException.class)
                .verify();
    }

    @Test
    void executesEmbedQueryWithSignedTenantUserAndResourceScope() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of("canvasId", "12"),
                300,
                List.of("reports.example.com")
        )).ticket();
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        when(queryExecutionService.execute(any(BiQueryRequest.class), any(BiQueryContext.class)))
                .thenReturn(new BiQueryResult(
                        "canvas_daily_stats",
                        List.of(new BiQueryColumn("total_executions", "MEASURE", "NUMBER")),
                        List.of(Map.of("total_executions", 42L)),
                        1,
                        12L,
                        "hash-embed"));
        BiQueryController controller = new BiQueryController(null, embedTicketService, queryExecutionService);
        BiQueryRequest query = new BiQueryRequest(
                "canvas_daily_stats",
                "canvas-effect",
                List.of(),
                List.of("total_executions"),
                List.of(new BiFilter("canvas_id", BiFilter.Operator.EQ, 12L)),
                List.of(),
                100
        );

        StepVerifier.create(controller.executeEmbedQuery(
                        new BiQueryController.EmbedQueryRequest(
                                ticket,
                                "DASHBOARD",
                                "canvas-effect",
                                "kpi-total",
                                query),
                        "https://reports.example.com",
                        null))
                .assertNext(response -> {
                    assertThat(response.getData().datasetKey()).isEqualTo("canvas_daily_stats");
                    assertThat(response.getData().rows()).singleElement()
                            .satisfies(row -> assertThat(row).containsEntry("total_executions", 42L));
                })
                .verifyComplete();

        ArgumentCaptor<BiQueryContext> context = ArgumentCaptor.forClass(BiQueryContext.class);
        verify(queryExecutionService).execute(any(BiQueryRequest.class), context.capture());
        assertThat(context.getValue().tenantId()).isEqualTo(7L);
        assertThat(context.getValue().username()).isEqualTo("external-viewer");
        assertThat(context.getValue().role()).isEqualTo(RoleNames.OPERATOR);
    }

    @Test
    void rejectsEmbedQueryWhenTicketDoesNotMatchRequestedResource() {
        BiEmbedTicketService embedTicketService = new BiEmbedTicketService(EMBED_SECRET, EMBED_CLOCK);
        String ticket = embedTicketService.createTicket(7L, "external-viewer", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of("canvasId", "12"),
                300,
                List.of("reports.example.com")
        )).ticket();
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiQueryController controller = new BiQueryController(null, embedTicketService, queryExecutionService);
        BiQueryRequest query = new BiQueryRequest(
                "canvas_daily_stats",
                "other-dashboard",
                List.of(),
                List.of("total_executions"),
                List.of(),
                List.of(),
                100
        );

        StepVerifier.create(controller.executeEmbedQuery(
                        new BiQueryController.EmbedQueryRequest(
                                ticket,
                                "DASHBOARD",
                                "other-dashboard",
                                "kpi-total",
                                query),
                        "https://reports.example.com",
                        null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("does not match"))
                .verify();

        verify(queryExecutionService, never()).execute(any(BiQueryRequest.class), any(BiQueryContext.class));
    }

    @Test
    void cleanupEmbedTicketsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiEmbedTicketService embedTicketService = mock(BiEmbedTicketService.class);
        when(embedTicketService.cleanupExpiredTokens(7L, 50))
                .thenReturn(new BiEmbedTokenCleanupResult(3, 2, 1));
        BiQueryController controller = new BiQueryController(resolver, embedTicketService);

        StepVerifier.create(controller.cleanupEmbedTickets(50))
                .assertNext(response -> {
                    assertThat(response.getData().checked()).isEqualTo(3);
                    assertThat(response.getData().revoked()).isEqualTo(2);
                    assertThat(response.getData().failed()).isEqualTo(1);
                })
                .verifyComplete();

        verify(embedTicketService).cleanupExpiredTokens(7L, 50);
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
        row.setValueType("DATE");
        row.setSemanticType("DATE");
        row.setPiiLevel("NORMAL");
        row.setAccessPolicy(accessPolicy);
        row.setMinRole(minRole);
        row.setAllowedUsages(allowedUsages);
        row.setLifecycleStatus("ACTIVE");
        return row;
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
                "BLOCK_ON_WARN",
                availability(status, from, to),
                List.of(),
                "consumer availability " + status);
    }
}
