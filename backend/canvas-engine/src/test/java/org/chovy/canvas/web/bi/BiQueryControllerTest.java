package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldAccessAuditDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldPolicyDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseFieldAccessAuditMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseFieldPolicyMapper;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketRequest;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketService;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketVerifyRequest;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealth;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiQueryCompiler;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryItem;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryRecorder;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiSort;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
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
                300
        )).ticket();

        StepVerifier.create(controller.verifyEmbedTicket(new BiEmbedTicketVerifyRequest(ticket)))
                .assertNext(response -> {
                    assertThat(response.getData().resourceType()).isEqualTo("DASHBOARD");
                    assertThat(response.getData().resourceKey()).isEqualTo("canvas-effect");
                    assertThat(response.getData().scope()).isEqualTo("EXTERNAL_TICKET");
                    assertThat(response.getData().filters()).containsEntry("canvasId", "12");
                })
                .verifyComplete();
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
