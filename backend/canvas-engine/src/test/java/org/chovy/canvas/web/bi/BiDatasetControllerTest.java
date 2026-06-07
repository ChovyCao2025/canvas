package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchedulerResult;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchedulerService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractCapacitySummaryView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractCleanupResultView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractRefreshRunView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFieldResource;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetVersionView;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetImpactView;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetLineageView;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewCommand;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewResult;
import org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewService;
import org.chovy.canvas.domain.bi.dataset.BiMetricResource;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDatasetControllerTest {

    @Test
    void saveDraftUsesCurrentTenantUserAndLockToken() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        BiDatasetResource request = dataset("DRAFT");
        when(service.saveDraft(7L, "alice", "TENANT_ADMIN", request, "lock-token-1")).thenReturn(request);
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.saveDraft("channel_performance_daily", "lock-token-1", request))
                .assertNext(response -> {
                    assertThat(response.getData().datasetKey()).isEqualTo("channel_performance_daily");
                    assertThat(response.getData().status()).isEqualTo("DRAFT");
                })
                .verifyComplete();

        verify(service).saveDraft(7L, "alice", "TENANT_ADMIN", request, "lock-token-1");
    }

    @Test
    void publishUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        when(service.publish(7L, "alice", "TENANT_ADMIN", "channel_performance_daily")).thenReturn(dataset("PUBLISHED"));
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.publish("channel_performance_daily"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PUBLISHED"))
                .verifyComplete();

        verify(service).publish(7L, "alice", "TENANT_ADMIN", "channel_performance_daily");
    }

    @Test
    void listReturnsDatasetResources() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        when(service.listResources(7L)).thenReturn(List.of(dataset("PUBLISHED")));
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.list())
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.datasetType()).isEqualTo("TABLE")))
                .verifyComplete();
    }

    @Test
    void archiveUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        when(service.archive(7L, "channel_performance_daily")).thenReturn(dataset("ARCHIVED"));
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.archive("channel_performance_daily"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ARCHIVED"))
                .verifyComplete();

        verify(service).archive(7L, "channel_performance_daily");
    }

    @Test
    void listVersionsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        BiDatasetVersionView version = new BiDatasetVersionView(
                51L,
                "channel_performance_daily",
                2,
                "PUBLISHED",
                dataset("PUBLISHED"),
                "alice",
                null);
        when(service.listVersions(7L, "channel_performance_daily", 5)).thenReturn(List.of(version));
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.listVersions("channel_performance_daily", 5))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.version()).isEqualTo(2)))
                .verifyComplete();

        verify(service).listVersions(7L, "channel_performance_daily", 5);
    }

    @Test
    void restoreVersionUsesCurrentTenantUserAndLockToken() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService service = mock(BiDatasetResourceService.class);
        when(service.restoreVersion(
                7L, "alice", "TENANT_ADMIN", "channel_performance_daily", 2, "lock-token-1"))
                .thenReturn(dataset("DRAFT"));
        BiDatasetController controller = new BiDatasetController(resolver, service);

        StepVerifier.create(controller.restoreVersion("channel_performance_daily", "lock-token-1", 2))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRAFT"))
                .verifyComplete();

        verify(service).restoreVersion(
                7L, "alice", "TENANT_ADMIN", "channel_performance_daily", 2, "lock-token-1");
    }

    @Test
    void createsDatasetFromDatasourceSchemaForCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService resourceService = mock(BiDatasetResourceService.class);
        BiDatasetFromDatasourceService datasourceService = mock(BiDatasetFromDatasourceService.class);
        BiDatasetFromDatasourceCommand command = new BiDatasetFromDatasourceCommand(
                11L,
                "campaign_daily",
                "campaign_daily",
                "Campaign Daily",
                "tenant_id",
                List.of("tenant_id", "stat_date", "total_cost"));
        when(datasourceService.createTableDataset(7L, "alice", "TENANT_ADMIN", command))
                .thenReturn(dataset("DRAFT"));
        BiDatasetController controller = new BiDatasetController(resolver, resourceService, datasourceService);

        StepVerifier.create(controller.createFromDatasourceSchema(command))
                .assertNext(response -> {
                    assertThat(response.getData().datasetKey()).isEqualTo("channel_performance_daily");
                    assertThat(response.getData().status()).isEqualTo("DRAFT");
                })
                .verifyComplete();

        verify(datasourceService).createTableDataset(7L, "alice", "TENANT_ADMIN", command);
    }

    @Test
    void upsertsAccelerationPolicyForCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService resourceService = mock(BiDatasetResourceService.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        BiDatasetAccelerationPolicyCommand command = new BiDatasetAccelerationPolicyCommand(
                true,
                "EXTRACT",
                "SCHEDULED",
                30L,
                900L,
                500_000L,
                "0 0/30 * * * ?");
        when(accelerationService.upsertPolicy(7L, "canvas_daily_stats", command, "alice"))
                .thenReturn(accelerationPolicy("canvas_daily_stats", "EXTRACT"));
        BiDatasetController controller = new BiDatasetController(
                resolver,
                resourceService,
                null,
                accelerationService);

        StepVerifier.create(controller.upsertAccelerationPolicy("canvas_daily_stats", command))
                .assertNext(response -> {
                    assertThat(response.getData().datasetKey()).isEqualTo("canvas_daily_stats");
                    assertThat(response.getData().accelerationMode()).isEqualTo("EXTRACT");
                })
                .verifyComplete();

        verify(accelerationService).upsertPolicy(7L, "canvas_daily_stats", command, "alice");
    }

    @Test
    void refreshesExtractDatasetForCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService resourceService = mock(BiDatasetResourceService.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        BiDatasetExtractRefreshRunView run = new BiDatasetExtractRefreshRunView(
                31L,
                "canvas_daily_stats",
                "SUCCESS",
                42_000L,
                137L,
                "bi_extract.t7_canvas_daily_stats_20260606101530",
                "alice",
                LocalDateTime.of(2026, 6, 6, 10, 15, 30),
                LocalDateTime.of(2026, 6, 6, 10, 15, 30),
                null);
        when(accelerationService.refreshNow(7L, "canvas_daily_stats", "alice")).thenReturn(run);
        BiDatasetController controller = new BiDatasetController(
                resolver,
                resourceService,
                null,
                accelerationService);

        StepVerifier.create(controller.refreshAcceleration("canvas_daily_stats"))
                .assertNext(response -> {
                    assertThat(response.getData().status()).isEqualTo("SUCCESS");
                    assertThat(response.getData().materializedTable()).startsWith("bi_extract.");
                })
                .verifyComplete();

        verify(accelerationService).refreshNow(7L, "canvas_daily_stats", "alice");
    }

    @Test
    void runsAccelerationSchedulerForCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService resourceService = mock(BiDatasetResourceService.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        BiDatasetAccelerationSchedulerService schedulerService = mock(BiDatasetAccelerationSchedulerService.class);
        when(schedulerService.runDueOnce(any(), any(), any()))
                .thenReturn(new BiDatasetAccelerationSchedulerResult(4, 2, 1, 1));
        BiDatasetController controller = new BiDatasetController(
                resolver,
                resourceService,
                null,
                accelerationService,
                schedulerService);

        StepVerifier.create(controller.runAccelerationScheduler())
                .assertNext(response -> {
                    assertThat(response.getData().policiesChecked()).isEqualTo(4);
                    assertThat(response.getData().refreshed()).isEqualTo(2);
                    assertThat(response.getData().skipped()).isEqualTo(1);
                    assertThat(response.getData().failed()).isEqualTo(1);
                })
                .verifyComplete();

        verify(schedulerService).runDueOnce(eq(7L), eq("alice"), any(LocalDateTime.class));
    }

    @Test
    void previewsSqlDatasetForCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService resourceService = mock(BiDatasetResourceService.class);
        BiSqlDatasetPreviewService previewService = mock(BiSqlDatasetPreviewService.class);
        BiSqlDatasetPreviewCommand command = new BiSqlDatasetPreviewCommand(dataset("DRAFT"), Map.of(), 20, true);
        BiSqlDatasetPreviewResult result = new BiSqlDatasetPreviewResult(
                "campaign_sql",
                "SELECT tenant_id, stat_date FROM campaign_daily",
                "SELECT stat_date AS stat_date FROM (SELECT tenant_id, stat_date FROM campaign_daily) sql_dataset WHERE tenant_id = ? LIMIT 20",
                1,
                List.of(new BiQueryColumn("stat_date", "DIMENSION", "DATE")),
                List.of(Map.of("stat_date", "2026-06-01")),
                1,
                20,
                true,
                null,
                new BiSqlDatasetLineageView(7L, List.of("campaign_daily"), List.of(), "tenant_id",
                        List.of("stat_date"), List.of(), true),
                new BiSqlDatasetImpactView(List.of("DATASET_DRAFT"), List.of("READ_ONLY_SQL_LINT"), List.of()));
        when(previewService.preview(7L, command)).thenReturn(result);
        BiDatasetController controller = new BiDatasetController(
                resolver,
                resourceService,
                null,
                null,
                null,
                previewService);

        StepVerifier.create(controller.previewSqlDataset(command))
                .assertNext(response -> {
                    assertThat(response.getData().datasetKey()).isEqualTo("campaign_sql");
                    assertThat(response.getData().sampleExecuted()).isTrue();
                    assertThat(response.getData().lineage().sourceTables()).containsExactly("campaign_daily");
                })
                .verifyComplete();

        verify(previewService).preview(7L, command);
    }

    @Test
    void returnsExtractCapacityForCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService resourceService = mock(BiDatasetResourceService.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        BiDatasetExtractCapacitySummaryView summary = new BiDatasetExtractCapacitySummaryView(
                "canvas_daily_stats",
                true,
                "EXTRACT",
                "SCHEDULED",
                "bi_extract.t7_canvas_daily_stats_20260606101530",
                "SUCCESS",
                LocalDateTime.of(2026, 6, 6, 10, 15, 30),
                2,
                3,
                1,
                2,
                1,
                0,
                83_500L,
                42_000L,
                137L);
        when(accelerationService.capacitySummary(7L, "canvas_daily_stats", 50)).thenReturn(summary);
        BiDatasetController controller = new BiDatasetController(
                resolver,
                resourceService,
                null,
                accelerationService);

        StepVerifier.create(controller.accelerationCapacity("canvas_daily_stats", 50))
                .assertNext(response -> {
                    assertThat(response.getData().activeTables()).isEqualTo(2);
                    assertThat(response.getData().retainedRows()).isEqualTo(83_500L);
                })
                .verifyComplete();

        verify(accelerationService).capacitySummary(7L, "canvas_daily_stats", 50);
    }

    @Test
    void cleansExtractRetentionForCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasetResourceService resourceService = mock(BiDatasetResourceService.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        BiDatasetExtractCleanupResultView result = new BiDatasetExtractCleanupResultView(
                "canvas_daily_stats",
                3,
                2,
                1,
                0);
        when(accelerationService.cleanupRetainedExtracts(7L, "canvas_daily_stats", 2)).thenReturn(result);
        BiDatasetController controller = new BiDatasetController(
                resolver,
                resourceService,
                null,
                accelerationService);

        StepVerifier.create(controller.cleanupAcceleration("canvas_daily_stats", 2))
                .assertNext(response -> {
                    assertThat(response.getData().droppedTables()).isEqualTo(1);
                    assertThat(response.getData().failedDrops()).isZero();
                })
                .verifyComplete();

        verify(accelerationService).cleanupRetainedExtracts(7L, "canvas_daily_stats", 2);
    }

    private BiDatasetResource dataset(String status) {
        return new BiDatasetResource(
                "channel_performance_daily",
                "Channel Performance Daily",
                "TABLE",
                "canvas_dws.channel_performance_daily",
                "tenant_id",
                Map.of("category", "CHANNEL"),
                List.of(new BiDatasetFieldResource("stat_date", "Date", "stat_date", "DIMENSION", "DATE", "DATE", null, null, null, true, "NORMAL", 10)),
                List.of(new BiMetricResource("send_count", "Send Count", "SUM(send_count)", "SUM", "NUMBER", "次", "#,##0", List.of("stat_date"), "alice", "Daily sends", "ACTIVE")),
                status,
                "PERSISTED");
    }

    private BiDatasetAccelerationPolicyView accelerationPolicy(String datasetKey, String mode) {
        return new BiDatasetAccelerationPolicyView(
                datasetKey,
                true,
                mode,
                "SCHEDULED",
                30L,
                900L,
                500_000L,
                "0 0/30 * * * ?",
                "bi_extract.t7_canvas_daily_stats_20260606101530",
                "SUCCESS",
                31L,
                LocalDateTime.of(2026, 6, 6, 10, 15, 30),
                List.of());
    }
}
