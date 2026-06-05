package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.export.BiExportCleanupResult;
import org.chovy.canvas.domain.bi.export.BiExportApprovalReviewCommand;
import org.chovy.canvas.domain.bi.export.BiExportDownload;
import org.chovy.canvas.domain.bi.export.BiExportJobCommand;
import org.chovy.canvas.domain.bi.export.BiExportJobView;
import org.chovy.canvas.domain.bi.export.BiExportQueueResult;
import org.chovy.canvas.domain.bi.export.BiExportRetryResult;
import org.chovy.canvas.domain.bi.export.BiSelfServiceExportService;
import org.chovy.canvas.domain.bi.export.BiSelfServicePreviewRequest;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiSelfServiceControllerTest {

    @Test
    void previewUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiSelfServiceExportService service = mock(BiSelfServiceExportService.class);
        BiSelfServicePreviewRequest request = new BiSelfServicePreviewRequest(query(), 100);
        when(service.preview(7L, "alice", RoleNames.OPERATOR, request)).thenReturn(result());
        BiSelfServiceController controller = new BiSelfServiceController(resolver, service);

        StepVerifier.create(controller.preview(request))
                .assertNext(response -> assertThat(response.getData().rowCount()).isEqualTo(1))
                .verifyComplete();

        verify(service).preview(7L, "alice", RoleNames.OPERATOR, request);
    }

    @Test
    void createExportUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiSelfServiceExportService service = mock(BiSelfServiceExportService.class);
        BiExportJobCommand command = new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "CSV",
                query(),
                100,
                false,
                false,
                null);
        when(service.createExport(7L, "alice", RoleNames.OPERATOR, command)).thenReturn(queuedExportView());
        BiSelfServiceController controller = new BiSelfServiceController(resolver, service);

        StepVerifier.create(controller.createExport(command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("QUEUED"))
                .verifyComplete();
    }

    @Test
    void reviewExportUsesCurrentTenantUserAndRole() {
        TenantContextResolver resolver = resolver();
        BiSelfServiceExportService service = mock(BiSelfServiceExportService.class);
        BiExportApprovalReviewCommand command = new BiExportApprovalReviewCommand("APPROVED", "ok");
        when(service.reviewExport(7L, "alice", RoleNames.OPERATOR, 55L, command)).thenReturn(exportView());
        BiSelfServiceController controller = new BiSelfServiceController(resolver, service);

        StepVerifier.create(controller.reviewExport(55L, command))
                .assertNext(response -> assertThat(response.getData().id()).isEqualTo(55L))
                .verifyComplete();

        verify(service).reviewExport(7L, "alice", RoleNames.OPERATOR, 55L, command);
    }

    @Test
    void downloadReturnsAttachment() {
        TenantContextResolver resolver = resolver();
        BiSelfServiceExportService service = mock(BiSelfServiceExportService.class);
        when(service.download(7L, 55L)).thenReturn(new BiExportDownload("export-55.csv", "text/csv", "a,b\n".getBytes()));
        BiSelfServiceController controller = new BiSelfServiceController(resolver, service);

        StepVerifier.create(controller.download(55L))
                .assertNext(response -> {
                    assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("export-55.csv");
                    assertThat(response.getBody()).containsExactly("a,b\n".getBytes());
                })
                .verifyComplete();
    }

    @Test
    void cleanupExportsUsesCurrentTenant() {
        TenantContextResolver resolver = resolver();
        BiSelfServiceExportService service = mock(BiSelfServiceExportService.class);
        when(service.cleanupExpiredExports(7L, 50)).thenReturn(new BiExportCleanupResult(3, 2, 2, 1));
        BiSelfServiceController controller = new BiSelfServiceController(resolver, service);

        StepVerifier.create(controller.cleanupExports(50))
                .assertNext(response -> {
                    assertThat(response.getData().checked()).isEqualTo(3);
                    assertThat(response.getData().expired()).isEqualTo(2);
                    assertThat(response.getData().filesDeleted()).isEqualTo(2);
                    assertThat(response.getData().failed()).isEqualTo(1);
                })
                .verifyComplete();

        verify(service).cleanupExpiredExports(7L, 50);
    }

    @Test
    void retryExportsUsesCurrentTenantUserAndRole() {
        TenantContextResolver resolver = resolver();
        BiSelfServiceExportService service = mock(BiSelfServiceExportService.class);
        when(service.retryFailedExports(7L, "alice", RoleNames.OPERATOR, 10))
                .thenReturn(new BiExportRetryResult(1, 1, 1, 0, List.of(exportView())));
        BiSelfServiceController controller = new BiSelfServiceController(resolver, service);

        StepVerifier.create(controller.retryExports(10))
                .assertNext(response -> {
                    assertThat(response.getData().checked()).isEqualTo(1);
                    assertThat(response.getData().completed()).isEqualTo(1);
                    assertThat(response.getData().jobs()).singleElement().satisfies(job ->
                            assertThat(job.id()).isEqualTo(55L));
                })
                .verifyComplete();

        verify(service).retryFailedExports(7L, "alice", RoleNames.OPERATOR, 10);
    }

    @Test
    void runExportQueueUsesCurrentTenantUserAndRole() {
        TenantContextResolver resolver = resolver();
        BiSelfServiceExportService service = mock(BiSelfServiceExportService.class);
        when(service.processQueuedExports(7L, "alice", RoleNames.OPERATOR, 10))
                .thenReturn(new BiExportQueueResult(1, 1, 1, 0, List.of(exportView())));
        BiSelfServiceController controller = new BiSelfServiceController(resolver, service);

        StepVerifier.create(controller.runExportQueue(10))
                .assertNext(response -> {
                    assertThat(response.getData().checked()).isEqualTo(1);
                    assertThat(response.getData().completed()).isEqualTo(1);
                    assertThat(response.getData().jobs()).singleElement().satisfies(job ->
                            assertThat(job.id()).isEqualTo(55L));
                })
                .verifyComplete();

        verify(service).processQueuedExports(7L, "alice", RoleNames.OPERATOR, 10);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "alice")));
        return resolver;
    }

    private BiQueryRequest query() {
        return new BiQueryRequest("canvas_daily_stats", List.of("stat_date"), List.of("total_executions"), List.of(), List.of(), 100);
    }

    private BiQueryResult result() {
        return new BiQueryResult(
                "canvas_daily_stats",
                List.of(new BiQueryColumn("total_executions", "METRIC", "NUMBER")),
                List.of(Map.of("total_executions", 42L)),
                1,
                12L,
                "abcdef");
    }

    private BiExportJobView exportView() {
        return new BiExportJobView(
                55L,
                7L,
                3L,
                "DATASET",
                "canvas_daily_stats",
                11L,
                "CSV",
                100,
                "COMPLETED",
                100,
                "/canvas/bi/self-service/exports/55/download",
                null,
                null,
                7,
                LocalDateTime.parse("2026-06-12T05:10:00"),
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                3,
                null,
                null,
                null,
                "alice",
                LocalDateTime.parse("2026-06-05T05:10:00"),
                LocalDateTime.parse("2026-06-05T05:10:00"));
    }

    private BiExportJobView queuedExportView() {
        return new BiExportJobView(
                55L,
                7L,
                3L,
                "DATASET",
                "canvas_daily_stats",
                11L,
                "CSV",
                100,
                "QUEUED",
                0,
                null,
                null,
                null,
                7,
                LocalDateTime.parse("2026-06-12T05:10:00"),
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                3,
                null,
                null,
                null,
                "alice",
                LocalDateTime.parse("2026-06-05T05:10:00"),
                LocalDateTime.parse("2026-06-05T05:10:00"));
    }
}
